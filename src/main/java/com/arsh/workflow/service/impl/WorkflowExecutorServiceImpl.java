package com.arsh.workflow.service.impl;

import com.arsh.workflow.enums.TaskStatus;
import com.arsh.workflow.model.Task;
import com.arsh.workflow.repository.TaskRepository;
import com.arsh.workflow.service.WorkflowExecutorService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutorService;

@Service
public class WorkflowExecutorServiceImpl implements WorkflowExecutorService {

    private final ExecutorService executorService;
    private final TaskServiceImpl taskService;
    private final TaskRepository taskRepository;

    public WorkflowExecutorServiceImpl(TaskServiceImpl taskService,
                                       TaskRepository taskRepository,
                                       @Qualifier("workflowExecutorPool") ExecutorService executorService) {
        this.executorService = executorService;
        this.taskService = taskService;
        this.taskRepository = taskRepository;
    }

    @Override
    public void executeWorkflow(Long workflowId) {

        List<Task> readyTasks =
                taskRepository.findByWorkflowIdAndStatus(workflowId, TaskStatus.READY);

        readyTasks.forEach(t ->
                executorService.submit(() -> runTask(t.getId()))
        );
    }

    @Override
    public void runTask(Long taskId) {

        Long workflowId = taskService.getWorkflowId(taskId);

        try {
            taskService.changeStatus(taskId, TaskStatus.IN_PROGRESS);

            Thread.sleep(2000);

            taskService.changeStatus(taskId, TaskStatus.COMPLETED);

            triggerNextTasks(workflowId);

        } catch (Exception e) {
            taskService.changeStatus(taskId, TaskStatus.FAILED);
        }
    }

    @Override
    public void triggerNextTasks(Long workflowId) {

        List<Task> readyTasks =
                taskRepository.findByWorkflowIdAndStatus(workflowId, TaskStatus.READY);

        readyTasks.forEach(t ->
                executorService.submit(() -> runTask(t.getId()))
        );

        // ❗ DO NOTHING ELSE
        // Workflow completion logic is in TaskServiceImpl.changeStatus()
    }
}
