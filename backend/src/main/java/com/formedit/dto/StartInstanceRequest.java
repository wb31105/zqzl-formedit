package com.formedit.dto;

import lombok.Data;
import java.util.Map;

@Data
public class StartInstanceRequest {
    private Long formId;
    private Map<String, Object> formData;
}
