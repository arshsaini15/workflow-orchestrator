package com.arsh.workflow.service;

import com.arsh.workflow.dto.*;
import com.arsh.workflow.enums.TaskStatus;
import com.arsh.workflow.enums.WorkflowStatus;
import org.springframework.data.domain.Pageable;


public interface WorkflowService {
    WorkflowResponse createWorkflow(CreateWorkflowRequest req);
    WorkflowResponse getWorkflow(Long workflowId);
    WorkflowResponse deleteWorkflow(Long workflowId);
    TaskResponse deleteTask(Long workflowId, Long taskId);
    TaskResponse addTask(Long workflowId, CreateTaskRequest req);
    WorkflowResponse startWorkflow(Long workflowId);
    WorkflowResponse completeWorkflow(Long workflowId);


    PaginatedResponse<WorkflowResponse> getAllWorkflows(
            WorkflowStatus status,
            String createdBy,
            String search,
            Pageable pageable
    );

    PaginatedResponse<TaskResponse> getTasksForWorkflow(
            Long workflowId,
            TaskStatus status,
            String search,
            Pageable pageable);

}
