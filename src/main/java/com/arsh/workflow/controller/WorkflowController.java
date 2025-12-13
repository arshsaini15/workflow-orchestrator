package com.arsh.workflow.controller;

import com.arsh.workflow.dto.request.CreateTaskRequest;
import com.arsh.workflow.dto.request.CreateWorkflowRequest;
import com.arsh.workflow.dto.response.PaginatedResponse;
import com.arsh.workflow.dto.response.TaskResponse;
import com.arsh.workflow.dto.response.WorkflowResponse;
import com.arsh.workflow.enums.TaskStatus;
import com.arsh.workflow.enums.WorkflowStatus;
import com.arsh.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @PostMapping("/create")
    public WorkflowResponse createWorkflow(@RequestBody CreateWorkflowRequest req) {
        return workflowService.createWorkflow(req);
    }

    @GetMapping("/{workflowId}")
    public WorkflowResponse getWorkflow(@PathVariable Long workflowId) {
        return workflowService.getWorkflow(workflowId);
    }

    @DeleteMapping("/delete/{workflowId}")
    public WorkflowResponse deleteWorkflow(@PathVariable Long workflowId) {
        return workflowService.deleteWorkflow(workflowId);
    }

    @PostMapping("/addTask/{workflowId}")
    public TaskResponse addTaskToWorkflow(@PathVariable Long workflowId, @RequestBody CreateTaskRequest req) {
        return workflowService.addTask(workflowId, req);
    }

    @PostMapping("/start/{workflowId}")
    public WorkflowResponse startWorkflow(@PathVariable Long workflowId) {
        return workflowService.startWorkflow(workflowId);
    }

    @PostMapping("/complete/{workflowId}")
    public WorkflowResponse completeWorkflow(@PathVariable Long workflowId) {
        return workflowService.completeWorkflow(workflowId);
    }

    @GetMapping
    public PaginatedResponse<WorkflowResponse> getAllWorkflows(
            @RequestParam(required = false) WorkflowStatus status,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String search,
            Pageable pageable
    ) {
        return workflowService.getAllWorkflows(status, createdBy, search, pageable);
    }

    @DeleteMapping("/{workflowId}/delete/{taskId}")
    public TaskResponse deleteTask(
            @PathVariable Long workflowId,
            @PathVariable Long taskId
    ) {
        return workflowService.deleteTask(workflowId, taskId);
    }

    @GetMapping("/{workflowId}/tasks")
    public PaginatedResponse<TaskResponse> getTasksForWorkflow(
            @PathVariable Long workflowId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) String search,
            Pageable pageable
    ) {
        return workflowService.getTasksForWorkflow(workflowId, status, search, pageable);
    }
}
