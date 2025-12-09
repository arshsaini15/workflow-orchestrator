package com.arsh.workflow.service.impl;

import com.arsh.workflow.dto.response.TaskResponse;
import com.arsh.workflow.enums.TaskStatus;
import com.arsh.workflow.enums.WorkflowStatus;
import com.arsh.workflow.exception.TaskNotFoundException;
import com.arsh.workflow.mapper.TaskMapper;
import com.arsh.workflow.model.Task;
import com.arsh.workflow.model.User;
import com.arsh.workflow.model.Workflow;
import com.arsh.workflow.repository.TaskRepository;
import com.arsh.workflow.repository.UserRepository;
import com.arsh.workflow.repository.WorkflowRepository;
import com.arsh.workflow.service.TaskService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final WorkflowRepository workflowRepository;

    public TaskServiceImpl(TaskRepository taskRepository,
                           UserRepository userRepository,
                           WorkflowRepository workflowRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.workflowRepository = workflowRepository;
    }

    @Override
    public TaskResponse assignTask(Long taskId, Long userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task with id " + taskId + " not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User with id " + userId + " not found"));

        task.setAssignedTo(user);
        task.setStatus(TaskStatus.IN_PROGRESS);

        task = taskRepository.save(task);
        return TaskMapper.toResponse(task);
    }

    @Override
    @Transactional
    public TaskResponse changeStatus(Long taskId, TaskStatus status) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task with id " + taskId + " not found"));

        task.setStatus(status);
        taskRepository.save(task);

        // ✅ DAG propagation: if this task just COMPLETED, check its dependents
        if (status == TaskStatus.COMPLETED) {
            activateDependentsIfReady(task);
        }

        Workflow workflow = task.getWorkflow();
        if (workflow != null) {
            List<Task> tasks = workflow.getTasks();

            boolean allCompleted = tasks.stream()
                    .allMatch(t -> t.getStatus() == TaskStatus.COMPLETED);

            boolean anyInProgress = tasks.stream()
                    .anyMatch(t -> t.getStatus() == TaskStatus.IN_PROGRESS);

            boolean anyFailed = tasks.stream()
                    .anyMatch(t -> t.getStatus() == TaskStatus.FAILED);

            if (allCompleted) {
                workflow.setStatus(WorkflowStatus.COMPLETED);
            } else if (anyFailed) {
                workflow.setStatus(WorkflowStatus.FAILED);
            } else if (anyInProgress) {
                workflow.setStatus(WorkflowStatus.RUNNING);
            } else {
                workflow.setStatus(WorkflowStatus.READY);
            }

            workflowRepository.save(workflow);
        }

        return TaskMapper.toResponse(task);
    }

    private void activateDependentsIfReady(Task completedTask) {
        if (completedTask.getDependents() == null) {
            return;
        }

        for (Task child : completedTask.getDependents()) {
            if (child.getStatus() == TaskStatus.COMPLETED) {
                continue;
            }

            List<Task> parents = child.getDependsOn();
            boolean allParentsCompleted =
                    (parents == null || parents.isEmpty()) ||
                            parents.stream().allMatch(p -> p.getStatus() == TaskStatus.COMPLETED);

            if (allParentsCompleted && child.getStatus() == TaskStatus.PENDING) {
                log.info("Activating child task {} because all parents of {} are COMPLETED",
                        child.getId(), completedTask.getId());
                child.setStatus(TaskStatus.READY);
            }
        }
    }

    @Override
    public TaskResponse getTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task with id " + taskId + " not found"));

        return TaskMapper.toResponse(task);
    }

    @Override
    public Long getWorkflowId(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        return task.getWorkflow().getId();
    }
}
