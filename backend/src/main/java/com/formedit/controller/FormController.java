package com.formedit.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formedit.dto.FormDto;
import com.formedit.dto.FormResponse;
import com.formedit.dto.PageResponse;
import com.formedit.dto.ValidationRequest;
import com.formedit.dto.ValidationResult;
import com.formedit.entity.Form;
import com.formedit.entity.FormField;
import com.formedit.service.FormService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/forms")
public class FormController {
    private final FormService formService;
    private final ObjectMapper objectMapper;

    public FormController(FormService formService, ObjectMapper objectMapper) {
        this.formService = formService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public PageResponse<FormResponse> getAllForms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Form> formPage = formService.searchForms(name, pageable);
        return convertToPageResponse(formPage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FormResponse> getFormById(@PathVariable Long id) {
        return formService.getFormById(id)
                .map(form -> ResponseEntity.ok(convertToResponse(form)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public FormResponse createForm(@Valid @RequestBody FormDto formDto) {
        Form form = formService.createForm(formDto);
        return convertToResponse(form);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FormResponse> updateForm(@PathVariable Long id, @Valid @RequestBody FormDto formDto) {
        return formService.updateForm(id, formDto)
                .map(form -> ResponseEntity.ok(convertToResponse(form)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteForm(@PathVariable Long id) {
        if (formService.deleteForm(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/validate")
    public ValidationResult validateForm(@RequestBody ValidationRequest request) {
        return formService.validateForm(request.getFormId(), request.getData());
    }

    private PageResponse<FormResponse> convertToPageResponse(Page<Form> formPage) {
        PageResponse<FormResponse> response = new PageResponse<>();
        response.setContent(formPage.getContent().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList()));
        response.setTotalPages(formPage.getTotalPages());
        response.setTotalElements(formPage.getTotalElements());
        response.setPageNumber(formPage.getNumber());
        response.setPageSize(formPage.getSize());
        return response;
    }

    private FormResponse convertToResponse(Form form) {
        FormResponse response = new FormResponse();
        response.setId(form.getId());
        response.setName(form.getName());
        response.setDescription(form.getDescription());
        response.setCreatedAt(form.getCreatedAt());
        response.setUpdatedAt(form.getUpdatedAt());
        response.setFields(parseFieldsFromJson(form.getFieldsJson()));
        return response;
    }

    private List<FormField> parseFieldsFromJson(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<FormField>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
