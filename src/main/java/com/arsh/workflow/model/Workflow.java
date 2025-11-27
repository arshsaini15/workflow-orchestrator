package com.arsh.workflow.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Workflow extends BaseAuditingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private WorkflowStatus status;

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL)
    private List<Task> tasks = new ArrayList<>();
}
