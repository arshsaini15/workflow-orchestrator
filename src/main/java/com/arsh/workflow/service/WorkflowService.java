package com.arsh.workflow.service;

import com.arsh.workflow.dto.request.CreateTaskRequest;
import com.arsh.workflow.dto.request.CreateWorkflowRequest;
import com.arsh.workflow.dto.response.TaskResponse;
import com.arsh.workflow.dto.response.WorkflowResponse;
public interface WorkflowService {
    WorkflowResponse createWorkflow(CreateWorkflowRequest req);
    WorkflowResponse getWorkflow(Long workflowId);
    WorkflowResponse deleteWorkflow(Long workflowId);
    TaskResponse addTask(Long workflowId, CreateTaskRequest req);
    WorkflowResponse startWorkflow(Long workflowId);
}
