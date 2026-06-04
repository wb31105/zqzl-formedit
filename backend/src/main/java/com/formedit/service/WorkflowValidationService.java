package com.formedit.service;

import com.formedit.dto.WorkflowDefinitionDto;
import com.formedit.dto.WorkflowValidationResult;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class WorkflowValidationService {

    public WorkflowValidationResult validateWorkflow(WorkflowDefinitionDto dto) {
        WorkflowValidationResult result = WorkflowValidationResult.success();

        if (dto.getNodes() == null || dto.getNodes().isEmpty()) {
            result.addError("流程至少需要一个节点");
            return result;
        }

        validateStartAndEndNodes(dto, result);
        validateUniqueNodeNames(dto, result);
        validateIsolatedNodes(dto, result);
        validateConditionBranches(dto, result);
        validateNodeConnections(dto, result);

        return result;
    }

    private void validateStartAndEndNodes(WorkflowDefinitionDto dto, WorkflowValidationResult result) {
        long startCount = dto.getNodes().stream()
                .filter(n -> "start".equals(n.getType()))
                .count();

        long endCount = dto.getNodes().stream()
                .filter(n -> "end".equals(n.getType()))
                .count();

        if (startCount == 0) {
            result.addError("流程必须包含一个开始节点");
        } else if (startCount > 1) {
            result.addError("流程只能包含一个开始节点");
        }

        if (endCount == 0) {
            result.addError("流程必须包含一个结束节点");
        }
    }

    private void validateUniqueNodeNames(WorkflowDefinitionDto dto, WorkflowValidationResult result) {
        Set<String> names = new HashSet<>();
        for (WorkflowDefinitionDto.Node node : dto.getNodes()) {
            String name = node.getName();
            if (name == null || name.trim().isEmpty()) {
                result.addError("存在未命名的节点");
            } else if (!names.add(name.trim())) {
                result.addError("节点名称重复: " + name);
            }
        }
    }

    private void validateIsolatedNodes(WorkflowDefinitionDto dto, WorkflowValidationResult result) {
        if (dto.getEdges() == null || dto.getEdges().isEmpty()) {
            if (dto.getNodes().size() > 1) {
                result.addError("存在孤立节点，节点之间必须有连接");
            }
            return;
        }

        Set<String> connectedNodeIds = new HashSet<>();
        for (WorkflowDefinitionDto.Edge edge : dto.getEdges()) {
            if (edge.getSource() != null) {
                connectedNodeIds.add(edge.getSource());
            }
            if (edge.getTarget() != null) {
                connectedNodeIds.add(edge.getTarget());
            }
        }

        for (WorkflowDefinitionDto.Node node : dto.getNodes()) {
            if (!connectedNodeIds.contains(node.getId()) && !"start".equals(node.getType()) && !"end".equals(node.getType())) {
                if (dto.getNodes().size() > 1) {
                    result.addError("节点 \"" + node.getName() + "\" 是孤立节点，没有连接");
                }
            }
        }
    }

    private void validateConditionBranches(WorkflowDefinitionDto dto, WorkflowValidationResult result) {
        if (dto.getEdges() == null) return;

        List<WorkflowDefinitionDto.Node> conditionNodes = dto.getNodes().stream()
                .filter(n -> "condition".equals(n.getType()))
                .collect(Collectors.toList());

        for (WorkflowDefinitionDto.Node conditionNode : conditionNodes) {
            List<WorkflowDefinitionDto.Edge> outgoingEdges = dto.getEdges().stream()
                    .filter(e -> conditionNode.getId().equals(e.getSource()))
                    .collect(Collectors.toList());

            if (outgoingEdges.size() < 2) {
                result.addError("条件分支节点 \"" + conditionNode.getName() + "\" 必须有两条出边");
                continue;
            }

            boolean hasApprovePath = outgoingEdges.stream().anyMatch(e -> isApproveLabel(e.getLabel()));
            boolean hasRejectPath = outgoingEdges.stream().anyMatch(e -> isRejectLabel(e.getLabel()));

            if (!hasApprovePath || !hasRejectPath) {
                result.addError("条件分支节点 \"" + conditionNode.getName() + "\" 必须有\"批准/同意/是\"和\"拒绝/退回/否\"两条路径，请设置出边标签");
            }
        }
    }

    private boolean isApproveLabel(String label) {
        if (label == null) return false;
        String lowerLabel = label.toLowerCase();
        return lowerLabel.contains("是") || lowerLabel.contains("yes") ||
               lowerLabel.contains("批准") || lowerLabel.contains("同意") ||
               lowerLabel.contains("通过") || lowerLabel.contains("ok") ||
               lowerLabel.contains("true");
    }

    private boolean isRejectLabel(String label) {
        if (label == null) return false;
        String lowerLabel = label.toLowerCase();
        return lowerLabel.contains("否") || lowerLabel.contains("no") ||
               lowerLabel.contains("拒绝") || lowerLabel.contains("退回") ||
               lowerLabel.contains("不通过") || lowerLabel.contains("驳回") ||
               lowerLabel.contains("false");
    }

    private void validateNodeConnections(WorkflowDefinitionDto dto, WorkflowValidationResult result) {
        if (dto.getEdges() == null) return;

        Map<String, List<WorkflowDefinitionDto.Edge>> outgoingEdges = new HashMap<>();
        Map<String, List<WorkflowDefinitionDto.Edge>> incomingEdges = new HashMap<>();

        for (WorkflowDefinitionDto.Edge edge : dto.getEdges()) {
            outgoingEdges.computeIfAbsent(edge.getSource(), k -> new ArrayList<>()).add(edge);
            incomingEdges.computeIfAbsent(edge.getTarget(), k -> new ArrayList<>()).add(edge);
        }

        for (WorkflowDefinitionDto.Node node : dto.getNodes()) {
            String nodeId = node.getId();
            String nodeType = node.getType();

            if ("start".equals(nodeType)) {
                if (outgoingEdges.getOrDefault(nodeId, Collections.emptyList()).isEmpty()) {
                    result.addError("开始节点 \"" + node.getName() + "\" 必须有出边");
                }
                if (!incomingEdges.getOrDefault(nodeId, Collections.emptyList()).isEmpty()) {
                    result.addError("开始节点 \"" + node.getName() + "\" 不能有入边");
                }
            } else if ("end".equals(nodeType)) {
                if (incomingEdges.getOrDefault(nodeId, Collections.emptyList()).isEmpty()) {
                    result.addError("结束节点 \"" + node.getName() + "\" 必须有入边");
                }
                if (!outgoingEdges.getOrDefault(nodeId, Collections.emptyList()).isEmpty()) {
                    result.addError("结束节点 \"" + node.getName() + "\" 不能有出边");
                }
            } else if ("approval".equals(nodeType) || "auto".equals(nodeType)) {
                if (incomingEdges.getOrDefault(nodeId, Collections.emptyList()).isEmpty()) {
                    result.addError("节点 \"" + node.getName() + "\" 必须有入边");
                }
                if (outgoingEdges.getOrDefault(nodeId, Collections.emptyList()).isEmpty()) {
                    result.addError("节点 \"" + node.getName() + "\" 必须有出边");
                }
            }
        }
    }
}
