package com.arsh.workflow.dto;

import com.arsh.workflow.enums.TaskStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TaskResponse {

    private Long id;
    private String title;
    private String description;
    private TaskStatus status;

    private Long assignedToId;
    private String assignedToName;

    private Long workflowId;
}
