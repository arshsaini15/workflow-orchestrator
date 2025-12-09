package com.arsh.workflow.exception;

import java.util.List;

public class InvalidWorkflowDefinitionException extends RuntimeException {
    private final List<String> errors;

    public InvalidWorkflowDefinitionException(List<String> errors) {
        super(String.join("; ", errors));  // single message for logs
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
