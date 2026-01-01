package com.arsh.workflow.controller;

import com.arsh.workflow.dto.request.CreateTaskRequest;
import com.arsh.workflow.dto.request.CreateWorkflowRequest;
import com.arsh.workflow.dto.response.PaginatedResponse;
import com.arsh.workflow.dto.response.TaskResponse;
import com.arsh.workflow.dto.response.WorkflowResponse;
import com.arsh.workflow.enums.TaskStatus;
import com.arsh.workflow.enums.WorkflowStatus;
import com.arsh.workflow.service.WorkflowExecutorService;
import com.arsh.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;
    private final WorkflowExecutorService workflowExecutorService;

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
        WorkflowResponse res = workflowService.startWorkflow(workflowId);
        workflowExecutorService.executeWorkflow(workflowId);
        return res;
    }
}
