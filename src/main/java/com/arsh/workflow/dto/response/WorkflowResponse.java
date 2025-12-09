package com.arsh.workflow.dto.response;

import com.arsh.workflow.enums.WorkflowStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class WorkflowResponse {

    private Long id;
    private String name;
    private WorkflowStatus status;

    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;

    private List<TaskResponse> tasks = new ArrayList<>();
}
