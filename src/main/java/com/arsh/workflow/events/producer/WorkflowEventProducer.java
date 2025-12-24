package com.arsh.workflow.events.producer;

import com.arsh.workflow.events.WorkflowEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkflowEventProducer {

    private final KafkaTemplate<Long, WorkflowEvent> kafkaTemplate;
    private static final String TOPIC = "workflow-events";

    public void publish(Long workflowId, WorkflowEvent event) {
        kafkaTemplate.send(TOPIC, workflowId, event);
    }
}
