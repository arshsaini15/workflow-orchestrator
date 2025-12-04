package com.arsh.workflow.repository;

import com.arsh.workflow.enums.WorkflowStatus;
import com.arsh.workflow.model.Workflow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface WorkflowRepository extends JpaRepository<Workflow, Long>, JpaSpecificationExecutor<Workflow> {
    Page<Workflow> findByStatus(WorkflowStatus status, Pageable pageable);
    Page<Workflow> findByCreatedBy(String createdBy, Pageable pageable);
    Page<Workflow> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
