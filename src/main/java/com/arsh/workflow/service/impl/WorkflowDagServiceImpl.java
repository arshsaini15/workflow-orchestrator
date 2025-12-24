package com.arsh.workflow.service.impl;

import com.arsh.workflow.dto.request.BatchTaskRequest;
import com.arsh.workflow.dto.response.TaskResponse;
import com.arsh.workflow.enums.TaskStatus;
import com.arsh.workflow.enums.WorkflowStatus;
import com.arsh.workflow.exception.WorkflowNotFoundException;
import com.arsh.workflow.mapper.TaskMapper;
import com.arsh.workflow.model.Task;
import com.arsh.workflow.model.Workflow;
import com.arsh.workflow.repository.WorkflowRepository;
import com.arsh.workflow.service.WorkflowDagService;
import com.arsh.workflow.validation.WorkflowGraphValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class WorkflowDagServiceImpl implements WorkflowDagService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowGraphValidator workflowGraphValidator;

    private String getCurrentUser() {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
    }

    @Override
    @Transactional
    public List<TaskResponse> createBatchDag(
            Long workflowId,
            List<BatchTaskRequest> batch
    ) {

        // DTO-level DAG validation
        workflowGraphValidator.validateOrThrow(batch);

        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() ->
                        new WorkflowNotFoundException("Workflow not found"));

        if (!workflow.getCreatedBy().equals(getCurrentUser())) {
            throw new AccessDeniedException("Not your workflow");
        }

        if (workflow.getStatus() != WorkflowStatus.CREATED) {
            throw new IllegalStateException(
                    "Cannot modify DAG after workflow start"
            );
        }

        // ---------- Build tasks in memory ----------
        Map<String, Task> aliasMap = new HashMap<>();

        for (BatchTaskRequest req : batch) {

            if (aliasMap.containsKey(req.getClientId())) {
                throw new IllegalStateException(
                        "Duplicate clientId: " + req.getClientId()
                );
            }

            Task task = new Task();
            task.setTitle(req.getTitle());
            task.setDescription(req.getDescription());
            task.setStatus(TaskStatus.PENDING);

            workflow.addTask(task);
            aliasMap.put(req.getClientId(), task);
        }

        // ---------- Resolve dependencies using helpers ----------
        for (BatchTaskRequest req : batch) {

            Task child = aliasMap.get(req.getClientId());
            List<String> parentAliases = req.getDependsOn();

            if (parentAliases == null || parentAliases.isEmpty()) {
                continue;
            }

            // Clear any existing dependencies (safety)
            child.clearDependencies();

            for (String parentAlias : parentAliases) {

                Task parent = aliasMap.get(parentAlias);

                if (parent == null) {
                    throw new IllegalStateException(
                            "Invalid dependsOn clientId: " + parentAlias
                    );
                }

                child.addDependency(parent);
            }
        }

        // ---------- Entity-level DAG validation ----------
        workflowGraphValidator.validateResolvedDag(workflow.getTasks());

        // ---------- Persist ONCE via aggregate root ----------
        workflowRepository.save(workflow);

        return workflow.getTasks()
                .stream()
                .map(TaskMapper::toResponse)
                .toList();
    }
}
