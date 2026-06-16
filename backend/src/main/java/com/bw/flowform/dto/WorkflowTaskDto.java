package com.bw.flowform.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class WorkflowTaskDto {

    private Long taskId;

    @NotBlank(message = "操作类型不能为空")
    private String action;

    private String comment;

    private String assignee;
}
