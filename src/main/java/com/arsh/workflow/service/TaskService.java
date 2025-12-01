package com.arsh.workflow.service;

import com.arsh.workflow.dto.TaskResponse;
import com.arsh.workflow.model.Task;
import com.arsh.workflow.enums.TaskStatus;

public interface TaskService {
    TaskResponse assignTask(Long taskId, Long userId);
    TaskResponse changeStatus(Long taskId, TaskStatus status);
    TaskResponse getTask(Long taskId);
}
