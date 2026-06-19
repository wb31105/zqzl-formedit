package com.bw.flowform.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import static com.bw.flowform.utils.JsonUtils.*;
import com.bw.flowform.common.ErrorCode;
import com.bw.flowform.dto.PageResponse;
import com.bw.flowform.dto.StartInstanceRequest;
import com.bw.flowform.dto.WorkflowTaskDto;
import com.bw.flowform.entity.WorkflowExecutionLog;
import com.bw.flowform.entity.WorkflowInstance;
import com.bw.flowform.entity.WorkflowTask;
import com.bw.flowform.exception.ResourceNotFoundException;
import com.bw.flowform.service.WorkflowInstanceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/workflow-instances")
public class WorkflowInstanceController {

    private final WorkflowInstanceService instanceService;

    public WorkflowInstanceController(WorkflowInstanceService instanceService) {
        this.instanceService = instanceService;
    }

    @GetMapping
    public PageResponse<Map<String, Object>> getAllInstances(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<WorkflowInstance> instancePage = instanceService.getAllInstances(pageable);
        return convertToPageResponse(instancePage);
    }

    @GetMapping("/definition/{definitionId}")
    public List<Map<String, Object>> getInstancesByDefinitionId(@PathVariable Long definitionId) {
        return instanceService.getInstancesByDefinitionId(definitionId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public Map<String, Object> getInstanceById(@PathVariable Long id) {
        WorkflowInstance instance = instanceService.getInstanceById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.WORKFLOW_INSTANCE_NOT_FOUND, "ID=" + id));
        return convertToDetailResponse(instance);
    }

    @GetMapping("/{id}/logs")
    public List<WorkflowExecutionLog> getExecutionLogs(@PathVariable Long id) {
        if (!instanceService.getInstanceById(id).isPresent()) {
            throw new ResourceNotFoundException(ErrorCode.WORKFLOW_INSTANCE_NOT_FOUND, "ID=" + id);
        }
        return instanceService.getExecutionLogs(id);
    }

    @GetMapping("/{id}/tasks")
    public List<WorkflowTask> getInstanceTasks(@PathVariable Long id) {
        if (!instanceService.getInstanceById(id).isPresent()) {
            throw new ResourceNotFoundException(ErrorCode.WORKFLOW_INSTANCE_NOT_FOUND, "ID=" + id);
        }
        return instanceService.getInstanceTasks(id);
    }

    @GetMapping("/{id}/pending-tasks")
    public List<WorkflowTask> getPendingTasks(@PathVariable Long id) {
        if (!instanceService.getInstanceById(id).isPresent()) {
            throw new ResourceNotFoundException(ErrorCode.WORKFLOW_INSTANCE_NOT_FOUND, "ID=" + id);
        }
        return instanceService.getPendingTasks(id);
    }

    @PostMapping("/start/{definitionId}")
    public Map<String, Object> startInstance(@PathVariable Long definitionId) {
        WorkflowInstance instance = instanceService.startInstance(definitionId);
        return convertToDetailResponse(instance);
    }

    @PostMapping("/start-with-form/{definitionId}")
    public Map<String, Object> startInstanceWithForm(@PathVariable Long definitionId,
                                                      @Valid @RequestBody(required = false) StartInstanceRequest request) {
        Long formId = null;
        Map<String, Object> formData = null;
        if (request != null) {
            formId = request.getFormId();
            formData = request.getFormData();
        }
        WorkflowInstance instance = instanceService.startInstance(definitionId, formId, formData);
        return convertToDetailResponse(instance);
    }

    @PostMapping("/{instanceId}/complete-task")
    public Map<String, Object> completeTask(
            @PathVariable Long instanceId,
            @Valid @RequestBody WorkflowTaskDto taskDto) {
        WorkflowInstance instance = instanceService.completeTask(
                instanceId,
                taskDto.getTaskId(),
                taskDto.getAction(),
                taskDto.getComment(),
                taskDto.getAssignee()
        );
        return convertToDetailResponse(instance);
    }

    @DeleteMapping("/{id}")
    public void deleteInstance(@PathVariable Long id) {
        instanceService.deleteInstanceOrThrow(id);
    }

    private PageResponse<Map<String, Object>> convertToPageResponse(Page<WorkflowInstance> page) {
        PageResponse<Map<String, Object>> response = new PageResponse<>();
        response.setContent(page.getContent().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList()));
        response.setTotalPages(page.getTotalPages());
        response.setTotalElements(page.getTotalElements());
        response.setPageNumber(page.getNumber());
        response.setPageSize(page.getSize());
        return response;
    }

    private Map<String, Object> convertToResponse(WorkflowInstance instance) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", instance.getId());
        response.put("definitionId", instance.getDefinitionId());
        response.put("definitionName", instance.getDefinitionName());
        response.put("status", instance.getStatus());
        response.put("currentNodeId", instance.getCurrentNodeId());
        response.put("currentNodeName", instance.getCurrentNodeName());
        response.put("startedAt", instance.getStartedAt());
        response.put("endedAt", instance.getEndedAt());
        response.put("formId", instance.getFormId());
        response.put("formName", instance.getFormName());
        response.put("formData", parseFormDataFromJson(instance.getFormDataJson()));
        return response;
    }

    private Map<String, Object> convertToDetailResponse(WorkflowInstance instance) {
        Map<String, Object> response = convertToResponse(instance);
        response.put("tasks", instanceService.getInstanceTasks(instance.getId()));
        response.put("logs", instanceService.getExecutionLogs(instance.getId()));
        response.put("pendingTasks", instanceService.getPendingTasks(instance.getId()));
        return response;
    }

    private Map<String, Object> parseFormDataFromJson(String json) {
        Map<String, Object> result = fromJson(json, new TypeReference<Map<String, Object>>() {});
        return result != null ? result : new HashMap<>();
    }
}
