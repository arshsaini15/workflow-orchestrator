package com.arsh.workflow.repository;

import com.arsh.workflow.model.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
    Page<Task> findByWorkflow_Id(Long workflowId, Pageable pageable);
}
