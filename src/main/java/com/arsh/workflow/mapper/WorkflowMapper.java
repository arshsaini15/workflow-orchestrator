package com.arsh.workflow.mapper;

import com.arsh.workflow.dto.CreateWorkflowRequest;
import com.arsh.workflow.dto.WorkflowResponse;
import com.arsh.workflow.model.Workflow;

public class WorkflowMapper {
    public static Workflow toEntity(CreateWorkflowRequest req) {
        Workflow workflow = new Workflow();
        workflow.setName(req.getName());
        return workflow;
    }

    public static WorkflowResponse toResponse(Workflow workflow) {
        WorkflowResponse res = new WorkflowResponse();

        res.setId(workflow.getId());
        res.setName(workflow.getName());
        res.setStatus(workflow.getStatus());

        res.setCreatedAt(workflow.getCreatedAt());
        res.setCreatedBy(workflow.getCreatedBy());
        res.setUpdatedAt(workflow.getUpdatedAt());
        res.setUpdatedBy(workflow.getUpdatedBy());

        res.setTasks(
                workflow.getTasks().stream()
                        .map(TaskMapper::toResponse)
                        .toList()
        );

        return res;
    }
}
