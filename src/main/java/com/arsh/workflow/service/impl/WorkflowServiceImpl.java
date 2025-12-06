package com.arsh.workflow.service.impl;

import com.arsh.workflow.dto.*;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executor;


@Service
public class WorkflowServiceImpl implements WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final TaskRepository taskRepository;
    private final WorkflowExecutorService workflowExecutorService;
    private final Executor executor;

    public WorkflowServiceImpl(WorkflowRepository workflowRepository,
                               TaskRepository taskRepository,
                               WorkflowExecutorService workflowExecutorService,
                               @Qualifier("workflowExecutor") Executor executor
    ) {
        this.workflowRepository = workflowRepository;
        this.taskRepository = taskRepository;
        this.workflowExecutorService = workflowExecutorService;
        this.executor = executor;
    }

    @Override
    public WorkflowResponse createWorkflow(CreateWorkflowRequest req) {

        String username = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

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
                .orElseThrow(() -> new WorkflowNotFoundException(
                        "Workflow with id " + workflowId + " not found!"
                ));

        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        if (!workflow.getCreatedBy().equals(username)) {
            throw new AccessDeniedException("You cannot access this workflow");
        }

        return WorkflowMapper.toResponse(workflow);
    }

    @Override
    public WorkflowResponse deleteWorkflow(Long workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException("Unable to find workflow with id " + workflowId));

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!workflow.getCreatedBy().equals(username)) {
            throw new AccessDeniedException("You cannot delete this workflow");
        }

        WorkflowResponse res = WorkflowMapper.toResponse(workflow);
        workflowRepository.delete(workflow);

        return res;
    }


    @Override
    public TaskResponse deleteTask(Long workflowId, Long taskId) {

        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found"));

        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        if (!workflow.getCreatedBy().equals(username)) {
            throw new AccessDeniedException("Not your workflow");
        }


        Task task = workflow.getTasks().stream()
                .filter(t -> t.getId().equals(taskId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "Task with id " + taskId + " not found in this workflow"
                ));

        TaskResponse res = TaskMapper.toResponse(task);

        workflow.removeTask(task);
        workflowRepository.save(workflow);

        return res;
    }

    @Override
    public TaskResponse addTask(Long workflowId, CreateTaskRequest req) {

        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found"));

        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        if (!workflow.getCreatedBy().equals(username)) {
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

        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        if (!workflow.getCreatedBy().equals(username)) {
            throw new AccessDeniedException("Not your workflow");
        }

        workflow.getTasks().forEach(t -> {
            if (t.getStatus() == TaskStatus.PENDING) {
                t.setStatus(TaskStatus.READY);
            }
        });

        workflow.setStatus(WorkflowStatus.READY);
        workflowRepository.save(workflow);

        workflowExecutorService.executeWorkflow(workflowId);

        return WorkflowMapper.toResponse(workflow);
    }

    @Override
    public WorkflowResponse completeWorkflow(Long workflowId) {

        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found"));

        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        if (!workflow.getCreatedBy().equals(username)) {
            throw new AccessDeniedException("Not your workflow");
        }


        for (Task task : workflow.getTasks()) {
            if (task.getStatus() != TaskStatus.COMPLETED) {
                throw new IllegalWorkflowOperationException(
                        "Cannot complete workflow. Task with id " + task.getId()
                                + " is not DONE. Status: " + task.getStatus()
                );
            }
        }

        workflow.setStatus(WorkflowStatus.COMPLETED);
        workflow = workflowRepository.save(workflow);

        return WorkflowMapper.toResponse(workflow);
    }

    @Override
    public PaginatedResponse<WorkflowResponse> getAllWorkflows(
            WorkflowStatus status,
            String createdBy,
            String search,
            Pageable pageable
    ) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

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

        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        if (!workflow.getCreatedBy().equals(username)) {
            throw new AccessDeniedException("You cannot access tasks of another user's workflow");
        }



        var spec = TaskSpecifications.filter(status, search)
                .and((root, query, cb) ->
                        cb.equal(root.get("workflow").get("id"), workflowId)
                );

        var page = taskRepository.findAll(spec, pageable);
        var mapped = page.map(TaskMapper::toResponse);

        return PageMapper.toResponse(mapped);
    }
}
