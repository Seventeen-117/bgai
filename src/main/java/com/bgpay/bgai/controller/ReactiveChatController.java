package com.bgpay.bgai.controller;

import com.bgpay.bgai.config.ReactiveFileProcessor;
import com.bgpay.bgai.entity.ApiConfig;
import com.bgpay.bgai.entity.UsageInfo;
import com.bgpay.bgai.exception.BillingException;
import com.bgpay.bgai.response.ChatResponse;
import com.bgpay.bgai.service.ApiConfigService;
import com.bgpay.bgai.service.deepseek.DeepSeekService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
@Slf4j
public class ReactiveChatController {

    private final ReactiveFileProcessor fileProcessor;
    private final ApiConfigService apiConfigService;
    private final DeepSeekService deepSeekService;

    @Autowired
    public ReactiveChatController(ReactiveFileProcessor fileProcessor,
                                  ApiConfigService apiConfigService,
                                  DeepSeekService deepSeekService) {
        this.fileProcessor = fileProcessor;
        this.apiConfigService = apiConfigService;
        this.deepSeekService = deepSeekService;
    }

    @PostMapping(
            value = "/chatGatWay",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<ChatResponse>> handleChatRequest(
            @RequestPart(value = "file", required = false) FilePart file,
            @RequestParam (value = "question", defaultValue = "请分析该内容") String question,
            @RequestParam (value = "apiUrl", required = false) String apiUrl,
            @RequestParam (value = "apiKey", required = false) String apiKey,
            @RequestParam (value = "modelName", required = false) String modelName,
            @RequestParam (value = "multiTurn", defaultValue = "false") boolean multiTurn,
            @RequestHeader("X-User-Id") String userId) {

        return Mono.defer(() -> {
            // 参数校验
            if ((file == null) && (question == null || question.isBlank())) {
                return Mono.just(errorResponse(400, "必须提供问题或文件"));
            }

            // 解析配置
            return resolveApiConfigReactive(apiUrl, apiKey, modelName, userId)
                    .flatMap(apiConfig ->
                            processContent(file, question, multiTurn)
                                    .flatMap(content ->
                                            deepSeekService.processRequestReactive(
                                                    content,
                                                    apiConfig.getApiUrl(),
                                                    apiConfig.getApiKey(),
                                                    apiConfig.getModelName(),
                                                    userId,
                                                    multiTurn
                                            )
                                    )
                    )
                    .map(ResponseEntity::ok)
                    .onErrorResume(e ->
                            Mono.just(errorResponse(500, "处理失败: " + e.getMessage()))
                    );
        });
    }

    private Mono<ApiConfig> resolveApiConfigReactive(String apiUrl, String apiKey, String modelName, String userId) {
        if (StringUtils.hasText(apiUrl)
                && StringUtils.hasText(apiKey)
                && StringUtils.hasText(modelName)) {
            return Mono.just(new ApiConfig()
                    .setApiUrl(apiUrl)
                    .setApiKey(apiKey)
                    .setModelName(modelName));
        }
        return Mono.fromCallable(() -> apiConfigService.getLatestConfig(userId))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("未找到用户API配置且未提供完整参数")))
                .doOnError(e -> log.error("配置解析失败", e)); // 添加错误日志
    }

    private Mono<String> processContent(FilePart file, String question, boolean multiTurn) {
        if (file == null) {
            return Mono.just(buildTextContent(question))
                    .doOnNext(c -> log.debug("Processing text-only request"));
        }

        return fileProcessor.processReactiveFile(file)
                .onErrorResume(e -> {
                    log.error("File processing failed", e);
                    return Mono.error(new BillingException("文件处理失败: " + e.getMessage()));
                })
                .map(fileContent -> buildFileContent(fileContent, question))
                .doOnNext(c -> log.debug("File content processed: {}", c.substring(0, 50)));
    }

    private String buildFileContent(String fileContent, String question) {
        return "【文件内容】\n" + fileContent + "\n\n【用户提问】" + question;
    }

    private String buildTextContent(String question) {
        return "【用户提问】" + question;
    }

    private ResponseEntity<ChatResponse> errorResponse(int code, String message) {
        ChatResponse response = new ChatResponse();
        response.setContent(String.format("{\"error\":{\"code\":%d,\"message\":\"%s\"}}", code, message));
        response.setUsage(new UsageInfo());
        return ResponseEntity.status(code).body(response);
    }
}