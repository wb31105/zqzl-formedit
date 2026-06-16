package com.bw.flowform.dto;

import lombok.Data;
import java.util.Map;

@Data
public class ValidationRequest {
    private Long formId;
    private Map<String, Object> data;
}
