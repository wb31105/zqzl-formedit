package com.bw.flowform.common;

public enum ErrorCode {

    SUCCESS(0, "成功"),

    BAD_REQUEST(40000, "请求参数错误"),
    VALIDATION_FAILED(40001, "校验失败"),
    FORM_NAME_DUPLICATE(40002, "表单名称已存在"),
    WORKFLOW_NAME_DUPLICATE(40003, "流程名称已存在"),
    WORKFLOW_VALIDATION_FAILED(40004, "流程定义校验失败"),
    FORM_VALIDATION_FAILED(40005, "表单校验失败"),
    INSTANCE_NOT_RUNNING(40006, "流程实例不是运行状态"),
    TASK_NOT_PENDING(40007, "任务不是待处理状态"),
    TASK_NOT_BELONG_TO_INSTANCE(40008, "任务不属于该流程实例"),
    INVALID_START_PARAMS(40009, "启动参数错误"),
    EXPRESSION_ERROR(40010, "表达式错误"),

    NOT_FOUND(40400, "资源不存在"),
    FORM_NOT_FOUND(40401, "表单不存在"),
    WORKFLOW_DEFINITION_NOT_FOUND(40402, "流程定义不存在"),
    WORKFLOW_INSTANCE_NOT_FOUND(40403, "流程实例不存在"),
    TASK_NOT_FOUND(40404, "任务不存在"),
    NODE_NOT_FOUND(40405, "节点不存在"),

    SYSTEM_ERROR(50000, "系统异常，请稍后重试"),
    DATABASE_ERROR(50001, "数据库操作异常"),
    INTERNAL_ERROR(50002, "服务器内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public int getHttpStatus() {
        return code / 100;
    }
}
