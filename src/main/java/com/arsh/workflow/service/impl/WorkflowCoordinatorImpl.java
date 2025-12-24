package com.arsh.workflow.service.impl;

import com.arsh.workflow.enums.TaskStatus;
import com.arsh.workflow.enums.WorkflowStatus;
import com.arsh.workflow.exception.TaskNotFoundException;
import com.arsh.workflow.model.Task;
import com.arsh.workflow.model.Workflow;
import com.arsh.workflow.repository.TaskRepository;
import com.arsh.workflow.repository.WorkflowRepository;
import com.arsh.workflow.service.WorkflowCoordinator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkflowCoordinatorImpl implements WorkflowCoordinator {

    private final TaskRepository taskRepository;
    private final WorkflowRepository workflowRepository;

    @Override
    @Transactional
    public void onTaskCompleted(Long taskId) {

        Task completedTask = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new TaskNotFoundException("Task not found: " + taskId));

        Workflow workflow = completedTask.getWorkflow();

        // 1️⃣ Unlock dependent tasks
        for (Task candidate : workflow.getTasks()) {

            if (candidate.getStatus() != TaskStatus.PENDING) {
                continue;
            }

            boolean allParentsDone = candidate.getDependsOn()
                    .stream()
                    .allMatch(parent -> parent.getStatus() == TaskStatus.COMPLETED);

            if (allParentsDone) {
                candidate.setStatus(TaskStatus.READY);
            }
        }

        // 2️⃣ Check workflow completion
        boolean allCompleted = workflow.getTasks()
                .stream()
                .allMatch(t -> t.getStatus() == TaskStatus.COMPLETED);

        if (allCompleted) {
            workflow.setStatus(WorkflowStatus.COMPLETED);
            return;
        }

        // 3️⃣ Otherwise workflow is running
        workflow.setStatus(WorkflowStatus.RUNNING);
    }

    @Override
    @Transactional
    public void onTaskFailed(Long taskId) {

        Task failedTask = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new TaskNotFoundException("Task not found: " + taskId));

        Workflow workflow = failedTask.getWorkflow();

        // Simple rule for now: any failure fails workflow
        workflow.setStatus(WorkflowStatus.FAILED);
    }
}
