package com.arsh.workflow.repository;

import com.arsh.workflow.model.Workflow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowRepository extends JpaRepository<Workflow, Long> {
    Page<Workflow> findByStatus(String status, Pageable pageable);
    Page<Workflow> findByCreatedBy(String createdBy, Pageable pageable);
    Page<Workflow> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
