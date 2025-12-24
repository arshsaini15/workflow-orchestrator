package com.arsh.workflow.model;

import com.arsh.workflow.enums.WorkflowStatus;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "workflows")
public class Workflow extends BaseAuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkflowStatus status;



    @OneToMany(
            mappedBy = "workflow",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Task> tasks = new ArrayList<>();

    public Workflow() {
        // JPA only
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public WorkflowStatus getStatus() {
        return status;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStatus(WorkflowStatus status) {
        this.status = status;
    }

    // ===== Relationship management =====

    public void addTask(Task task) {
        if (task == null) return;

        tasks.add(task);
        task.setWorkflow(this);
    }

    public void removeTask(Task task) {
        if (task == null) return;

        tasks.remove(task);
        task.setWorkflow(null);
    }
}
