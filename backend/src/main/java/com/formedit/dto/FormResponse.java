package com.formedit.dto;

import com.formedit.entity.FormField;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class FormResponse {
    private Long id;
    private String name;
    private String description;
    private List<FormField> fields;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
