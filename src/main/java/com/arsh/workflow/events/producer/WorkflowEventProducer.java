package com.arsh.workflow.events.producer;

import com.arsh.workflow.events.WorkflowEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WorkflowEventProducer {

    private static final String TOPIC = "workflow-events";

    private final KafkaTemplate<Long, WorkflowEvent> kafkaTemplate;

    public WorkflowEventProducer(KafkaTemplate<Long, WorkflowEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(WorkflowEvent event) {
        try {
            kafkaTemplate.send(
                    TOPIC,
                    event.getWorkflowId(), // partition key
                    event
            );
        } catch (Exception e) {
            log.error("Failed to publish workflow event: {}", event, e);
            throw e;
        }
    }
}
