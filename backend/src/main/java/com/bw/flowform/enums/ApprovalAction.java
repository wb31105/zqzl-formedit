package com.bw.flowform.enums;

public enum ApprovalAction {
    APPROVE("approve"),
    REJECT("reject");

    private final String code;

    ApprovalAction(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static ApprovalAction fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ApprovalAction action : values()) {
            if (action.getCode().equals(code)) {
                return action;
            }
        }
        return null;
    }
}
