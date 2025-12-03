package com.arsh.workflow.service;

public interface WorkflowExecutorService {
    void executeWorkflow(Long workflowId);
    void runTask(Long taskId);
    void triggerNextTasks(Long taskId);
}
