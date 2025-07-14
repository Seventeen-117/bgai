package com.bgpay.bgai.controller;

import com.bgpay.bgai.service.impl.TestCaseExcelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "测试用例", description = "测试用例生成相关接口")
public class TestCaseController {

    private final TestCaseExcelService testCaseExcelService;
    private final RestTemplate restTemplate;

    @Operation(summary = "从AI生成测试用例", description = "通过调用chatGatWay-internal API获取内容并转换为各种格式的测试用例")
    @PostMapping("/generate-test-case")
    public ResponseEntity<?> generateTestCaseFromAI(
            @RequestBody Map<String, Object> request,
            @RequestParam(required = false, defaultValue = "markdown") String format,
            @RequestParam(required = false, defaultValue = "test-cases") String outputDir,
            @RequestParam(required = false, defaultValue = "api") String testType) {
        
        try {
            // 调用chatGatWay-internal接口
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                "http://localhost:8688/api/chatGatWay-internal", 
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            if (response.getStatusCode() != HttpStatus.OK) {
                log.error("调用chatGatWay-internal失败: {}", response.getStatusCode());
                return ResponseEntity.status(response.getStatusCode())
                    .body(Map.of("error", "调用chatGatWay-internal失败"));
            }
            
            // 解析响应内容
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || !responseBody.containsKey("content")) {
                log.error("chatGatWay-internal响应缺少content字段");
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "chatGatWay-internal响应格式错误"));
            }
            
            String content = (String) responseBody.get("content");
            if (content == null || content.trim().isEmpty()) {
                log.error("chatGatWay-internal返回的content为空");
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "chatGatWay-internal返回的content为空"));
            }
            
            // 创建输出目录
            File directory = new File(outputDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            
            // 生成测试用例文件
            String filename = generateFilename(testType, format);
            String filePath = outputDir + File.separator + filename;
            
            // 基于AI返回的内容，转换为不同格式的测试用例
            if ("excel".equals(format) || "xlsx".equals(format)) {
                // 解析AI返回的内容，提取关键信息用于Excel生成
                Map<String, Object> parsedContent = parseAIContent(content, testType);
                generateExcelTestCase(testType, filePath, parsedContent);
            } else if ("csv".equals(format)) {
                String csvContent = convertToCSV(content, testType);
                try (FileWriter writer = new FileWriter(filePath)) {
                    writer.write(csvContent);
                }
            } else if ("postman".equals(format)) {
                String postmanContent = convertToPostman(content, testType);
                try (FileWriter writer = new FileWriter(filePath)) {
                    writer.write(postmanContent);
                }
            } else {
                // 默认为Markdown格式，可直接使用content或稍作格式化
                String markdownContent = formatAsMarkdown(content, testType);
                try (FileWriter writer = new FileWriter(filePath)) {
                    writer.write(markdownContent);
                }
            }
            
            log.info("从AI生成的测试用例已保存: {}", filePath);
            
            return ResponseEntity.ok(Map.of(
                "message", "测试用例生成成功",
                "filePath", filePath,
                "format", format
            ));
            
        } catch (Exception e) {
            log.error("从AI生成测试用例时发生错误", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 解析AI返回的内容，提取关键信息
     */
    private Map<String, Object> parseAIContent(String content, String testType) {
        Map<String, Object> result = new HashMap<>();
        
        // 解析逻辑，根据不同的测试用例类型提取关键信息
        // 这里是简化的实现，实际应用中可能需要更复杂的解析逻辑
        
        // 示例：提取测试场景、步骤、预期结果等信息
        if (content.contains("步骤")) {
            String[] steps = content.split("步骤")[1].split("预期")[0].split("[\\d]+\\.");
            result.put("steps", steps);
        }
        
        if (content.contains("预期结果")) {
            String expectedResult = content.split("预期结果")[1].trim();
            result.put("expectedResult", expectedResult);
        }
        
        // 添加可能用到的其他测试用例信息
        result.put("rawContent", content);
        result.put("testType", testType);
        result.put("generatedTime", LocalDateTime.now().toString());
        
        return result;
    }
    
    /**
     * 转换为CSV格式
     */
    private String convertToCSV(String content, String testType) {
        // 根据测试类型和内容转换为CSV格式
        StringBuilder csv = new StringBuilder();
        
        switch (testType) {
            case "api":
                csv.append("用例ID,接口URL,请求方法,请求参数,预期状态码,预期响应\n");
                // 从content中提取相关信息并格式化为CSV行
                // 这里是简化实现，实际应用需要更详细的解析
                csv.append("TC-001,/api/example,POST,{\"param\":\"value\"},200,{\"result\":\"success\"}\n");
                break;
            case "functional":
                csv.append("用例ID,功能模块,测试场景,前置条件,测试步骤,预期结果\n");
                // 提取相关信息...
                break;
            default:
                csv.append("未支持的测试用例类型: ").append(testType).append("\n");
                csv.append(content);
        }
        
        return csv.toString();
    }
    
    /**
     * 转换为Postman Collection格式
     */
    private String convertToPostman(String content, String testType) {
        // 只有API测试才真正适用于Postman格式
        if (!"api".equals(testType)) {
            return "{\n\"info\": {\n\"name\": \"" + testType + " 测试集合\",\n\"schema\": \"https://schema.getpostman.com/json/collection/v2.1.0/collection.json\"\n},\n\"item\": []\n}";
        }
        
        // 从content中提取API相关信息
        // 构建Postman Collection JSON
        StringBuilder postman = new StringBuilder();
        postman.append("{\n");
        postman.append("  \"info\": {\n");
        postman.append("    \"_postman_id\": \"").append(java.util.UUID.randomUUID().toString()).append("\",\n");
        postman.append("    \"name\": \"API测试集合\",\n");
        postman.append("    \"schema\": \"https://schema.getpostman.com/json/collection/v2.1.0/collection.json\"\n");
        postman.append("  },\n");
        postman.append("  \"item\": [\n");
        postman.append("    {\n");
        postman.append("      \"name\": \"API测试用例\",\n");
        postman.append("      \"request\": {\n");
        postman.append("        \"method\": \"POST\",\n");
        postman.append("        \"header\": [\n");
        postman.append("          {\n");
        postman.append("            \"key\": \"Content-Type\",\n");
        postman.append("            \"value\": \"application/json\"\n");
        postman.append("          }\n");
        postman.append("        ],\n");
        postman.append("        \"body\": {\n");
        postman.append("          \"mode\": \"raw\",\n");
        postman.append("          \"raw\": \"{\\\"param\\\":\\\"value\\\"}\"\n");
        postman.append("        },\n");
        postman.append("        \"url\": {\n");
        postman.append("          \"raw\": \"{{baseUrl}}/api/example\",\n");
        postman.append("          \"host\": [\"{{baseUrl}}\"],\n");
        postman.append("          \"path\": [\"api\", \"example\"]\n");
        postman.append("        }\n");
        postman.append("      },\n");
        postman.append("      \"response\": []\n");
        postman.append("    }\n");
        postman.append("  ]\n");
        postman.append("}\n");
        
        return postman.toString();
    }
    
    /**
     * 格式化为Markdown
     */
    private String formatAsMarkdown(String content, String testType) {
        // 如果内容已经是良好格式的Markdown，可以直接返回
        // 否则，进行简单的格式化
        
        StringBuilder markdown = new StringBuilder();
        markdown.append("# ").append(testType.toUpperCase()).append(" 测试用例\n\n");
        markdown.append("## 生成时间\n\n");
        markdown.append(LocalDateTime.now()).append("\n\n");
        markdown.append("## 用例内容\n\n");
        markdown.append(content);
        
        return markdown.toString();
    }
    
    private void generateExcelTestCase(String testType, String filePath, Map<String, Object> parameters) throws Exception {
        switch (testType) {
            case "api":
                testCaseExcelService.generateApiTestCaseExcel(filePath, parameters);
                break;
            case "functional":
                testCaseExcelService.generateFunctionalTestCaseExcel(filePath, parameters);
                break;
            default:
                // 默认生成API测试用例
                testCaseExcelService.generateApiTestCaseExcel(filePath, parameters);
        }
    }
    
    private String generateFilename(String testType, String format) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = now.format(formatter);
        
        String extension;
        switch (format) {
            case "excel":
            case "xlsx":
                extension = ".xlsx";
                break;
            case "csv":
                extension = ".csv";
                break;
            case "word":
                extension = ".docx";
                break;
            case "postman":
                extension = ".json";
                break;
            case "swagger":
            case "openapi":
                extension = ".yaml";
                break;
            case "jmeter":
                extension = ".jmx";
                break;
            case "markdown":
            default:
                extension = ".md";
                break;
        }
        
        return testType + "_test_case_" + timestamp + extension;
    }



} 