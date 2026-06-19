package com.bw.flowform.engine.handler;

import com.bw.flowform.common.WorkflowConstants;
import com.bw.flowform.engine.FlowHelper;
import com.bw.flowform.engine.NodeHandler;
import com.bw.flowform.engine.NodeHandlerContext;
import com.bw.flowform.engine.NodeHandlerResult;
import com.bw.flowform.enums.NodeType;
import org.springframework.stereotype.Component;

@Component
public class AutoNodeHandler implements NodeHandler {

    private final FlowHelper flowHelper;

    public AutoNodeHandler(FlowHelper flowHelper) {
        this.flowHelper = flowHelper;
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.AUTO;
    }

    @Override
    public NodeHandlerResult handle(NodeHandlerContext context) {
        flowHelper.addExecutionLog(
                context.getInstanceId(),
                context.getNodeId(),
                context.getNodeName(),
                context.getNodeType(),
                WorkflowConstants.LOG_ACTION_EXECUTE_AUTO,
                null
        );
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        flowHelper.addExecutionLog(
                context.getInstanceId(),
                context.getNodeId(),
                context.getNodeName(),
                context.getNodeType(),
                WorkflowConstants.LOG_ACTION_AUTO_DONE,
                null
        );
        return NodeHandlerResult.proceed();
    }
}
