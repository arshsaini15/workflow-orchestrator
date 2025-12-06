package com.arsh.workflow.repository;

import com.arsh.workflow.enums.TaskStatus;
import com.arsh.workflow.model.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Arrays;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
    Page<Task> findByWorkflowId(Long workflowId, Pageable pageable);
    List<Task> findByWorkflowIdAndStatus(Long workflowId, TaskStatus status);
    List<Task> findByWorkflowId(Long workflowId);

    List<Task> findByWorkflowIdAndStatusIn(Long workflowId, List<TaskStatus> pending);
}
