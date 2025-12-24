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
import jakarta.transaction.Transactional;
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
    private final WorkflowGraphValidator workflowGraphValidator;

    public WorkflowServiceImpl(
            WorkflowRepository workflowRepository,
            WorkflowGraphValidator workflowGraphValidator
    ) {
        this.workflowRepository = workflowRepository;
        this.workflowGraphValidator = workflowGraphValidator;
    }

    private String getCurrentUser() {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
    }

    @Override
    @Transactional
    public WorkflowResponse createWorkflow(CreateWorkflowRequest req) {

        Workflow workflow = WorkflowMapper.toEntity(req);
        workflow.setCreatedBy(getCurrentUser());
        workflow.setUpdatedBy(getCurrentUser());
        workflow.setStatus(WorkflowStatus.CREATED);

        workflowRepository.save(workflow);
        return WorkflowMapper.toResponse(workflow);
    }

    @Override
    public WorkflowResponse getWorkflow(Long workflowId) {

        Workflow workflow = workflowRepository.findByIdWithTasks(workflowId)
                .orElseThrow(() ->
                        new WorkflowNotFoundException("Workflow not found"));

        authorize(workflow);
        return WorkflowMapper.toResponse(workflow);
    }


    @Override
    @Transactional
    public WorkflowResponse deleteWorkflow(Long workflowId) {

        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() ->
                        new WorkflowNotFoundException("Workflow not found"));

        authorize(workflow);
        workflowRepository.delete(workflow);

        return WorkflowMapper.toResponse(workflow);
    }

    @Override
    @Transactional
    public TaskResponse addTask(Long workflowId, CreateTaskRequest req) {

        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() ->
                        new WorkflowNotFoundException("Workflow not found"));

        authorize(workflow);

        if (workflow.getStatus() != WorkflowStatus.CREATED) {
            throw new IllegalWorkflowOperationException(
                    "Cannot add tasks once workflow has started"
            );
        }

        Task task = TaskMapper.toEntity(req);
        task.setStatus(TaskStatus.PENDING);

        workflow.addTask(task);

        return TaskMapper.toResponse(task);
    }

    @Override
    @Transactional
    public WorkflowResponse startWorkflow(Long workflowId) {

        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() ->
                        new WorkflowNotFoundException("Workflow not found"));

        authorize(workflow);

        if (workflow.getStatus() != WorkflowStatus.CREATED) {
            throw new IllegalWorkflowOperationException(
                    "Workflow already started"
            );
        }

        if (workflow.getTasks().isEmpty()) {
            throw new IllegalWorkflowOperationException(
                    "Cannot start workflow without tasks"
            );
        }

        // Only DAG sources are allowed to start
        workflow.getTasks().forEach(task -> {
            if (task.getDependsOn() == null || task.getDependsOn().isEmpty()) {
                task.setStatus(TaskStatus.READY);
            }
        });

        workflow.setStatus(WorkflowStatus.READY);
        return WorkflowMapper.toResponse(workflow);
    }

    private void authorize(Workflow workflow) {
        if (!workflow.getCreatedBy().equals(getCurrentUser())) {
            throw new AccessDeniedException("Not your workflow");
        }
    }
}
