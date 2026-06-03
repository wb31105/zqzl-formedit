package com.formedit.controller;

import com.formedit.dto.PageResponse;
import com.formedit.dto.WorkflowTaskDto;
import com.formedit.entity.WorkflowExecutionLog;
import com.formedit.entity.WorkflowInstance;
import com.formedit.entity.WorkflowTask;
import com.formedit.service.WorkflowInstanceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Map<String, Object>> getInstanceById(@PathVariable Long id) {
        return instanceService.getInstanceById(id)
                .map(instance -> ResponseEntity.ok(convertToDetailResponse(instance)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/logs")
    public ResponseEntity<List<WorkflowExecutionLog>> getExecutionLogs(@PathVariable Long id) {
        if (!instanceService.getInstanceById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(instanceService.getExecutionLogs(id));
    }

    @GetMapping("/{id}/tasks")
    public ResponseEntity<List<WorkflowTask>> getInstanceTasks(@PathVariable Long id) {
        if (!instanceService.getInstanceById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(instanceService.getInstanceTasks(id));
    }

    @GetMapping("/{id}/pending-tasks")
    public ResponseEntity<List<WorkflowTask>> getPendingTasks(@PathVariable Long id) {
        if (!instanceService.getInstanceById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(instanceService.getPendingTasks(id));
    }

    @PostMapping("/start/{definitionId}")
    public ResponseEntity<?> startInstance(@PathVariable Long definitionId) {
        try {
            WorkflowInstance instance = instanceService.startInstance(definitionId);
            return ResponseEntity.ok(convertToDetailResponse(instance));
        } catch (IllegalArgumentException | IllegalStateException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/{instanceId}/complete-task")
    public ResponseEntity<?> completeTask(
            @PathVariable Long instanceId,
            @Valid @RequestBody WorkflowTaskDto taskDto) {
        try {
            WorkflowInstance instance = instanceService.completeTask(
                    instanceId,
                    taskDto.getTaskId(),
                    taskDto.getAction(),
                    taskDto.getComment(),
                    taskDto.getAssignee()
            );
            return ResponseEntity.ok(convertToDetailResponse(instance));
        } catch (IllegalArgumentException | IllegalStateException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInstance(@PathVariable Long id) {
        if (instanceService.deleteInstance(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
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
        return response;
    }

    private Map<String, Object> convertToDetailResponse(WorkflowInstance instance) {
        Map<String, Object> response = convertToResponse(instance);
        response.put("tasks", instanceService.getInstanceTasks(instance.getId()));
        response.put("logs", instanceService.getExecutionLogs(instance.getId()));
        response.put("pendingTasks", instanceService.getPendingTasks(instance.getId()));
        return response;
    }
}
