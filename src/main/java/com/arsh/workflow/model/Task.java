package com.arsh.workflow.model;

import com.arsh.workflow.enums.TaskStatus;
import jakarta.persistence.*;

import java.util.List;

@Entity
public class Task extends BaseAuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    @ManyToOne
    private User assignedTo;

    @ManyToOne
    @JoinColumn(name = "workflow_id")
    private Workflow workflow;

    // PARENTS: tasks this task depends on
    @ManyToMany
    @JoinTable(
            name = "task_dependencies",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "depends_on_task_id")
    )
    private List<Task> dependsOn;

    // CHILDREN: tasks that depend on THIS task
    @ManyToMany(mappedBy = "dependsOn")
    private List<Task> dependents;

    public Task() {}

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

    public void setDependsOn(List<Task> dependsOn) {
        this.dependsOn = dependsOn;
    }

    public List<Task> getDependents() {
        return dependents;
    }

    public void setDependents(List<Task> dependents) {
        this.dependents = dependents;
    }

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

    void setWorkflowInternal(Workflow workflow) {
        this.workflow = workflow;
    }
}
