package com.arsh.workflow.repository;

import com.arsh.workflow.events.idempotency.ProcessedEvent;

public interface ProcessedEventRepository
        extends JpaRepository<ProcessedEvent, String> {
}
