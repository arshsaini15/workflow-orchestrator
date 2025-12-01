package com.arsh.workflow.service;

import com.arsh.workflow.dto.CreateTaskRequest;
import com.arsh.workflow.dto.CreateWorkflowRequest;
import com.arsh.workflow.dto.TaskResponse;
import com.arsh.workflow.dto.WorkflowResponse;
import com.arsh.workflow.model.Workflow;

import java.util.List;

public interface WorkflowService {
    WorkflowResponse createWorkflow(CreateWorkflowRequest req);
    WorkflowResponse getWorkflow(Long workflowId);
    WorkflowResponse deleteWorkflow(Long workflowId);
    TaskResponse deleteTask(Long workflowId, Long taskId);
    TaskResponse addTask(Long workflowId, CreateTaskRequest req);
    WorkflowResponse startWorkflow(Long workflowId);
    WorkflowResponse completeWorkflow(Long workflowId);
    List<WorkflowResponse> getAllWorkflows();
}
