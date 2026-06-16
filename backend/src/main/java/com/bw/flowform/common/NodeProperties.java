package com.bw.flowform.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NodeProperties {

    public static final String COUNTERSIGN_TYPE = "countersignType";
    public static final String ACTION_TYPE = "actionType";
    public static final String APPROVER = "approver";
    public static final String APPROVERS = "approvers";
    public static final String APPROVE_TEXT = "approveText";
    public static final String REJECT_TEXT = "rejectText";
    public static final String COMMENT_LABEL = "commentLabel";
    public static final String DESCRIPTION = "description";
    public static final String EXPRESSION = "expression";
    public static final String TASK_TYPE = "taskType";
    public static final String CONFIG = "config";

    private NodeProperties() {
    }

    public static String getCountersignType(Map<String, Object> props) {
        Object value = props != null ? props.get(COUNTERSIGN_TYPE) : null;
        return value != null ? value.toString() : "all";
    }

    public static String getActionType(Map<String, Object> props) {
        Object value = props != null ? props.get(ACTION_TYPE) : null;
        return value != null ? value.toString() : null;
    }

    public static String getApprover(Map<String, Object> props) {
        Object value = props != null ? props.get(APPROVER) : null;
        return value != null ? value.toString() : null;
    }

    public static List<String> getApprovers(Map<String, Object> props) {
        Object value = props != null ? props.get(APPROVERS) : null;
        if (value == null) {
            return Collections.emptyList();
        }
        String str = value.toString().trim();
        if (str.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(str.split("[,，、;；\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public static String getExpression(Map<String, Object> props) {
        Object value = props != null ? props.get(EXPRESSION) : null;
        return value != null ? value.toString() : null;
    }

    public static String getApproveText(Map<String, Object> props) {
        Object value = props != null ? props.get(APPROVE_TEXT) : null;
        return value != null ? value.toString() : "通过";
    }

    public static String getRejectText(Map<String, Object> props) {
        Object value = props != null ? props.get(REJECT_TEXT) : null;
        return value != null ? value.toString() : "拒绝";
    }

    public static String getCommentLabel(Map<String, Object> props) {
        Object value = props != null ? props.get(COMMENT_LABEL) : null;
        return value != null ? value.toString() : null;
    }

    public static String getDescription(Map<String, Object> props) {
        Object value = props != null ? props.get(DESCRIPTION) : null;
        return value != null ? value.toString() : null;
    }

    public static String getTaskType(Map<String, Object> props) {
        Object value = props != null ? props.get(TASK_TYPE) : null;
        return value != null ? value.toString() : null;
    }

    public static Object getConfig(Map<String, Object> props) {
        return props != null ? props.get(CONFIG) : null;
    }
}
