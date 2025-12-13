package com.arsh.workflow.controller;

import com.arsh.workflow.dto.response.TaskResponse;
import com.arsh.workflow.enums.TaskStatus;
import com.arsh.workflow.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/task")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PutMapping("/{taskId}/assign/{userId}")
    public TaskResponse assignTask(@PathVariable Long taskId, @PathVariable Long userId) {
        return taskService.assignTask(taskId, userId);
    }

    @PutMapping("/{taskId}/status")
    public TaskResponse changeStatus(@PathVariable Long taskId, @RequestParam TaskStatus status) {
        return taskService.changeStatus(taskId, status);
    }

    @GetMapping("/{taskId}")
    public TaskResponse getTask(@PathVariable Long taskId) {
        return taskService.getTask(taskId);
    }
}
