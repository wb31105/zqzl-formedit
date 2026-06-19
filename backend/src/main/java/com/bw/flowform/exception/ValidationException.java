package com.bw.flowform.exception;

import com.bw.flowform.common.ErrorCode;

import java.util.Collections;
import java.util.Map;

public class ValidationException extends BusinessException {

    private final Map<String, String> fieldErrors;

    public ValidationException(ErrorCode errorCode) {
        super(errorCode);
        this.fieldErrors = Collections.emptyMap();
    }

    public ValidationException(ErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
        this.fieldErrors = Collections.emptyMap();
    }

    public ValidationException(ErrorCode errorCode, String detailMessage, Map<String, String> fieldErrors) {
        super(errorCode, detailMessage);
        this.fieldErrors = fieldErrors != null ? fieldErrors : Collections.emptyMap();
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
