package com.bw.flowform.dto;

import lombok.Data;
import java.util.Map;

@Data
public class ValidationResult {
    private boolean valid;
    private Map<String, String> errors;
}
