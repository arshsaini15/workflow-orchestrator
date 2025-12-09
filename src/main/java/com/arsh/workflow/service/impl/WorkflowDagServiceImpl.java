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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class WorkflowDagServiceImpl implements WorkflowDagService {

    private final WorkflowRepository workflowRepository;
    private final TaskRepository taskRepository;
    private final WorkflowGraphValidator workflowGraphValidator;

    public WorkflowDagServiceImpl(
            WorkflowRepository workflowRepository,
            TaskRepository taskRepository,
            WorkflowGraphValidator workflowGraphValidator
    ) {
        this.workflowRepository = workflowRepository;
        this.taskRepository = taskRepository;
        this.workflowGraphValidator = workflowGraphValidator;
    }

    @Override
    @Transactional
    public List<TaskResponse> createBatchDag(Long workflowId, List<BatchTaskRequest> batch) {

        // ✅ 🔥 HARD DAG VALIDATION FIRST
        workflowGraphValidator.validateOrThrow(batch);

        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found"));

        String username = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        if (!workflow.getCreatedBy().equals(username)) {
            throw new AccessDeniedException("Not your workflow");
        }

        if (workflow.getStatus() != WorkflowStatus.CREATED) {
            throw new IllegalStateException("Cannot modify DAG after workflow start");
        }

        // ✅ PHASE 1 — CREATE ALL TASKS
        Map<String, Task> aliasMap = new HashMap<>();

        for (BatchTaskRequest req : batch) {

            Task task = new Task();
            task.setTitle(req.getTitle());
            task.setDescription(req.getDescription());
            task.setStatus(TaskStatus.PENDING);

            workflow.addTask(task);
            taskRepository.save(task);

            aliasMap.put(req.getClientId(), task);
        }

        // ✅ PHASE 2 — RESOLVE DEPENDENCIES
        for (BatchTaskRequest req : batch) {

            Task child = aliasMap.get(req.getClientId());
            List<String> parentAliases = req.getDependsOn();

            if (parentAliases == null || parentAliases.isEmpty()) continue;

            List<Task> parents = new ArrayList<>();

            for (String parentAlias : parentAliases) {
                Task parent = aliasMap.get(parentAlias);
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
