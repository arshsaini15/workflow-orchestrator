package com.arsh.workflow.service;

import com.arsh.workflow.dto.response.TaskResponse;
import com.arsh.workflow.enums.TaskStatus;

public interface TaskService {
    TaskResponse assignTask(Long taskId, Long userId);
    TaskResponse changeStatus(Long taskId, TaskStatus status);
    TaskResponse getTask(Long taskId);
    Long getWorkflowId(Long taskId);
}
