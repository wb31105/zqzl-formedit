package com.formedit.controller;

import com.formedit.dto.PageResponse;
import com.formedit.dto.WorkflowDefinitionDto;
import com.formedit.dto.WorkflowValidationResult;
import com.formedit.entity.WorkflowDefinition;
import com.formedit.service.WorkflowDefinitionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<WorkflowDefinitionDto> getDefinitionById(@PathVariable Long id) {
        WorkflowDefinitionDto dto = definitionService.getDefinitionDtoById(id);
        if (dto != null) {
            return ResponseEntity.ok(dto);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<?> createDefinition(@Valid @RequestBody WorkflowDefinitionDto dto) {
        try {
            WorkflowDefinition definition = definitionService.createDefinition(dto);
            return ResponseEntity.ok(convertToResponse(definition));
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDefinition(@PathVariable Long id, @Valid @RequestBody WorkflowDefinitionDto dto) {
        try {
            return definitionService.updateDefinition(id, dto)
                    .map(definition -> ResponseEntity.ok(convertToResponse(definition)))
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDefinition(@PathVariable Long id) {
        if (definitionService.deleteDefinition(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
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
