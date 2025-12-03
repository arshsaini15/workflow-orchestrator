package com.arsh.workflow.service.impl;

import com.arsh.workflow.enums.TaskStatus;
import com.arsh.workflow.enums.WorkflowStatus;
import com.arsh.workflow.exception.WorkflowNotFoundException;
import com.arsh.workflow.model.Workflow;
import com.arsh.workflow.repository.WorkflowRepository;
import com.arsh.workflow.service.WorkflowExecutorService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;

@Service
public class WorkflowExecutorServiceImpl implements WorkflowExecutorService {

    private final ExecutorService executorService;
    private final TaskServiceImpl taskService;
    private final WorkflowRepository workflowRepository;

    public WorkflowExecutorServiceImpl(TaskServiceImpl taskService,
                                       WorkflowRepository workflowRepository,
                                       @Qualifier("workflowExecutorPool") ExecutorService executorService
    ) {
        this.executorService = executorService;
        this.taskService = taskService;
        this.workflowRepository = workflowRepository;
    }

    public void executeWorkflow(Long workflowId) {

        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow with id: " + workflowId + " not found"));

        // mark workflow as RUNNING
        workflow.setStatus(WorkflowStatus.RUNNING);
        workflowRepository.save(workflow);

        // pick READY tasks
        workflow.getTasks().stream()
                .filter(t -> t.getStatus() == TaskStatus.READY)
                .forEach(t -> executorService.submit(() -> runTask(t.getId())));
    }

    private void runTask(Long taskId) {

        // Mark IN_PROGRESS
        taskService.changeStatus(taskId, TaskStatus.IN_PROGRESS);

        try {
            Thread.sleep(2000);
            // Mark COMPLETED
            taskService.changeStatus(taskId, TaskStatus.COMPLETED);
        } catch (Exception e) {
            taskService.changeStatus(taskId, TaskStatus.FAILED);
        }
    }
}
