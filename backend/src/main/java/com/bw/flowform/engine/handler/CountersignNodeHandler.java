package com.bw.flowform.engine.handler;

import com.bw.flowform.common.NodeProperties;
import com.bw.flowform.common.WorkflowConstants;
import com.bw.flowform.engine.FlowHelper;
import com.bw.flowform.engine.NodeHandler;
import com.bw.flowform.engine.NodeHandlerContext;
import com.bw.flowform.engine.NodeHandlerResult;
import com.bw.flowform.enums.CountersignType;
import com.bw.flowform.enums.NodeType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CountersignNodeHandler implements NodeHandler {

    private final FlowHelper flowHelper;

    public CountersignNodeHandler(FlowHelper flowHelper) {
        this.flowHelper = flowHelper;
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.COUNTERSIGN;
    }

    @Override
    public NodeHandlerResult handle(NodeHandlerContext context) {
        var props = context.getNode().getProperties();
        List<String> approvers = NodeProperties.getApprovers(props);
        if (approvers.isEmpty()) {
            approvers.add(WorkflowConstants.DEFAULT_APPROVER_1);
            approvers.add(WorkflowConstants.DEFAULT_APPROVER_2);
        }
        for (String approver : approvers) {
            flowHelper.createPendingTask(
                    context.getInstanceId(),
                    context.getNodeId(),
                    context.getNodeName(),
                    approver
            );
        }
        String countersignTypeCode = NodeProperties.getCountersignType(props);
        CountersignType csType = CountersignType.fromCode(countersignTypeCode);
        String typeDesc = csType != null ? csType.getDescription() : CountersignType.ALL.getDescription();

        String comment = WorkflowConstants.LOG_COMMENT_APPROVERS_PREFIX
                + String.join(", ", approvers)
                + WorkflowConstants.LOG_COMMENT_RULE_PREFIX
                + typeDesc;

        flowHelper.addExecutionLog(
                context.getInstanceId(),
                context.getNodeId(),
                context.getNodeName(),
                context.getNodeType(),
                WorkflowConstants.LOG_ACTION_WAIT_COUNTERSIGN,
                comment
        );
        return NodeHandlerResult.waitForTask();
    }
}
