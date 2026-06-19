package com.bw.flowform.exception;

import com.bw.flowform.common.ErrorCode;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ResourceNotFoundException(ErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }
}
