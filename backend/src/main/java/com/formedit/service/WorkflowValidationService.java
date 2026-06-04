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

        List<WorkflowDefinitionDto.Node> branchNodes = dto.getNodes().stream()
                .filter(n -> "condition".equals(n.getType()) || "approval".equals(n.getType()) || "countersign".equals(n.getType()))
                .collect(Collectors.toList());

        for (WorkflowDefinitionDto.Node node : branchNodes) {
            List<WorkflowDefinitionDto.Edge> outgoingEdges = dto.getEdges().stream()
                    .filter(e -> node.getId().equals(e.getSource()))
                    .collect(Collectors.toList());

            if (outgoingEdges.size() <= 1) {
                continue;
            }

            boolean hasApprovePath = outgoingEdges.stream().anyMatch(e -> "approve".equals(e.getBranchType()));
            boolean hasRejectPath = outgoingEdges.stream().anyMatch(e -> "reject".equals(e.getBranchType()));
            long approveCount = outgoingEdges.stream().filter(e -> "approve".equals(e.getBranchType())).count();
            long rejectCount = outgoingEdges.stream().filter(e -> "reject".equals(e.getBranchType())).count();

            String nodeTypeDesc = getNodeTypeDescription(node.getType());

            for (WorkflowDefinitionDto.Edge edge : outgoingEdges) {
                if (edge.getBranchType() == null || edge.getBranchType().trim().isEmpty()) {
                    result.addError(nodeTypeDesc + " \"" + node.getName() + "\" 的出边必须设置分支类型（批准路径/拒绝路径）");
                    break;
                }
                if (!"approve".equals(edge.getBranchType()) && !"reject".equals(edge.getBranchType())) {
                    result.addError(nodeTypeDesc + " \"" + node.getName() + "\" 的出边分支类型无效，只能是\"批准路径\"或\"拒绝路径\"");
                    break;
                }
            }

            if (approveCount != 1 || rejectCount != 1) {
                result.addError(nodeTypeDesc + " \"" + node.getName() + "\" 必须恰好有一条\"批准路径\"和一条\"拒绝路径\"，当前批准路径: " + approveCount + " 条, 拒绝路径: " + rejectCount + " 条");
            }
        }
    }

    private String getNodeTypeDescription(String nodeType) {
        switch (nodeType) {
            case "condition":
                return "条件分支节点";
            case "approval":
                return "审批节点";
            case "countersign":
                return "会签节点";
            default:
                return "节点";
        }
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
