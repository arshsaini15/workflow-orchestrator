package com.arsh.workflow.service.impl;

import com.arsh.workflow.enums.TaskStatus;
import com.arsh.workflow.enums.WorkflowStatus;
import com.arsh.workflow.exception.TaskNotFoundException;
import com.arsh.workflow.model.Task;
import com.arsh.workflow.model.Workflow;
import com.arsh.workflow.repository.TaskRepository;
import com.arsh.workflow.repository.WorkflowRepository;
import com.arsh.workflow.service.WorkflowCoordinator;
import com.arsh.workflow.service.WorkflowExecutorService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkflowCoordinatorImpl implements WorkflowCoordinator {

    private final TaskRepository taskRepository;
    private final WorkflowExecutorService workflowExecutorService;

    @Override
    @Transactional
    public void onTaskCompleted(Long taskId) {

        Task completedTask = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task not found"));

        Workflow workflow = completedTask.getWorkflow();

        // unlock dependent tasks
        for (Task candidate : workflow.getTasks()) {

            if (candidate.getStatus() != TaskStatus.PENDING) continue;

            boolean ready = candidate.getDependsOn()
                    .stream()
                    .allMatch(t -> t.getStatus() == TaskStatus.COMPLETED);

            if (ready) {
                candidate.setStatus(TaskStatus.READY);
            }
        }

        // hand control back to executor
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
}
