package com.bgpay.bgai.controller;

import com.bgpay.bgai.deepseek.DeepSeekService;
import com.bgpay.bgai.deepseek.FileProcessor;
import com.bgpay.bgai.entity.ApiConfig;
import com.bgpay.bgai.service.ApiConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
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

    @PostMapping(value = "/chat", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> handleChatRequest(
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "question", defaultValue = "请分析该内容") String question,
            @RequestParam(value = "apiUrl", required = false) String apiUrl,
            @RequestParam(value = "apiKey", required = false) String apiKey,
            @RequestParam(value = "modelName", required = false) String modelName,
            @RequestHeader("X-User-Id") String userId) {

        try {
            // 参数校验
            if ((file == null || file.isEmpty()) && question.isBlank()) {
                return errorResponse(400, "必须提供问题或文件");
            }

            // 获取API配置
            ApiConfig apiConfig = resolveApiConfig(apiUrl, apiKey, modelName, userId);

            // 构建请求内容
            String content = buildContent(file, question);

            // 调用服务
            String result = String.valueOf(deepSeekService.processRequest(
                    content,
                    apiConfig.getApiUrl(),
                    apiConfig.getApiKey(),
                    apiConfig.getModelName()
            ));
//            deepSeekService.saveCompletionDataAsync(result);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return errorResponse(400, e.getMessage());
        } catch (Exception e) {
            return errorResponse(500, "处理失败: " + e.getMessage());
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

        // 查询数据库配置
        ApiConfig dbConfig = apiConfigService.getLatestConfig(userId);
        if (dbConfig == null) {
            throw new IllegalArgumentException("未找到用户API配置且未提供完整参数");
        }
        return dbConfig;
    }

    private String buildContent(MultipartFile file, String question) throws Exception {
        StringBuilder content = new StringBuilder();
        if (file != null && !file.isEmpty()) {
            content.append("【文件内容】\n")
                    .append(fileProcessor.processFile(file))
                    .append("\n\n");
        }
        content.append("【用户提问】").append(question);
        return content.toString();
    }

    private ResponseEntity<String> errorResponse(int code, String message) {
        String errorJson = String.format("{\"error\":{\"code\":%d,\"message\":\"%s\"}}", code, message);
        return ResponseEntity.status(code).body(errorJson);
    }
}