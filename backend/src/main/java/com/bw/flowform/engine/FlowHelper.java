package com.bw.flowform.engine;

import com.bw.flowform.common.NodeProperties;
import com.bw.flowform.common.WorkflowConstants;
import com.bw.flowform.entity.WorkflowExecutionLog;
import com.bw.flowform.entity.WorkflowInstance;
import com.bw.flowform.entity.WorkflowTask;
import com.bw.flowform.enums.InstanceStatus;
import com.bw.flowform.enums.TaskStatus;
import com.bw.flowform.repository.WorkflowExecutionLogRepository;
import com.bw.flowform.repository.WorkflowInstanceRepository;
import com.bw.flowform.repository.WorkflowTaskRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class FlowHelper {

    private final WorkflowInstanceRepository instanceRepository;
    private final WorkflowTaskRepository taskRepository;
    private final WorkflowExecutionLogRepository executionLogRepository;

    public FlowHelper(WorkflowInstanceRepository instanceRepository,
                       WorkflowTaskRepository taskRepository,
                       WorkflowExecutionLogRepository executionLogRepository) {
        this.instanceRepository = instanceRepository;
        this.taskRepository = taskRepository;
        this.executionLogRepository = executionLogRepository;
    }

    public void addExecutionLog(Long instanceId, String nodeId, String nodeName,
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

    public void failInstance(WorkflowInstance instance, String reason) {
        instance.setStatus(InstanceStatus.ERROR.getCode());
        instance.setEndedAt(LocalDateTime.now());
        instanceRepository.save(instance);
    }

    public WorkflowTask createPendingTask(Long instanceId, String nodeId, String nodeName, String assignee) {
        WorkflowTask task = new WorkflowTask();
        task.setInstanceId(instanceId);
        task.setNodeId(nodeId);
        task.setNodeName(nodeName);
        task.setStatus(TaskStatus.PENDING.getCode());
        if (assignee != null) {
            task.setAssignee(assignee);
        }
        return taskRepository.save(task);
    }

    public void saveInstance(WorkflowInstance instance) {
        instanceRepository.save(instance);
    }

    public WorkflowTaskRepository getTaskRepository() {
        return taskRepository;
    }

    public WorkflowInstanceRepository getInstanceRepository() {
        return instanceRepository;
    }

    public WorkflowExecutionLogRepository getExecutionLogRepository() {
        return executionLogRepository;
    }
}
