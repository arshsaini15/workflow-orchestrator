package com.arsh.workflow.service;

import com.arsh.workflow.dto.request.BatchTaskRequest;
import com.arsh.workflow.dto.response.TaskResponse;

import java.util.List;

public interface WorkflowDagService {

    List<TaskResponse> createBatchDag(Long workflowId,
                                      List<BatchTaskRequest> batch);
}
