package com.bw.flowform.service;

import com.fasterxml.jackson.core.type.TypeReference;
import static com.bw.flowform.utils.JsonUtils.*;
import com.bw.flowform.common.NodeProperties;
import com.bw.flowform.dto.ValidationResult;
import com.bw.flowform.dto.WorkflowDefinitionDto;
import com.bw.flowform.entity.Form;
import com.bw.flowform.entity.WorkflowExecutionLog;
import com.bw.flowform.entity.WorkflowInstance;
import com.bw.flowform.entity.WorkflowTask;
import com.bw.flowform.enums.ApprovalAction;
import com.bw.flowform.enums.BranchType;
import com.bw.flowform.enums.CountersignType;
import com.bw.flowform.enums.InstanceStatus;
import com.bw.flowform.enums.NodeType;
import com.bw.flowform.enums.TaskStatus;
import com.bw.flowform.repository.FormRepository;
import com.bw.flowform.repository.WorkflowExecutionLogRepository;
import com.bw.flowform.repository.WorkflowInstanceRepository;
import com.bw.flowform.repository.WorkflowTaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@Service
public class WorkflowInstanceService {

    private static final java.util.concurrent.ConcurrentHashMap<String, Object> NODE_LOCKS =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static final Map<CountersignType, BiFunction<Long, Long, String>> COUNTERSIGN_STRATEGIES = new EnumMap<>(CountersignType.class);

    static {
        COUNTERSIGN_STRATEGIES.put(CountersignType.VETO, (approve, reject) ->
                reject > 0 ? ApprovalAction.REJECT.getCode() : ApprovalAction.APPROVE.getCode());
        COUNTERSIGN_STRATEGIES.put(CountersignType.MAJORITY, (approve, reject) -> {
            long total = approve + reject;
            return approve > total / 2 ? ApprovalAction.APPROVE.getCode() : ApprovalAction.REJECT.getCode();
        });
        COUNTERSIGN_STRATEGIES.put(CountersignType.ALL, (approve, reject) -> {
            long total = approve + reject;
            return (approve == total && total > 0) ? ApprovalAction.APPROVE.getCode() : ApprovalAction.REJECT.getCode();
        });
    }

    private final WorkflowInstanceRepository instanceRepository;
    private final WorkflowTaskRepository taskRepository;
    private final WorkflowExecutionLogRepository executionLogRepository;
    private final WorkflowDefinitionService definitionService;
    private final FormRepository formRepository;
    private final FormService formService;

    public WorkflowInstanceService(WorkflowInstanceRepository instanceRepository,
                                    WorkflowTaskRepository taskRepository,
                                    WorkflowExecutionLogRepository executionLogRepository,
                                    WorkflowDefinitionService definitionService,
                                    FormRepository formRepository,
                                    FormService formService) {
        this.instanceRepository = instanceRepository;
        this.taskRepository = taskRepository;
        this.executionLogRepository = executionLogRepository;
        this.definitionService = definitionService;
        this.formRepository = formRepository;
        this.formService = formService;
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
        return taskRepository.findByInstanceIdAndStatusOrderByIdAsc(instanceId, TaskStatus.PENDING.getCode());
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
        instance.setStatus(InstanceStatus.RUNNING.getCode());

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

        addExecutionLog(instance.getId(), NodeType.START.getCode(), "开始", NodeType.START.getCode(), "流程启动", null);

        WorkflowDefinitionDto.Node startNode = definition.getNodes().stream()
                .filter(n -> NodeType.START.getCode().equals(n.getType()))
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

        if (!InstanceStatus.RUNNING.getCode().equals(instance.getStatus())) {
            throw new IllegalStateException("流程实例不是运行状态");
        }

        WorkflowTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在"));

        if (!task.getInstanceId().equals(instanceId)) {
            throw new IllegalArgumentException("任务不属于该流程实例");
        }

        if (!TaskStatus.PENDING.getCode().equals(task.getStatus())) {
            throw new IllegalStateException("任务不是待处理状态");
        }

        task.setStatus(TaskStatus.COMPLETED.getCode());
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

        if (NodeType.COUNTERSIGN.getCode().equals(currentNode.getType())) {
            String actionDisplay = ApprovalAction.APPROVE.getCode().equals(action) ? "批准" : "拒绝";
            addExecutionLog(instanceId, task.getNodeId(), task.getNodeName(), NodeType.COUNTERSIGN.getCode(),
                    action,
                    (assignee != null ? assignee : "系统管理员") + ": " + (comment != null ? comment : actionDisplay));

            String countersignType = NodeProperties.getCountersignType(currentNode.getProperties());

            if (!currentNode.getId().equals(instance.getCurrentNodeId())) {
                return instance;
            }

            String finalizeKey = instanceId + "_" + currentNode.getId();
            Object lock = NODE_LOCKS.computeIfAbsent(finalizeKey, k -> new Object());
            synchronized (lock) {
                WorkflowInstance freshInstance = instanceRepository.findById(instanceId).orElse(instance);
                if (!currentNode.getId().equals(freshInstance.getCurrentNodeId())) {
                    return freshInstance;
                }

                List<WorkflowTask> freshPendingTasks =
                        taskRepository.findByInstanceIdAndNodeIdAndStatusOrderByIdAsc(instanceId, currentNode.getId(), TaskStatus.PENDING.getCode());
                List<WorkflowTask> freshAllTasks =
                        taskRepository.findByInstanceIdAndNodeIdOrderByIdAsc(instanceId, currentNode.getId());
                long freshApprove = 0;
                long freshReject = 0;
                for (WorkflowTask t : freshAllTasks) {
                    if (TaskStatus.COMPLETED.getCode().equals(t.getStatus()) && t.getAction() != null) {
                        if (ApprovalAction.APPROVE.getCode().equals(t.getAction())) freshApprove++;
                        else freshReject++;
                    }
                }
                long freshTotal = freshAllTasks.size();

                boolean doFinalize = false;
                String finalResult = null;

                if (CountersignType.VETO.getCode().equals(countersignType) && ApprovalAction.REJECT.getCode().equals(action)) {
                    doFinalize = true;
                    finalResult = ApprovalAction.REJECT.getCode();
                } else if (CountersignType.ALL.getCode().equals(countersignType) && ApprovalAction.REJECT.getCode().equals(action)) {
                    doFinalize = true;
                    finalResult = ApprovalAction.REJECT.getCode();
                }

                if (!doFinalize && CountersignType.MAJORITY.getCode().equals(countersignType) && freshPendingTasks != null && !freshPendingTasks.isEmpty()) {
                    long remaining = freshPendingTasks.size();
                    if (freshApprove > freshTotal / 2) {
                        doFinalize = true;
                        finalResult = ApprovalAction.APPROVE.getCode();
                    } else if (freshApprove + remaining <= freshTotal / 2) {
                        doFinalize = true;
                        finalResult = ApprovalAction.REJECT.getCode();
                    }
                }

                if (!doFinalize && (freshPendingTasks == null || freshPendingTasks.isEmpty())) {
                    finalResult = evaluateCountersignResult(instance, currentNode);
                    doFinalize = true;
                }

                if (doFinalize) {
                    if (freshPendingTasks != null) {
                        for (WorkflowTask pendingTask : freshPendingTasks) {
                            pendingTask.setStatus(TaskStatus.CANCELLED.getCode());
                            pendingTask.setComment("会签已结束，任务自动取消");
                            pendingTask.setCompletedAt(LocalDateTime.now());
                            taskRepository.save(pendingTask);
                        }
                    }
                    String finalResultDisplay = ApprovalAction.APPROVE.getCode().equals(finalResult) ? "通过" : "拒绝";
                    addExecutionLog(instanceId, currentNode.getId(), currentNode.getName(), NodeType.COUNTERSIGN.getCode(),
                            finalResult, "会签结果: " + finalResultDisplay);
                    proceedToNextNode(freshInstance, definition, currentNode, finalResult);
                    return freshInstance;
                } else {
                    addExecutionLog(instanceId, currentNode.getId(), currentNode.getName(), NodeType.COUNTERSIGN.getCode(),
                            "等待其他审批人", "剩余 " + freshPendingTasks.size() + " 人未审批");
                }
            }
        } else {
            String actionDisplay = ApprovalAction.APPROVE.getCode().equals(action) ? "批准" : "拒绝";
            addExecutionLog(instanceId, task.getNodeId(), task.getNodeName(), NodeType.APPROVAL.getCode(),
                    action, comment != null ? comment : actionDisplay);
            proceedToNextNode(instance, definition, currentNode, action);
        }

        return instance;
    }

    private String evaluateCountersignResult(WorkflowInstance instance, WorkflowDefinitionDto.Node countersignNode) {
        List<WorkflowTask> allTasks = taskRepository.findByInstanceIdAndNodeIdOrderByIdAsc(instance.getId(), countersignNode.getId());

        long approveCount = 0;
        long rejectCount = 0;

        for (WorkflowTask task : allTasks) {
            if (TaskStatus.COMPLETED.getCode().equals(task.getStatus()) && task.getAction() != null) {
                if (ApprovalAction.APPROVE.getCode().equals(task.getAction())) {
                    approveCount++;
                } else {
                    rejectCount++;
                }
            }
        }

        String countersignTypeCode = NodeProperties.getCountersignType(countersignNode.getProperties());
        CountersignType countersignType = CountersignType.fromCode(countersignTypeCode);
        if (countersignType == null) {
            countersignType = CountersignType.ALL;
        }

        BiFunction<Long, Long, String> strategy = COUNTERSIGN_STRATEGIES.get(countersignType);
        return strategy.apply(approveCount, rejectCount);
    }

    private void proceedToNextNode(WorkflowInstance instance, WorkflowDefinitionDto definition,
                                   WorkflowDefinitionDto.Node currentNode) {
        proceedToNextNode(instance, definition, currentNode, null);
    }

    private void proceedToNextNode(WorkflowInstance instance, WorkflowDefinitionDto definition,
                                   WorkflowDefinitionDto.Node currentNode, String lastAction) {
        if (!InstanceStatus.RUNNING.getCode().equals(instance.getStatus())) {
            return;
        }
        WorkflowDefinitionDto.Edge nextEdge = findNextEdge(definition, currentNode, lastAction, instance);

        if (!InstanceStatus.RUNNING.getCode().equals(instance.getStatus())) {
            return;
        }

        if (nextEdge == null) {
            instance.setStatus(InstanceStatus.COMPLETED.getCode());
            instance.setCurrentNodeId(null);
            instance.setCurrentNodeName(null);
            instance.setEndedAt(LocalDateTime.now());
            instanceRepository.save(instance);

            WorkflowDefinitionDto.Node endNode = definition.getNodes().stream()
                    .filter(n -> NodeType.END.getCode().equals(n.getType()))
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

        NodeType currentNodeType = NodeType.fromCode(currentNode.getType());
        boolean isBranchNode = currentNodeType == NodeType.CONDITION
                || currentNodeType == NodeType.APPROVAL
                || currentNodeType == NodeType.COUNTERSIGN;

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
                if (!BranchType.APPROVE.getCode().equals(bt) && !BranchType.REJECT.getCode().equals(bt)) {
                    String errMsg = "节点 \"" + currentNode.getName() + "\" (" + currentNode.getType()
                            + ") 出边 \"" + (edge.getLabel() != null ? edge.getLabel() : edge.getId())
                            + "\" 的 branchType=\"" + bt + "\" 无效，只能是 approve 或 reject";
                    addExecutionLog(instance.getId(), currentNode.getId(), currentNode.getName(),
                            currentNode.getType(), "路由失败", errMsg);
                    failInstance(instance, errMsg);
                    return null;
                }
            }

            String expectedBranchType = ApprovalAction.APPROVE.getCode().equals(lastAction) ? BranchType.APPROVE.getCode() : BranchType.REJECT.getCode();
            String nodeTypeDesc = NodeType.CONDITION == currentNodeType ? "条件分支节点"
                    : NodeType.APPROVAL == currentNodeType ? "审批节点" : "会签节点";

            WorkflowDefinitionDto.Edge matched = outgoingEdges.stream()
                    .filter(e -> expectedBranchType.equals(e.getBranchType()))
                    .findFirst()
                    .orElse(null);

            if (matched == null) {
                long approveCount = outgoingEdges.stream().filter(e -> BranchType.APPROVE.getCode().equals(e.getBranchType())).count();
                long rejectCount = outgoingEdges.stream().filter(e -> BranchType.REJECT.getCode().equals(e.getBranchType())).count();
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
        instance.setStatus(InstanceStatus.ERROR.getCode());
        instance.setEndedAt(LocalDateTime.now());
        instanceRepository.save(instance);
    }

    private String resolveConditionExpression(WorkflowDefinitionDto.Node conditionNode, WorkflowInstance instance) {
        String expression = NodeProperties.getExpression(conditionNode.getProperties());
        if (expression != null && !expression.trim().isEmpty()) {
            Map<String, Object> context = getInstanceContext(instance);
            try {
                boolean result = evaluateExpression(expression, context);
                return result ? ApprovalAction.APPROVE.getCode() : ApprovalAction.REJECT.getCode();
            } catch (Exception e) {
                String errMsg = "条件节点 \"" + conditionNode.getName()
                        + "\" 表达式解析失败: " + e.getMessage()
                        + "，表达式=\"" + expression + "\"";
                addExecutionLog(instance.getId(), conditionNode.getId(), conditionNode.getName(),
                        NodeType.CONDITION.getCode(), "表达式解析失败", errMsg);
                failInstance(instance, errMsg);
                return null;
            }
        }

        String lastApprovalAction = getLastApprovalAction(instance.getId());
        return lastApprovalAction != null ? lastApprovalAction : ApprovalAction.APPROVE.getCode();
    }

    private String getLastApprovalAction(Long instanceId) {
        List<WorkflowExecutionLog> logs = executionLogRepository.findByInstanceIdOrderByIdDesc(instanceId);
        for (WorkflowExecutionLog log : logs) {
            NodeType nodeType = NodeType.fromCode(log.getNodeType());
            if (nodeType == NodeType.APPROVAL || nodeType == NodeType.COUNTERSIGN) {
                if (ApprovalAction.APPROVE.getCode().equals(log.getAction())) {
                    return ApprovalAction.APPROVE.getCode();
                } else if (ApprovalAction.REJECT.getCode().equals(log.getAction())) {
                    return ApprovalAction.REJECT.getCode();
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
        if (!InstanceStatus.RUNNING.getCode().equals(instance.getStatus())) {
            return;
        }
        NodeType nodeType = NodeType.fromCode(node.getType());
        if (nodeType == null) {
            proceedToNextNode(instance, definition, node);
            return;
        }
        switch (nodeType) {
            case END:
                instance.setStatus(InstanceStatus.COMPLETED.getCode());
                instance.setCurrentNodeId(null);
                instance.setCurrentNodeName(null);
                instance.setEndedAt(LocalDateTime.now());
                instanceRepository.save(instance);
                addExecutionLog(instance.getId(), node.getId(), node.getName(), node.getType(), "流程结束", null);
                break;

            case APPROVAL:
                WorkflowTask task = new WorkflowTask();
                task.setInstanceId(instance.getId());
                task.setNodeId(node.getId());
                task.setNodeName(node.getName());
                task.setStatus(TaskStatus.PENDING.getCode());
                String approver = NodeProperties.getApprover(node.getProperties());
                if (approver != null) {
                    task.setAssignee(approver);
                }
                taskRepository.save(task);
                addExecutionLog(instance.getId(), node.getId(), node.getName(), node.getType(), "等待审批", null);
                break;

            case COUNTERSIGN:
                Map<String, Object> countersignProps = node.getProperties();
                List<String> approvers = NodeProperties.getApprovers(countersignProps);
                if (approvers.isEmpty()) {
                    approvers.add("审批人1");
                    approvers.add("审批人2");
                }
                for (String approverItem : approvers) {
                    WorkflowTask csTask = new WorkflowTask();
                    csTask.setInstanceId(instance.getId());
                    csTask.setNodeId(node.getId());
                    csTask.setNodeName(node.getName());
                    csTask.setStatus(TaskStatus.PENDING.getCode());
                    csTask.setAssignee(approverItem);
                    taskRepository.save(csTask);
                }
                String countersignTypeCode = NodeProperties.getCountersignType(countersignProps);
                CountersignType csType = CountersignType.fromCode(countersignTypeCode);
                String typeDesc = csType != null ? csType.getDescription() : CountersignType.ALL.getDescription();
                addExecutionLog(instance.getId(), node.getId(), node.getName(), node.getType(),
                        "等待会签", "审批人: " + String.join(", ", approvers) + "，规则: " + typeDesc);
                break;

            case AUTO:
                addExecutionLog(instance.getId(), node.getId(), node.getName(), node.getType(), "执行自动任务", null);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                addExecutionLog(instance.getId(), node.getId(), node.getName(), node.getType(), "自动任务完成", null);
                proceedToNextNode(instance, definition, node);
                break;

            case CONDITION:
                addExecutionLog(instance.getId(), node.getId(), node.getName(), node.getType(), "条件判断", null);
                String conditionAction = resolveConditionExpression(node, instance);
                if (conditionAction == null) {
                    return;
                }
                String conditionDisplay = ApprovalAction.APPROVE.getCode().equals(conditionAction) ? "是" : "否";
                addExecutionLog(instance.getId(), node.getId(), node.getName(), node.getType(),
                        conditionAction, "条件判断结果: " + conditionDisplay);
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
        String result = toJson(formData);
        return result != null ? result : "{}";
    }

    private Map<String, Object> parseFormDataFromJson(String json) {
        Map<String, Object> result = fromJson(json, new TypeReference<Map<String, Object>>() {});
        return result != null ? result : new HashMap<>();
    }
}
