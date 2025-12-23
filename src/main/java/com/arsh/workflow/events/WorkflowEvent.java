package com.arsh.workflow.events;

import com.arsh.workflow.enums.EventType;
import com.arsh.workflow.enums.WorkflowStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowEvent {

    // For idempotency (MANDATORY)
    private String eventId;

    // What happened (MANDATORY)
    private EventType eventType;

    // Partition key (MANDATORY)
    private Long workflowId;

    // Only for task events
    private Long taskId;

    // Resulting state
    private WorkflowStatus status;

    // Which service produced this
    private String source;

    // Event time (not processing time)
    private Instant occurredAt;

    // Optional extra data
    private Map<String, Object> payload;

    // Schema evolution
    private int version;
}
