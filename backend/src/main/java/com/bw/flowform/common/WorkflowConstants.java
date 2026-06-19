package com.bw.flowform.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowConstants {

    private static final Logger log = LoggerFactory.getLogger(WorkflowConstants.class);

    private WorkflowConstants() {
    }

    public static final String DEFAULT_ASSIGNEE = "系统管理员";

    public static final String DEFAULT_APPROVER_1 = "审批人1";
    public static final String DEFAULT_APPROVER_2 = "审批人2";

    public static final String ACTION_APPROVE_DISPLAY = "批准";
    public static final String ACTION_REJECT_DISPLAY = "拒绝";
    public static final String ACTION_PASS_DISPLAY = "通过";

    public static final String LOG_ACTION_FLOW_START = "流程启动";
    public static final String LOG_ACTION_ENTER_NODE = "进入节点";
    public static final String LOG_ACTION_FLOW_END = "流程结束";
    public static final String LOG_ACTION_WAIT_APPROVAL = "等待审批";
    public static final String LOG_ACTION_WAIT_COUNTERSIGN = "等待会签";
    public static final String LOG_ACTION_EXECUTE_AUTO = "执行自动任务";
    public static final String LOG_ACTION_AUTO_DONE = "自动任务完成";
    public static final String LOG_ACTION_CONDITION_EVAL = "条件判断";
    public static final String LOG_ACTION_ROUTE_FAILED = "路由失败";
    public static final String LOG_ACTION_EXPRESSION_ERROR = "表达式解析失败";
    public static final String LOG_ACTION_WAIT_OTHERS = "等待其他审批人";

    public static final String LOG_COMMENT_COUNTERSIGN_RESULT = "会签结果: ";
    public static final String LOG_COMMENT_COUNTERSIGN_REMAINING = "剩余 ";
    public static final String LOG_COMMENT_COUNTERSIGN_REMAINING_SUFFIX = " 人未审批";
    public static final String LOG_COMMENT_APPROVERS_PREFIX = "审批人: ";
    public static final String LOG_COMMENT_RULE_PREFIX = "，规则: ";
    public static final String LOG_COMMENT_CONDITION_RESULT = "条件判断结果: ";

    public static final String CONDITION_YES = "是";
    public static final String CONDITION_NO = "否";

    public static final String TASK_CANCEL_COMMENT = "会签已结束，任务自动取消";

    public static final String FIELD_TYPE_TEXT = "text";
    public static final String FIELD_TYPE_TEXTAREA = "textarea";
    public static final String FIELD_TYPE_EMAIL = "email";
    public static final String FIELD_TYPE_NUMBER = "number";
    public static final String FIELD_TYPE_SELECT = "select";
    public static final String FIELD_TYPE_RADIO = "radio";
    public static final String FIELD_TYPE_CHECKBOX = "checkbox";
    public static final String FIELD_TYPE_DATE = "date";

    public static boolean isTextFieldType(String type) {
        return FIELD_TYPE_TEXT.equals(type)
                || FIELD_TYPE_TEXTAREA.equals(type)
                || FIELD_TYPE_EMAIL.equals(type)
                || FIELD_TYPE_NUMBER.equals(type);
    }
}
