package com.arsh.workflow.repository;

import com.arsh.workflow.model.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WorkflowRepository extends JpaRepository<Workflow, Long> {

    @Query("""
        select distinct w
        from Workflow w
        left join fetch w.tasks
        where w.id = :id
    """)
    Optional<Workflow> findByIdWithTasks(@Param("id") Long id);
}
