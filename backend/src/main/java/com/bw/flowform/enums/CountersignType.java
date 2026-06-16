package com.bw.flowform.enums;

public enum CountersignType {
    VETO("veto", "一票否决"),
    MAJORITY("majority", "过半通过"),
    ALL("all", "全部同意才通过");

    private final String code;
    private final String description;

    CountersignType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static CountersignType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (CountersignType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
