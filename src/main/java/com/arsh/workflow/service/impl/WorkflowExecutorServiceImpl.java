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

    // Lock tuning
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);
    private static final long LOCK_WAIT_MS = 2000L;
    private static final long LOCK_RETRY_MS = 100L;

    // 🔹 NEW: how long we keep the "done" flag in Redis
    private static final Duration EXECUTION_TTL = Duration.ofHours(24);

    public WorkflowExecutorServiceImpl(TaskServiceImpl taskService,
                                       TaskRepository taskRepository,
                                       WorkflowRepository workflowRepository,
                                       RedisDistributedLock redisDistributedLock,
                                       @Qualifier("workflowExecutorPool") ExecutorService executorService) {
        this.executorService = executorService;
        this.taskService = taskService;
        this.taskRepository = taskRepository;
        this.workflowRepository = workflowRepository;
        this.redisDistributedLock = redisDistributedLock;
    }

    @Override
    public void executeWorkflow(Long workflowId) {
        // Try to mark workflow RUNNING
        try {
            Optional<Workflow> maybeWf = workflowRepository.findById(workflowId);
            if (maybeWf.isPresent()) {
                Workflow wf = maybeWf.get();
                if (wf.getStatus() != WorkflowStatus.RUNNING) {
                    wf.setStatus(WorkflowStatus.RUNNING);
                    workflowRepository.save(wf);
                }
            }
        } catch (Exception e) {
            log.warn("Unable to mark workflow {} RUNNING: {}", workflowId, e.toString());
        }

        // Only schedule tasks that are READY (DAG logic already handled elsewhere)
        List<Task> readyTasks =
                taskRepository.findByWorkflowIdAndStatus(workflowId, TaskStatus.READY);

        if (readyTasks == null || readyTasks.isEmpty()) {
            log.debug("No READY tasks found for workflow {}", workflowId);
            return;
        }

        for (Task t : readyTasks) {
            Long taskId = t.getId();
            executorService.submit(() -> runTaskWithLock(taskId));
        }
    }

    @Override
    public void runTask(Long taskId) {
        // Public entry goes through lock as well
        runTaskWithLock(taskId);
    }

    /**
     * Core execution gate: Redis lock → idempotency → retries → business logic
     */
    private void runTaskWithLock(Long taskId) {
        String lockKey = "task:lock:" + taskId;
        String doneKey = "task:done:" + taskId;   // 🔹 NEW: idempotency key
        String token = null;

        try {
            // 🔹 1) FAST PATH: if already marked executed in Redis, skip immediately
            if (redisDistributedLock.isAlreadyExecuted(doneKey)) {
                log.info("Task {}: already marked executed in Redis. Skipping.", taskId);
                return;
            }

            // Acquire distributed lock
            token = redisDistributedLock.lockBlocking(
                    lockKey,
                    LOCK_TTL,
                    LOCK_WAIT_MS,
                    LOCK_RETRY_MS
            );

            if (token == null) {
                log.info("Task {}: could not acquire lock (likely running elsewhere). Skipping.", taskId);
                return;
            }

            // 🔹 2) DOUBLE-CHECK after acquiring lock to avoid race between check and lock
            if (redisDistributedLock.isAlreadyExecuted(doneKey)) {
                log.info("Task {}: already executed (post-lock check). Skipping.", taskId);
                return;
            }

            // Inside lock → safe across nodes
            runTaskWithRetries(taskId, doneKey);

        } catch (Exception e) {
            log.error("Task {}: error during execution with lock", taskId, e);
        } finally {
            if (token != null) {
                boolean released = redisDistributedLock.releaseLock(lockKey, token);
                if (!released) {
                    log.warn("Task {}: Redis lock {} not released (expired or stolen).", taskId, lockKey);
                }
            }
        }
    }

    @Override
    public void triggerNextTasks(Long workflowId) {
        executeWorkflow(workflowId);
    }

    // 🔹 UPDATED SIGNATURE: we pass doneKey so we can mark idempotency on success
    private void runTaskWithRetries(Long taskId, String doneKey) {
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                runTaskOnce(taskId, attempt, doneKey);
                return;
            } catch (Exception ex) {
                log.error("Task {} failed on attempt {}/{}: {}", taskId, attempt, maxRetries, ex.toString());
                if (attempt >= maxRetries) {
                    log.error("Task {} reached max retries. Marking FAILED.", taskId);
                    markTaskFailed(taskId, ex);
                    return;
                } else {
                    long backoff = baseBackoffMs * (1L << (attempt - 1));
                    try {
                        TimeUnit.MILLISECONDS.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Backoff sleep interrupted for task {}: {}", taskId, ie.toString());
                    }
                }
            }
        }
    }

    // 🔹 UPDATED SIGNATURE: receives doneKey
    private void runTaskOnce(Long taskId, int attemptNumber, String doneKey) {
        Optional<Task> maybeTask = taskRepository.findById(taskId);
        if (maybeTask.isEmpty()) {
            throw new RuntimeException("Task not found: " + taskId);
        }

        Task task = maybeTask.get();

        // DB-level idempotency check
        if (task.getStatus() == TaskStatus.COMPLETED) {
            log.info("Task {} already COMPLETED in DB, skipping.", taskId);
            return;
        }

        if (task.getStatus() == TaskStatus.IN_PROGRESS) {
            log.info("Task {} already IN_PROGRESS, skipping duplicate run.", taskId);
            return;
        }

        // Mark IN_PROGRESS via TaskService (also updates workflow status)
        taskService.changeStatus(taskId, TaskStatus.IN_PROGRESS);

        try {
            performTaskBusinessLogic(task);

            // Mark COMPLETED and unlock children (DAG logic in TaskServiceImpl)
            taskService.changeStatus(taskId, TaskStatus.COMPLETED);

            // 🔹 3) MARK EXECUTED IN REDIS (idempotency flag)
            redisDistributedLock.markExecuted(doneKey, EXECUTION_TTL);

            // After children potentially moved to READY, schedule them
            Long workflowId = taskService.getWorkflowId(taskId);
            triggerNextTasks(workflowId);

            log.info("Task {} completed successfully (attempt {}).", taskId, attemptNumber);
        } catch (Exception e) {
            log.error("Exception executing task {} on attempt {}: {}", taskId, attemptNumber, e.toString());
            throw new RuntimeException(e);
        }
    }

    private void markTaskFailed(Long taskId, Exception cause) {
        Optional<Task> maybeTask = taskRepository.findById(taskId);
        if (maybeTask.isEmpty()) {
            log.warn("markTaskFailed: task {} not found", taskId);
            return;
        }
        Task task = maybeTask.get();
        try {
            taskService.changeStatus(taskId, TaskStatus.FAILED);
        } catch (Exception e) {
            log.warn("Unable to set task {} status to FAILED via taskService: {}", taskId, e.toString());
            task.setStatus(TaskStatus.FAILED);
            taskRepository.save(task);
        }

        Long workflowId = task.getWorkflow().getId();
        try {
            Optional<Workflow> maybeWf = workflowRepository.findById(workflowId);
            if (maybeWf.isPresent()) {
                Workflow wf = maybeWf.get();
                wf.setStatus(WorkflowStatus.FAILED);
                workflowRepository.save(wf);
            }
        } catch (Exception e) {
            log.warn("Failed to mark workflow {} FAILED after task {} failure: {}", workflowId, taskId, e.toString());
        }

        log.error("Task {} marked FAILED. Cause: {}", taskId, cause.toString());
    }

    private void evaluateWorkflowCompletion(Long workflowId) {
        try {
            List<Task> pendingOrReady = taskRepository.findByWorkflowIdAndStatusIn(
                    workflowId,
                    List.of(TaskStatus.PENDING, TaskStatus.READY)
            );

            if (pendingOrReady == null || pendingOrReady.isEmpty()) {
                Optional<Workflow> maybeWf = workflowRepository.findById(workflowId);
                if (maybeWf.isPresent()) {
                    Workflow wf = maybeWf.get();
                    wf.setStatus(WorkflowStatus.COMPLETED);
                    workflowRepository.save(wf);
                    log.info("Workflow {} marked COMPLETED (no PENDING/READY tasks).", workflowId);
                }
            }
        } catch (Exception e) {
            log.warn("evaluateWorkflowCompletion failed for workflow {}: {}", workflowId, e.toString());
        }
    }

    protected void performTaskBusinessLogic(Task task) throws Exception {
        log.info("Performing business logic for task {}", task.getId());
    }
}
