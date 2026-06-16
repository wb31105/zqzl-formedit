package com.bw.flowform.enums;

public enum NodeType {
    START("start", "开始节点"),
    APPROVAL("approval", "审批节点"),
    COUNTERSIGN("countersign", "会签节点"),
    CONDITION("condition", "条件分支节点"),
    AUTO("auto", "自动任务节点"),
    END("end", "结束节点");

    private final String code;
    private final String description;

    NodeType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static NodeType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (NodeType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
