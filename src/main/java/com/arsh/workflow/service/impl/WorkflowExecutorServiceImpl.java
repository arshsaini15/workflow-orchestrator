package com.arsh.workflow.service.impl;

import com.arsh.workflow.enums.TaskStatus;
import com.arsh.workflow.enums.WorkflowStatus;
import com.arsh.workflow.model.Task;
import com.arsh.workflow.model.Workflow;
import com.arsh.workflow.repository.TaskRepository;
import com.arsh.workflow.repository.WorkflowRepository;
import com.arsh.workflow.service.WorkflowExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

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

    private final int maxRetries = 3;
    private final long baseBackoffMs = 500L;

    public WorkflowExecutorServiceImpl(TaskServiceImpl taskService,
                                       TaskRepository taskRepository,
                                       WorkflowRepository workflowRepository,
                                       @Qualifier("workflowExecutorPool") ExecutorService executorService) {
        this.executorService = executorService;
        this.taskService = taskService;
        this.taskRepository = taskRepository;
        this.workflowRepository = workflowRepository;
    }


    @Override
    public void executeWorkflow(Long workflowId) {
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

        List<Task> readyTasks = taskRepository.findByWorkflowIdAndStatus(workflowId, TaskStatus.READY);

        if (readyTasks == null || readyTasks.isEmpty()) {
            log.debug("No READY tasks found for workflow {}", workflowId);
            return;
        }

        for (Task t : readyTasks) {
            Long taskId = t.getId();
            executorService.submit(() -> runTask(taskId));
        }
    }

    @Override
    public void runTask(Long taskId) {
        runTaskWithRetries(taskId);
    }

    @Override
    public void triggerNextTasks(Long workflowId) {
        List<Task> pendingTasks = taskRepository.findByWorkflowIdAndStatus(workflowId, TaskStatus.PENDING);

        if (pendingTasks == null || pendingTasks.isEmpty()) {
            log.debug("No PENDING tasks found for workflow {}", workflowId);
            evaluateWorkflowCompletion(workflowId);
            return;
        }

        for (Task task : pendingTasks) {
            List<Task> deps = task.getDependsOn();
            boolean allDepsCompleted = true;

            if (deps != null && !deps.isEmpty()) {
                for (Task dep : deps) {
                    Optional<Task> maybeDep = taskRepository.findById(dep.getId());
                    if (maybeDep.isEmpty() || maybeDep.get().getStatus() != TaskStatus.COMPLETED) {
                        allDepsCompleted = false;
                        break;
                    }
                }
            }

            if (allDepsCompleted) {
                try {
                    task.setStatus(TaskStatus.READY);
                    taskRepository.save(task);
                    log.info("Task {} promoted to READY (workflow {})", task.getId(), workflowId);
                    executorService.submit(() -> runTask(task.getId()));
                } catch (Exception e) {
                    log.error("Failed to promote/schedule task {}: {}", task.getId(), e.toString());
                }
            }
        }
        evaluateWorkflowCompletion(workflowId);
    }

    private void runTaskWithRetries(Long taskId) {
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                runTaskOnce(taskId, attempt);
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

    private void runTaskOnce(Long taskId, int attemptNumber) {
        Optional<Task> maybeTask = taskRepository.findById(taskId);
        if (maybeTask.isEmpty()) {
            throw new RuntimeException("Task not found: " + taskId);
        }

        Task task = maybeTask.get();

        if (task.getStatus() == TaskStatus.COMPLETED) {
            log.info("Task {} already COMPLETED, skipping.", taskId);
            return;
        }

        if (task.getStatus() == TaskStatus.IN_PROGRESS) {
            log.info("Task {} already IN_PROGRESS, skipping duplicate run.", taskId);
            return;
        }

        taskService.changeStatus(taskId, TaskStatus.IN_PROGRESS);

        try {
            performTaskBusinessLogic(task);

            taskService.changeStatus(taskId, TaskStatus.COMPLETED);

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
            List<Task> pendingOrReady = taskRepository.findByWorkflowIdAndStatusIn(workflowId,
                    List.of(TaskStatus.PENDING, TaskStatus.READY));

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
