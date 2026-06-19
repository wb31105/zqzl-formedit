package com.bw.flowform.engine;

import com.bw.flowform.enums.NodeType;

public interface NodeHandler {

    NodeType getNodeType();

    NodeHandlerResult handle(NodeHandlerContext context);
}
