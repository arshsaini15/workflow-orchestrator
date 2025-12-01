package com.arsh.workflow.controller;

import com.arsh.workflow.dto.CreateTaskRequest;
import com.arsh.workflow.dto.CreateWorkflowRequest;
import com.arsh.workflow.dto.TaskResponse;
import com.arsh.workflow.dto.WorkflowResponse;
import com.arsh.workflow.service.WorkflowServiceImpl;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public void addTaskToWorkflow(@PathVariable Long workflowId, @RequestBody CreateTaskRequest req) {
        workflowService.addTask(workflowId, req);
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
    public Page<WorkflowResponse> getAllWorkflows(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "10") int size)
    {
        return workflowService.getAllWorkflows(page, size);
    }

    @DeleteMapping("/{workflowId}/delete/{taskId}")
    public TaskResponse deleteTask(@PathVariable Long workflowId, @PathVariable Long taskId) {
        return workflowService.deleteTask(workflowId, taskId);
    }
}
