package com.formedit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowValidationResult {

    private boolean valid;
    private List<String> errors = new ArrayList<>();

    public static WorkflowValidationResult success() {
        return new WorkflowValidationResult(true, new ArrayList<>());
    }

    public static WorkflowValidationResult failure(List<String> errors) {
        return new WorkflowValidationResult(false, errors);
    }

    public void addError(String error) {
        this.errors.add(error);
        this.valid = false;
    }
}
