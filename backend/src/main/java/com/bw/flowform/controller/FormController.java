package com.bw.flowform.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import static com.bw.flowform.utils.JsonUtils.*;
import com.bw.flowform.common.ErrorCode;
import com.bw.flowform.dto.FormDto;
import com.bw.flowform.dto.FormResponse;
import com.bw.flowform.dto.PageResponse;
import com.bw.flowform.dto.ValidationRequest;
import com.bw.flowform.dto.ValidationResult;
import com.bw.flowform.entity.Form;
import com.bw.flowform.entity.FormField;
import com.bw.flowform.exception.ResourceNotFoundException;
import com.bw.flowform.service.FormService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/forms")
public class FormController {
    private final FormService formService;

    public FormController(FormService formService) {
        this.formService = formService;
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

    @GetMapping("/list")
    public List<FormResponse> getAllFormsList() {
        return formService.getAllFormsList().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public FormResponse getFormById(@PathVariable Long id) {
        Form form = formService.getFormById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.FORM_NOT_FOUND, "ID=" + id));
        return convertToResponse(form);
    }

    @PostMapping
    public FormResponse createForm(@Valid @RequestBody FormDto formDto) {
        Form form = formService.createForm(formDto);
        return convertToResponse(form);
    }

    @PutMapping("/{id}")
    public FormResponse updateForm(@PathVariable Long id, @Valid @RequestBody FormDto formDto) {
        Form form = formService.updateFormOrThrow(id, formDto);
        return convertToResponse(form);
    }

    @DeleteMapping("/{id}")
    public void deleteForm(@PathVariable Long id) {
        formService.deleteFormOrThrow(id);
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
        List<FormField> result = fromJson(json, new TypeReference<List<FormField>>() {});
        return result != null ? result : new ArrayList<>();
    }
}
