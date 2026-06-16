package com.bw.flowform.service;

import com.bw.flowform.common.NodeProperties;
import com.bw.flowform.dto.WorkflowDefinitionDto;
import com.bw.flowform.dto.WorkflowValidationResult;
import com.bw.flowform.entity.FormField;
import com.bw.flowform.enums.BranchType;
import com.bw.flowform.enums.NodeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class WorkflowValidationService {

    private final FormService formService;

    @Autowired
    public WorkflowValidationService(FormService formService) {
        this.formService = formService;
    }

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
        validateConditionExpressions(dto, result);
        validateConditionExpressionFields(dto, result);

        return result;
    }

    private void validateConditionExpressions(WorkflowDefinitionDto dto, WorkflowValidationResult result) {
        if (dto.getNodes() == null) return;
        for (WorkflowDefinitionDto.Node node : dto.getNodes()) {
            if (!NodeType.CONDITION.getCode().equals(node.getType())) continue;
            Map<String, Object> props = node.getProperties();
            String expr = NodeProperties.getExpression(props);
            if (expr == null) continue;
            expr = expr.trim();
            if (expr.isEmpty()) continue;
            try {
                ExpressionEvaluator.validateSyntax(expr);
            } catch (Exception e) {
                result.addError("条件节点 \"" + node.getName() + "\" 的表达式语法错误: "
                        + e.getMessage() + "，表达式=\"" + expr + "\"");
            }
        }
    }

    private void validateConditionExpressionFields(WorkflowDefinitionDto dto, WorkflowValidationResult result) {
        if (dto.getFormId() == null) return;
        if (dto.getNodes() == null) return;

        List<FormField> formFields = formService.getFormFields(dto.getFormId());
        if (formFields == null || formFields.isEmpty()) return;

        Set<String> fieldIds = formFields.stream()
                .map(FormField::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (WorkflowDefinitionDto.Node node : dto.getNodes()) {
            if (!NodeType.CONDITION.getCode().equals(node.getType())) continue;
            Map<String, Object> props = node.getProperties();
            String expr = NodeProperties.getExpression(props);
            if (expr == null) continue;
            expr = expr.trim();
            if (expr.isEmpty()) continue;

            try {
                List<String> variables = extractVariables(expr);
                List<String> invalidFields = new ArrayList<>();
                for (String var : variables) {
                    if (isSystemVariable(var)) continue;
                    if (!fieldIds.contains(var)) {
                        invalidFields.add(var);
                    }
                }
                if (!invalidFields.isEmpty()) {
                    result.addError("条件节点 \"" + node.getName() + "\" 的表达式中使用了绑定表单不存在的字段: "
                            + String.join(", ", invalidFields)
                            + "（请检查字段ID是否正确，或先绑定正确的表单）");
                }
            } catch (Exception e) {
            }
        }
    }

    private List<String> extractVariables(String expression) {
        List<String> variables = new ArrayList<>();
        List<ExpressionEvaluator.Token> tokens = ExpressionEvaluator.tokenize(expression);
        Set<String> seen = new HashSet<>();
        for (ExpressionEvaluator.Token token : tokens) {
            if (token.type == ExpressionEvaluator.TokenType.IDENT) {
                if (seen.add(token.value)) {
                    variables.add(token.value);
                }
            }
        }
        return variables;
    }

    private boolean isSystemVariable(String varName) {
        if (varName == null || varName.isEmpty()) return false;
        if (varName.startsWith("node_") && varName.endsWith("_action")) return true;
        if ("true".equalsIgnoreCase(varName) || "false".equalsIgnoreCase(varName)) return true;
        return false;
    }

    private void validateStartAndEndNodes(WorkflowDefinitionDto dto, WorkflowValidationResult result) {
        long startCount = dto.getNodes().stream()
                .filter(n -> NodeType.START.getCode().equals(n.getType()))
                .count();

        long endCount = dto.getNodes().stream()
                .filter(n -> NodeType.END.getCode().equals(n.getType()))
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
            if (!connectedNodeIds.contains(node.getId()) && !NodeType.START.getCode().equals(node.getType()) && !NodeType.END.getCode().equals(node.getType())) {
                if (dto.getNodes().size() > 1) {
                    result.addError("节点 \"" + node.getName() + "\" 是孤立节点，没有连接");
                }
            }
        }
    }

    private void validateConditionBranches(WorkflowDefinitionDto dto, WorkflowValidationResult result) {
        if (dto.getEdges() == null) return;

        List<WorkflowDefinitionDto.Node> branchNodes = dto.getNodes().stream()
                .filter(n -> NodeType.CONDITION.getCode().equals(n.getType())
                        || NodeType.APPROVAL.getCode().equals(n.getType())
                        || NodeType.COUNTERSIGN.getCode().equals(n.getType()))
                .collect(Collectors.toList());

        for (WorkflowDefinitionDto.Node node : branchNodes) {
            List<WorkflowDefinitionDto.Edge> outgoingEdges = dto.getEdges().stream()
                    .filter(e -> node.getId().equals(e.getSource()))
                    .collect(Collectors.toList());

            if (outgoingEdges.size() <= 1) {
                continue;
            }

            boolean hasApprovePath = outgoingEdges.stream().anyMatch(e -> BranchType.APPROVE.getCode().equals(e.getBranchType()));
            boolean hasRejectPath = outgoingEdges.stream().anyMatch(e -> BranchType.REJECT.getCode().equals(e.getBranchType()));
            long approveCount = outgoingEdges.stream().filter(e -> BranchType.APPROVE.getCode().equals(e.getBranchType())).count();
            long rejectCount = outgoingEdges.stream().filter(e -> BranchType.REJECT.getCode().equals(e.getBranchType())).count();

            String nodeTypeDesc = getNodeTypeDescription(node.getType());

            for (WorkflowDefinitionDto.Edge edge : outgoingEdges) {
                if (edge.getBranchType() == null || edge.getBranchType().trim().isEmpty()) {
                    result.addError(nodeTypeDesc + " \"" + node.getName() + "\" 的出边必须设置分支类型（批准路径/拒绝路径）");
                    break;
                }
                if (!BranchType.APPROVE.getCode().equals(edge.getBranchType()) && !BranchType.REJECT.getCode().equals(edge.getBranchType())) {
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
        NodeType type = NodeType.fromCode(nodeType);
        if (type != null) {
            return type.getDescription();
        }
        return "节点";
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

            if (NodeType.START.getCode().equals(nodeType)) {
                if (outgoingEdges.getOrDefault(nodeId, Collections.emptyList()).isEmpty()) {
                    result.addError("开始节点 \"" + node.getName() + "\" 必须有出边");
                }
                if (!incomingEdges.getOrDefault(nodeId, Collections.emptyList()).isEmpty()) {
                    result.addError("开始节点 \"" + node.getName() + "\" 不能有入边");
                }
            } else if (NodeType.END.getCode().equals(nodeType)) {
                if (incomingEdges.getOrDefault(nodeId, Collections.emptyList()).isEmpty()) {
                    result.addError("结束节点 \"" + node.getName() + "\" 必须有入边");
                }
                if (!outgoingEdges.getOrDefault(nodeId, Collections.emptyList()).isEmpty()) {
                    result.addError("结束节点 \"" + node.getName() + "\" 不能有出边");
                }
            } else if (NodeType.APPROVAL.getCode().equals(nodeType) || NodeType.AUTO.getCode().equals(nodeType)) {
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
