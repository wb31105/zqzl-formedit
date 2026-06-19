package com.bw.flowform.engine.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import static com.bw.flowform.utils.JsonUtils.*;
import com.bw.flowform.common.NodeProperties;
import com.bw.flowform.common.WorkflowConstants;
import com.bw.flowform.dto.WorkflowDefinitionDto;
import com.bw.flowform.engine.FlowHelper;
import com.bw.flowform.engine.NodeHandler;
import com.bw.flowform.engine.NodeHandlerContext;
import com.bw.flowform.engine.NodeHandlerResult;
import com.bw.flowform.entity.WorkflowExecutionLog;
import com.bw.flowform.entity.WorkflowInstance;
import com.bw.flowform.enums.ApprovalAction;
import com.bw.flowform.enums.NodeType;
import com.bw.flowform.service.ExpressionEvaluator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ConditionNodeHandler implements NodeHandler {

    private final FlowHelper flowHelper;

    public ConditionNodeHandler(FlowHelper flowHelper) {
        this.flowHelper = flowHelper;
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.CONDITION;
    }

    @Override
    public NodeHandlerResult handle(NodeHandlerContext context) {
        WorkflowDefinitionDto.Node node = context.getNode();

        flowHelper.addExecutionLog(
                context.getInstanceId(),
                node.getId(),
                node.getName(),
                node.getType(),
                WorkflowConstants.LOG_ACTION_CONDITION_EVAL,
                null
        );

        String conditionAction = resolveConditionExpression(node, context.getInstance());
        if (conditionAction == null) {
            return NodeHandlerResult.complete();
        }

        String conditionDisplay = ApprovalAction.APPROVE.getCode().equals(conditionAction)
                ? WorkflowConstants.CONDITION_YES
                : WorkflowConstants.CONDITION_NO;

        flowHelper.addExecutionLog(
                context.getInstanceId(),
                node.getId(),
                node.getName(),
                node.getType(),
                conditionAction,
                WorkflowConstants.LOG_COMMENT_CONDITION_RESULT + conditionDisplay
        );

        return NodeHandlerResult.proceed(conditionAction);
    }

    private String resolveConditionExpression(WorkflowDefinitionDto.Node conditionNode, WorkflowInstance instance) {
        String expression = NodeProperties.getExpression(conditionNode.getProperties());
        if (expression != null && !expression.trim().isEmpty()) {
            Map<String, Object> context = getInstanceContext(instance);
            try {
                boolean result = ExpressionEvaluator.evaluate(expression, context);
                return result ? ApprovalAction.APPROVE.getCode() : ApprovalAction.REJECT.getCode();
            } catch (Exception e) {
                String errMsg = "条件节点 \"" + conditionNode.getName()
                        + "\" 表达式解析失败: " + e.getMessage()
                        + "，表达式=\"" + expression + "\"";
                flowHelper.addExecutionLog(
                        instance.getId(),
                        conditionNode.getId(),
                        conditionNode.getName(),
                        NodeType.CONDITION.getCode(),
                        WorkflowConstants.LOG_ACTION_EXPRESSION_ERROR,
                        errMsg
                );
                flowHelper.failInstance(instance, errMsg);
                return null;
            }
        }

        String lastApprovalAction = getLastApprovalAction(instance.getId());
        return lastApprovalAction != null ? lastApprovalAction : ApprovalAction.APPROVE.getCode();
    }

    private String getLastApprovalAction(Long instanceId) {
        List<WorkflowExecutionLog> logs =
                flowHelper.getExecutionLogRepository().findByInstanceIdOrderByIdDesc(instanceId);
        for (WorkflowExecutionLog log : logs) {
            NodeType nodeType = NodeType.fromCode(log.getNodeType());
            if (nodeType == NodeType.APPROVAL || nodeType == NodeType.COUNTERSIGN) {
                if (ApprovalAction.APPROVE.getCode().equals(log.getAction())) {
                    return ApprovalAction.APPROVE.getCode();
                } else if (ApprovalAction.REJECT.getCode().equals(log.getAction())) {
                    return ApprovalAction.REJECT.getCode();
                }
            }
        }
        return null;
    }

    private Map<String, Object> getInstanceContext(WorkflowInstance instance) {
        Map<String, Object> context = new HashMap<>();
        context.put("instanceId", instance.getId());
        context.put("definitionId", instance.getDefinitionId());
        context.put("definitionName", instance.getDefinitionName());
        context.put("status", instance.getStatus());

        List<WorkflowExecutionLog> logs =
                flowHelper.getExecutionLogRepository().findByInstanceIdOrderByIdAsc(instance.getId());
        for (WorkflowExecutionLog log : logs) {
            String key = "node_" + log.getNodeId() + "_action";
            context.put(key, log.getAction());
            if (log.getComment() != null) {
                context.put("node_" + log.getNodeId() + "_comment", log.getComment());
            }
        }

        if (instance.getFormDataJson() != null && !instance.getFormDataJson().isEmpty()) {
            Map<String, Object> formData = parseFormDataFromJson(instance.getFormDataJson());
            for (Map.Entry<String, Object> entry : formData.entrySet()) {
                context.put("form_" + entry.getKey(), entry.getValue());
                context.put(entry.getKey(), entry.getValue());
            }
            if (instance.getFormId() != null) {
                context.put("formId", instance.getFormId());
            }
            if (instance.getFormName() != null) {
                context.put("formName", instance.getFormName());
            }
        }

        return context;
    }

    private Map<String, Object> parseFormDataFromJson(String json) {
        Map<String, Object> result = fromJson(json, new TypeReference<Map<String, Object>>() {});
        return result != null ? result : new HashMap<>();
    }
}
