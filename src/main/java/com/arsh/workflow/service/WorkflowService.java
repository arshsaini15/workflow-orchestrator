package com.arsh.workflow.service;

import com.arsh.workflow.dto.CreateTaskRequest;
import com.arsh.workflow.dto.CreateWorkflowRequest;
import com.arsh.workflow.dto.TaskResponse;
import com.arsh.workflow.dto.WorkflowResponse;
import org.springframework.data.domain.Page;


public interface WorkflowService {
    WorkflowResponse createWorkflow(CreateWorkflowRequest req);
    WorkflowResponse getWorkflow(Long workflowId);
    WorkflowResponse deleteWorkflow(Long workflowId);
    TaskResponse deleteTask(Long workflowId, Long taskId);
    TaskResponse addTask(Long workflowId, CreateTaskRequest req);
    WorkflowResponse startWorkflow(Long workflowId);
    WorkflowResponse completeWorkflow(Long workflowId);
    Page<WorkflowResponse> getAllWorkflows(int page, int size);
}
