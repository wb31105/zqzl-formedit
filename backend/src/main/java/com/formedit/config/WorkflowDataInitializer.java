package com.formedit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formedit.dto.WorkflowDefinitionDto;
import com.formedit.entity.WorkflowDefinition;
import com.formedit.repository.WorkflowDefinitionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class WorkflowDataInitializer implements CommandLineRunner {

    private final WorkflowDefinitionRepository definitionRepository;
    private final ObjectMapper objectMapper;

    public WorkflowDataInitializer(WorkflowDefinitionRepository definitionRepository,
                                    ObjectMapper objectMapper) {
        this.definitionRepository = definitionRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) {
        if (definitionRepository.count() == 0) {
            WorkflowDefinition simpleFlow = createSimpleApprovalFlow();
            WorkflowDefinition conditionFlow = createConditionBranchFlow();
            WorkflowDefinition multiStepFlow = createMultiStepApprovalFlow();
            WorkflowDefinition leaveApprovalFlow = createLeaveApprovalFlow();
            WorkflowDefinition countersignFlow = createCountersignApprovalFlow();
            WorkflowDefinition complexCountersignFlow = createComplexCountersignFlow();
            definitionRepository.saveAll(Arrays.asList(simpleFlow, conditionFlow, multiStepFlow, leaveApprovalFlow, countersignFlow, complexCountersignFlow));
        }
    }

    private WorkflowDefinition createSimpleApprovalFlow() {
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setName("简单审批流程");
        definition.setDescription("开始 → 审批 → 结束");

        List<WorkflowDefinitionDto.Node> nodes = new ArrayList<>();
        List<WorkflowDefinitionDto.Edge> edges = new ArrayList<>();

        WorkflowDefinitionDto.Node start = new WorkflowDefinitionDto.Node();
        start.setId("node-start");
        start.setType("start");
        start.setName("开始");
        start.setX(100);
        start.setY(150);
        nodes.add(start);

        WorkflowDefinitionDto.Node approval = new WorkflowDefinitionDto.Node();
        approval.setId("node-approval");
        approval.setType("approval");
        approval.setName("主管审批");
        approval.setX(300);
        approval.setY(150);
        nodes.add(approval);

        WorkflowDefinitionDto.Node end = new WorkflowDefinitionDto.Node();
        end.setId("node-end");
        end.setType("end");
        end.setName("结束");
        end.setX(500);
        end.setY(150);
        nodes.add(end);

        WorkflowDefinitionDto.Edge edge1 = new WorkflowDefinitionDto.Edge();
        edge1.setId("edge-1");
        edge1.setSource("node-start");
        edge1.setTarget("node-approval");
        edges.add(edge1);

        WorkflowDefinitionDto.Edge edge2 = new WorkflowDefinitionDto.Edge();
        edge2.setId("edge-2");
        edge2.setSource("node-approval");
        edge2.setTarget("node-end");
        edges.add(edge2);

        definition.setNodesJson(convertToJson(nodes));
        definition.setEdgesJson(convertToJson(edges));
        return definition;
    }

    private WorkflowDefinition createConditionBranchFlow() {
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setName("条件分支审批流程");
        definition.setDescription("开始 → 审批 → 条件判断 → 批准/拒绝");

        List<WorkflowDefinitionDto.Node> nodes = new ArrayList<>();
        List<WorkflowDefinitionDto.Edge> edges = new ArrayList<>();

        WorkflowDefinitionDto.Node start = new WorkflowDefinitionDto.Node();
        start.setId("node-start");
        start.setType("start");
        start.setName("开始");
        start.setX(80);
        start.setY(200);
        nodes.add(start);

        WorkflowDefinitionDto.Node approval = new WorkflowDefinitionDto.Node();
        approval.setId("node-approval");
        approval.setType("approval");
        approval.setName("申请审批");
        approval.setX(250);
        approval.setY(200);
        nodes.add(approval);

        WorkflowDefinitionDto.Node condition = new WorkflowDefinitionDto.Node();
        condition.setId("node-condition");
        condition.setType("condition");
        condition.setName("审批结果");
        condition.setX(420);
        condition.setY(200);
        nodes.add(condition);

        WorkflowDefinitionDto.Node approved = new WorkflowDefinitionDto.Node();
        approved.setId("node-approved");
        approved.setType("auto");
        approved.setName("批准通知");
        approved.setX(590);
        approved.setY(100);
        nodes.add(approved);

        WorkflowDefinitionDto.Node rejected = new WorkflowDefinitionDto.Node();
        rejected.setId("node-rejected");
        rejected.setType("auto");
        rejected.setName("拒绝通知");
        rejected.setX(590);
        rejected.setY(300);
        nodes.add(rejected);

        WorkflowDefinitionDto.Node end = new WorkflowDefinitionDto.Node();
        end.setId("node-end");
        end.setType("end");
        end.setName("结束");
        end.setX(760);
        end.setY(200);
        nodes.add(end);

        WorkflowDefinitionDto.Edge edge1 = new WorkflowDefinitionDto.Edge();
        edge1.setId("edge-1");
        edge1.setSource("node-start");
        edge1.setTarget("node-approval");
        edges.add(edge1);

        WorkflowDefinitionDto.Edge edge2 = new WorkflowDefinitionDto.Edge();
        edge2.setId("edge-2");
        edge2.setSource("node-approval");
        edge2.setTarget("node-condition");
        edges.add(edge2);

        WorkflowDefinitionDto.Edge edge3 = new WorkflowDefinitionDto.Edge();
        edge3.setId("edge-3");
        edge3.setSource("node-condition");
        edge3.setTarget("node-approved");
        edge3.setLabel("是");
        edges.add(edge3);

        WorkflowDefinitionDto.Edge edge4 = new WorkflowDefinitionDto.Edge();
        edge4.setId("edge-4");
        edge4.setSource("node-condition");
        edge4.setTarget("node-rejected");
        edge4.setLabel("否");
        edges.add(edge4);

        WorkflowDefinitionDto.Edge edge5 = new WorkflowDefinitionDto.Edge();
        edge5.setId("edge-5");
        edge5.setSource("node-approved");
        edge5.setTarget("node-end");
        edges.add(edge5);

        WorkflowDefinitionDto.Edge edge6 = new WorkflowDefinitionDto.Edge();
        edge6.setId("edge-6");
        edge6.setSource("node-rejected");
        edge6.setTarget("node-end");
        edges.add(edge6);

        definition.setNodesJson(convertToJson(nodes));
        definition.setEdgesJson(convertToJson(edges));
        return definition;
    }

    private WorkflowDefinition createMultiStepApprovalFlow() {
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setName("多步审批流程");
        definition.setDescription("开始 → 部门经理审批 → 总监审批 → 结束");

        List<WorkflowDefinitionDto.Node> nodes = new ArrayList<>();
        List<WorkflowDefinitionDto.Edge> edges = new ArrayList<>();

        WorkflowDefinitionDto.Node start = new WorkflowDefinitionDto.Node();
        start.setId("node-start");
        start.setType("start");
        start.setName("开始");
        start.setX(80);
        start.setY(150);
        nodes.add(start);

        WorkflowDefinitionDto.Node managerApproval = new WorkflowDefinitionDto.Node();
        managerApproval.setId("node-manager");
        managerApproval.setType("approval");
        managerApproval.setName("部门经理审批");
        managerApproval.setX(260);
        managerApproval.setY(150);
        nodes.add(managerApproval);

        WorkflowDefinitionDto.Node directorApproval = new WorkflowDefinitionDto.Node();
        directorApproval.setId("node-director");
        directorApproval.setType("approval");
        directorApproval.setName("总监审批");
        directorApproval.setX(440);
        directorApproval.setY(150);
        nodes.add(directorApproval);

        WorkflowDefinitionDto.Node autoTask = new WorkflowDefinitionDto.Node();
        autoTask.setId("node-auto");
        autoTask.setType("auto");
        autoTask.setName("发送通知");
        autoTask.setX(620);
        autoTask.setY(150);
        nodes.add(autoTask);

        WorkflowDefinitionDto.Node end = new WorkflowDefinitionDto.Node();
        end.setId("node-end");
        end.setType("end");
        end.setName("结束");
        end.setX(800);
        end.setY(150);
        nodes.add(end);

        WorkflowDefinitionDto.Edge edge1 = new WorkflowDefinitionDto.Edge();
        edge1.setId("edge-1");
        edge1.setSource("node-start");
        edge1.setTarget("node-manager");
        edges.add(edge1);

        WorkflowDefinitionDto.Edge edge2 = new WorkflowDefinitionDto.Edge();
        edge2.setId("edge-2");
        edge2.setSource("node-manager");
        edge2.setTarget("node-director");
        edges.add(edge2);

        WorkflowDefinitionDto.Edge edge3 = new WorkflowDefinitionDto.Edge();
        edge3.setId("edge-3");
        edge3.setSource("node-director");
        edge3.setTarget("node-auto");
        edges.add(edge3);

        WorkflowDefinitionDto.Edge edge4 = new WorkflowDefinitionDto.Edge();
        edge4.setId("edge-4");
        edge4.setSource("node-auto");
        edge4.setTarget("node-end");
        edges.add(edge4);

        definition.setNodesJson(convertToJson(nodes));
        definition.setEdgesJson(convertToJson(edges));
        return definition;
    }

    private WorkflowDefinition createLeaveApprovalFlow() {
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setName("员工请假审批流程（支持退回）");
        definition.setDescription("员工请假 → 部门经理审批（不批退回到员工）→ 经理审批（不批退回到部门经理）→ 发送通知 → 结束");

        List<WorkflowDefinitionDto.Node> nodes = new ArrayList<>();
        List<WorkflowDefinitionDto.Edge> edges = new ArrayList<>();

        WorkflowDefinitionDto.Node start = new WorkflowDefinitionDto.Node();
        start.setId("node-start");
        start.setType("start");
        start.setName("开始");
        start.setX(80);
        start.setY(250);
        nodes.add(start);

        WorkflowDefinitionDto.Node employeeSubmit = new WorkflowDefinitionDto.Node();
        employeeSubmit.setId("node-employee-submit");
        employeeSubmit.setType("approval");
        employeeSubmit.setName("员工提交申请");
        employeeSubmit.setX(260);
        employeeSubmit.setY(250);
        Map<String, Object> employeeProps = new HashMap<>();
        employeeProps.put("actionType", "submit");
        employeeProps.put("approveText", "提交");
        employeeProps.put("commentLabel", "请假理由");
        employeeProps.put("approver", "员工本人");
        employeeProps.put("description", "员工填写请假申请并提交");
        employeeSubmit.setProperties(employeeProps);
        nodes.add(employeeSubmit);

        WorkflowDefinitionDto.Node deptManagerApproval = new WorkflowDefinitionDto.Node();
        deptManagerApproval.setId("node-dept-manager");
        deptManagerApproval.setType("approval");
        deptManagerApproval.setName("部门经理审批");
        deptManagerApproval.setX(440);
        deptManagerApproval.setY(250);
        Map<String, Object> deptMgrProps = new HashMap<>();
        deptMgrProps.put("actionType", "review");
        deptMgrProps.put("approveText", "批准");
        deptMgrProps.put("rejectText", "退回");
        deptMgrProps.put("commentLabel", "审批意见");
        deptMgrProps.put("approver", "部门经理");
        deptMgrProps.put("description", "部门经理审批请假申请，不批准退回给员工修改");
        deptManagerApproval.setProperties(deptMgrProps);
        nodes.add(deptManagerApproval);

        WorkflowDefinitionDto.Node deptCondition = new WorkflowDefinitionDto.Node();
        deptCondition.setId("node-dept-condition");
        deptCondition.setType("condition");
        deptCondition.setName("部门经理审批结果");
        deptCondition.setX(620);
        deptCondition.setY(250);
        Map<String, Object> deptCondProps = new HashMap<>();
        deptCondProps.put("description", "根据部门经理审批结果判断流程走向：批准继续到经理，退回到员工");
        deptCondition.setProperties(deptCondProps);
        nodes.add(deptCondition);

        WorkflowDefinitionDto.Node managerApproval = new WorkflowDefinitionDto.Node();
        managerApproval.setId("node-manager");
        managerApproval.setType("approval");
        managerApproval.setName("经理审批");
        managerApproval.setX(800);
        managerApproval.setY(250);
        Map<String, Object> mgrProps = new HashMap<>();
        mgrProps.put("actionType", "review");
        mgrProps.put("approveText", "批准");
        mgrProps.put("rejectText", "退回");
        mgrProps.put("commentLabel", "审批意见");
        mgrProps.put("approver", "经理");
        mgrProps.put("description", "经理最终审批，不批准退回给部门经理重新审批");
        managerApproval.setProperties(mgrProps);
        nodes.add(managerApproval);

        WorkflowDefinitionDto.Node managerCondition = new WorkflowDefinitionDto.Node();
        managerCondition.setId("node-manager-condition");
        managerCondition.setType("condition");
        managerCondition.setName("经理审批结果");
        managerCondition.setX(980);
        managerCondition.setY(250);
        Map<String, Object> mgrCondProps = new HashMap<>();
        mgrCondProps.put("description", "根据经理审批结果判断流程走向：批准发送通知，退回到部门经理");
        managerCondition.setProperties(mgrCondProps);
        nodes.add(managerCondition);

        WorkflowDefinitionDto.Node sendNotification = new WorkflowDefinitionDto.Node();
        sendNotification.setId("node-notification");
        sendNotification.setType("auto");
        sendNotification.setName("发送审批结果通知");
        sendNotification.setX(1160);
        sendNotification.setY(250);
        Map<String, Object> notifyProps = new HashMap<>();
        notifyProps.put("taskType", "notification");
        notifyProps.put("description", "自动发送请假审批结果通知给员工");
        notifyProps.put("config", "发送请假审批结果邮件和系统消息");
        sendNotification.setProperties(notifyProps);
        nodes.add(sendNotification);

        WorkflowDefinitionDto.Node end = new WorkflowDefinitionDto.Node();
        end.setId("node-end");
        end.setType("end");
        end.setName("结束");
        end.setX(1340);
        end.setY(250);
        nodes.add(end);

        WorkflowDefinitionDto.Edge edge1 = new WorkflowDefinitionDto.Edge();
        edge1.setId("edge-1");
        edge1.setSource("node-start");
        edge1.setTarget("node-employee-submit");
        edges.add(edge1);

        WorkflowDefinitionDto.Edge edge2 = new WorkflowDefinitionDto.Edge();
        edge2.setId("edge-2");
        edge2.setSource("node-employee-submit");
        edge2.setTarget("node-dept-manager");
        edges.add(edge2);

        WorkflowDefinitionDto.Edge edge3 = new WorkflowDefinitionDto.Edge();
        edge3.setId("edge-3");
        edge3.setSource("node-dept-manager");
        edge3.setTarget("node-dept-condition");
        edges.add(edge3);

        WorkflowDefinitionDto.Edge edge4 = new WorkflowDefinitionDto.Edge();
        edge4.setId("edge-4");
        edge4.setSource("node-dept-condition");
        edge4.setTarget("node-employee-submit");
        edge4.setLabel("退回");
        edges.add(edge4);

        WorkflowDefinitionDto.Edge edge5 = new WorkflowDefinitionDto.Edge();
        edge5.setId("edge-5");
        edge5.setSource("node-dept-condition");
        edge5.setTarget("node-manager");
        edge5.setLabel("批准");
        edges.add(edge5);

        WorkflowDefinitionDto.Edge edge6 = new WorkflowDefinitionDto.Edge();
        edge6.setId("edge-6");
        edge6.setSource("node-manager");
        edge6.setTarget("node-manager-condition");
        edges.add(edge6);

        WorkflowDefinitionDto.Edge edge7 = new WorkflowDefinitionDto.Edge();
        edge7.setId("edge-7");
        edge7.setSource("node-manager-condition");
        edge7.setTarget("node-dept-manager");
        edge7.setLabel("退回");
        edges.add(edge7);

        WorkflowDefinitionDto.Edge edge8 = new WorkflowDefinitionDto.Edge();
        edge8.setId("edge-8");
        edge8.setSource("node-manager-condition");
        edge8.setTarget("node-notification");
        edge8.setLabel("批准");
        edges.add(edge8);

        WorkflowDefinitionDto.Edge edge9 = new WorkflowDefinitionDto.Edge();
        edge9.setId("edge-9");
        edge9.setSource("node-notification");
        edge9.setTarget("node-end");
        edges.add(edge9);

        definition.setNodesJson(convertToJson(nodes));
        definition.setEdgesJson(convertToJson(edges));
        return definition;
    }

    private WorkflowDefinition createCountersignApprovalFlow() {
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setName("会签审批流程（全部同意）");
        definition.setDescription("开始 → 提交申请 → 委员会会签（全部同意才通过）→ 结束");

        List<WorkflowDefinitionDto.Node> nodes = new ArrayList<>();
        List<WorkflowDefinitionDto.Edge> edges = new ArrayList<>();

        WorkflowDefinitionDto.Node start = new WorkflowDefinitionDto.Node();
        start.setId("node-start");
        start.setType("start");
        start.setName("开始");
        start.setX(80);
        start.setY(200);
        nodes.add(start);

        WorkflowDefinitionDto.Node submit = new WorkflowDefinitionDto.Node();
        submit.setId("node-submit");
        submit.setType("approval");
        submit.setName("提交申请");
        submit.setX(250);
        submit.setY(200);
        Map<String, Object> submitProps = new HashMap<>();
        submitProps.put("actionType", "submit");
        submitProps.put("approveText", "提交");
        submitProps.put("commentLabel", "申请理由");
        submitProps.put("approver", "申请人");
        submitProps.put("description", "请填写申请内容并提交");
        submit.setProperties(submitProps);
        nodes.add(submit);

        WorkflowDefinitionDto.Node countersign = new WorkflowDefinitionDto.Node();
        countersign.setId("node-countersign");
        countersign.setType("countersign");
        countersign.setName("委员会审批");
        countersign.setX(450);
        countersign.setY(200);
        Map<String, Object> countersignProps = new HashMap<>();
        countersignProps.put("approvers", "张主任,李副主任,王委员,赵委员");
        countersignProps.put("countersignType", "all");
        countersignProps.put("actionType", "approval");
        countersignProps.put("commentLabel", "会签意见");
        countersignProps.put("description", "需要所有委员全部同意才能通过");
        countersign.setProperties(countersignProps);
        nodes.add(countersign);

        WorkflowDefinitionDto.Node condition = new WorkflowDefinitionDto.Node();
        condition.setId("node-condition");
        condition.setType("condition");
        condition.setName("会签结果");
        condition.setX(650);
        condition.setY(200);
        nodes.add(condition);

        WorkflowDefinitionDto.Node approved = new WorkflowDefinitionDto.Node();
        approved.setId("node-approved");
        approved.setType("auto");
        approved.setName("通过通知");
        approved.setX(820);
        approved.setY(100);
        nodes.add(approved);

        WorkflowDefinitionDto.Node rejected = new WorkflowDefinitionDto.Node();
        rejected.setId("node-rejected");
        rejected.setType("auto");
        rejected.setName("拒绝通知");
        rejected.setX(820);
        rejected.setY(300);
        nodes.add(rejected);

        WorkflowDefinitionDto.Node end = new WorkflowDefinitionDto.Node();
        end.setId("node-end");
        end.setType("end");
        end.setName("结束");
        end.setX(1000);
        end.setY(200);
        nodes.add(end);

        WorkflowDefinitionDto.Edge edge1 = new WorkflowDefinitionDto.Edge();
        edge1.setId("edge-1");
        edge1.setSource("node-start");
        edge1.setTarget("node-submit");
        edges.add(edge1);

        WorkflowDefinitionDto.Edge edge2 = new WorkflowDefinitionDto.Edge();
        edge2.setId("edge-2");
        edge2.setSource("node-submit");
        edge2.setTarget("node-countersign");
        edges.add(edge2);

        WorkflowDefinitionDto.Edge edge3 = new WorkflowDefinitionDto.Edge();
        edge3.setId("edge-3");
        edge3.setSource("node-countersign");
        edge3.setTarget("node-condition");
        edges.add(edge3);

        WorkflowDefinitionDto.Edge edge4 = new WorkflowDefinitionDto.Edge();
        edge4.setId("edge-4");
        edge4.setSource("node-condition");
        edge4.setTarget("node-approved");
        edge4.setLabel("通过");
        edges.add(edge4);

        WorkflowDefinitionDto.Edge edge5 = new WorkflowDefinitionDto.Edge();
        edge5.setId("edge-5");
        edge5.setSource("node-condition");
        edge5.setTarget("node-rejected");
        edge5.setLabel("拒绝");
        edges.add(edge5);

        WorkflowDefinitionDto.Edge edge6 = new WorkflowDefinitionDto.Edge();
        edge6.setId("edge-6");
        edge6.setSource("node-approved");
        edge6.setTarget("node-end");
        edges.add(edge6);

        WorkflowDefinitionDto.Edge edge7 = new WorkflowDefinitionDto.Edge();
        edge7.setId("edge-7");
        edge7.setSource("node-rejected");
        edge7.setTarget("node-end");
        edges.add(edge7);

        definition.setNodesJson(convertToJson(nodes));
        definition.setEdgesJson(convertToJson(edges));
        return definition;
    }

    private WorkflowDefinition createComplexCountersignFlow() {
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setName("项目评审会签流程（一票否决）");
        definition.setDescription("开始 → 项目申报 → 技术评审会签（一票否决）→ 预算审批 → 结束");

        List<WorkflowDefinitionDto.Node> nodes = new ArrayList<>();
        List<WorkflowDefinitionDto.Edge> edges = new ArrayList<>();

        WorkflowDefinitionDto.Node start = new WorkflowDefinitionDto.Node();
        start.setId("node-start");
        start.setType("start");
        start.setName("开始");
        start.setX(80);
        start.setY(250);
        nodes.add(start);

        WorkflowDefinitionDto.Node declare = new WorkflowDefinitionDto.Node();
        declare.setId("node-declare");
        declare.setType("approval");
        declare.setName("项目申报");
        declare.setX(250);
        declare.setY(250);
        Map<String, Object> declareProps = new HashMap<>();
        declareProps.put("actionType", "submit");
        declareProps.put("approveText", "提交申报");
        declareProps.put("commentLabel", "项目说明");
        declareProps.put("approver", "项目负责人");
        declareProps.put("description", "请详细填写项目申报内容");
        declare.setProperties(declareProps);
        nodes.add(declare);

        WorkflowDefinitionDto.Node techCountersign = new WorkflowDefinitionDto.Node();
        techCountersign.setId("node-tech-countersign");
        techCountersign.setType("countersign");
        techCountersign.setName("技术评审会签");
        techCountersign.setX(450);
        techCountersign.setY(250);
        Map<String, Object> techProps = new HashMap<>();
        techProps.put("approvers", "技术专家A,技术专家B,技术专家C");
        techProps.put("countersignType", "veto");
        techProps.put("actionType", "review");
        techProps.put("approveText", "通过");
        techProps.put("rejectText", "否决");
        techProps.put("commentLabel", "技术评审意见");
        techProps.put("description", "技术评审委员会，任一专家否决则不通过");
        techCountersign.setProperties(techProps);
        nodes.add(techCountersign);

        WorkflowDefinitionDto.Node techCondition = new WorkflowDefinitionDto.Node();
        techCondition.setId("node-tech-condition");
        techCondition.setType("condition");
        techCondition.setName("技术评审结果");
        techCondition.setX(650);
        techCondition.setY(250);
        nodes.add(techCondition);

        WorkflowDefinitionDto.Node budgetApproval = new WorkflowDefinitionDto.Node();
        budgetApproval.setId("node-budget");
        budgetApproval.setType("countersign");
        budgetApproval.setName("预算审批会签");
        budgetApproval.setX(850);
        budgetApproval.setY(250);
        Map<String, Object> budgetProps = new HashMap<>();
        budgetProps.put("approvers", "财务主管,财务经理,CFO");
        budgetProps.put("countersignType", "majority");
        budgetProps.put("actionType", "approval");
        budgetProps.put("commentLabel", "预算审批意见");
        budgetProps.put("description", "财务部门会签，过半同意即可通过");
        budgetApproval.setProperties(budgetProps);
        nodes.add(budgetApproval);

        WorkflowDefinitionDto.Node finalApproval = new WorkflowDefinitionDto.Node();
        finalApproval.setId("node-final");
        finalApproval.setType("approval");
        finalApproval.setName("CEO终审");
        finalApproval.setX(1050);
        finalApproval.setY(250);
        Map<String, Object> finalProps = new HashMap<>();
        finalProps.put("actionType", "approval");
        finalProps.put("approver", "CEO");
        finalProps.put("commentLabel", "终审意见");
        finalProps.put("description", "CEO最终审批");
        finalApproval.setProperties(finalProps);
        nodes.add(finalApproval);

        WorkflowDefinitionDto.Node rejected = new WorkflowDefinitionDto.Node();
        rejected.setId("node-rejected");
        rejected.setType("auto");
        rejected.setName("评审不通过通知");
        rejected.setX(850);
        rejected.setY(400);
        nodes.add(rejected);

        WorkflowDefinitionDto.Node end = new WorkflowDefinitionDto.Node();
        end.setId("node-end");
        end.setType("end");
        end.setName("结束");
        end.setX(1250);
        end.setY(250);
        nodes.add(end);

        WorkflowDefinitionDto.Edge edge1 = new WorkflowDefinitionDto.Edge();
        edge1.setId("edge-1");
        edge1.setSource("node-start");
        edge1.setTarget("node-declare");
        edges.add(edge1);

        WorkflowDefinitionDto.Edge edge2 = new WorkflowDefinitionDto.Edge();
        edge2.setId("edge-2");
        edge2.setSource("node-declare");
        edge2.setTarget("node-tech-countersign");
        edges.add(edge2);

        WorkflowDefinitionDto.Edge edge3 = new WorkflowDefinitionDto.Edge();
        edge3.setId("edge-3");
        edge3.setSource("node-tech-countersign");
        edge3.setTarget("node-tech-condition");
        edges.add(edge3);

        WorkflowDefinitionDto.Edge edge4 = new WorkflowDefinitionDto.Edge();
        edge4.setId("edge-4");
        edge4.setSource("node-tech-condition");
        edge4.setTarget("node-budget");
        edge4.setLabel("通过");
        edges.add(edge4);

        WorkflowDefinitionDto.Edge edge5 = new WorkflowDefinitionDto.Edge();
        edge5.setId("edge-5");
        edge5.setSource("node-tech-condition");
        edge5.setTarget("node-rejected");
        edge5.setLabel("否决");
        edges.add(edge5);

        WorkflowDefinitionDto.Edge edge6 = new WorkflowDefinitionDto.Edge();
        edge6.setId("edge-6");
        edge6.setSource("node-budget");
        edge6.setTarget("node-final");
        edge6.setLabel("通过");
        edges.add(edge6);

        WorkflowDefinitionDto.Edge edge7 = new WorkflowDefinitionDto.Edge();
        edge7.setId("edge-7");
        edge7.setSource("node-budget");
        edge7.setTarget("node-rejected");
        edge7.setLabel("拒绝");
        edges.add(edge7);

        WorkflowDefinitionDto.Edge edge8 = new WorkflowDefinitionDto.Edge();
        edge8.setId("edge-8");
        edge8.setSource("node-final");
        edge8.setTarget("node-end");
        edges.add(edge8);

        WorkflowDefinitionDto.Edge edge9 = new WorkflowDefinitionDto.Edge();
        edge9.setId("edge-9");
        edge9.setSource("node-rejected");
        edge9.setTarget("node-end");
        edges.add(edge9);

        definition.setNodesJson(convertToJson(nodes));
        definition.setEdgesJson(convertToJson(edges));
        return definition;
    }

    private String convertToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }
}
