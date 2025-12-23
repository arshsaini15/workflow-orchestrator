package com.arsh.workflow.service.impl;

import com.arsh.workflow.dto.response.TaskResponse;
import com.arsh.workflow.enums.EventType;
import com.arsh.workflow.enums.TaskStatus;
import com.arsh.workflow.enums.WorkflowStatus;
import com.arsh.workflow.events.WorkflowEvent;
import com.arsh.workflow.events.producer.WorkflowEventProducer;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowEventProducer eventProducer;

    public TaskServiceImpl(
            TaskRepository taskRepository,
            UserRepository userRepository,
            WorkflowRepository workflowRepository,
            WorkflowEventProducer eventProducer
    ) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.workflowRepository = workflowRepository;
        this.eventProducer = eventProducer;
    }

    @Override
    public TaskResponse assignTask(Long taskId, Long userId) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new TaskNotFoundException("Task with id " + taskId + " not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User with id " + userId + " not found"));

        task.setAssignedTo(user);
        taskRepository.save(task);

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
    public Long getWorkflowId(Long taskId) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new TaskNotFoundException("Task with id " + taskId + " not found"));

        return task.getWorkflow().getId();
    }

    @Override
    @Transactional
    public TaskResponse changeStatus(Long taskId, TaskStatus newStatus) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new TaskNotFoundException("Task with id " + taskId + " not found"));

        TaskStatus previousStatus = task.getStatus();

        // No-op guard (CRITICAL)
        if (previousStatus == newStatus) {
            return TaskMapper.toResponse(task);
        }

        // Update task
        task.setStatus(newStatus);
        taskRepository.save(task);

        // Emit TASK-level event
        emitTaskEvent(task, previousStatus, newStatus);

        // ---- Workflow state recalculation ----
        Workflow workflow = task.getWorkflow();
        if (workflow != null) {
            List<Task> tasks = workflow.getTasks();

            boolean allCompleted = tasks.stream()
                    .allMatch(t -> t.getStatus() == TaskStatus.COMPLETED);

            boolean anyFailed = tasks.stream()
                    .anyMatch(t -> t.getStatus() == TaskStatus.FAILED);

            boolean anyInProgress = tasks.stream()
                    .anyMatch(t -> t.getStatus() == TaskStatus.IN_PROGRESS);

            WorkflowStatus previousWorkflowStatus = workflow.getStatus();

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

            // Emit WORKFLOW-level completion/failure event if needed
            emitWorkflowEventIfNeeded(workflow, previousWorkflowStatus);
        }

        return TaskMapper.toResponse(task);
    }

    private void emitTaskEvent(Task task, TaskStatus from, TaskStatus to) {

        EventType eventType = null;

        if (from == TaskStatus.READY && to == TaskStatus.IN_PROGRESS) {
            eventType = EventType.TASK_STARTED;
        } else if (from == TaskStatus.IN_PROGRESS && to == TaskStatus.COMPLETED) {
            eventType = EventType.TASK_COMPLETED;
        } else if (from == TaskStatus.IN_PROGRESS && to == TaskStatus.FAILED) {
            eventType = EventType.TASK_FAILED;
        }

        if (eventType == null) return;

        WorkflowEvent event = WorkflowEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .workflowId(task.getWorkflow().getId())
                .taskId(task.getId())
                .status(task.getWorkflow().getStatus())
                .source("workflow-service")
                .occurredAt(Instant.now())
                .version(1)
                .build();

        eventProducer.publish(event);
    }

    private void emitWorkflowEventIfNeeded(
            Workflow workflow,
            WorkflowStatus previousStatus
    ) {
        if (workflow.getStatus() == previousStatus) return;

        EventType type = null;

        if (workflow.getStatus() == WorkflowStatus.COMPLETED) {
            type = EventType.WORKFLOW_COMPLETED;
        } else if (workflow.getStatus() == WorkflowStatus.FAILED) {
            type = EventType.WORKFLOW_FAILED;
        }

        if (type == null) return;

        WorkflowEvent event = WorkflowEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(type)
                .workflowId(workflow.getId())
                .status(workflow.getStatus())
                .source("workflow-service")
                .occurredAt(Instant.now())
                .version(1)
                .build();

        eventProducer.publish(event);
    }
}
