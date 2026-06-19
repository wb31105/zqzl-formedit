package com.bw.flowform.controller;

import com.bw.flowform.common.ErrorCode;
import com.bw.flowform.dto.PageResponse;
import com.bw.flowform.dto.WorkflowDefinitionDto;
import com.bw.flowform.dto.WorkflowValidationResult;
import com.bw.flowform.entity.WorkflowDefinition;
import com.bw.flowform.exception.ResourceNotFoundException;
import com.bw.flowform.service.WorkflowDefinitionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/workflow-definitions")
public class WorkflowDefinitionController {

    private final WorkflowDefinitionService definitionService;

    public WorkflowDefinitionController(WorkflowDefinitionService definitionService) {
        this.definitionService = definitionService;
    }

    @GetMapping
    public PageResponse<Map<String, Object>> getAllDefinitions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<WorkflowDefinition> definitionPage = definitionService.getAllDefinitions(pageable);
        return convertToPageResponse(definitionPage);
    }

    @GetMapping("/{id}")
    public WorkflowDefinitionDto getDefinitionById(@PathVariable Long id) {
        WorkflowDefinitionDto dto = definitionService.getDefinitionDtoById(id);
        if (dto == null) {
            throw new ResourceNotFoundException(ErrorCode.WORKFLOW_DEFINITION_NOT_FOUND, "ID=" + id);
        }
        return dto;
    }

    @PostMapping
    public Map<String, Object> createDefinition(@Valid @RequestBody WorkflowDefinitionDto dto) {
        WorkflowDefinition definition = definitionService.createDefinition(dto);
        return convertToResponse(definition);
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateDefinition(@PathVariable Long id, @Valid @RequestBody WorkflowDefinitionDto dto) {
        WorkflowDefinition definition = definitionService.updateDefinitionOrThrow(id, dto);
        return convertToResponse(definition);
    }

    @DeleteMapping("/{id}")
    public void deleteDefinition(@PathVariable Long id) {
        definitionService.deleteDefinitionOrThrow(id);
    }

    @PostMapping("/validate")
    public WorkflowValidationResult validateDefinition(@RequestBody WorkflowDefinitionDto dto) {
        return definitionService.validateDefinition(dto);
    }

    private PageResponse<Map<String, Object>> convertToPageResponse(Page<WorkflowDefinition> page) {
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

    private Map<String, Object> convertToResponse(WorkflowDefinition definition) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", definition.getId());
        response.put("name", definition.getName());
        response.put("description", definition.getDescription());
        response.put("formId", definition.getFormId());
        response.put("formName", definition.getFormName());
        response.put("createdAt", definition.getCreatedAt());
        response.put("updatedAt", definition.getUpdatedAt());
        return response;
    }
}
