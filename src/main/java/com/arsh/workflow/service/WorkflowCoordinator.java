package com.arsh.workflow.service;

import com.sun.source.util.TaskEvent;

public interface WorkflowCoordinator {
    void onTaskCompleted(Long taskId);
    void onTaskFailed(Long taskId);
}
