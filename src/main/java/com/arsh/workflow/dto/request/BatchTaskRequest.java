package com.arsh.workflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BatchTaskRequest {

    @NotBlank
    private String clientId;      // ✅ TECHNICAL ID (alias)

    @NotBlank
    private String title;         // ✅ UI LABEL

    private String description;

    private List<String> dependsOn;   // ✅ list of clientIds
}
