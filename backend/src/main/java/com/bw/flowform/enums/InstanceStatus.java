package com.bw.flowform.enums;

public enum InstanceStatus {
    RUNNING("RUNNING"),
    COMPLETED("COMPLETED"),
    ERROR("ERROR");

    private final String code;

    InstanceStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static InstanceStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (InstanceStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
