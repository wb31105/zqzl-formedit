package com.bw.flowform.engine;

import com.bw.flowform.dto.WorkflowDefinitionDto;
import com.bw.flowform.entity.WorkflowInstance;

public class NodeHandlerContext {

    private final WorkflowInstance instance;
    private final WorkflowDefinitionDto definition;
    private final WorkflowDefinitionDto.Node node;

    public NodeHandlerContext(WorkflowInstance instance,
                               WorkflowDefinitionDto definition,
                               WorkflowDefinitionDto.Node node) {
        this.instance = instance;
        this.definition = definition;
        this.node = node;
    }

    public WorkflowInstance getInstance() {
        return instance;
    }

    public WorkflowDefinitionDto getDefinition() {
        return definition;
    }

    public WorkflowDefinitionDto.Node getNode() {
        return node;
    }

    public Long getInstanceId() {
        return instance.getId();
    }

    public String getNodeId() {
        return node.getId();
    }

    public String getNodeName() {
        return node.getName();
    }

    public String getNodeType() {
        return node.getType();
    }
}
