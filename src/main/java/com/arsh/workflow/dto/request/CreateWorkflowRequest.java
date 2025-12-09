package com.arsh.workflow.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateWorkflowRequest {

    @NotBlank
    private String name;

    @NotEmpty
    @Valid
    private List<BatchTaskRequest> tasks;
}
