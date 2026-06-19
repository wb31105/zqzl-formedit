package com.bw.flowform.engine.handler;

import com.bw.flowform.common.NodeProperties;
import com.bw.flowform.common.WorkflowConstants;
import com.bw.flowform.engine.FlowHelper;
import com.bw.flowform.engine.NodeHandler;
import com.bw.flowform.engine.NodeHandlerContext;
import com.bw.flowform.engine.NodeHandlerResult;
import com.bw.flowform.enums.NodeType;
import org.springframework.stereotype.Component;

@Component
public class ApprovalNodeHandler implements NodeHandler {

    private final FlowHelper flowHelper;

    public ApprovalNodeHandler(FlowHelper flowHelper) {
        this.flowHelper = flowHelper;
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.APPROVAL;
    }

    @Override
    public NodeHandlerResult handle(NodeHandlerContext context) {
        String approver = NodeProperties.getApprover(context.getNode().getProperties());
        flowHelper.createPendingTask(
                context.getInstanceId(),
                context.getNodeId(),
                context.getNodeName(),
                approver
        );
        flowHelper.addExecutionLog(
                context.getInstanceId(),
                context.getNodeId(),
                context.getNodeName(),
                context.getNodeType(),
                WorkflowConstants.LOG_ACTION_WAIT_APPROVAL,
                null
        );
        return NodeHandlerResult.waitForTask();
    }
}
