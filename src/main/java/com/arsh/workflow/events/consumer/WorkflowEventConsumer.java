package com.arsh.workflow.events.consumer;


import com.arsh.workflow.events.WorkflowEvent;
import com.arsh.workflow.events.idempotency.ProcessedEvent;
import com.arsh.workflow.repository.ProcessedEventRepository;
import com.arsh.workflow.service.WorkflowCoordinator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class WorkflowEventConsumer {

    private final WorkflowCoordinator workflowCoordinator;
    private final ProcessedEventRepository processedEventRepository;

    @KafkaListener(
            topics = "workflow-events",
            groupId = "workflow-executor-consumer"
    )
    @Transactional
    public void consume(WorkflowEvent event) {

        if (processedEventRepository.existsById(event.getEventId())) {
            log.warn("Duplicate event ignored {}", event.getEventId());
            return;
        }

        switch (event.getEventType()) {

            case TASK_STARTED ->
                    log.info("Task {} started", event.getTaskId());

            case TASK_COMPLETED ->
                    workflowCoordinator.onTaskCompleted(event.getTaskId());

            case TASK_FAILED ->
                    workflowCoordinator.onTaskFailed(event.getTaskId());

            default ->
                    log.warn("Ignoring event type {}", event.getEventType());
        }

        processedEventRepository.save(
                new ProcessedEvent(event.getEventId())
        );
    }
}
