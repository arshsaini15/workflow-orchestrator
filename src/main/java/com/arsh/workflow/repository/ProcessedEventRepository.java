package com.arsh.workflow.repository;

import com.arsh.workflow.events.idempotency.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
        boolean existsById(String eventId);
}
