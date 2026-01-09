package com.arsh.workflow.service.impl;

import com.arsh.workflow.enums.TaskStatus;
import com.arsh.workflow.enums.WorkflowStatus;
import com.arsh.workflow.exception.TaskNotFoundException;
import com.arsh.workflow.model.Task;
import com.arsh.workflow.model.Workflow;
import com.arsh.workflow.repository.TaskRepository;
import com.arsh.workflow.service.WorkflowCoordinator;
import com.arsh.workflow.service.WorkflowExecutorService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowCoordinatorImpl implements WorkflowCoordinator {

    private final TaskRepository taskRepository;
    private final WorkflowExecutorService workflowExecutorService;

    @Override
    @Transactional
    public void onTaskCompleted(Long taskId) {

        Task completedTask = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task not found"));

        Workflow workflow = completedTask.getWorkflow();

        // 1. Unlock dependent tasks
        for (Task candidate : workflow.getTasks()) {

            if (candidate.getStatus() != TaskStatus.PENDING) continue;

            boolean ready = candidate.getDependsOn()
                    .stream()
                    .allMatch(t -> t.getStatus() == TaskStatus.COMPLETED);

            if (ready) {
                candidate.setStatus(TaskStatus.READY);
                log.info("Task {} unlocked (workflow={})",
                        candidate.getId(), workflow.getId());
            }
        }

        checkAndCompleteWorkflow(workflow);
        workflowExecutorService.executeWorkflow(workflow.getId());
    }

    @Override
    @Transactional
    public void onTaskFailed(Long taskId) {

        Task failedTask = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task not found"));

        workflowExecutorService.executeWorkflow(
                failedTask.getWorkflow().getId()
        );
    }

    private void checkAndCompleteWorkflow(Workflow workflow) {
        boolean allCompleted = workflow.getTasks()
                .stream()
                .allMatch(t -> t.getStatus() == TaskStatus.COMPLETED);

        if (allCompleted && workflow.getStatus() != WorkflowStatus.COMPLETED) {
            workflow.setStatus(WorkflowStatus.COMPLETED);
            log.info("WORKFLOW COMPLETED | workflowId={}", workflow.getId());
        }
    }
}
