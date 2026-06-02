package com.formedit.entity;

import lombok.Data;
import java.util.List;

@Data
public class FormField {
    private String id;
    private String type;
    private String label;
    private String placeholder;
    private Boolean required;
    private Integer span;
    private Integer minLength;
    private Integer maxLength;
    private String pattern;
    private String patternMessage;
    private List<Option> options;
    private String defaultValue;
    private Boolean disabled;
}
