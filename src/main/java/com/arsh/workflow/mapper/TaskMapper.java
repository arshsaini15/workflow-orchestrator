package com.arsh.workflow.mapper;

import com.arsh.workflow.dto.request.CreateTaskRequest;
import com.arsh.workflow.dto.response.TaskResponse;
import com.arsh.workflow.model.Task;

public class TaskMapper {

    public static Task toEntity(CreateTaskRequest dto) {
        Task task = new Task();
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());

        return task;
    }


    public static TaskResponse toResponse(Task task) {
        TaskResponse res = new TaskResponse();

        res.setId(task.getId());
        res.setTitle(task.getTitle());
        res.setDescription(task.getDescription());
        res.setStatus(task.getStatus());

        // assignedTo (safe check)
        if (task.getAssignedTo() != null) {
            res.setAssignedToId(task.getAssignedTo().getId());
            res.setAssignedToName(task.getAssignedTo().getUsername());
        } else {
            res.setAssignedToId(null);
            res.setAssignedToName(null);
        }

        if (task.getWorkflow() != null) {
            res.setWorkflowId(task.getWorkflow().getId());
        } else {
            res.setWorkflowId(null);
        }

        return res;
    }

}
