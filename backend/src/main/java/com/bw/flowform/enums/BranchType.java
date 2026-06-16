package com.bw.flowform.enums;

public enum BranchType {
    APPROVE("approve"),
    REJECT("reject");

    private final String code;

    BranchType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static BranchType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (BranchType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
