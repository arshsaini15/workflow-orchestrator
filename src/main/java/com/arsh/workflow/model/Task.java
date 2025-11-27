package com.arsh.workflow.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
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
}
