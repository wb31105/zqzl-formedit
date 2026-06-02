package com.formedit.dto;

import com.formedit.entity.FormField;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class FormDto {
    private Long id;

    @NotBlank(message = "表单名称不能为空")
    private String name;

    private String description;

    private List<FormField> fields;
}
