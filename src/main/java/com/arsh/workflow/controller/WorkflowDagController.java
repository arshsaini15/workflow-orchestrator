package com.arsh.workflow.controller;

import com.arsh.workflow.dto.request.BatchTaskRequest;
import com.arsh.workflow.dto.response.TaskResponse;
import com.arsh.workflow.service.WorkflowDagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/workflow")
public class WorkflowDagController {

    private final WorkflowDagService workflowDagService;

    public WorkflowDagController(WorkflowDagService workflowDagService) {
        this.workflowDagService = workflowDagService;
    }

    @PostMapping("/{workflowId}/tasks/batch")
    public ResponseEntity<List<TaskResponse>> createDagBatch(@PathVariable Long workflowId,
                                                             @RequestBody List<BatchTaskRequest> batch)
    {

        List<TaskResponse> response = workflowDagService.createBatchDag(workflowId, batch);
        return ResponseEntity.ok(response);
    }
}
