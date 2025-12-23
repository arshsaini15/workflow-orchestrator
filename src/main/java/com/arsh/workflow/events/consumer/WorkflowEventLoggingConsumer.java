package com.arsh.workflow.events.consumer;

import com.arsh.workflow.events.WorkflowEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WorkflowEventLoggingConsumer {

    @KafkaListener(
            topics = "workflow-events",
            groupId = "workflow-logging-consumer"
    )
    public void consume(ConsumerRecord<Long, WorkflowEvent> record) {

        Long key = record.key();
        WorkflowEvent event = record.value();

        log.info(
                """
                🔔 Kafka Event Received
                ──────────────────────
                Partition : {}
                Offset    : {}
                Key       : {}
                EventType : {}
                Workflow  : {}
                Task      : {}
                Status    : {}
                EventId   : {}
                Version   : {}
                Time      : {}
                """,
                record.partition(),
                record.offset(),
                key,
                event.getEventType(),
                event.getWorkflowId(),
                event.getTaskId(),
                event.getStatus(),
                event.getEventId(),
                event.getVersion(),
                event.getOccurredAt()
        );
    }
}
