package com.formedit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formedit.entity.Form;
import com.formedit.entity.FormField;
import com.formedit.entity.Option;
import com.formedit.repository.FormRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {
    private final FormRepository formRepository;
    private final ObjectMapper objectMapper;

    public DataInitializer(FormRepository formRepository, ObjectMapper objectMapper) {
        this.formRepository = formRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) {
        if (formRepository.count() == 0) {
            Form form1 = createUserRegistrationForm();
            Form form2 = createSurveyForm();
            Form form3 = createContactForm();
            formRepository.saveAll(Arrays.asList(form1, form2, form3));
        }
    }

    private Form createUserRegistrationForm() {
        Form form = new Form();
        form.setName("用户注册表单");
        form.setDescription("新用户注册信息收集");

        List<FormField> fields = new ArrayList<>();

        FormField username = new FormField();
        username.setId("username");
        username.setType("text");
        username.setLabel("用户名");
        username.setPlaceholder("请输入用户名");
        username.setRequired(true);
        username.setMinLength(3);
        username.setMaxLength(20);
        fields.add(username);

        FormField email = new FormField();
        email.setId("email");
        email.setType("email");
        email.setLabel("邮箱");
        email.setPlaceholder("请输入邮箱地址");
        email.setRequired(true);
        email.setPattern("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
        email.setPatternMessage("请输入有效的邮箱地址");
        fields.add(email);

        FormField phone = new FormField();
        phone.setId("phone");
        phone.setType("text");
        phone.setLabel("手机号");
        phone.setPlaceholder("请输入手机号");
        phone.setRequired(true);
        phone.setPattern("^1[3-9]\\d{9}$");
        phone.setPatternMessage("请输入有效的手机号");
        fields.add(phone);

        FormField gender = new FormField();
        gender.setId("gender");
        gender.setType("radio");
        gender.setLabel("性别");
        gender.setRequired(true);
        gender.setOptions(Arrays.asList(
                createOption("男", "male"),
                createOption("女", "female")
        ));
        fields.add(gender);

        FormField birthday = new FormField();
        birthday.setId("birthday");
        birthday.setType("date");
        birthday.setLabel("出生日期");
        birthday.setRequired(false);
        fields.add(birthday);

        form.setFieldsJson(convertToJson(fields));
        return form;
    }

    private Form createSurveyForm() {
        Form form = new Form();
        form.setName("满意度调查表");
        form.setDescription("用户满意度调查");

        List<FormField> fields = new ArrayList<>();

        FormField rating = new FormField();
        rating.setId("rating");
        rating.setType("select");
        rating.setLabel("满意度评分");
        rating.setPlaceholder("请选择评分");
        rating.setRequired(true);
        rating.setOptions(Arrays.asList(
                createOption("非常满意", "5"),
                createOption("满意", "4"),
                createOption("一般", "3"),
                createOption("不满意", "2"),
                createOption("非常不满意", "1")
        ));
        fields.add(rating);

        FormField feedback = new FormField();
        feedback.setId("feedback");
        feedback.setType("textarea");
        feedback.setLabel("反馈意见");
        feedback.setPlaceholder("请输入您的宝贵意见");
        feedback.setRequired(false);
        feedback.setMaxLength(500);
        fields.add(feedback);

        FormField recommend = new FormField();
        recommend.setId("recommend");
        recommend.setType("checkbox");
        recommend.setLabel("您愿意推荐给朋友吗");
        recommend.setRequired(false);
        recommend.setOptions(Arrays.asList(
                createOption("是", "yes")
        ));
        fields.add(recommend);

        form.setFieldsJson(convertToJson(fields));
        return form;
    }

    private Form createContactForm() {
        Form form = new Form();
        form.setName("联系我们表单");
        form.setDescription("客户联系信息收集");

        List<FormField> fields = new ArrayList<>();

        FormField name = new FormField();
        name.setId("name");
        name.setType("text");
        name.setLabel("姓名");
        name.setPlaceholder("请输入您的姓名");
        name.setRequired(true);
        fields.add(name);

        FormField email = new FormField();
        email.setId("contactEmail");
        email.setType("email");
        email.setLabel("联系邮箱");
        email.setPlaceholder("请输入联系邮箱");
        email.setRequired(true);
        email.setPattern("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
        fields.add(email);

        FormField subject = new FormField();
        subject.setId("subject");
        subject.setType("select");
        subject.setLabel("咨询类型");
        subject.setPlaceholder("请选择咨询类型");
        subject.setRequired(true);
        subject.setOptions(Arrays.asList(
                createOption("产品咨询", "product"),
                createOption("技术支持", "support"),
                createOption("商务合作", "business"),
                createOption("其他", "other")
        ));
        fields.add(subject);

        FormField message = new FormField();
        message.setId("message");
        message.setType("textarea");
        message.setLabel("留言内容");
        message.setPlaceholder("请详细描述您的问题或需求");
        message.setRequired(true);
        message.setMinLength(10);
        message.setMaxLength(1000);
        fields.add(message);

        form.setFieldsJson(convertToJson(fields));
        return form;
    }

    private Option createOption(String label, String value) {
        Option option = new Option();
        option.setLabel(label);
        option.setValue(value);
        return option;
    }

    private String convertToJson(List<FormField> fields) {
        try {
            return objectMapper.writeValueAsString(fields);
        } catch (Exception e) {
            return "[]";
        }
    }
}
