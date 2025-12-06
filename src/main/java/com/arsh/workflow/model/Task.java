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


    @ManyToMany
    @JoinTable(name = "task_dependencies")
    private List<Task> dependsOn;

    public List<Task> getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(List<Task> dependsOn) {
        this.dependsOn = dependsOn;
    }

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
