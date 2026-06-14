package com.formedit.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formedit.dto.FormDto;
import com.formedit.dto.ValidationResult;
import com.formedit.entity.Form;
import com.formedit.entity.FormField;
import com.formedit.repository.FormRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class FormService {
    private final FormRepository formRepository;
    private final ObjectMapper objectMapper;

    public FormService(FormRepository formRepository, ObjectMapper objectMapper) {
        this.formRepository = formRepository;
        this.objectMapper = objectMapper;
    }

    public Page<Form> getAllForms(Pageable pageable) {
        return formRepository.findAllByOrderByIdDesc(pageable);
    }

    public List<Form> getAllFormsList() {
        return formRepository.findAllByOrderByIdDesc();
    }

    public Page<Form> searchForms(String name, Pageable pageable) {
        if (name == null || name.trim().isEmpty()) {
            return formRepository.findAllByOrderByIdDesc(pageable);
        }
        return formRepository.findByNameContaining(name.trim(), pageable);
    }

    public Optional<Form> getFormById(Long id) {
        return formRepository.findById(id);
    }

    public Form createForm(FormDto formDto) {
        if (formRepository.existsByName(formDto.getName())) {
            throw new IllegalArgumentException("表单名称已存在: " + formDto.getName());
        }
        Form form = new Form();
        BeanUtils.copyProperties(formDto, form);
        form.setFieldsJson(convertFieldsToJson(formDto.getFields()));
        return formRepository.save(form);
    }

    public Optional<Form> updateForm(Long id, FormDto formDto) {
        if (formRepository.existsByNameAndIdNot(formDto.getName(), id)) {
            throw new IllegalArgumentException("表单名称已存在: " + formDto.getName());
        }
        return formRepository.findById(id).map(form -> {
            form.setName(formDto.getName());
            form.setDescription(formDto.getDescription());
            form.setFieldsJson(convertFieldsToJson(formDto.getFields()));
            return formRepository.save(form);
        });
    }

    public boolean deleteForm(Long id) {
        if (formRepository.existsById(id)) {
            formRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public ValidationResult validateForm(Long formId, Map<String, Object> data) {
        ValidationResult result = new ValidationResult();
        Map<String, String> errors = new HashMap<>();

        Optional<Form> formOpt = formRepository.findById(formId);
        if (!formOpt.isPresent()) {
            errors.put("form", "表单不存在");
            result.setValid(false);
            result.setErrors(errors);
            return result;
        }

        Form form = formOpt.get();
        List<FormField> fields = parseFieldsFromJson(form.getFieldsJson());

        if (fields != null) {
            for (FormField field : fields) {
                String fieldId = field.getId();
                Object value = data.get(fieldId);
                validateField(field, value, errors);
            }
        }

        result.setValid(errors.isEmpty());
        result.setErrors(errors);
        return result;
    }

    private void validateField(FormField field, Object value, Map<String, String> errors) {
        String fieldId = field.getId();
        String stringValue = value != null ? String.valueOf(value).trim() : "";

        if (Boolean.TRUE.equals(field.getRequired()) && (value == null || stringValue.isEmpty())) {
            errors.put(fieldId, field.getLabel() + "不能为空");
            return;
        }

        if (value == null || stringValue.isEmpty()) {
            return;
        }

        boolean isTextLike = "text".equals(field.getType()) || "textarea".equals(field.getType())
                || "email".equals(field.getType()) || "number".equals(field.getType());

        if (isTextLike && field.getMinLength() != null && stringValue.length() < field.getMinLength()) {
            errors.put(fieldId, field.getLabel() + "最少需要" + field.getMinLength() + "个字符");
        }

        if (isTextLike && field.getMaxLength() != null && stringValue.length() > field.getMaxLength()) {
            errors.put(fieldId, field.getLabel() + "最多允许" + field.getMaxLength() + "个字符");
        }

        if (isTextLike && field.getPattern() != null && !field.getPattern().isEmpty() && !stringValue.isEmpty()) {
            Pattern pattern = Pattern.compile(field.getPattern());
            if (!pattern.matcher(stringValue).matches()) {
                String message = field.getPatternMessage();
                if (message == null || message.isEmpty()) {
                    message = field.getLabel() + "格式不正确";
                }
                errors.put(fieldId, message);
            }
        }
    }

    private String convertFieldsToJson(List<FormField> fields) {
        if (fields == null) return null;
        try {
            return objectMapper.writeValueAsString(fields);
        } catch (Exception e) {
            return null;
        }
    }

    public List<FormField> getFormFields(Long formId) {
        if (formId == null) return new ArrayList<>();
        Optional<Form> formOpt = formRepository.findById(formId);
        if (!formOpt.isPresent()) return new ArrayList<>();
        return parseFieldsFromJson(formOpt.get().getFieldsJson());
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
