package com.formedit.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

@Data
public class WorkflowDefinitionDto {

    private Long id;

    @NotBlank(message = "流程名称不能为空")
    private String name;

    private String description;

    private Long formId;

    private String formName;

    private List<Node> nodes;

    private List<Edge> edges;

    @Data
    public static class Node {
        private String id;
        private String type;
        private String name;
        private double x;
        private double y;
        private Map<String, Object> properties;
    }

    @Data
    public static class Edge {
        private String id;
        private String source;
        private String target;
        private String label;
    }
}
