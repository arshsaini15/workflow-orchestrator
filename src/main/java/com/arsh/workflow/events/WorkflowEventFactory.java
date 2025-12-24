package com.arsh.workflow.events;

import com.arsh.workflow.enums.EventType;
import com.arsh.workflow.enums.TaskStatus;
import com.arsh.workflow.enums.WorkflowStatus;

import java.time.Instant;
import java.util.UUID;

public final class WorkflowEventFactory {

    private static final String SOURCE = "TASK_SERVICE";
    private static final int VERSION = 1;

    private WorkflowEventFactory() {}

    public static WorkflowEvent fromTaskStatusChange(
            Long workflowId,
            Long taskId,
            TaskStatus from,
            TaskStatus to
    ) {

        return WorkflowEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(resolveEventType(from, to))
                .workflowId(workflowId)
                .taskId(taskId)
                .status(resolveWorkflowStatus(to))
                .source(SOURCE)
                .occurredAt(Instant.now())
                .version(VERSION)
                .build();
    }

    private static EventType resolveEventType(TaskStatus from, TaskStatus to) {

        if (from == TaskStatus.READY && to == TaskStatus.IN_PROGRESS) {
            return EventType.TASK_STARTED;
        }

        if (from == TaskStatus.IN_PROGRESS && to == TaskStatus.COMPLETED) {
            return EventType.TASK_COMPLETED;
        }

        if (from == TaskStatus.IN_PROGRESS && to == TaskStatus.FAILED) {
            return EventType.TASK_FAILED;
        }

        throw new IllegalArgumentException(
                "No event mapping for transition " + from + " → " + to
        );
    }

    private static WorkflowStatus resolveWorkflowStatus(TaskStatus to) {

        return switch (to) {
            case IN_PROGRESS, COMPLETED -> WorkflowStatus.RUNNING;
            case FAILED -> WorkflowStatus.FAILED;
            default -> WorkflowStatus.RUNNING;
        };
    }
}
