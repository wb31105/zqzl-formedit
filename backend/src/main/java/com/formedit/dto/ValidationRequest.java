package com.formedit.dto;

import lombok.Data;
import java.util.Map;

@Data
public class ValidationRequest {
    private Long formId;
    private Map<String, Object> data;
}
