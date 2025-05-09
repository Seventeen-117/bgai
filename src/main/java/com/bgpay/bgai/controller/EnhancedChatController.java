package com.bgpay.bgai.controller;

import com.bgpay.bgai.service.deepseek.DeepSeekService;
import com.bgpay.bgai.service.deepseek.FileProcessor;
import com.bgpay.bgai.entity.ApiConfig;
import com.bgpay.bgai.entity.UsageInfo;
import com.bgpay.bgai.response.ChatResponse;
import com.bgpay.bgai.service.ApiConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@RestController
@RequestMapping("/Api")
@Slf4j
public class EnhancedChatController {
    private final FileProcessor fileProcessor;
    private final ApiConfigService apiConfigService;
    private final DeepSeekService deepSeekService;

    @Autowired
    public EnhancedChatController(FileProcessor fileProcessor,
                                  ApiConfigService apiConfigService,
                                  DeepSeekService deepSeekService) {
        this.fileProcessor = fileProcessor;
        this.apiConfigService = apiConfigService;
        this.deepSeekService = deepSeekService;
    }

    @PostMapping(value = "/chat",
            consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ChatResponse>> handleChatRequest(
            @RequestPart(value = "file", required = false) Mono<FilePart> fileMono,
            @RequestParam(value = "question", defaultValue = "请分析该内容") String question,
            @RequestParam(value = "apiUrl", required = false) String apiUrl,
            @RequestParam(value = "apiKey", required = false) String apiKey,
            @RequestParam(value = "modelName", required = false) String modelName,
            @RequestParam(value = "multiTurn", defaultValue = "false") boolean multiTurn,
            ServerWebExchange exchange) {

        log.info("Received request to /Api/chat - question length: {}, multiTurn: {}, modelName: '{}', apiUrl: '{}', apiKey: '{}'", 
                question.length(), multiTurn, modelName, apiUrl, apiKey);
        
        // 记录请求参数和headers以便调试
        log.info("Request headers: {}", exchange.getRequest().getHeaders());
        log.info("Request query params: {}", exchange.getRequest().getQueryParams());
            
        // 从请求头中获取用户ID
        String userId = getUserIdFromExchange(exchange);
        if (userId == null) {
            return Mono.just(errorResponse(401, "未提供用户ID"));
        }

        // 获取表单数据以确保能获取到所有参数
        return exchange.getFormData()
            .flatMap(formData -> {
                // 尝试从表单数据中获取modelName（如果上面的@RequestParam没有成功获取）
                if (modelName == null && formData.containsKey("modelName")) {
                    String formModelName = formData.getFirst("modelName");
                    log.info("Found modelName in form data: '{}'", formModelName);
                    
                    // 使用从表单中获取的modelName处理请求
                    return processRequestWithFormData(
                        fileMono, 
                        question, 
                        formData.getFirst("apiUrl"), 
                        formData.getFirst("apiKey"), 
                        formModelName, 
                        multiTurn, 
                        userId
                    );
                } else {
                    // 使用通过@RequestParam注解获取的参数处理请求
                    return processRequestWithFormData(
                        fileMono, 
                        question, 
                        apiUrl, 
                        apiKey, 
                        modelName, 
                        multiTurn, 
                        userId
                    );
                }
            });
    }

    // 提取请求处理逻辑到单独的方法
    private Mono<ResponseEntity<ChatResponse>> processRequestWithFormData(
            Mono<FilePart> fileMono, 
            String question, 
            String apiUrl, 
            String apiKey, 
            String modelName, 
            boolean multiTurn, 
            String userId) {
        
        log.info("Processing request with params: question length={}, modelName='{}', userId='{}'", 
                question.length(), modelName, userId);
            
        // 处理文件上传和内容构建
        return fileMono
                .flatMap(filePart -> processFilePart(filePart))
                .defaultIfEmpty("")
                .flatMap(fileContent -> {
                    try {
                        // 验证请求
                        if (fileContent.isEmpty() && question.isBlank()) {
                            return Mono.just(errorResponse(400, "必须提供问题或文件"));
                        }

                        // 解析API配置
                        ApiConfig apiConfig = resolveApiConfig(apiUrl, apiKey, modelName, userId);
                        
                        log.info("Resolved API config: url={}, model={}", 
                                apiConfig.getApiUrl(), apiConfig.getModelName());

                        // 构建内容
                        String content = buildContent(fileContent, question, multiTurn);

                        // 处理请求
                        return Mono.fromCallable(() -> deepSeekService.processRequest(
                                content,
                                apiConfig.getApiUrl(),
                                apiConfig.getApiKey(),
                                apiConfig.getModelName(),
                                userId,
                                multiTurn
                        ))
                        .map(ResponseEntity::ok);
                    } catch (IllegalArgumentException e) {
                        log.error("Invalid argument error: {}", e.getMessage());
                        return Mono.just(errorResponse(400, e.getMessage()));
                    } catch (Exception e) {
                        log.error("处理请求失败", e);
                        return Mono.just(errorResponse(500, "处理失败: " + e.getMessage()));
                    }
                });
    }

    // 添加一个处理普通表单提交的方法，帮助调试
    @PostMapping(value = "/chat-form-data")
    public Mono<ResponseEntity<String>> handleFormDataRequest(ServerWebExchange exchange) {
        log.info("Received form data request");
        
        return exchange.getFormData()
            .map(formData -> {
                StringBuilder sb = new StringBuilder();
                sb.append("Received form data:\n");
                
                formData.forEach((key, values) -> {
                    sb.append(key).append(": ").append(values).append("\n");
                });
                
                // 特别检查modelName参数
                if (formData.containsKey("modelName")) {
                    sb.append("\nmodelName found with value: ").append(formData.getFirst("modelName"));
                } else {
                    sb.append("\nmodelName NOT found in form data");
                }
                
                // 检查Content-Type
                String contentType = exchange.getRequest().getHeaders().getFirst("Content-Type");
                sb.append("\nContent-Type: ").append(contentType);
                
                log.info(sb.toString());
                
                return ResponseEntity.ok(sb.toString());
            });
    }

    private String getUserIdFromExchange(ServerWebExchange exchange) {
        // 首先尝试从 X-User-Id 头获取
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        
        // 如果头中没有，尝试从 exchange 属性中获取
        if (userId == null) {
            Object userIdAttr = exchange.getAttribute("userId");
            if (userIdAttr != null) {
                userId = userIdAttr.toString();
            }
        }
        
        return userId;
    }

    private Mono<String> processFilePart(FilePart filePart) {
        if (filePart == null) {
            return Mono.just("");
        }

        try {
            // 创建临时文件
            Path tempFile = Files.createTempFile("upload_", "_" + filePart.filename());
            
            // 将 FilePart 写入临时文件
            return filePart.transferTo(tempFile)
                    .then(Mono.fromCallable(() -> {
                        try {
                            // 处理文件
                            String content = fileProcessor.processFile(tempFile.toFile());
                            // 处理完成后删除临时文件
                            Files.deleteIfExists(tempFile);
                            return content;
                        } catch (Exception e) {
                            log.error("处理文件失败", e);
                            try {
                                Files.deleteIfExists(tempFile);
                            } catch (Exception ex) {
                                // 忽略删除临时文件的异常
                            }
                            throw new RuntimeException("处理文件失败: " + e.getMessage(), e);
                        }
                    }));
        } catch (Exception e) {
            log.error("创建临时文件失败", e);
            return Mono.error(e);
        }
    }

    private ApiConfig resolveApiConfig(String apiUrl, String apiKey, String modelName, String userId) {
        // 用户提供完整参数
        if (StringUtils.hasText(apiUrl)
                && StringUtils.hasText(apiKey)
                && StringUtils.hasText(modelName)) {
            return new ApiConfig()
                    .setApiUrl(apiUrl)
                    .setApiKey(apiKey)
                    .setModelName(modelName);
        }
        ApiConfig dbConfig;
        String defualtModelName;
        
        // 添加空值检查，确保modelName不为null
        if (modelName == null || modelName.isEmpty()) {
            defualtModelName = "deepseek-chat";
            dbConfig = apiConfigService.findAlternativeConfig(userId, defualtModelName);
        } else {
            dbConfig = apiConfigService.findAlternativeConfig(userId, modelName);
        }

        if (dbConfig == null) {
            throw new IllegalArgumentException("未找到用户API配置且未提供完整参数");
        }
        return dbConfig;
    }

    private String buildContent(String fileContent, String question, boolean multiTurn) {
        StringBuilder content = new StringBuilder();

        // 无论是否多轮对话都处理文件内容
        if (!fileContent.isEmpty()) {
            content.append("【文件内容】\n")
                    .append(fileContent)
                    .append("\n\n");
        }

        content.append("【用户提问】").append(question);
        return content.toString();
    }

    // 修改 errorResponse 方法，使其返回 ResponseEntity<ChatResponse>
    private ResponseEntity<ChatResponse> errorResponse(int code, String message) {
        ChatResponse chatResponse = new ChatResponse();
        String errorJson = String.format("{\"error\":{\"code\":%d,\"message\":\"%s\"}}", code, message);
        chatResponse.setContent(errorJson);
        chatResponse.setUsage(new UsageInfo());
        return ResponseEntity.status(code).body(chatResponse);
    }
}