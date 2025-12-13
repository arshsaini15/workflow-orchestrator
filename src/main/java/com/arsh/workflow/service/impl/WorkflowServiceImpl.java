package com.arsh.workflow.service.impl;

import com.arsh.workflow.dto.request.CreateTaskRequest;
import com.arsh.workflow.dto.request.CreateWorkflowRequest;
import com.arsh.workflow.dto.response.PaginatedResponse;
import com.arsh.workflow.dto.response.TaskResponse;
import com.arsh.workflow.dto.response.WorkflowResponse;
import com.arsh.workflow.enums.TaskStatus;
import com.arsh.workflow.enums.WorkflowStatus;
import com.arsh.workflow.exception.IllegalWorkflowOperationException;
import com.arsh.workflow.exception.WorkflowNotFoundException;
import com.arsh.workflow.mapper.PageMapper;
import com.arsh.workflow.mapper.TaskMapper;
import com.arsh.workflow.mapper.WorkflowMapper;
import com.arsh.workflow.model.Task;
import com.arsh.workflow.model.Workflow;
import com.arsh.workflow.repository.TaskRepository;
import com.arsh.workflow.repository.WorkflowRepository;
import com.arsh.workflow.service.WorkflowExecutorService;
import com.arsh.workflow.service.WorkflowService;
import com.arsh.workflow.util.TaskSpecifications;
import com.arsh.workflow.util.WorkflowSpecifications;
import com.arsh.workflow.validation.WorkflowGraphValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

@Service
public class WorkflowServiceImpl implements WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final TaskRepository taskRepository;
    private final WorkflowExecutorService workflowExecutorService;
    private final WorkflowGraphValidator workflowGraphValidator;

    @Qualifier("workflowExecutorPool")
    private final Executor executor; // unused but preserved in case other services rely on it

    public WorkflowServiceImpl(
            WorkflowRepository workflowRepository,
            TaskRepository taskRepository,
            WorkflowExecutorService workflowExecutorService,
            WorkflowGraphValidator workflowGraphValidator,
            @Qualifier("workflowExecutorPool") ExecutorService executorService
    ) {
        this.workflowRepository = workflowRepository;
        this.taskRepository = taskRepository;
        this.workflowExecutorService = workflowExecutorService;
        this.workflowGraphValidator = workflowGraphValidator;
        this.executor = executorService;
    }


    // Helper for cleaner auth usage
    private String getCurrentUser() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @Override
    public WorkflowResponse createWorkflow(CreateWorkflowRequest req) {

        workflowGraphValidator.validateOrThrow(req.getTasks());

        String username = getCurrentUser();

        Workflow workflow = WorkflowMapper.toEntity(req);
        workflow.setCreatedBy(username);
        workflow.setUpdatedBy(username);
        workflow.setStatus(WorkflowStatus.CREATED);

        workflow = workflowRepository.save(workflow);
        return WorkflowMapper.toResponse(workflow);
    }

    @Override
    public WorkflowResponse getWorkflow(Long workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow with id " + workflowId + " not found!"));

        if (!workflow.getCreatedBy().equals(getCurrentUser())) {
            throw new AccessDeniedException("You cannot access this workflow");
        }

        return WorkflowMapper.toResponse(workflow);
    }

    @Override
    public WorkflowResponse deleteWorkflow(Long workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException("Unable to find workflow with id " + workflowId));

        if (!workflow.getCreatedBy().equals(getCurrentUser())) {
            throw new AccessDeniedException("You cannot delete this workflow");
        }

        WorkflowResponse response = WorkflowMapper.toResponse(workflow);
        workflowRepository.delete(workflow);
        return response;
    }

    @Override
    public TaskResponse deleteTask(Long workflowId, Long taskId) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found"));

        if (!workflow.getCreatedBy().equals(getCurrentUser())) {
            throw new AccessDeniedException("Not your workflow");
        }

        Task task = workflow.getTasks().stream()
                .filter(t -> t.getId().equals(taskId))
                .findFirst()
                .orElseThrow(() -> new IllegalWorkflowOperationException(
                        "Task with id " + taskId + " not found in this workflow"
                ));

        TaskResponse response = TaskMapper.toResponse(task);

        workflow.removeTask(task);
        workflowRepository.save(workflow);

        return response;
    }

    @Override
    public TaskResponse addTask(Long workflowId, CreateTaskRequest req) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found"));

        if (!workflow.getCreatedBy().equals(getCurrentUser())) {
            throw new AccessDeniedException("Not your workflow");
        }

        Task task = TaskMapper.toEntity(req);
        task.setStatus(TaskStatus.PENDING);
        task.setAssignedTo(null);

        workflow.addTask(task);
        workflowRepository.save(workflow);

        if (workflow.getStatus() == WorkflowStatus.RUNNING) {
            workflowExecutorService.executeWorkflow(workflowId);
        }

        return TaskMapper.toResponse(task);
    }

    @Override
    public WorkflowResponse startWorkflow(Long workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found"));

        if (!workflow.getCreatedBy().equals(getCurrentUser())) {
            throw new AccessDeniedException("Not your workflow");
        }

        // Activate only source nodes
        for (Task t : workflow.getTasks()) {
            if (t.getStatus() == TaskStatus.PENDING) {
                List<Task> parents = t.getDependsOn();
                if (parents == null || parents.isEmpty()) {
                    t.setStatus(TaskStatus.READY);
                }
            }
        }

        workflow.setStatus(WorkflowStatus.READY);
        workflowRepository.save(workflow);

        workflowExecutorService.executeWorkflow(workflowId);
        return WorkflowMapper.toResponse(workflow);
    }

    @Override
    public WorkflowResponse completeWorkflow(Long workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found"));

        if (!workflow.getCreatedBy().equals(getCurrentUser())) {
            throw new AccessDeniedException("Not your workflow");
        }

        for (Task t : workflow.getTasks()) {
            if (t.getStatus() != TaskStatus.COMPLETED) {
                throw new IllegalWorkflowOperationException(
                        "Cannot complete workflow. Task " + t.getId() + " is " + t.getStatus()
                );
            }
        }

        workflow.setStatus(WorkflowStatus.COMPLETED);
        workflowRepository.save(workflow);

        return WorkflowMapper.toResponse(workflow);
    }

    @Override
    public PaginatedResponse<WorkflowResponse> getAllWorkflows(
            WorkflowStatus status,
            String createdBy,
            String search,
            Pageable pageable
    ) {

        String username = getCurrentUser();
        var spec = WorkflowSpecifications.filter(status, username, search);

        Page<Workflow> page = workflowRepository.findAll(spec, pageable);
        return PageMapper.toResponse(page.map(WorkflowMapper::toResponse));
    }

    @Override
    public PaginatedResponse<TaskResponse> getTasksForWorkflow(
            Long workflowId,
            TaskStatus status,
            String search,
            Pageable pageable
    ) {

        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found"));

        if (!workflow.getCreatedBy().equals(getCurrentUser())) {
            throw new AccessDeniedException("You cannot access tasks of another user's workflow");
        }

        var spec = TaskSpecifications.filter(status, search)
                .and((root, query, cb) ->
                        cb.equal(root.get("workflow").get("id"), workflowId)
                );

        Page<Task> page = taskRepository.findAll(spec, pageable);
        return PageMapper.toResponse(page.map(TaskMapper::toResponse));
    }
}
