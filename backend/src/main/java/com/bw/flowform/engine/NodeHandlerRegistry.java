package com.bw.flowform.engine;

import com.bw.flowform.enums.NodeType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class NodeHandlerRegistry {

    private final Map<NodeType, NodeHandler> handlerMap = new EnumMap<>(NodeType.class);

    public NodeHandlerRegistry(List<NodeHandler> handlers) {
        for (NodeHandler handler : handlers) {
            handlerMap.put(handler.getNodeType(), handler);
        }
    }

    public NodeHandler getHandler(NodeType nodeType) {
        return handlerMap.get(nodeType);
    }

    public boolean hasHandler(NodeType nodeType) {
        return handlerMap.containsKey(nodeType);
    }
}
