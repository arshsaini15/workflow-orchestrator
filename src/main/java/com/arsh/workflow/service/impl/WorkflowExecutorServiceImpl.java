package com.arsh.workflow.service.impl;

import com.arsh.workflow.enums.TaskStatus;
import com.arsh.workflow.enums.WorkflowStatus;
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
import java.util.List;
import java.util.Optional;
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
            @Qualifier("workflowExecutorPool") ExecutorService executorService
    ) {
        this.executorService = executorService;
        this.taskService = taskService;
        this.taskRepository = taskRepository;
        this.workflowRepository = workflowRepository;
        this.redisDistributedLock = redisDistributedLock;
    }

    @Override
    public void executeWorkflow(Long workflowId) {
        // Mark workflow RUNNING if not already
        try {
            Optional<Workflow> maybe = workflowRepository.findById(workflowId);
            if (maybe.isPresent()) {
                Workflow wf = maybe.get();
                if (wf.getStatus() != WorkflowStatus.RUNNING) {
                    wf.setStatus(WorkflowStatus.RUNNING);
                    workflowRepository.save(wf);
                }
            }
        } catch (Exception e) {
            log.warn("Unable to mark workflow {} RUNNING: {}", workflowId, e.getMessage());
        }

        // Pick READY tasks only
        List<Task> readyTasks = taskRepository.findByWorkflowIdAndStatus(workflowId, TaskStatus.READY);
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
        // Public call still goes through lock
        runTaskWithLock(taskId);
    }

    private void runTaskWithLock(Long taskId) {
        String lockKey = "task:lock:" + taskId;
        String doneKey = "task:done:" + taskId;
        String token = null;

        try {
            // Idempotency pre-check
            if (redisDistributedLock.isAlreadyExecuted(doneKey)) {
                log.info("Task {} already executed (fast path). Skipping.", taskId);
                return;
            }

            // Acquire lock
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

            // Double check for idempotency
            if (redisDistributedLock.isAlreadyExecuted(doneKey)) {
                log.info("Task {} already executed (post-lock). Skipping.", taskId);
                return;
            }

            // Run with retry logic
            runTaskWithRetries(taskId, doneKey);

        } catch (Exception e) {
            log.error("Task {} error during execution: {}", taskId, e.getMessage(), e);
        } finally {
            if (token != null) {
                boolean released = redisDistributedLock.releaseLock(lockKey, token);
                if (!released) {
                    log.warn("Task {} lock not released (expired or stolen).", taskId);
                }
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
                log.error("Task {} attempt {}/{} failed: {}", taskId, attempt, maxRetries, ex.getMessage());

                if (attempt >= maxRetries) {
                    markTaskFailed(taskId, ex);
                    return;
                }

                long backoff = baseBackoffMs * (1L << (attempt - 1));
                try {
                    TimeUnit.MILLISECONDS.sleep(backoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private void runTaskOnce(Long taskId, int attempt, String doneKey) throws Exception {
        Optional<Task> maybe = taskRepository.findById(taskId);
        if (maybe.isEmpty()) {
            throw new RuntimeException("Task not found: " + taskId);
        }

        Task task = maybe.get();

        // DB idempotency
        if (task.getStatus() == TaskStatus.COMPLETED) {
            log.info("Task {} already COMPLETED. Skipping.", taskId);
            return;
        }

        if (task.getStatus() == TaskStatus.IN_PROGRESS) {
            log.info("Task {} already IN_PROGRESS. Skipping duplicate execution.", taskId);
            return;
        }

        // Mark IN_PROGRESS
        taskService.changeStatus(taskId, TaskStatus.IN_PROGRESS);

        try {
            // ---- YOUR BUSINESS LOGIC ----
            performTaskBusinessLogic(task);

            // Mark COMPLETED (also unlocks dependent tasks)
            taskService.changeStatus(taskId, TaskStatus.COMPLETED);

            // Mark idempotency flag
            redisDistributedLock.markExecuted(doneKey, EXECUTION_TTL);

            // Trigger next tasks
            Long wfId = task.getWorkflow().getId();
            triggerNextTasks(wfId);

            log.info("Task {} finished successfully on attempt {}", taskId, attempt);

        } catch (Exception e) {
            log.error("Exception while executing task {} on attempt {}: {}", taskId, attempt, e.getMessage());
            throw e;
        }
    }

    private void markTaskFailed(Long taskId, Exception cause) {
        Optional<Task> maybe = taskRepository.findById(taskId);
        if (maybe.isEmpty()) {
            return;
        }

        Task task = maybe.get();

        try {
            taskService.changeStatus(taskId, TaskStatus.FAILED);
        } catch (Exception ignored) {
            task.setStatus(TaskStatus.FAILED);
            taskRepository.save(task);
        }

        Long wfId = task.getWorkflow().getId();

        Optional<Workflow> wfMaybe = workflowRepository.findById(wfId);
        wfMaybe.ifPresent(wf -> {
            wf.setStatus(WorkflowStatus.FAILED);
            workflowRepository.save(wf);
        });

        log.error("Task {} permanently FAILED. Cause: {}", taskId, cause.getMessage());
    }

    @Override
    public void triggerNextTasks(Long workflowId) {
        executeWorkflow(workflowId);
    }

    protected void performTaskBusinessLogic(Task task) throws Exception {
        // Replace this with actual logic later
        log.info("Executing business logic for task {}", task.getId());
    }
}
