package com.bgpay.bgai.entity;

import lombok.Data;
import java.util.Map;

@Data
public class TestCaseRequest {
    private String testType;        // api, functional, performance, security, compatibility
    private String format;          // markdown, excel, csv, word, postman, swagger, jmeter
    private String outputDir;       // 输出目录路径
    private Map<String, Object> parameters; // 自定义参数
    private String templateName;    // 可选的模板名称
} 