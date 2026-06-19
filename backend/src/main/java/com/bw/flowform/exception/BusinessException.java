package com.bw.flowform.exception;

import com.bw.flowform.common.ErrorCode;

public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String detailMessage;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detailMessage = null;
    }

    public BusinessException(ErrorCode errorCode, String detailMessage) {
        super(detailMessage != null ? detailMessage : errorCode.getMessage());
        this.errorCode = errorCode;
        this.detailMessage = detailMessage;
    }

    public BusinessException(ErrorCode errorCode, String detailMessage, Throwable cause) {
        super(detailMessage != null ? detailMessage : errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.detailMessage = detailMessage;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getDetailMessage() {
        return detailMessage;
    }

    public String getDisplayMessage() {
        if (detailMessage != null && !detailMessage.isEmpty()) {
            return errorCode.getMessage() + ": " + detailMessage;
        }
        return errorCode.getMessage();
    }
}
