package com.arsh.workflow.controller;

import com.arsh.workflow.dto.*;
import com.arsh.workflow.enums.WorkflowStatus;
import com.arsh.workflow.model.Task;
import com.arsh.workflow.service.impl.WorkflowServiceImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workflow")
public class WorkflowController {

    private WorkflowServiceImpl workflowService;

    public WorkflowController(WorkflowServiceImpl workflowService) {
        this.workflowService = workflowService;
    }

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
    ){
        return workflowService.getAllWorkflows(status, createdBy, search, pageable);
    }

    @DeleteMapping("/{workflowId}/delete/{taskId}")
    public TaskResponse deleteTask(@PathVariable Long workflowId, @PathVariable Long taskId) {
        return workflowService.deleteTask(workflowId, taskId);
    }

    @GetMapping("/{workflowId}/tasks")
    public PaginatedResponse<TaskResponse> getTasksForWorkflow(
            @PathVariable Long workflowId,
            Pageable pageable
    ) {
        return workflowService.getTasksForWorkflow(workflowId, pageable);
    }
}
