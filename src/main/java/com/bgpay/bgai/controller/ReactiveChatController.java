package com.bgpay.bgai.controller;

import com.bgpay.bgai.config.ReactiveFileProcessor;
import com.bgpay.bgai.config.RequestAttributesProvider;
import com.bgpay.bgai.entity.ApiConfig;
import com.bgpay.bgai.entity.UsageInfo;
import com.bgpay.bgai.exception.BillingException;
import com.bgpay.bgai.response.ChatResponse;
import com.bgpay.bgai.service.ApiConfigService;
import com.bgpay.bgai.service.impl.FallbackService;
import com.bgpay.bgai.service.deepseek.DeepSeekService;
import com.bgpay.bgai.transaction.TransactionCoordinator;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/api")
@Slf4j
public class ReactiveChatController {

    private final ReactiveFileProcessor fileProcessor;
    private final ApiConfigService apiConfigService;
    private final DeepSeekService deepSeekService;
    private final ReactiveCircuitBreakerFactory circuitBreakerFactory;
    private final FallbackService fallbackService;
    private final TransactionCoordinator transactionCoordinator;
    private final RequestAttributesProvider attributesProvider;

    @Autowired
    public ReactiveChatController(ReactiveFileProcessor fileProcessor,
                                  ApiConfigService apiConfigService,
                                  DeepSeekService deepSeekService,
                                  ReactiveCircuitBreakerFactory circuitBreakerFactory,
                                  FallbackService fallbackService,
                                  TransactionCoordinator transactionCoordinator,
                                  RequestAttributesProvider attributesProvider) {
        this.fileProcessor = fileProcessor;
        this.apiConfigService = apiConfigService;
        this.deepSeekService = deepSeekService;
        this.circuitBreakerFactory = circuitBreakerFactory;
        this.fallbackService = fallbackService;
        this.transactionCoordinator = transactionCoordinator;
        this.attributesProvider = attributesProvider;
    }

    /**
     * 处理文本聊天请求
     *
     * @param content 聊天内容
     * @param apiUrl API URL（可选）
     * @param apiKey API密钥（可选）
     * @param modelName 模型名称（可选）
     * @param multiTurn 是否多轮对话
     * @return 聊天响应
     */
    @PostMapping("/chat")
    public Mono<ResponseEntity<ChatResponse>> processChat(
            @RequestParam String content,
            @RequestParam(required = false) String apiUrl,
            @RequestParam(required = false) String apiKey,
            @RequestParam(required = false) String modelName,
            @RequestParam(defaultValue = "true") boolean multiTurn,
            ServerWebExchange exchange) {

        log.info("Received chat request: content length = {}, multiTurn = {}",
                 content.length(), multiTurn);

        // 从请求上下文获取当前用户ID
        return attributesProvider.getUserId(exchange)
                .switchIfEmpty(Mono.error(new BillingException("用户未认证")))
                .flatMap(userId -> {
                    if (!StringUtils.hasText(content)) {
                        return fallbackService.handleEmptyQuestionFallback();
                    }

                    // 创建熔断器
                    ReactiveCircuitBreaker circuitBreaker = circuitBreakerFactory
                            .create("deepseekApiBreaker");

                    // 查找配置
                    return findMatchingConfig(userId, apiUrl, apiKey, modelName)
                            // 处理请求
                            .flatMap(config -> {
                                log.info("Processing request with API URL: {}, Model: {}",
                                        config.getApiUrl(), config.getModelName());

                                return circuitBreaker.run(
                                        deepSeekService.processRequestReactive(
                                                content,
                                                config.getApiUrl(),
                                                config.getApiKey(),
                                                config.getModelName(),
                                                userId,
                                                multiTurn
                                        ),
                                        throwable -> {
                                            log.error("Circuit breaker fallback for API request", throwable);
                                            return Mono.just(createFallbackResponse(throwable));
                                        }
                                );
                            })
                            // 处理异常
                            .onErrorResume(e -> {
                                log.error("Error in chat processing: {}", e.getMessage(), e);
                                return Mono.just(createErrorResponse("处理请求时出错: " + e.getMessage()));
                            })
                            // 构建响应
                            .map(ResponseEntity::ok);
                });
    }

    /**
     * 处理文件聊天请求
     * 已被新版本替代，保留此方法仅用于内部调用
     * 
     * @param file 上传的文件
     * @param question 问题内容
     * @param apiUrl API URL（可选）
     * @param apiKey API密钥（可选）
     * @param modelName 模型名称（可选）
     * @param multiTurn 是否多轮对话
     * @return 聊天响应
     * @deprecated 使用SimpleChatController提供的新接口
     */
    @Deprecated
    @ApiOperation(value = "处理文件聊天请求", hidden = true)
    @PostMapping(
            value = "/chatGatWay-internal",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<ChatResponse>> handleChatRequest(
            @RequestPart(value = "file", required = false) FilePart file,
            @RequestParam(value = "question", required = false) String question,
            @RequestParam(value = "apiUrl", required = false) String apiUrl,
            @RequestParam(value = "apiKey", required = false) String apiKey,
            @RequestParam(value = "modelName", required = false) String modelName,
            @RequestParam(value = "multiTurn", defaultValue = "false") boolean multiTurn,
            ServerWebExchange exchange) {

        log.info("Received chat request: question = {}, file = {}, modelName = {}, multiTurn = {}",
                question, file != null ? file.filename() : "none", modelName, multiTurn);

        // 1. 验证请求参数和用户信息
        return attributesProvider.getUserId(exchange)
                .switchIfEmpty(Mono.error(new BillingException("需要SSO认证")))
                .flatMap(userId -> {
                    log.info("Processing request for user ID: {}", userId);

                    if ((file == null || (file.filename() != null && file.filename().isEmpty())) && 
                        (question == null || question.trim().isEmpty())) {
                        log.error("Both file and question are empty");
                return Mono.just(errorResponse(400, "必须提供问题或文件"));
            }

                    // 对于非用户请求，验证完整的API参数
                    if ("default".equals(userId)) {
                        if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(apiUrl)) {
                            log.error("Must provide complete API parameters when no user ID is available");
                            return Mono.just(errorResponse(400, "未提供用户ID时，必须提供完整的API参数(apiUrl和apiKey)"));
                        }
                    }

                    // 2. 处理文件 - 使用熔断器
                    Mono<String> contentMono = file == null ?
                        Mono.just(buildTextContent(question != null ? question : "")) :
                        processFileWithCircuitBreaker(file, question != null ? question : "", multiTurn);

                    return resolveApiConfigReactive(apiUrl, apiKey, modelName, userId)
                            .flatMap(apiConfig -> {
                                log.info("API config resolved: url={}, model={}",
                                        apiConfig.getApiUrl(), apiConfig.getModelName());

                                return contentMono.flatMap(content -> {
                                    log.info("Content processed, length: {}", content.length());

                                    // 3. API调用 - 添加超时熔断器
                                    return callDeepSeekApiWithCircuitBreaker(
                                            content,
                                            apiConfig,
                                            userId,
                                            multiTurn
                                    );
                                });
                            })
                            .onErrorResume(e -> {
                                if (e instanceof IllegalArgumentException) {
                                    log.error("API configuration error: {}", e.getMessage());
                                    return Mono.just(errorResponse(400, e.getMessage()));
                                }
                                if (e instanceof RedisSystemException) {
                                    log.error("Redis error: {}", e.getMessage());
                                    return Mono.just(errorResponse(503, "系统暂时不可用，请稍后重试"));
                                }
                                log.error("Request processing failed: {}", e.getMessage(), e);

                                // 创建通用错误熔断器
                                ReactiveCircuitBreaker generalErrorBreaker = createCircuitBreaker("generalErrorBreaker");

                                return generalErrorBreaker.run(
                                    fallbackService.handleGeneralFallback(e),
                                    throwable -> fallbackService.handleGeneralFallback(e)
                                );
                            });
                })
                .onErrorResume(e -> {
                    log.error("Authentication error: {}", e.getMessage());
                    if (e instanceof BillingException) {
                        return Mono.just(errorResponse(401, e.getMessage()));
                    }
                    return Mono.just(errorResponse(500, "处理失败: " + e.getMessage()));
                });
    }

    /**
     * 创建指定名称的熔断器
     */
    private ReactiveCircuitBreaker createCircuitBreaker(String name) {
        return circuitBreakerFactory.create(name);
    }

    /**
     * 使用熔断器处理文件内容
     */
    private Mono<String> processFileWithCircuitBreaker(FilePart file, String question, boolean multiTurn) {
        log.info("Processing file with circuit breaker: fileName={}, question={}", file.filename(), question);

        // 创建文件处理熔断器
        ReactiveCircuitBreaker fileProcessingBreaker = createCircuitBreaker("fileProcessingBreaker");

        return fileProcessingBreaker.run(
            fileProcessor.processReactiveFile(file)
                .onErrorResume(e -> {
                    log.error("File processing failed: {}", e.getMessage(), e);
                    return Mono.error(new RuntimeException("文件处理失败: " + e.getMessage()));
                })
                .map(fileContent -> buildFileContent(fileContent, question))
                .doOnNext(c -> log.debug("File+text content built, length: {}", c.length())),
            throwable -> {
                log.error("File processing circuit broken: {}", throwable.getMessage());
                // 文件处理失败，但仍然返回问题内容以便能够处理纯文本问题
                return Mono.just(buildTextContent(question + " (文件处理失败，仅处理文本问题)"));
            }
        );
    }

    /**
     * 使用熔断器调用DeepSeek API
     */
    private Mono<ResponseEntity<ChatResponse>> callDeepSeekApiWithCircuitBreaker(
            String content, ApiConfig apiConfig, String userId, boolean multiTurn) {

        
        // 创建API调用熔断器
        ReactiveCircuitBreaker deepseekApiBreaker = createCircuitBreaker("deepseekApiBreaker");
        
        return deepseekApiBreaker.run(
                                            deepSeekService.processRequestReactive(
                                                    content,
                                                    apiConfig.getApiUrl(),
                                                    apiConfig.getApiKey(),
                                                    apiConfig.getModelName(),
                                                    userId,
                                                    multiTurn
            )
            .doOnNext(resp -> {
                log.info("Processed response for user {}, content length: {}", 
                        userId, resp.getContent().length());
                
                // 提取实际返回的chatCompletionId，与缓存比较确认一致性
                if (resp.getUsage() != null && resp.getUsage().getChatCompletionId() != null) {
                    String responseId = resp.getUsage().getChatCompletionId();
                    String cachedId = transactionCoordinator.getCurrentCompletionId(userId);
                    
                    if (cachedId != null && !cachedId.equals(responseId)) {
                        log.warn("ChatCompletionId inconsistency detected, using transaction coordinator ID");
                        // 优先使用事务协调器中的ID
                        resp.getUsage().setChatCompletionId(cachedId);
                    }
                }
            })
                    .map(ResponseEntity::ok)
            .timeout(Duration.ofSeconds(60))  // 添加超时
            .retryWhen(Retry.backoff(1, Duration.ofSeconds(2))
                .filter(throwable -> throwable instanceof TimeoutException)
                .doBeforeRetry(signal -> 
                    log.warn("Retrying DeepSeek API call after timeout: {}", signal.failure().getMessage()))
            ),
            throwable -> {
                log.error("Circuit broken for API call: {}", throwable.getMessage());
                
                // 使用事务协调器的回滚机制获取一致的ID
                String rollbackId = transactionCoordinator.rollback(userId);
                log.info("Using rolled back chatCompletionId for circuit breaker fallback: {}", rollbackId);
                
                // 构建包含一致ID的回退响应
                return fallbackService.handleCircuitBreakerFallback(userId, throwable, rollbackId);
            }
        );
    }

    private Mono<ApiConfig> resolveApiConfigReactive(String apiUrl, String apiKey, String modelName, String userId) {
        // 如果提供了完整API参数，直接使用这些参数创建配置
        if (StringUtils.hasText(apiUrl) && StringUtils.hasText(apiKey)) {
            // 如果modelName为空，使用默认值
            String finalModelName = StringUtils.hasText(modelName) ? modelName : "deepseek-chat";
            log.info("PARAM TRACE [{}]: 直接使用提供的参数 - apiUrl={}, modelName={} (原始: {})", 
                    userId, apiUrl, finalModelName, modelName);
            
            return Mono.just(new ApiConfig()
                    .setApiUrl(apiUrl)
                    .setApiKey(apiKey)
                    .setModelName(finalModelName));
        }
        
        // 尝试根据提供的任意参数和userId查询匹配的配置
        return Mono.fromCallable(() -> {
            // 从数据库中查找匹配的配置
            log.info("PARAM TRACE [{}]: 尝试查找匹配配置 - 参数: apiUrl={}, modelName={}", 
                    userId, apiUrl, modelName);
            ApiConfig config = apiConfigService.findMatchingConfig(userId, apiUrl, apiKey, modelName);
            
            // 如果找到了配置
            if (config != null) {
                log.info("PARAM TRACE [{}]: 找到匹配配置 - apiUrl={}, modelName={} (原始请求: {})", 
                        userId, config.getApiUrl(), config.getModelName(), modelName);
                
                // 如果传入了modelName，使用传入的modelName
                if (StringUtils.hasText(modelName)) {
                    config.setModelName(modelName);
                    log.info("PARAM TRACE [{}]: 使用传入的modelName: {}", userId, modelName);
                } else if (!StringUtils.hasText(config.getModelName())) {
                    // 如果配置中的modelName为空，使用默认值
                    config.setModelName("deepseek-chat");
                    log.info("PARAM TRACE [{}]: 配置中模型名称为空，使用默认值: {}", userId, config.getModelName());
    }

                return config;
            }
            
            // 如果没有任何参数传入，尝试获取最新的配置
            if (!StringUtils.hasText(apiUrl) && !StringUtils.hasText(apiKey) && !StringUtils.hasText(modelName)) {
                log.info("PARAM TRACE [{}]: 未提供任何参数，尝试获取最新配置", userId);
                ApiConfig latestConfig = apiConfigService.getLatestConfig(userId);
                if (latestConfig != null) {
                    log.info("PARAM TRACE [{}]: 使用最新配置 - apiUrl={}, modelName={}", 
                            userId, latestConfig.getApiUrl(), latestConfig.getModelName());
                    
                    // 如果传入了modelName，使用传入的modelName
                    if (StringUtils.hasText(modelName)) {
                        latestConfig.setModelName(modelName);
                        log.info("PARAM TRACE [{}]: 使用传入的modelName: {}", userId, modelName);
                    } else if (!StringUtils.hasText(latestConfig.getModelName())) {
                        // 如果配置中的modelName为空，使用默认值
                        latestConfig.setModelName("deepseek-chat");
                        log.info("PARAM TRACE [{}]: 最新配置中模型名称为空，使用默认值: {}", userId, latestConfig.getModelName());
                    }
                    
                    return latestConfig;
                }
            }
            
            // 如果无法找到配置，抛出异常
            log.warn("PARAM TRACE [{}]: 未找到匹配的API配置 - 原始请求参数: apiUrl={}, modelName={}", 
                    userId, apiUrl, modelName);
            throw new IllegalArgumentException("未找到匹配的API配置");
                })
        .switchIfEmpty(Mono.error(new IllegalArgumentException("未找到用户API配置且未提供完整参数")))
        .doOnError(e -> log.error("PARAM TRACE [{}]: 配置解析失败 - {}", userId, e.getMessage(), e));
    }

    private String buildFileContent(String fileContent, String question) {
        return "【File Content】\n" + fileContent + "\n\n【User Question】" + question;
    }

    private String buildTextContent(String question) {
        return "【User Question】" + question;
    }

    private ResponseEntity<ChatResponse> errorResponse(int code, String message) {
        ChatResponse response = new ChatResponse();
        response.setContent(String.format("{\"error\":{\"code\":%d,\"message\":\"%s\"}}", code, message));
        response.setUsage(new UsageInfo());
        return ResponseEntity.status(code).body(response);
    }

    /**
     * 创建一个带有错误信息的响应
     * 
     * @param errorMessage 错误信息
     * @return 错误响应
     */
    private ChatResponse createErrorResponse(String errorMessage) {
        ChatResponse response = new ChatResponse();
        response.setContent("发生错误: " + errorMessage);
        response.setUsage(new UsageInfo());
        return response;
    }
    
    /**
     * 创建一个熔断器触发的回退响应
     * 
     * @param throwable 导致熔断的异常
     * @return 回退响应
     */
    private ChatResponse createFallbackResponse(Throwable throwable) {
        ChatResponse response = new ChatResponse();
        response.setContent("系统繁忙，请稍后重试。错误原因: " + throwable.getMessage());
        response.setUsage(new UsageInfo());
        return response;
    }
    
    /**
     * 查找匹配的API配置
     * 
     * @param userId 用户ID
     * @param apiUrl API URL
     * @param apiKey API密钥
     * @param modelName 模型名称
     * @return 匹配的API配置
     */
    private Mono<ApiConfig> findMatchingConfig(String userId, String apiUrl, String apiKey, String modelName) {
        // 如果提供了完整API参数，直接使用
        if (StringUtils.hasText(apiUrl) && StringUtils.hasText(apiKey)) {
            String finalModelName = StringUtils.hasText(modelName) ? modelName : "deepseek-chat";
            log.info("Using provided API config directly: url={}, model={}", apiUrl, finalModelName);
            
            return Mono.just(new ApiConfig()
                    .setApiUrl(apiUrl)
                    .setApiKey(apiKey)
                    .setModelName(finalModelName));
        }
        
        // 尝试根据提供的任意参数和userId查询匹配的配置
        return Mono.fromCallable(() -> {
            // 从数据库中查找匹配的配置
            ApiConfig config = apiConfigService.findMatchingConfig(userId, apiUrl, apiKey, modelName);
            
            // 如果找到了配置
            if (config != null) {
                log.info("Found matching API config for user {}: url={}, model={}", 
                        userId, config.getApiUrl(), config.getModelName());
                
                // 使用默认值填充缺失的字段
                if (!StringUtils.hasText(config.getModelName())) {
                    config.setModelName("deepseek-chat");
                    log.info("Using default model name: {}", config.getModelName());
                }
                
                return config;
            }
            
            // 如果没有任何参数传入，尝试获取最新的配置
            if (!StringUtils.hasText(apiUrl) && !StringUtils.hasText(apiKey) && !StringUtils.hasText(modelName)) {
                ApiConfig latestConfig = apiConfigService.getLatestConfig(userId);
                if (latestConfig != null) {
                    log.info("Using latest API config for user {}: url={}, model={}", 
                            userId, latestConfig.getApiUrl(), latestConfig.getModelName());
                    
                    // 使用默认值填充缺失的字段
                    if (!StringUtils.hasText(latestConfig.getModelName())) {
                        latestConfig.setModelName("deepseek-chat");
                        log.info("Using default model name: {}", latestConfig.getModelName());
                    }
                    
                    return latestConfig;
                }
            }
            
            // 如果无法找到配置，抛出异常
            throw new IllegalArgumentException("未找到匹配的API配置");
        });
    }
}