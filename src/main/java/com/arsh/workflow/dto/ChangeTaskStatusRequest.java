package com.arsh.workflow.dto;

import com.arsh.workflow.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeTaskStatusRequest {
    @NotNull
    private TaskStatus status;
}
