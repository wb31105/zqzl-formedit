package com.formedit.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formedit.dto.ValidationResult;
import com.formedit.dto.WorkflowDefinitionDto;
import com.formedit.entity.Form;
import com.formedit.entity.WorkflowExecutionLog;
import com.formedit.entity.WorkflowInstance;
import com.formedit.entity.WorkflowTask;
import com.formedit.repository.FormRepository;
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

    private static final java.util.concurrent.ConcurrentHashMap<String, Object> NODE_LOCKS =
            new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Set<String> FINALIZED_NODES =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    private final WorkflowInstanceRepository instanceRepository;
    private final WorkflowTaskRepository taskRepository;
    private final WorkflowExecutionLogRepository executionLogRepository;
    private final WorkflowDefinitionService definitionService;
    private final FormRepository formRepository;
    private final FormService formService;
    private final ObjectMapper objectMapper;

    public WorkflowInstanceService(WorkflowInstanceRepository instanceRepository,
                                    WorkflowTaskRepository taskRepository,
                                    WorkflowExecutionLogRepository executionLogRepository,
                                    WorkflowDefinitionService definitionService,
                                    FormRepository formRepository,
                                    FormService formService,
                                    ObjectMapper objectMapper) {
        this.instanceRepository = instanceRepository;
        this.taskRepository = taskRepository;
        this.executionLogRepository = executionLogRepository;
        this.definitionService = definitionService;
        this.formRepository = formRepository;
        this.formService = formService;
        this.objectMapper = objectMapper;
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
        return startInstance(definitionId, null, null);
    }

    @Transactional
    public WorkflowInstance startInstance(Long definitionId, Long formId, Map<String, Object> formData) {
        WorkflowDefinitionDto definition = definitionService.getDefinitionDtoById(definitionId);
        if (definition == null) {
            throw new IllegalArgumentException("流程定义不存在");
        }

        Long boundFormId = definition.getFormId();

        if (formId != null) {
            if (boundFormId == null) {
                throw new IllegalArgumentException("该流程未绑定表单，启动时不能传入formId");
            }
            if (!formId.equals(boundFormId)) {
                throw new IllegalArgumentException("传入的formId与流程定义绑定的表单不匹配");
            }
        } else if (boundFormId != null) {
            throw new IllegalArgumentException("该流程已绑定表单，启动时必须传入formId");
        }

        WorkflowInstance instance = new WorkflowInstance();
        instance.setDefinitionId(definitionId);
        instance.setDefinitionName(definition.getName());
        instance.setStatus("RUNNING");

        if (formId != null) {
            Form form = formRepository.findById(formId)
                    .orElseThrow(() -> new IllegalArgumentException("表单不存在"));
            instance.setFormId(formId);
            instance.setFormName(form.getName());
            if (formData == null) {
                throw new IllegalArgumentException("该流程绑定了表单，启动时必须传入formData");
            }
            ValidationResult validationResult = formService.validateForm(formId, formData);
            if (!validationResult.isValid()) {
                String errorMsg = "表单验证失败: " + String.join(", ", validationResult.getErrors().values());
                throw new IllegalArgumentException(errorMsg);
            }
            instance.setFormDataJson(convertFormDataToJson(formData));
        }

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
        task.setAction(action);
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

            long approveCount = 0;
            long rejectCount = 0;
            for (WorkflowTask t : allNodeTasks) {
                if ("COMPLETED".equals(t.getStatus()) && t.getAction() != null) {
                    if ("approve".equals(t.getAction())) {
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

            String finalizeKey = instanceId + "_" + currentNode.getId();

            if (FINALIZED_NODES.contains(finalizeKey)) {
                return instance;
            }
            if (isCountersignFinalized(instanceId, currentNode.getId())) {
                FINALIZED_NODES.add(finalizeKey);
                return instance;
            }

            Object lock = NODE_LOCKS.computeIfAbsent(finalizeKey, k -> new Object());
            synchronized (lock) {
                if (FINALIZED_NODES.contains(finalizeKey)) {
                    return instance;
                }
                if (isCountersignFinalized(instanceId, currentNode.getId())) {
                    FINALIZED_NODES.add(finalizeKey);
                    return instance;
                }

                List<WorkflowTask> freshPendingTasks =
                        taskRepository.findByInstanceIdAndNodeIdAndStatusOrderByIdAsc(instanceId, currentNode.getId(), "PENDING");
                List<WorkflowTask> freshAllTasks =
                        taskRepository.findByInstanceIdAndNodeIdOrderByIdAsc(instanceId, currentNode.getId());
                long freshApprove = 0;
                long freshReject = 0;
                for (WorkflowTask t : freshAllTasks) {
                    if ("COMPLETED".equals(t.getStatus()) && t.getAction() != null) {
                        if ("approve".equals(t.getAction())) freshApprove++;
                        else freshReject++;
                    }
                }
                long freshTotal = freshAllTasks.size();

                boolean doFinalize = false;
                String finalResult = null;

                if ("veto".equals(countersignType) && "reject".equals(action)) {
                    doFinalize = true;
                    finalResult = "reject";
                } else if ("all".equals(countersignType) && "reject".equals(action)) {
                    doFinalize = true;
                    finalResult = "reject";
                }

                if (!doFinalize && "majority".equals(countersignType) && freshPendingTasks != null && !freshPendingTasks.isEmpty()) {
                    long remaining = freshPendingTasks.size();
                    if (freshApprove > freshTotal / 2) {
                        doFinalize = true;
                        finalResult = "approve";
                    } else if (freshApprove + remaining <= freshTotal / 2) {
                        doFinalize = true;
                        finalResult = "reject";
                    }
                }

                if (!doFinalize && (freshPendingTasks == null || freshPendingTasks.isEmpty())) {
                    finalResult = evaluateCountersignResult(instance, currentNode);
                    doFinalize = true;
                }

                if (doFinalize) {
                    FINALIZED_NODES.add(finalizeKey);
                    if (freshPendingTasks != null) {
                        for (WorkflowTask pendingTask : freshPendingTasks) {
                            pendingTask.setStatus("CANCELLED");
                            pendingTask.setComment("会签已结束，任务自动取消");
                            pendingTask.setCompletedAt(LocalDateTime.now());
                            taskRepository.save(pendingTask);
                        }
                    }
                    addExecutionLog(instanceId, currentNode.getId(), currentNode.getName(), "countersign",
                            "会签结果: " + ("approve".equals(finalResult) ? "通过" : "拒绝"), null);
                    proceedToNextNode(instance, definition, currentNode, finalResult);
                } else {
                    addExecutionLog(instanceId, currentNode.getId(), currentNode.getName(), "countersign",
                            "等待其他审批人...", "剩余 " + freshPendingTasks.size() + " 人未审批");
                }
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

        long approveCount = 0;
        long rejectCount = 0;

        for (WorkflowTask task : allTasks) {
            if ("COMPLETED".equals(task.getStatus()) && task.getAction() != null) {
                if ("approve".equals(task.getAction())) {
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

    private boolean isCountersignFinalized(Long instanceId, String nodeId) {
        List<WorkflowExecutionLog> nodeLogs = executionLogRepository.findByInstanceIdAndNodeIdOrderByIdAsc(instanceId, nodeId);
        for (WorkflowExecutionLog log : nodeLogs) {
            if (log.getAction() != null && log.getAction().startsWith("会签结果:")) {
                return true;
            }
        }
        return false;
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
        if (!"RUNNING".equals(instance.getStatus())) {
            return;
        }
        WorkflowDefinitionDto.Edge nextEdge = findNextEdge(definition, currentNode, lastAction, instance);

        if (!"RUNNING".equals(instance.getStatus())) {
            return;
        }

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
                                                    WorkflowDefinitionDto.Node currentNode, String lastAction,
                                                    WorkflowInstance instance) {
        List<WorkflowDefinitionDto.Edge> outgoingEdges = definition.getEdges().stream()
                .filter(e -> e.getSource().equals(currentNode.getId()))
                .collect(Collectors.toList());

        if (outgoingEdges.isEmpty()) {
            return null;
        }

        boolean isBranchNode = "condition".equals(currentNode.getType())
                || "approval".equals(currentNode.getType())
                || "countersign".equals(currentNode.getType());

        if (isBranchNode && outgoingEdges.size() > 1) {
            for (WorkflowDefinitionDto.Edge edge : outgoingEdges) {
                String bt = edge.getBranchType();
                if (bt == null || bt.trim().isEmpty()) {
                    String errMsg = "节点 \"" + currentNode.getName() + "\" (" + currentNode.getType()
                            + ") 存在多条出边但连线 \"" + (edge.getLabel() != null ? edge.getLabel() : edge.getId())
                            + "\" 缺少 branchType（批准路径/拒绝路径），无法路由";
                    addExecutionLog(instance.getId(), currentNode.getId(), currentNode.getName(),
                            currentNode.getType(), "路由失败", errMsg);
                    failInstance(instance, errMsg);
                    return null;
                }
                if (!"approve".equals(bt) && !"reject".equals(bt)) {
                    String errMsg = "节点 \"" + currentNode.getName() + "\" (" + currentNode.getType()
                            + ") 出边 \"" + (edge.getLabel() != null ? edge.getLabel() : edge.getId())
                            + "\" 的 branchType=\"" + bt + "\" 无效，只能是 approve 或 reject";
                    addExecutionLog(instance.getId(), currentNode.getId(), currentNode.getName(),
                            currentNode.getType(), "路由失败", errMsg);
                    failInstance(instance, errMsg);
                    return null;
                }
            }

            String expectedBranchType = "approve".equals(lastAction) ? "approve" : "reject";
            String nodeTypeDesc = "condition".equals(currentNode.getType()) ? "条件分支节点"
                    : "approval".equals(currentNode.getType()) ? "审批节点" : "会签节点";

            WorkflowDefinitionDto.Edge matched = outgoingEdges.stream()
                    .filter(e -> expectedBranchType.equals(e.getBranchType()))
                    .findFirst()
                    .orElse(null);

            if (matched == null) {
                long approveCount = outgoingEdges.stream().filter(e -> "approve".equals(e.getBranchType())).count();
                long rejectCount = outgoingEdges.stream().filter(e -> "reject".equals(e.getBranchType())).count();
                String errMsg = nodeTypeDesc + " \"" + currentNode.getName() + "\" 需要匹配 branchType=\""
                        + expectedBranchType + "\" 的出边（lastAction=" + lastAction
                        + "），但未找到。当前批准路径: " + approveCount + " 条, 拒绝路径: " + rejectCount + " 条";
                addExecutionLog(instance.getId(), currentNode.getId(), currentNode.getName(),
                        currentNode.getType(), "路由失败", errMsg);
                failInstance(instance, errMsg);
                return null;
            }

            return matched;
        }

        return outgoingEdges.get(0);
    }

    private void failInstance(WorkflowInstance instance, String reason) {
        instance.setStatus("ERROR");
        instance.setEndedAt(LocalDateTime.now());
        instanceRepository.save(instance);
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
                    String errMsg = "条件节点 \"" + conditionNode.getName()
                            + "\" 表达式解析失败: " + e.getMessage()
                            + "，表达式=\"" + expression + "\"";
                    addExecutionLog(instance.getId(), conditionNode.getId(), conditionNode.getName(),
                            "condition", "表达式解析失败", errMsg);
                    failInstance(instance, errMsg);
                    return null;
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

        if (instance.getFormDataJson() != null && !instance.getFormDataJson().isEmpty()) {
            Map<String, Object> formData = parseFormDataFromJson(instance.getFormDataJson());
            for (Map.Entry<String, Object> entry : formData.entrySet()) {
                context.put("form_" + entry.getKey(), entry.getValue());
                context.put(entry.getKey(), entry.getValue());
            }
            if (instance.getFormId() != null) {
                context.put("formId", instance.getFormId());
            }
            if (instance.getFormName() != null) {
                context.put("formName", instance.getFormName());
            }
        }

        return context;
    }

    private boolean evaluateExpression(String expression, Map<String, Object> context) {
        return ExpressionEvaluator.evaluate(expression, context);
    }

    private void processNode(WorkflowInstance instance, WorkflowDefinitionDto definition,
                             WorkflowDefinitionDto.Node node) {
        if (!"RUNNING".equals(instance.getStatus())) {
            return;
        }
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
                if (conditionAction == null) {
                    return;
                }
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

    public Map<String, Object> getInstanceFormData(Long instanceId) {
        return instanceRepository.findById(instanceId)
                .map(instance -> {
                    if (instance.getFormDataJson() == null || instance.getFormDataJson().isEmpty()) {
                        return new HashMap<String, Object>();
                    }
                    return parseFormDataFromJson(instance.getFormDataJson());
                })
                .orElse(new HashMap<>());
    }

    private String convertFormDataToJson(Map<String, Object> formData) {
        if (formData == null) return null;
        try {
            return objectMapper.writeValueAsString(formData);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Map<String, Object> parseFormDataFromJson(String json) {
        if (json == null || json.isEmpty()) return new HashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}
