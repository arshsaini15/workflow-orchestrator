package com.arsh.workflow.model;

import com.arsh.workflow.enums.TaskStatus;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tasks")
public class Task extends BaseAuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id")
    private User assignedTo;

    // ---- Workflow ownership (MANDATORY) ----
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    // ---- DAG relationships ----

    // Parents (tasks this task depends on)
    @ManyToMany
    @JoinTable(
            name = "task_dependencies",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "depends_on_task_id")
    )
    private List<Task> dependsOn = new ArrayList<>();

    // Children (tasks depending on this task)
    @ManyToMany(mappedBy = "dependsOn")
    private List<Task> dependents = new ArrayList<>();

    public Task() {
        // JPA only
    }

    // ---------- Getters ----------

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public User getAssignedTo() {
        return assignedTo;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public List<Task> getDependsOn() {
        return dependsOn;
    }

    public List<Task> getDependents() {
        return dependents;
    }

    // ---------- Setters ----------

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public void setAssignedTo(User assignedTo) {
        this.assignedTo = assignedTo;
    }

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    // ---------- DAG helpers (USE THESE) ----------

    public void addDependency(Task parent) {
        if (parent == null) {
            throw new IllegalArgumentException("Parent task cannot be null");
        }
        if (parent == this) {
            throw new IllegalStateException("Task cannot depend on itself");
        }
        this.dependsOn.add(parent);
    }

    public void clearDependencies() {
        this.dependsOn.clear();
    }
}
