package com.formedit.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formedit.dto.WorkflowDefinitionDto;
import com.formedit.dto.WorkflowValidationResult;
import com.formedit.entity.Form;
import com.formedit.entity.WorkflowDefinition;
import com.formedit.repository.FormRepository;
import com.formedit.repository.WorkflowDefinitionRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class WorkflowDefinitionService {

    private final WorkflowDefinitionRepository definitionRepository;
    private final WorkflowValidationService validationService;
    private final FormRepository formRepository;
    private final ObjectMapper objectMapper;

    public WorkflowDefinitionService(WorkflowDefinitionRepository definitionRepository,
                                      WorkflowValidationService validationService,
                                      FormRepository formRepository,
                                      ObjectMapper objectMapper) {
        this.definitionRepository = definitionRepository;
        this.validationService = validationService;
        this.formRepository = formRepository;
        this.objectMapper = objectMapper;
    }

    public Page<WorkflowDefinition> getAllDefinitions(Pageable pageable) {
        return definitionRepository.findAllByOrderByIdDesc(pageable);
    }

    public Optional<WorkflowDefinition> getDefinitionById(Long id) {
        return definitionRepository.findById(id);
    }

    public WorkflowDefinitionDto getDefinitionDtoById(Long id) {
        return definitionRepository.findById(id)
                .map(this::convertToDto)
                .orElse(null);
    }

    @Transactional
    public WorkflowDefinition createDefinition(WorkflowDefinitionDto dto) {
        WorkflowValidationResult validation = validationService.validateWorkflow(dto);
        if (!validation.isValid()) {
            throw new IllegalArgumentException("流程验证失败: " + String.join("; ", validation.getErrors()));
        }

        if (definitionRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("流程名称已存在: " + dto.getName());
        }

        WorkflowDefinition definition = new WorkflowDefinition();
        BeanUtils.copyProperties(dto, definition);
        definition.setNodesJson(convertToJson(dto.getNodes()));
        definition.setEdgesJson(convertToJson(dto.getEdges()));
        if (dto.getFormId() != null) {
            Form form = formRepository.findById(dto.getFormId()).orElse(null);
            if (form != null) {
                definition.setFormName(form.getName());
            }
        }
        return definitionRepository.save(definition);
    }

    @Transactional
    public Optional<WorkflowDefinition> updateDefinition(Long id, WorkflowDefinitionDto dto) {
        WorkflowValidationResult validation = validationService.validateWorkflow(dto);
        if (!validation.isValid()) {
            throw new IllegalArgumentException("流程验证失败: " + String.join("; ", validation.getErrors()));
        }

        if (definitionRepository.existsByNameAndIdNot(dto.getName(), id)) {
            throw new IllegalArgumentException("流程名称已存在: " + dto.getName());
        }

        return definitionRepository.findById(id).map(definition -> {
            definition.setName(dto.getName());
            definition.setDescription(dto.getDescription());
            definition.setNodesJson(convertToJson(dto.getNodes()));
            definition.setEdgesJson(convertToJson(dto.getEdges()));
            definition.setFormId(dto.getFormId());
            if (dto.getFormId() != null) {
                Form form = formRepository.findById(dto.getFormId()).orElse(null);
                definition.setFormName(form != null ? form.getName() : null);
            } else {
                definition.setFormName(null);
            }
            return definitionRepository.save(definition);
        });
    }

    @Transactional
    public boolean deleteDefinition(Long id) {
        if (definitionRepository.existsById(id)) {
            definitionRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public WorkflowValidationResult validateDefinition(WorkflowDefinitionDto dto) {
        return validationService.validateWorkflow(dto);
    }

    private String convertToJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }

    public WorkflowDefinitionDto convertToDto(WorkflowDefinition definition) {
        WorkflowDefinitionDto dto = new WorkflowDefinitionDto();
        BeanUtils.copyProperties(definition, dto);
        dto.setNodes(parseJson(definition.getNodesJson(), new TypeReference<List<WorkflowDefinitionDto.Node>>() {}));
        dto.setEdges(parseJson(definition.getEdgesJson(), new TypeReference<List<WorkflowDefinitionDto.Edge>>() {}));
        return dto;
    }

    private <T> T parseJson(String json, TypeReference<T> type) {
        if (json == null || json.isEmpty()) return null;
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            return null;
        }
    }
}
