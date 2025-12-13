package com.arsh.workflow.service.impl;

import com.arsh.workflow.dto.request.BatchTaskRequest;
import com.arsh.workflow.dto.response.TaskResponse;
import com.arsh.workflow.enums.TaskStatus;
import com.arsh.workflow.enums.WorkflowStatus;
import com.arsh.workflow.exception.WorkflowNotFoundException;
import com.arsh.workflow.mapper.TaskMapper;
import com.arsh.workflow.model.Task;
import com.arsh.workflow.model.Workflow;
import com.arsh.workflow.repository.TaskRepository;
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
    private final TaskRepository taskRepository;
    private final WorkflowGraphValidator workflowGraphValidator;

    private String getCurrentUser() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @Override
    @Transactional
    public List<TaskResponse> createBatchDag(Long workflowId, List<BatchTaskRequest> batch) {

        workflowGraphValidator.validateOrThrow(batch);

        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found"));

        String username = getCurrentUser();

        if (!workflow.getCreatedBy().equals(username)) {
            throw new AccessDeniedException("Not your workflow");
        }

        if (workflow.getStatus() != WorkflowStatus.CREATED) {
            throw new IllegalStateException("Cannot modify DAG after workflow start");
        }

        Map<String, Task> aliasMap = new HashMap<>();

        // Create tasks first
        for (BatchTaskRequest req : batch) {
            Task task = new Task();
            task.setTitle(req.getTitle());
            task.setDescription(req.getDescription());
            task.setStatus(TaskStatus.PENDING);

            workflow.addTask(task);
            taskRepository.save(task);

            aliasMap.put(req.getClientId(), task);
        }

        // Resolve dependencies
        for (BatchTaskRequest req : batch) {

            Task child = aliasMap.get(req.getClientId());
            if (child == null) {
                throw new IllegalStateException("Invalid clientId: " + req.getClientId());
            }

            List<String> parentAliases = req.getDependsOn();
            if (parentAliases == null || parentAliases.isEmpty()) continue;

            List<Task> parents = new ArrayList<>();

            for (String parentAlias : parentAliases) {
                Task parent = aliasMap.get(parentAlias);
                if (parent == null) {
                    throw new IllegalStateException("Invalid dependsOn clientId: " + parentAlias);
                }
                parents.add(parent);
            }

            child.setDependsOn(parents);
        }

        workflowRepository.save(workflow);

        return workflow.getTasks()
                .stream()
                .map(TaskMapper::toResponse)
                .toList();
    }
}
