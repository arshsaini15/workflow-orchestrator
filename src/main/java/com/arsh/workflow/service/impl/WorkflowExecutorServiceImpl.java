package com.arsh.workflow.service.impl;

import com.arsh.workflow.enums.EventType;
import com.arsh.workflow.enums.TaskStatus;
import com.arsh.workflow.enums.WorkflowStatus;
import com.arsh.workflow.events.WorkflowEvent;
import com.arsh.workflow.events.producer.WorkflowEventProducer;
import com.arsh.workflow.model.Task;
import com.arsh.workflow.model.Workflow;
import com.arsh.workflow.repository.TaskRepository;
import com.arsh.workflow.repository.WorkflowRepository;
import com.arsh.workflow.service.WorkflowExecutorService;
import com.arsh.workflow.util.RedisDistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class WorkflowExecutorServiceImpl implements WorkflowExecutorService {

    private final ExecutorService executorService;
    private final TaskServiceImpl taskService;
    private final TaskRepository taskRepository;
    private final WorkflowRepository workflowRepository;
    private final RedisDistributedLock redisDistributedLock;
    private final WorkflowEventProducer eventProducer;

    private final int maxRetries = 3;
    private final long baseBackoffMs = 500L;

    // Lock config
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);
    private static final long LOCK_WAIT_MS = 2000L;
    private static final long LOCK_RETRY_MS = 100L;

    // Idempotency TTL
    private static final Duration EXECUTION_TTL = Duration.ofHours(24);

    public WorkflowExecutorServiceImpl(
            TaskServiceImpl taskService,
            TaskRepository taskRepository,
            WorkflowRepository workflowRepository,
            RedisDistributedLock redisDistributedLock,
            WorkflowEventProducer eventProducer,
            @Qualifier("workflowExecutorPool") ExecutorService executorService
    ) {
        this.executorService = executorService;
        this.taskService = taskService;
        this.taskRepository = taskRepository;
        this.workflowRepository = workflowRepository;
        this.redisDistributedLock = redisDistributedLock;
        this.eventProducer = eventProducer;
    }

    @Override
    public void executeWorkflow(Long workflowId) {

        // ---- WORKFLOW START (ONLY READY → RUNNING) ----
        try {
            Optional<Workflow> maybe = workflowRepository.findById(workflowId);
            if (maybe.isPresent()) {
                Workflow wf = maybe.get();

                if (wf.getStatus() == WorkflowStatus.READY) {
                    wf.setStatus(WorkflowStatus.RUNNING);
                    workflowRepository.save(wf);

                    log.info(
                            "WORKFLOW STARTED | workflowId={} | totalTasks={}",
                            wf.getId(),
                            wf.getTasks().size()
                    );

                    emitWorkflowStartedEvent(wf);
                }
            }
        } catch (Exception e) {
            log.warn("Unable to mark workflow {} RUNNING: {}", workflowId, e.getMessage());
        }

        // ---- PICK READY TASKS ----
        List<Task> readyTasks =
                taskRepository.findByWorkflowIdAndStatus(workflowId, TaskStatus.READY);

        if (readyTasks == null || readyTasks.isEmpty()) {
            log.debug("No READY tasks for workflow {}", workflowId);
            return;
        }

        for (Task t : readyTasks) {
            executorService.submit(() -> runTaskWithLock(t.getId()));
        }
    }

    @Override
    public void runTask(Long taskId) {
        runTaskWithLock(taskId);
    }

    private void runTaskWithLock(Long taskId) {

        String lockKey = "task:lock:" + taskId;
        String doneKey = "task:done:" + taskId;
        String token = null;

        try {
            if (redisDistributedLock.isAlreadyExecuted(doneKey)) {
                log.info("Task {} already executed. Skipping.", taskId);
                return;
            }

            token = redisDistributedLock.lockBlocking(
                    lockKey,
                    LOCK_TTL,
                    LOCK_WAIT_MS,
                    LOCK_RETRY_MS
            );

            if (token == null) {
                log.info("Task {} lock unavailable. Skipping.", taskId);
                return;
            }

            if (redisDistributedLock.isAlreadyExecuted(doneKey)) {
                log.info("Task {} already executed after lock. Skipping.", taskId);
                return;
            }

            runTaskWithRetries(taskId, doneKey);

        } catch (Exception e) {
            log.error("Task {} execution error: {}", taskId, e.getMessage(), e);
        } finally {
            if (token != null) {
                redisDistributedLock.releaseLock(lockKey, token);
            }
        }
    }

    private void runTaskWithRetries(Long taskId, String doneKey) {

        int attempt = 0;

        while (true) {
            attempt++;
            try {
                runTaskOnce(taskId, attempt, doneKey);
                return;
            } catch (Exception ex) {

                if (attempt >= maxRetries) {
                    markTaskFailed(taskId, ex);
                    return;
                }

                try {
                    TimeUnit.MILLISECONDS.sleep(baseBackoffMs * (1L << (attempt - 1)));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private void runTaskOnce(Long taskId, int attempt, String doneKey) throws Exception {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        if (task.getStatus() == TaskStatus.COMPLETED ||
                task.getStatus() == TaskStatus.IN_PROGRESS) {
            return;
        }

        taskService.changeStatus(taskId, TaskStatus.IN_PROGRESS);

        try {
            performTaskBusinessLogic(task);

            taskService.changeStatus(taskId, TaskStatus.COMPLETED);

            redisDistributedLock.markExecuted(doneKey, EXECUTION_TTL);

            log.info("Task {} completed on attempt {}", taskId, attempt);

        } catch (Exception e) {
            throw e;
        }
    }

    private void markTaskFailed(Long taskId, Exception cause) {
        try {
            taskService.changeStatus(taskId, TaskStatus.FAILED);
        } catch (Exception e) {
            log.error("Unable to mark task {} FAILED: {}", taskId, e.getMessage());
        }
        log.error("Task {} permanently FAILED. Cause: {}", taskId, cause.getMessage());
    }

    @Override
    public void triggerNextTasks(Long workflowId) {
        executeWorkflow(workflowId);
    }

    protected void performTaskBusinessLogic(Task task) {
        log.info("Executing business logic for task {}", task.getId());
    }

    // ---- TEMPORARY (Step 3 only) ----
    private void emitWorkflowStartedEvent(Workflow workflow) {

        WorkflowEvent event = WorkflowEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(EventType.WORKFLOW_STARTED)
                .workflowId(workflow.getId())
                .status(WorkflowStatus.RUNNING)
                .source("workflow-service")
                .occurredAt(Instant.now())
                .version(1)
                .build();

        eventProducer.publish(workflow.getId(), event);
    }
}
