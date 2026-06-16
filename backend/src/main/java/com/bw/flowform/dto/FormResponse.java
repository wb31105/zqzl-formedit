package com.bw.flowform.dto;

import com.bw.flowform.entity.FormField;
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
