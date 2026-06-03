package com.formedit.service;

import com.formedit.dto.WorkflowDefinitionDto;
import com.formedit.entity.WorkflowExecutionLog;
import com.formedit.entity.WorkflowInstance;
import com.formedit.entity.WorkflowTask;
import com.formedit.repository.WorkflowExecutionLogRepository;
import com.formedit.repository.WorkflowInstanceRepository;
import com.formedit.repository.WorkflowTaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WorkflowInstanceService {

    private final WorkflowInstanceRepository instanceRepository;
    private final WorkflowTaskRepository taskRepository;
    private final WorkflowExecutionLogRepository executionLogRepository;
    private final WorkflowDefinitionService definitionService;

    public WorkflowInstanceService(WorkflowInstanceRepository instanceRepository,
                                    WorkflowTaskRepository taskRepository,
                                    WorkflowExecutionLogRepository executionLogRepository,
                                    WorkflowDefinitionService definitionService) {
        this.instanceRepository = instanceRepository;
        this.taskRepository = taskRepository;
        this.executionLogRepository = executionLogRepository;
        this.definitionService = definitionService;
    }

    public Page<WorkflowInstance> getAllInstances(Pageable pageable) {
        return instanceRepository.findAllByOrderByIdDesc(pageable);
    }

    public List<WorkflowInstance> getInstancesByDefinitionId(Long definitionId) {
        return instanceRepository.findByDefinitionIdOrderByIdDesc(definitionId);
    }

    public Optional<WorkflowInstance> getInstanceById(Long id) {
        return instanceRepository.findById(id);
    }

    public List<WorkflowExecutionLog> getExecutionLogs(Long instanceId) {
        return executionLogRepository.findByInstanceIdOrderByIdAsc(instanceId);
    }

    public List<WorkflowTask> getInstanceTasks(Long instanceId) {
        return taskRepository.findByInstanceIdOrderByIdAsc(instanceId);
    }

    public List<WorkflowTask> getPendingTasks(Long instanceId) {
        return taskRepository.findByInstanceIdAndStatusOrderByIdAsc(instanceId, "PENDING");
    }

    @Transactional
    public WorkflowInstance startInstance(Long definitionId) {
        WorkflowDefinitionDto definition = definitionService.getDefinitionDtoById(definitionId);
        if (definition == null) {
            throw new IllegalArgumentException("流程定义不存在");
        }

        WorkflowInstance instance = new WorkflowInstance();
        instance.setDefinitionId(definitionId);
        instance.setDefinitionName(definition.getName());
        instance.setStatus("RUNNING");
        instance = instanceRepository.save(instance);

        addExecutionLog(instance.getId(), "start", "开始", "start", "流程启动", null);

        WorkflowDefinitionDto.Node startNode = definition.getNodes().stream()
                .filter(n -> "start".equals(n.getType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("流程没有开始节点"));

        addExecutionLog(instance.getId(), startNode.getId(), startNode.getName(), startNode.getType(), "进入节点", null);

        proceedToNextNode(instance, definition, startNode);

        return instance;
    }

    @Transactional
    public WorkflowInstance completeTask(Long instanceId, Long taskId, String action, String comment, String assignee) {
        WorkflowInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("流程实例不存在"));

        if (!"RUNNING".equals(instance.getStatus())) {
            throw new IllegalStateException("流程实例不是运行状态");
        }

        WorkflowTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在"));

        if (!task.getInstanceId().equals(instanceId)) {
            throw new IllegalArgumentException("任务不属于该流程实例");
        }

        if (!"PENDING".equals(task.getStatus())) {
            throw new IllegalStateException("任务不是待处理状态");
        }

        task.setStatus("COMPLETED");
        task.setComment(comment);
        task.setAssignee(assignee != null ? assignee : "系统管理员");
        task.setCompletedAt(LocalDateTime.now());
        taskRepository.save(task);

        WorkflowDefinitionDto definition = definitionService.getDefinitionDtoById(instance.getDefinitionId());

        WorkflowDefinitionDto.Node currentNode = definition.getNodes().stream()
                .filter(n -> n.getId().equals(task.getNodeId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("节点不存在"));

        if ("countersign".equals(currentNode.getType())) {
            addExecutionLog(instanceId, task.getNodeId(), task.getNodeName(), "countersign",
                    "approve".equals(action) ? "批准" : "拒绝",
                    (assignee != null ? assignee : "系统管理员") + ": " + comment);

            String countersignType = "all";
            Map<String, Object> csProps = currentNode.getProperties();
            if (csProps != null && csProps.get("countersignType") != null) {
                countersignType = csProps.get("countersignType").toString();
            }

            boolean shouldFinalize = false;
            String countersignResult = null;

            if ("veto".equals(countersignType) && "reject".equals(action)) {
                shouldFinalize = true;
                countersignResult = "reject";
            } else if ("all".equals(countersignType) && "reject".equals(action)) {
                shouldFinalize = true;
                countersignResult = "reject";
            }

            List<WorkflowTask> pendingTasks = taskRepository.findByInstanceIdAndNodeIdAndStatusOrderByIdAsc(instanceId, currentNode.getId(), "PENDING");
            List<WorkflowTask> allNodeTasks = taskRepository.findByInstanceIdAndNodeIdOrderByIdAsc(instanceId, currentNode.getId());
            long totalApprovers = allNodeTasks.size();
            List<WorkflowExecutionLog> nodeLogs = executionLogRepository.findByInstanceIdAndNodeIdOrderByIdAsc(instanceId, currentNode.getId());

            long approveCount = 0;
            long rejectCount = 0;
            for (WorkflowTask t : allNodeTasks) {
                if ("COMPLETED".equals(t.getStatus()) && t.getAssignee() != null) {
                    boolean approved = false;
                    for (WorkflowExecutionLog log : nodeLogs) {
                        if (log.getComment() != null && log.getComment().startsWith(t.getAssignee() + ":")) {
                            if ("批准".equals(log.getAction())) {
                                approved = true;
                            }
                            break;
                        }
                    }
                    if (approved) {
                        approveCount++;
                    } else {
                        rejectCount++;
                    }
                }
            }
            long completedCount = approveCount + rejectCount;

            if ("majority".equals(countersignType) && pendingTasks != null && !pendingTasks.isEmpty()) {
                long remainingPending = pendingTasks.size();
                if (approveCount > totalApprovers / 2) {
                    shouldFinalize = true;
                    countersignResult = "approve";
                } else if (approveCount + remainingPending <= totalApprovers / 2) {
                    shouldFinalize = true;
                    countersignResult = "reject";
                }
            }

            if (shouldFinalize) {
                for (WorkflowTask pendingTask : pendingTasks) {
                    pendingTask.setStatus("CANCELLED");
                    pendingTask.setComment("会签已结束，任务自动取消");
                    pendingTask.setCompletedAt(LocalDateTime.now());
                    taskRepository.save(pendingTask);
                }
                addExecutionLog(instanceId, currentNode.getId(), currentNode.getName(), "countersign",
                        "会签结果: " + ("approve".equals(countersignResult) ? "通过" : "拒绝"), null);
                proceedToNextNode(instance, definition, currentNode, countersignResult);
            } else if (pendingTasks.isEmpty()) {
                countersignResult = evaluateCountersignResult(instance, currentNode);
                addExecutionLog(instanceId, currentNode.getId(), currentNode.getName(), "countersign",
                        "会签结果: " + ("approve".equals(countersignResult) ? "通过" : "拒绝"), null);
                proceedToNextNode(instance, definition, currentNode, countersignResult);
            } else {
                addExecutionLog(instanceId, currentNode.getId(), currentNode.getName(), "countersign",
                        "等待其他审批人...", "剩余 " + pendingTasks.size() + " 人未审批");
            }
        } else {
            addExecutionLog(instanceId, task.getNodeId(), task.getNodeName(), "approval",
                    "approve".equals(action) ? "批准" : "拒绝", comment);
            proceedToNextNode(instance, definition, currentNode, action);
        }

        return instance;
    }

    private String evaluateCountersignResult(WorkflowInstance instance, WorkflowDefinitionDto.Node countersignNode) {
        List<WorkflowTask> allTasks = taskRepository.findByInstanceIdAndNodeIdOrderByIdAsc(instance.getId(), countersignNode.getId());

        List<WorkflowExecutionLog> nodeLogs = executionLogRepository.findByInstanceIdAndNodeIdOrderByIdAsc(
                instance.getId(), countersignNode.getId());

        long approveCount = 0;
        long rejectCount = 0;

        for (WorkflowTask task : allTasks) {
            if ("COMPLETED".equals(task.getStatus())) {
                boolean approved = false;
                for (WorkflowExecutionLog log : nodeLogs) {
                    if (log.getComment() != null && task.getAssignee() != null &&
                            log.getComment().startsWith(task.getAssignee() + ":")) {
                        if ("批准".equals(log.getAction())) {
                            approved = true;
                        }
                        break;
                    }
                }
                if (approved) {
                    approveCount++;
                } else {
                    rejectCount++;
                }
            }
        }

        long totalCompleted = approveCount + rejectCount;

        String countersignType = "all";
        Map<String, Object> properties = countersignNode.getProperties();
        if (properties != null && properties.get("countersignType") != null) {
            countersignType = properties.get("countersignType").toString();
        }

        switch (countersignType) {
            case "veto":
                if (rejectCount > 0) {
                    return "reject";
                }
                return "approve";
            case "majority":
                if (approveCount > totalCompleted / 2) {
                    return "approve";
                }
                return "reject";
            case "all":
            default:
                if (approveCount == totalCompleted && totalCompleted > 0) {
                    return "approve";
                }
                return "reject";
        }
    }

    private String getCountersignTypeDescription(String countersignType) {
        switch (countersignType) {
            case "veto":
                return "一票否决";
            case "majority":
                return "过半通过";
            case "all":
            default:
                return "全部同意才通过";
        }
    }

    private void proceedToNextNode(WorkflowInstance instance, WorkflowDefinitionDto definition,
                                   WorkflowDefinitionDto.Node currentNode) {
        proceedToNextNode(instance, definition, currentNode, null);
    }

    private void proceedToNextNode(WorkflowInstance instance, WorkflowDefinitionDto definition,
                                   WorkflowDefinitionDto.Node currentNode, String lastAction) {
        WorkflowDefinitionDto.Edge nextEdge = findNextEdge(definition, currentNode, lastAction);

        if (nextEdge == null) {
            instance.setStatus("COMPLETED");
            instance.setCurrentNodeId(null);
            instance.setCurrentNodeName(null);
            instance.setEndedAt(LocalDateTime.now());
            instanceRepository.save(instance);

            WorkflowDefinitionDto.Node endNode = definition.getNodes().stream()
                    .filter(n -> "end".equals(n.getType()))
                    .findFirst()
                    .orElse(null);

            if (endNode != null) {
                addExecutionLog(instance.getId(), endNode.getId(), endNode.getName(), endNode.getType(), "流程结束", null);
            }
            return;
        }

        WorkflowDefinitionDto.Node nextNode = definition.getNodes().stream()
                .filter(n -> n.getId().equals(nextEdge.getTarget()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("目标节点不存在"));

        instance.setCurrentNodeId(nextNode.getId());
        instance.setCurrentNodeName(nextNode.getName());
        instanceRepository.save(instance);

        addExecutionLog(instance.getId(), nextNode.getId(), nextNode.getName(), nextNode.getType(), "进入节点", null);

        processNode(instance, definition, nextNode);
    }

    private WorkflowDefinitionDto.Edge findNextEdge(WorkflowDefinitionDto definition,
                                                    WorkflowDefinitionDto.Node currentNode, String lastAction) {
        List<WorkflowDefinitionDto.Edge> outgoingEdges = definition.getEdges().stream()
                .filter(e -> e.getSource().equals(currentNode.getId()))
                .collect(Collectors.toList());

        if (outgoingEdges.isEmpty()) {
            return null;
        }

        if ("condition".equals(currentNode.getType())) {
            if ("approve".equals(lastAction)) {
                return outgoingEdges.stream()
                        .filter(e -> isApproveLabel(e.getLabel()))
                        .findFirst()
                        .orElse(outgoingEdges.get(0));
            } else {
                return outgoingEdges.stream()
                        .filter(e -> isRejectLabel(e.getLabel()))
                        .findFirst()
                        .orElse(outgoingEdges.size() > 1 ? outgoingEdges.get(1) : outgoingEdges.get(0));
            }
        }

        if (("approval".equals(currentNode.getType()) || "countersign".equals(currentNode.getType())) && outgoingEdges.size() > 1) {
            if ("approve".equals(lastAction)) {
                return outgoingEdges.stream()
                        .filter(e -> isApproveLabel(e.getLabel()))
                        .findFirst()
                        .orElse(outgoingEdges.get(0));
            } else {
                return outgoingEdges.stream()
                        .filter(e -> isRejectLabel(e.getLabel()))
                        .findFirst()
                        .orElse(outgoingEdges.size() > 1 ? outgoingEdges.get(1) : outgoingEdges.get(0));
            }
        }

        return outgoingEdges.get(0);
    }

    private boolean isApproveLabel(String label) {
        if (label == null) return false;
        String lowerLabel = label.toLowerCase();
        return lowerLabel.equals("是") || lowerLabel.equals("yes") ||
               lowerLabel.equals("批准") || lowerLabel.equals("同意") ||
               lowerLabel.equals("通过") || lowerLabel.equals("ok") ||
               lowerLabel.equals("true");
    }

    private boolean isRejectLabel(String label) {
        if (label == null) return false;
        String lowerLabel = label.toLowerCase();
        return lowerLabel.equals("否") || lowerLabel.equals("no") ||
               lowerLabel.equals("拒绝") || lowerLabel.equals("退回") ||
               lowerLabel.equals("不通过") || lowerLabel.equals("驳回") ||
               lowerLabel.equals("false");
    }

    private String resolveConditionExpression(WorkflowDefinitionDto.Node conditionNode, WorkflowInstance instance) {
        Map<String, Object> properties = conditionNode.getProperties();
        if (properties != null && properties.get("expression") != null) {
            String expression = properties.get("expression").toString().trim();
            if (!expression.isEmpty()) {
                Map<String, Object> context = getInstanceContext(instance);
                try {
                    boolean result = evaluateExpression(expression, context);
                    return result ? "approve" : "reject";
                } catch (Exception e) {
                    addExecutionLog(instance.getId(), conditionNode.getId(), conditionNode.getName(),
                            "condition", "条件表达式解析失败: " + e.getMessage(), null);
                }
            }
        }

        String lastApprovalAction = getLastApprovalAction(instance.getId());
        return lastApprovalAction != null ? lastApprovalAction : "approve";
    }

    private String getLastApprovalAction(Long instanceId) {
        List<WorkflowExecutionLog> logs = executionLogRepository.findByInstanceIdOrderByIdDesc(instanceId);
        for (WorkflowExecutionLog log : logs) {
            if ("approval".equals(log.getNodeType()) || "countersign".equals(log.getNodeType())) {
                if ("批准".equals(log.getAction()) || "approve".equals(log.getAction()) || "会签结果: 通过".equals(log.getAction())) {
                    return "approve";
                } else if ("拒绝".equals(log.getAction()) || "reject".equals(log.getAction()) || "会签结果: 拒绝".equals(log.getAction())) {
                    return "reject";
                }
            }
        }
        return null;
    }

    private Map<String, Object> getInstanceContext(WorkflowInstance instance) {
        Map<String, Object> context = new HashMap<>();
        context.put("instanceId", instance.getId());
        context.put("definitionId", instance.getDefinitionId());
        context.put("definitionName", instance.getDefinitionName());
        context.put("status", instance.getStatus());

        List<WorkflowExecutionLog> logs = executionLogRepository.findByInstanceIdOrderByIdAsc(instance.getId());
        for (WorkflowExecutionLog log : logs) {
            String key = "node_" + log.getNodeId() + "_action";
            context.put(key, log.getAction());
            if (log.getComment() != null) {
                context.put("node_" + log.getNodeId() + "_comment", log.getComment());
            }
        }

        return context;
    }

    private boolean evaluateExpression(String expression, Map<String, Object> context) {
        String expr = expression.toLowerCase().trim();

        for (Map.Entry<String, Object> entry : context.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            expr = expr.replace("${" + key + "}", value);
            expr = expr.replace("{" + key + "}", value);
        }

        if (expr.contains(">=")) {
            String[] parts = expr.split(">=");
            if (parts.length == 2) {
                try {
                    double left = Double.parseDouble(parts[0].trim());
                    double right = Double.parseDouble(parts[1].trim());
                    return left >= right;
                } catch (NumberFormatException e) {
                    return parts[0].trim().compareTo(parts[1].trim()) >= 0;
                }
            }
        }
        if (expr.contains("<=")) {
            String[] parts = expr.split("<=");
            if (parts.length == 2) {
                try {
                    double left = Double.parseDouble(parts[0].trim());
                    double right = Double.parseDouble(parts[1].trim());
                    return left <= right;
                } catch (NumberFormatException e) {
                    return parts[0].trim().compareTo(parts[1].trim()) <= 0;
                }
            }
        }
        if (expr.contains(">")) {
            String[] parts = expr.split(">");
            if (parts.length == 2) {
                try {
                    double left = Double.parseDouble(parts[0].trim());
                    double right = Double.parseDouble(parts[1].trim());
                    return left > right;
                } catch (NumberFormatException e) {
                    return parts[0].trim().compareTo(parts[1].trim()) > 0;
                }
            }
        }
        if (expr.contains("<")) {
            String[] parts = expr.split("<");
            if (parts.length == 2) {
                try {
                    double left = Double.parseDouble(parts[0].trim());
                    double right = Double.parseDouble(parts[1].trim());
                    return left < right;
                } catch (NumberFormatException e) {
                    return parts[0].trim().compareTo(parts[1].trim()) < 0;
                }
            }
        }
        if (expr.contains("==")) {
            String[] parts = expr.split("==");
            if (parts.length == 2) {
                return parts[0].trim().equals(parts[1].trim());
            }
        }
        if (expr.contains("!=")) {
            String[] parts = expr.split("!=");
            if (parts.length == 2) {
                return !parts[0].trim().equals(parts[1].trim());
            }
        }

        if ("true".equals(expr) || "yes".equals(expr) || "是".equals(expr) || "批准".equals(expr)) {
            return true;
        }
        if ("false".equals(expr) || "no".equals(expr) || "否".equals(expr) || "拒绝".equals(expr)) {
            return false;
        }

        return true;
    }

    private void processNode(WorkflowInstance instance, WorkflowDefinitionDto definition,
                             WorkflowDefinitionDto.Node node) {
        switch (node.getType()) {
            case "end":
                instance.setStatus("COMPLETED");
                instance.setCurrentNodeId(null);
                instance.setCurrentNodeName(null);
                instance.setEndedAt(LocalDateTime.now());
                instanceRepository.save(instance);
                addExecutionLog(instance.getId(), node.getId(), node.getName(), node.getType(), "流程结束", null);
                break;

            case "approval":
                WorkflowTask task = new WorkflowTask();
                task.setInstanceId(instance.getId());
                task.setNodeId(node.getId());
                task.setNodeName(node.getName());
                task.setStatus("PENDING");
                Map<String, Object> approvalProps = node.getProperties();
                if (approvalProps != null && approvalProps.get("approver") != null) {
                    task.setAssignee(approvalProps.get("approver").toString());
                }
                taskRepository.save(task);
                addExecutionLog(instance.getId(), node.getId(), node.getName(), node.getType(), "等待审批", null);
                break;

            case "countersign":
                Map<String, Object> countersignProps = node.getProperties();
                List<String> approvers = new ArrayList<>();
                if (countersignProps != null && countersignProps.get("approvers") != null) {
                    String approversStr = countersignProps.get("approvers").toString();
                    if (!approversStr.trim().isEmpty()) {
                        String[] approverArray = approversStr.split("[,，、;；\\s]+");
                        for (String approver : approverArray) {
                            if (!approver.trim().isEmpty()) {
                                approvers.add(approver.trim());
                            }
                        }
                    }
                }
                if (approvers.isEmpty()) {
                    approvers.add("审批人1");
                    approvers.add("审批人2");
                }
                for (String approver : approvers) {
                    WorkflowTask csTask = new WorkflowTask();
                    csTask.setInstanceId(instance.getId());
                    csTask.setNodeId(node.getId());
                    csTask.setNodeName(node.getName());
                    csTask.setStatus("PENDING");
                    csTask.setAssignee(approver);
                    taskRepository.save(csTask);
                }
                String countersignType = "all";
                if (countersignProps != null && countersignProps.get("countersignType") != null) {
                    countersignType = countersignProps.get("countersignType").toString();
                }
                String typeDesc = getCountersignTypeDescription(countersignType);
                addExecutionLog(instance.getId(), node.getId(), node.getName(), node.getType(),
                        "等待会签", "审批人: " + String.join(", ", approvers) + "，规则: " + typeDesc);
                break;

            case "auto":
                addExecutionLog(instance.getId(), node.getId(), node.getName(), node.getType(), "执行自动任务", null);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                addExecutionLog(instance.getId(), node.getId(), node.getName(), node.getType(), "自动任务完成", null);
                proceedToNextNode(instance, definition, node);
                break;

            case "condition":
                addExecutionLog(instance.getId(), node.getId(), node.getName(), node.getType(), "条件判断", null);
                String conditionAction = resolveConditionExpression(node, instance);
                addExecutionLog(instance.getId(), node.getId(), node.getName(), node.getType(), "条件判断结果: " + ("approve".equals(conditionAction) ? "是" : "否"), null);
                proceedToNextNode(instance, definition, node, conditionAction);
                break;

            default:
                proceedToNextNode(instance, definition, node);
                break;
        }
    }

    private void addExecutionLog(Long instanceId, String nodeId, String nodeName,
                                 String nodeType, String action, String comment) {
        WorkflowExecutionLog log = new WorkflowExecutionLog();
        log.setInstanceId(instanceId);
        log.setNodeId(nodeId);
        log.setNodeName(nodeName);
        log.setNodeType(nodeType);
        log.setAction(action);
        log.setComment(comment);
        executionLogRepository.save(log);
    }

    @Transactional
    public boolean deleteInstance(Long id) {
        if (instanceRepository.existsById(id)) {
            executionLogRepository.deleteAll(executionLogRepository.findByInstanceIdOrderByIdAsc(id));
            taskRepository.deleteAll(taskRepository.findByInstanceIdOrderByIdAsc(id));
            instanceRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
