package com.bw.flowform.engine.handler;

import com.bw.flowform.common.WorkflowConstants;
import com.bw.flowform.engine.FlowHelper;
import com.bw.flowform.engine.NodeHandler;
import com.bw.flowform.engine.NodeHandlerContext;
import com.bw.flowform.engine.NodeHandlerResult;
import com.bw.flowform.enums.InstanceStatus;
import com.bw.flowform.enums.NodeType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class EndNodeHandler implements NodeHandler {

    private final FlowHelper flowHelper;

    public EndNodeHandler(FlowHelper flowHelper) {
        this.flowHelper = flowHelper;
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.END;
    }

    @Override
    public NodeHandlerResult handle(NodeHandlerContext context) {
        context.getInstance().setStatus(InstanceStatus.COMPLETED.getCode());
        context.getInstance().setCurrentNodeId(null);
        context.getInstance().setCurrentNodeName(null);
        context.getInstance().setEndedAt(LocalDateTime.now());
        flowHelper.saveInstance(context.getInstance());
        flowHelper.addExecutionLog(
                context.getInstanceId(),
                context.getNodeId(),
                context.getNodeName(),
                context.getNodeType(),
                WorkflowConstants.LOG_ACTION_FLOW_END,
                null
        );
        return NodeHandlerResult.complete();
    }
}
