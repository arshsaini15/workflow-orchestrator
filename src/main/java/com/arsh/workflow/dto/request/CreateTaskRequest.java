package com.arsh.workflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateTaskRequest {

    @NotBlank
    private String alias;      // ✅ TECHNICAL ID (used in dependsOn)

    @NotBlank
    private String title;      // ✅ UI LABEL (optional but useful)

    private String description;

    private List<String> dependsOn;
}
