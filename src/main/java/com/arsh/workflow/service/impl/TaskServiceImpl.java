package com.arsh.workflow.service.impl;

import com.arsh.workflow.dto.response.TaskResponse;
import com.arsh.workflow.enums.TaskStatus;
import com.arsh.workflow.events.WorkflowEvent;
import com.arsh.workflow.events.WorkflowEventFactory;
import com.arsh.workflow.events.producer.WorkflowEventProducer;
import com.arsh.workflow.exception.TaskNotFoundException;
import com.arsh.workflow.mapper.TaskMapper;
import com.arsh.workflow.model.Task;
import com.arsh.workflow.model.User;
import com.arsh.workflow.repository.TaskRepository;
import com.arsh.workflow.repository.UserRepository;
import com.arsh.workflow.service.TaskService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;


@Service
@Slf4j
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final WorkflowEventProducer eventProducer;

    public TaskServiceImpl(
            TaskRepository taskRepository,
            UserRepository userRepository,
            WorkflowEventProducer eventProducer
    ) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.eventProducer = eventProducer;
    }


    @Override
    @Transactional
    public TaskResponse assignTask(Long taskId, Long userId) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new TaskNotFoundException("Task with id " + taskId + " not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User with id " + userId + " not found"));

        task.setAssignedTo(user);
        return TaskMapper.toResponse(task);
    }

    @Override
    public TaskResponse getTask(Long taskId) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new TaskNotFoundException("Task with id " + taskId + " not found"));

        return TaskMapper.toResponse(task);
    }

    @Override
    @Transactional
    public TaskResponse changeStatus(Long taskId, TaskStatus newStatus) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new TaskNotFoundException(
                                "Task with id " + taskId + " not found"));

        TaskStatus current = task.getStatus();

        if (current == newStatus) {
            return TaskMapper.toResponse(task);
        }

        validateTransition(current, newStatus);

        task.setStatus(newStatus);
        taskRepository.save(task);

        publishAfterCommit(task, current, newStatus);

        log.info(
                "Task {} status changed {} → {} (event scheduled after commit)",
                task.getId(), current, newStatus
        );

        return TaskMapper.toResponse(task);
    }


    private void validateTransition(TaskStatus from, TaskStatus to) {

        if (from == TaskStatus.COMPLETED || from == TaskStatus.FAILED) {
            throw new IllegalStateException("Terminal state: " + from);
        }

        if (from == TaskStatus.PENDING && to != TaskStatus.READY) {
            throw new IllegalStateException("PENDING → READY only");
        }

        if (from == TaskStatus.READY && to != TaskStatus.IN_PROGRESS) {
            throw new IllegalStateException("READY → IN_PROGRESS only");
        }

        if (from == TaskStatus.IN_PROGRESS &&
                to != TaskStatus.COMPLETED &&
                to != TaskStatus.FAILED) {
            throw new IllegalStateException("Invalid IN_PROGRESS transition");
        }
    }

    private void publishAfterCommit(Task task,
                                    TaskStatus from,
                                    TaskStatus to) {

        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("No active transaction");
        }

        WorkflowEvent event =
                WorkflowEventFactory.fromTaskStatusChange(
                        task.getWorkflow().getId(),
                        task.getId(),
                        from,
                        to
                );

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        eventProducer.publish(
                                event.getWorkflowId(),
                                event
                        );
                    }
                }
        );
    }
}
