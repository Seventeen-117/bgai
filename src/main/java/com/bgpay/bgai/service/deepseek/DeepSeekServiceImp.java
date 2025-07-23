package com.bgpay.bgai.service.deepseek;

import com.bgpay.bgai.datasource.DS;
import com.bgpay.bgai.entity.UsageCalculationDTO;
import com.bgpay.bgai.response.ChatResponse;
import com.bgpay.bgai.service.ChatCompletionsService;
import com.bgpay.bgai.service.UsageInfoService;
import com.bgpay.bgai.service.mq.MQCallback;
import com.bgpay.bgai.service.mq.RocketMQProducerService;
import io.seata.spring.annotation.GlobalTransactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import com.bgpay.bgai.entity.ChatCompletions;
import com.bgpay.bgai.entity.UsageInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.bgpay.bgai.transaction.TransactionCoordinator;
import com.bgpay.bgai.service.impl.BGAIServiceImpl;

/**
 * This class implements the DeepSeekService interface, providing methods to process requests
 * to the DeepSeek API, including content sanitization, request building, retry mechanisms,
 * and asynchronous data saving.
 */
@Service("deepSeekService")
public class DeepSeekServiceImp implements DeepSeekService {
    private static final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            
    private static final Logger log = LoggerFactory.getLogger(DeepSeekServiceImp.class);

    @Value("${stream:false}")
    private boolean stream;

    @Value("${retry.count:5}")
    private int maxRetries;

    @Value("${retry.initial_delay:2000}")
    private long initialDelay;

    @Value("${retry.backoff_factor:1.5}")
    private double backoffFactor;

    @Value("${max.request.length:8000}")
    private int maxRequestLength;

    @Autowired
    private ChatCompletionsService chatCompletionsService;

    @Autowired
    private UsageInfoService usageInfoService;

    private final MeterRegistry meterRegistry;


    @Autowired
    private ConversationHistoryService historyService;

    @Autowired
    private FileWriterService fileWriterService;

    @Autowired
    @Qualifier("asyncTaskExcutor")
    private Executor asyncRequestExecutor;

    @Autowired
    private RocketMQProducerService rocketMQProducer;

    private final CloseableHttpClient httpClient;

    @Autowired(required = false)
    private WebClient webClient;

    private static final int CPU_CORES = Runtime.getRuntime().availableProcessors();

    private final ScheduledExecutorService retryExecutor = new ScheduledThreadPoolExecutor(
            CPU_CORES * 2,
            new CustomThreadFactory("Retry-")
    );

    @Autowired
    private TransactionCoordinator transactionCoordinator;

    @Autowired
    private BGAIServiceImpl bgaiService;

    /**
     * Constructor for DeepSeekServiceImp.
     * Initializes the CloseableHttpClient with the given configurations,
     * and sets up the necessary connection managers and request configurations.
     *
     * @param maxConn      The maximum number of connections in the connection pool.
     * @param maxPerRoute  The maximum number of connections per route in the connection pool.
     * @param meterRegistry The MeterRegistry instance for monitoring.
     */
    @Autowired
    public DeepSeekServiceImp(
            @Value("${http.max.conn:500}") int maxConn,
            @Value("${http.max.conn.per.route:50}") int maxPerRoute, 
            MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(
                30, TimeUnit.SECONDS
        );
        connManager.setMaxTotal(maxConn);
        connManager.setDefaultMaxPerRoute(maxPerRoute);
        connManager.setValidateAfterInactivity(30_000);

        // 配置超时参数
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(30_000)
                .setSocketTimeout(60_000)
                .build();

        this.httpClient = HttpClients.custom()
                .setConnectionManager(connManager)
                .setDefaultRequestConfig(requestConfig)
                .setKeepAliveStrategy((response, context) -> 60_000)
                .evictIdleConnections(60, TimeUnit.SECONDS)
                .setRetryHandler(new DefaultHttpRequestRetryHandler(3, true))
                .build();
    }

    /**
     * Process the request to the DeepSeek API, including content sanitization, request building,
     * execution with retry, and parsing the response.
     *
     * @param content   The input content for the chat
     * @param apiUrl    The URL of the DeepSeek API
     * @param apiKey    The API key for authentication
     * @param modelName The name of the model to use
     * @return A ChatResponse object containing the response content and usage information
     */
        @GlobalTransactional(name = "deepseek-process-tx", rollbackFor = Exception.class)    @DS("master")
        public ChatResponse processRequest(String content,
                                       String apiUrl,
                                       String apiKey,
                                       String modelName,
                                       String userId,
                                       boolean multiTurn) {
        ChatResponse chatResponse = new ChatResponse();
        String requestBody = "";
        try {
            List<Map<String, Object>> history = multiTurn ?
                    historyService.getValidHistory(userId) :
                    new ArrayList<>();

            // 创建当前用户消息（始终包含最新内容）
            Map<String, Object> currentMessage = createMessage("user", content);

            // 构建完整消息序列 = 历史记录 + 当前消息
            List<Map<String, Object>> messagesForRequest = new ArrayList<>(history);
            messagesForRequest.add(currentMessage);

            // 构建请求
            requestBody = buildRequest(messagesForRequest, modelName);
            CompletableFuture<String> future = executeWithRetry(apiUrl, apiKey, requestBody);
            String response = future.get();
            if (multiTurn) {
                String assistantContent = extractContent(response);
                historyService.addMessage(userId, "user", content);  // 包含文件内容的问题
                historyService.addMessage(userId, "assistant", assistantContent);
            }
            saveCompletionDataAsync(response);
            JsonNode root = mapper.readTree(response);
            JsonNode choices = root.path("choices");
            if (!choices.isEmpty()) {
                JsonNode message = choices.get(0).path("message");
                if (!message.isEmpty()) {
                    String responseContent = message.path("content").asText();
                    chatResponse.setContent(responseContent);
                    fileWriterService.writeContentToFile(responseContent, "response_" + System.currentTimeMillis() + ".txt");
                }
            }

            JsonNode usageNode = root.path("usage");
            if (!usageNode.isEmpty()) {
                UsageInfo usage = extractUsageInfo(usageNode, root, userId);
                chatResponse.setUsage(usage);
                UsageCalculationDTO calculationDTO = convertToDTO(usage);
                rocketMQProducer.sendBillingMessage(calculationDTO, userId);
                String messageId = UUID.randomUUID().toString();
                rocketMQProducer.sendChatLogAsync(
                        messageId,
                        requestBody,
                        chatResponse,
                        userId,
                        new MQCallback() {
                            @Override
                            public void onSuccess(String msgId) {
                                meterRegistry.counter("mq.message.success", "msgId", msgId).increment();
                                log.info("Message {} 发送成功，执行清理操作", msgId);
                            }

                            @Override
                            public void onFailure(String msgId, Throwable e) {
                                meterRegistry.counter("mq.message.failure", "msgId", msgId).increment();
                                log.error("Message {} 发送失败", msgId, e);
                            }
                        }
                );
            }
        } catch (Exception e) {
            String errorMessage = "Processing failed: " + e.getMessage();
            chatResponse.setContent(buildErrorResponse(500, errorMessage));
            chatResponse.setUsage(new UsageInfo());
        }
        return chatResponse;
    }
    /**
     * Process the request to the DeepSeek API in a reactive way, including building the request JSON,
     * making the request, parsing the response, and handling related asynchronous tasks.
     *
     * @param content   The input content for the chat.
     * @param apiUrl    The URL of the DeepSeek API.
     * @param apiKey    The API key for authentication.
     * @param modelName The name of the model to use.
     * @param userId    The ID of the user making the request.
     * @param multiTurn Whether it is a multi-turn conversation.
     * @return A Mono that emits a ChatResponse object containing the response content and usage information.
     */
    @GlobalTransactional(name = "deepseek-process-tx", rollbackFor = Exception.class)
    @DS("master")
    @Override
    public Mono<ChatResponse> processRequestReactive(String content,
                                                     String apiUrl,
                                                     String apiKey,
                                                     String modelName,
                                                     String userId,
                                                     boolean multiTurn) {
        log.info("Processing reactive request: content length={}, apiUrl={}, modelName={}, userId={}, multiTurn={}",
                content.length(), apiUrl, modelName, userId, multiTurn);

        // 生成业务键，用于Saga状态机
        String businessKey = userId + ":" + UUID.randomUUID().toString();
        
        return Mono.just(content)
                .<ChatResponse>flatMap(c -> {
                    // 第一阶段：准备事务
                    String chatCompletionId = transactionCoordinator.prepare(userId);
                    log.info("Transaction prepared with chatCompletionId: {}", chatCompletionId);
                    
                    // 构建请求
                    Map<String, Object> requestMap = buildRequestJson(c, modelName, multiTurn, userId);
                    requestMap.put("chatCompletionId", chatCompletionId);
                    
                    return executeRequest(apiUrl, apiKey, requestMap)
                            .flatMap(response -> {
                                log.debug("Response received from DeepSeek API: {}", 
                                        response.length() > 100 ? response.substring(0, 100) + "..." : response);
                                
                                // 解析响应
                                return parseResponse(response)
                                    .map(chatResponse -> {
                                        chatResponse.getUsage().setChatCompletionId(chatCompletionId);
                                        
                                        // 第二阶段：提交事务
                                        boolean committed = transactionCoordinator.commit(userId, chatCompletionId, modelName);
                                        if (!committed) {
                                            log.warn("Failed to commit transaction, using compensation mechanism");
                                            // 使用Saga补偿模式处理失败情况
                                            bgaiService.executeFirstStep(businessKey);
                                        }
                                        
                                        return chatResponse;
                                    });
                            })
                            .onErrorResume(e -> {
                                log.error("Error processing request: {}", e.getMessage(), e);
                                // 发生错误时回滚事务
                                String rollbackId = transactionCoordinator.rollback(userId);
                                
                                ChatResponse errorResponse = new ChatResponse();
                                errorResponse.setContent(buildErrorResponse(500, e.getMessage()));
                                UsageInfo usage = new UsageInfo();
                                usage.setChatCompletionId(rollbackId);
                                errorResponse.setUsage(usage);
                                
                                return Mono.just(errorResponse);
                            });
                })
                .doOnSuccess(response -> {
                    // 构建请求信息Map，用于聊天记录
                    Map<String, Object> requestInfoMap = new HashMap<>();
                    requestInfoMap.put("content", content);
                    requestInfoMap.put("apiUrl", apiUrl);
                    requestInfoMap.put("modelName", modelName);
                    requestInfoMap.put("multiTurn", multiTurn);
                    requestInfoMap.put("timestamp", System.currentTimeMillis());
                    
                    // 发送账单消息和聊天日志
                    if (response.getUsage() != null) {
                        // 1. 发送账单消息
                        sendBillingMessage(response, userId)
                                .subscribe(
                                        unused -> log.info("Billing message sent successfully"),
                                        error -> log.error("Failed to send billing message: {}", error.getMessage(), error)
                                );
                        
                        // 2. 发送聊天日志消息，即使账单消息失败也要发送
                        sendChatLogAsync(response, userId, requestInfoMap)
                                .subscribe(
                                        unused -> log.info("Chat log message sent successfully"),
                                        error -> log.error("Failed to send chat log message: {}", error.getMessage(), error)
                                );
                    } else {
                        // 即使没有usage信息，也要尝试发送聊天日志，防止丢失记录
                        log.warn("Response没有usage信息，但仍将发送聊天日志");
                        UsageInfo defaultUsage = new UsageInfo();
                        defaultUsage.setUserId(userId);
                        defaultUsage.setChatCompletionId("error-" + UUID.randomUUID().toString());
                        defaultUsage.setCreatedAt(LocalDateTime.now());
                        response.setUsage(defaultUsage);
                        
                        sendChatLogAsync(response, userId, requestInfoMap)
                                .subscribe(
                                        unused -> log.info("Default chat log message sent successfully"),
                                        error -> log.error("Failed to send default chat log message: {}", error.getMessage(), error)
                                );
                    }
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Asynchronously send the chat log message to the message queue.
     *
     * @param response The ChatResponse object containing the response information.
     * @param userId   The ID of the user.
     * @return A Mono that completes when the message sending operation is finished.
     */
    private Mono<Void> sendChatLogAsync(ChatResponse response, String userId,Map map) {
        return Mono.fromRunnable(() -> {
            String messageId = UUID.randomUUID().toString();
            rocketMQProducer.sendChatLogAsync(
                    messageId,
                    map.toString(),
                    response,
                    userId,
                    msgId -> {
                        meterRegistry.counter("mq.message.success").increment();
                    },
                    (msgId, e) -> {
                        meterRegistry.counter("mq.message.failure").increment();
                    }
            );
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
    /**
     * Asynchronously send the billing message to the message queue, and handle retries if failed.
     *
     * @param response The ChatResponse object containing the usage information.
     * @param userId   The ID of the user.
     * @return A Mono that completes when the message sending operation is finished.
     */
    private Mono<Void> sendBillingMessage(ChatResponse response, String userId) {
        UsageCalculationDTO dto = convertToDTO(response.getUsage());
        String chatCompletionId = dto.getChatCompletionId();
        String businessKey = userId + ":" + chatCompletionId;
        
        log.info("开始发送账单消息，chatCompletionId: {}, userId: {}", chatCompletionId, userId);
        
        return rocketMQProducer.sendBillingMessageReactive(dto, userId)
                .retryWhen(Retry.backoff(3, Duration.ofMillis(100))
                        .maxBackoff(Duration.ofSeconds(1))
                        .doBeforeRetry(ctx ->
                                log.warn("账单消息发送第{}次重试，原因：{}, chatCompletionId: {}",
                                        ctx.totalRetries(), ctx.failure(), chatCompletionId))
                )
                .doOnSuccess(v -> {
                    meterRegistry.counter("billing.sent.success").increment();
                    log.info("账单消息发送成功，chatCompletionId: {}, userId: {}", chatCompletionId, userId);
                    
                    // 启动Saga流程
                    bgaiService.executeFirstStep(businessKey);
                })
                .doOnError(e -> {
                    meterRegistry.counter("billing.sent.failure").increment();
                    log.error("账单消息发送失败，chatCompletionId: {}, userId: {}, 错误: {}", 
                            chatCompletionId, userId, e.getMessage(), e);
                    
                    // 发送失败时执行补偿
                    bgaiService.compensateFirstStep(businessKey);
                });
    }
    /**
     * Convert a UsageInfo object to a UsageCalculationDTO object.
     *
     * @param usage The UsageInfo object to be converted.
     * @return A UsageCalculationDTO object containing the relevant information.
     */
    private UsageCalculationDTO convertToDTO(UsageInfo usage) {
        UsageCalculationDTO calculationDTO = new UsageCalculationDTO();
        calculationDTO.setChatCompletionId(usage.getChatCompletionId());
        calculationDTO.setModelType(usage.getModelType());
        calculationDTO.setPromptCacheHitTokens(usage.getPromptTokensCached());
        calculationDTO.setPromptCacheMissTokens(usage.getPromptCacheMissTokens());
        calculationDTO.setPromptTokensCached(usage.getPromptTokensCached());
        calculationDTO.setCompletionTokens(usage.getCompletionTokens());
        calculationDTO.setCompletionReasoningTokens(usage.getCompletionReasoningTokens());
        calculationDTO.setCreatedAt(LocalDateTime.now());
        return calculationDTO;
    }
    /**
     * Parse the API response string and extract relevant information to construct a ChatResponse object.
     *
     * @param responseBody The API response string in JSON format.
     * @return A Mono that emits a ChatResponse object containing the parsed information.
     */
    private Mono<ChatResponse> parseResponse(String responseBody) {
        try {
            ChatResponse response = new ChatResponse();
            
            JsonNode root = mapper.readTree(responseBody);
            JsonNode choicesNode = root.path("choices");
            
            if (choicesNode.isArray() && choicesNode.size() > 0) {
                JsonNode messageNode = choicesNode.get(0).path("message");
                if (messageNode.has("content")) {
                    response.setContent(messageNode.path("content").asText());
                }
            }
            
            // 获取响应中的用户ID（如果存在）
            String userId = null;
            JsonNode usageNode = root.path("usage");
            
            if (usageNode.has("userId") && !usageNode.path("userId").isNull() && 
                !usageNode.path("userId").asText().isEmpty()) {
                userId = usageNode.path("userId").asText();
                log.info("从API响应获取到用户ID: {}", userId);
            }
            
            // 从响应中提取使用信息
            UsageInfo usage = extractUsageInfo(usageNode, root, userId);
            
            // 确保UsageInfo中的userId被正确设置，即使从响应中没有找到
            if (usage.getUserId() == null || usage.getUserId().isEmpty()) {
                log.warn("UsageInfo中的userId为空，设置为default防止数据库问题，请检查头部或身份验证过程是否正确设置了用户ID");
                usage.setUserId("default");
            }
            
            response.setUsage(usage);
            log.info("解析完成的响应，使用用户ID: {}, 聊天完成ID: {}", usage.getUserId(), usage.getChatCompletionId());
            return Mono.just(response);
        } catch (Exception e) {
            log.error("Error parsing response: {}", e.getMessage(), e);
            return Mono.error(e);
        }
    }
    /**
     * Build the request JSON map for the API request, including adding historical messages if it's a multi-turn conversation
     * and the current user message.
     *
     * @param content   The input content for the chat.
     * @param model     The name of the model to use.
     * @param multiTurn Whether it is a multi-turn conversation.
     * @param userId    The ID of the user making the request.
     * @return A Map representing the request JSON structure.
     */
    private Map<String, Object> buildRequestJson(String content, String model, boolean multiTurn, String userId) {
        // 限制内容长度，确保不超过API限制
        content = limitContentLength(content, 16000);  // 限制大约16k tokens
        
        List<Map<String, Object>> messages = new ArrayList<>();
        
        // 添加系统消息以提高成功率
        messages.add(Map.of(
                "role", "system",
                "content", "You are a helpful assistant."
        ));

        if (multiTurn) {
            List<Map<String, Object>> history = historyService.getValidHistory(userId);
            messages.addAll(history);
        }
        
        messages.add(Map.of(
                "role", "user",
                "content", content
        ));

        // 根据内容智能设置 temperature
        double temperature = determineTemperature(content);

        // 确保 model 名称符合 DeepSeek API 的要求
        String finalModel = model;
        if (model == null || model.trim().isEmpty()) {
            finalModel = "deepseek-chat";  // 使用默认模型
            log.warn("Using default model 'deepseek-chat' because model name was empty");
        }

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model", finalModel);
        requestMap.put("messages", messages);
        requestMap.put("stream", false);
        requestMap.put("temperature", temperature);
        requestMap.put("max_tokens", 2000);  // 降低 max_tokens 以避免超过限制
        
        return requestMap;
    }

    /**
     * 限制内容长度，确保不超过API限制
     * 
     * @param content 原始内容
     * @param maxChars 最大字符数
     * @return 限制长度后的内容
     */
    private String limitContentLength(String content, int maxChars) {
        if (content == null) {
            return "";
        }
        
        if (content.length() <= maxChars) {
            return content;
        }
        
        // 截取内容，保留开头和结尾
        int halfLength = maxChars / 2;
        String beginning = content.substring(0, halfLength);
        String ending = content.substring(content.length() - halfLength);
        
        return beginning + "\n\n...[内容太长，已截断]...\n\n" + ending;
    }

    private Map<String, Object> createMessage(String role, String content) {
        return Map.of(
                "role", role,
                "content", sanitizeContent(content),
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 智能判断内容场景并设置 temperature
     * 代码生成/数学解题 → 0.0
     * 数据抽取/分析 → 1.0
     * 翻译 → 1.3
     * 创意类写作 → 1.5
     * 通用对话 → 1.3
     * 其他场景 → 1.0
     */
    private double determineTemperature(String content) {
        String lowerContent = content.toLowerCase();

        // 代码生成/数学解题场景
        if (lowerContent.matches(".*(代码|编程|函数|数学|算法|实现|编写|python|java|def|public|class).*")
                || lowerContent.matches(".*\\b(code|function|program|calculate)\\b.*")
                || lowerContent.matches(".*\\d+\\s*[+\\-*/=]\\s*\\d+.*")) {
            return 0.0;
        }

        // 数据抽取/分析场景
        if (lowerContent.matches(".*(数据|分析|统计|报表|表格|处理|清洗|抽取|excel|csv|sql).*")) {
            return 1.0;
        }

        // 翻译场景（中英互译）
        if (lowerContent.matches(".*(翻译|translate|英文|中文|日文|法语).*")) {
            return 1.3;
        }

        // 创意写作场景
        if (lowerContent.matches(".*(诗|诗歌|故事|小说|创意|剧本|散文|创作|想象).*")) {
            return 1.5;
        }

        // 通用对话场景（问答类内容）
        if (lowerContent.matches("^(你好|您好|hi|hello|早上好|下午好).*")
                || lowerContent.contains("吗？")
                || lowerContent.matches(".*(怎么|如何|为什么|哪|谁|什么时候|哪里|？).*")) {
            return 1.3;
        }

        return 1.0;
    }

    private String extractContent(String response) throws JsonProcessingException {
        JsonNode root = mapper.readTree(response);
        return root.at("/choices/0/message/content").asText();
    }


    private String sanitizeContent(String content) {
        return truncateUtf8(content
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n"), maxRequestLength);
    }

    /**
     * Build the request body in JSON format.
     *
     * @param modelName The name of the model
     * @return The JSON string of the request body
     * @throws JsonProcessingException if there is an error in JSON processing
     */
    private String buildRequest(List<Map<String, Object>> history, String modelName)
            throws JsonProcessingException {
        ObjectNode requestNode = mapper.createObjectNode();
        requestNode.put("model", modelName);
        requestNode.put("stream", this.stream);

        ArrayNode messages = requestNode.putArray("messages");

        // 自动携带最近的附件信息
        history.forEach(msg -> {
            ObjectNode msgNode = mapper.createObjectNode();
            msgNode.put("role", (String) msg.get("role"));

            // 对含附件的消息添加标记
            String content = (String) msg.get("content");
            if ((boolean)msg.getOrDefault("hasAttachment", false)) {
                content += "\n[包含附件信息]";
            }

            msgNode.put("content", content);
            messages.add(msgNode);
        });

        return mapper.writeValueAsString(requestNode);
    }


    /**
     * Execute the request with a retry mechanism.
     *
     * @param apiUrl      The URL of the API
     * @param apiKey      The API key for authentication
     * @param requestBody The request body in JSON format
     * @return A CompletableFuture that will complete with the response string
     */
    private CompletableFuture<String> executeWithRetry(String apiUrl, String apiKey, String requestBody) {
        CompletableFuture<String> future = new CompletableFuture<>();
        AtomicInteger retries = new AtomicInteger(0);

        Runnable task = new Runnable() {
            @Override
            public void run() {
                if (retries.get() >= maxRetries) {
                    // If the maximum number of retries is reached, complete with an error response
                    future.complete(buildErrorResponse(503, "Service temporarily unavailable"));
                    return;
                }

                try {
                    // Send the request and complete the future with the result
                    String result = sendRequest(apiUrl, apiKey, requestBody);
                    future.complete(result);
                } catch (Exception e) {
                    if (retries.incrementAndGet() < maxRetries) {
                        // If the retry limit is not reached, schedule the next retry
                        long delay = (long) (initialDelay * Math.pow(backoffFactor, retries.get()));
                        retryExecutor.schedule(this, delay, TimeUnit.MILLISECONDS);
                    } else {
                        // If the retry limit is reached, complete the future exceptionally
                        future.completeExceptionally(e);
                    }
                }
            }
        };

        asyncRequestExecutor.execute(task);
        return future;
    }

    /**
     * Send the HTTP POST request to the API.
     *
     * @param apiUrl      The URL of the API
     * @param apiKey      The API key for authentication
     * @param requestBody The request body in JSON format
     * @return The response string from the API
     * @throws IOException if there is an I/O error during the request
     */
    private String sendRequest(String apiUrl, String apiKey, String requestBody) throws IOException {
        HttpPost post = new HttpPost(apiUrl);
        try {
            post.setHeader("Content-Type", "application/json; charset=UTF-8");
            post.setHeader("Authorization", "Bearer " + apiKey);
            post.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));

            HttpResponse response = httpClient.execute(post);
            return parseResponse(response);
        } catch (SocketTimeoutException e) {
            log.error("请求超时: {}", apiUrl, e);
            throw new RuntimeException("API请求超时", e);
        } catch (ConnectException e) {
            log.error("连接拒绝: {}", apiUrl, e);
            throw new RuntimeException("无法连接到API服务", e);
        } finally {
            post.releaseConnection();
        }
    }

    /**
     * Parse the HTTP response and handle different status codes.
     *
     * @param response The HTTP response object
     * @return The parsed response string
     * @throws IOException if there is an I/O error during response parsing
     */
    private String parseResponse(HttpResponse response) throws IOException {
        int statusCode = response.getStatusLine().getStatusCode();
        String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        EntityUtils.consume(response.getEntity()); // 确保实体被完全消费

        if (statusCode == HttpStatus.SC_OK) {
            validateJson(body);
            return body;
        }
        return buildErrorResponse(statusCode, "API error: " + body);
    }
    /**
     * Validate the JSON string to ensure it is in a valid format.
     *
     * @param json The JSON string to validate
     */
    private void validateJson(String json) {
        try {
            mapper.readTree(json);
        } catch (IOException e) {
            throw new RuntimeException("Invalid JSON response");
        }
    }

    /**
     * Truncate the UTF-8 string to a specified maximum number of bytes.
     *
     * @param input    The input string
     * @param maxBytes The maximum number of bytes
     * @return The truncated string
     */
    private String truncateUtf8(String input, int maxBytes) {
        if (input == null || maxBytes <= 0) return "";

        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= maxBytes) return input;

        // Reverse search for a valid character boundary
        int truncLength = maxBytes;
        while (truncLength > 0 && (bytes[truncLength] & 0xC0) == 0x80) {
            truncLength--;
        }
        return new String(bytes, 0, truncLength, StandardCharsets.UTF_8) + "[TRUNCATED]";
    }

    /**
     * Build an error response in JSON format.
     *
     * @param code    The error code
     * @param message The error message
     * @return The JSON string of the error response
     */
    private String buildErrorResponse(int code, String message) {
        try {
            ObjectNode errorNode = mapper.createObjectNode();
            errorNode.putObject("error")
                    .put("code", code)
                    .put("message", message);
            return mapper.writeValueAsString(errorNode);
        } catch (JsonProcessingException e) {
            return "{\"error\":{\"code\":500,\"message\":\"Failed to generate error message\"}}";
        }
    }

    /**
     * Asynchronously save the completion data, including chat completions and usage information.
     *
     * @param responseJson The JSON string of the API response
     */
    @Transactional
    @Async
    public void saveCompletionDataAsync(String responseJson) {
        try {
            JsonNode root = mapper.readTree(responseJson);
            ChatCompletions completion = parseChatCompletion(root);
            chatCompletionsService.insertChatCompletions(completion);
            CompletableFuture.runAsync(() -> {
                try {
                    UsageInfo usage = parseUsageInfo(root);
                    usageInfoService.insertUsageInfo(usage);
                } catch (Exception e) {
                    log.error("Exception occurred while saving UsageInfo", e);
                    throw new RuntimeException("Exception occurred while saving UsageInfo", e);
                }
            }).exceptionally(ex -> {
                // Handle exceptions in the asynchronous task
                log.error("Exception occurred while saving UsageInfo asynchronously", ex);
                throw new RuntimeException("Exception occurred while saving UsageInfo asynchronously", ex);
            });
        } catch (Exception e) {
            // Log the exception and throw a runtime exception
            log.error("Exception occurred while saving CompletionData", e);
            throw new RuntimeException("Exception occurred while saving CompletionData", e);
        }
    }

    /**
     * Parse the JSON response to extract chat completions data.
     *
     * @param root The root JsonNode of the response
     * @return A ChatCompletions object containing the parsed data
     */
    private ChatCompletions parseChatCompletion(JsonNode root) {
        ChatCompletions chatCompletions = new ChatCompletions();
        chatCompletions.setObject(root.path("object").asText());
        chatCompletions.setCreated(root.path("created").asLong());
        chatCompletions.setModel(root.path("model").asText());
        chatCompletions.setSystemFingerprint(root.path("system_fingerprint").asText());
        chatCompletions.setApiKeyId(root.path("id").asText());

        return chatCompletions;
    }

    /**
     * Parse the JSON response to extract usage information data.
     *
     * @param root The root JsonNode of the response
     * @return A UsageInfo object containing the parsed data
     */
    private UsageInfo parseUsageInfo(JsonNode root) {
        JsonNode usageNode = root.path("usage");
        JsonNode promptDetails = usageNode.path("prompt_tokens_details");
        JsonNode completionDetails = usageNode.path("completion_tokens_details");
        UsageInfo usageInfo = new UsageInfo();
        usageInfo.setChatCompletionId(root.path("id").asText());
        usageInfo.setPromptTokens(usageNode.path("prompt_tokens").asInt());
        usageInfo.setTotalTokens(usageNode.path("total_tokens").asInt());
        usageInfo.setCompletionTokens(usageNode.path("completion_tokens").asInt());
        usageInfo.setPromptTokensCached(promptDetails.path("cached_tokens").asInt());
        usageInfo.setCompletionReasoningTokens(completionDetails.path("reasoning_tokens").asInt());
        usageInfo.setPromptCacheHitTokens(usageNode.path(" prompt_cache_hit_tokens").asInt());
        usageInfo.setPromptCacheMissTokens(usageNode.path("prompt_cache_miss_tokens").asInt());
        usageInfo.setCreatedAt(LocalDateTime.now());
        usageInfo.setModelType(root.path("model").asText());
        return usageInfo;
    }

    /**
     * Extract usage information from the JSON response and generate a unique ID.
     *
     * @param usageNode The JsonNode containing usage information
     * @param root      The root JsonNode of the response
     * @return A UsageInfo object containing the extracted data
     */
    private UsageInfo extractUsageInfo(JsonNode usageNode, JsonNode root, String userId) {
        JsonNode promptDetails = usageNode.path("prompt_tokens_details");
        JsonNode completionDetails = usageNode.path("completion_tokens_details");
        
        UsageInfo usage = new UsageInfo();
        UUID uuid = UUID.randomUUID();
        long mostSignificantBits = uuid.getMostSignificantBits();
        long leastSignificantBits = uuid.getLeastSignificantBits();
        long combined = (mostSignificantBits << 32) | (leastSignificantBits & 0xFFFFFFFFL);
        int thirtyBitInt = (int) (combined & 0x3FFFFFFFL);
        
        usage.setId((long) thirtyBitInt);
        usage.setChatCompletionId(root.path("id").asText());
        usage.setPromptTokens(usageNode.path("prompt_tokens").asInt());
        usage.setTotalTokens(usageNode.path("total_tokens").asInt());
        usage.setCompletionTokens(usageNode.path("completion_tokens").asInt());
        usage.setPromptTokensCached(promptDetails.path("cached_tokens").asInt());
        usage.setCompletionReasoningTokens(completionDetails.path("reasoning_tokens").asInt());
        usage.setPromptCacheHitTokens(usageNode.path("prompt_cache_hit_tokens").asInt());
        usage.setPromptCacheMissTokens(usageNode.path("prompt_cache_miss_tokens").asInt());
        usage.setCreatedAt(LocalDateTime.now());
        usage.setUpdatedAt(LocalDateTime.now());
        usage.setModelType(root.path("model").asText());
        
        // 确保设置userId，使用提供的userId或者默认值
        if (userId != null && !userId.trim().isEmpty()) {
            usage.setUserId(userId);
        } else {
            // 如果没有提供userId，使用一个默认值而不是null
            usage.setUserId("default");
            log.warn("在extractUsageInfo中使用默认userId：'default'，而不是null或空字符串");
        }
        
        return usage;
    }

    /**
     * Custom thread factory for creating threads with a specific name prefix and daemon flag.
     */
    static class CustomThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(0);
        private final String namePrefix;

        /**
         * Constructor for CustomThreadFactory.
         *
         * @param prefix The prefix for the thread names
         */
        CustomThreadFactory(String prefix) {
            this.namePrefix = prefix;
        }

        /**
         * Create a new thread with the specified runnable and name.
         *
         * @param r The runnable task
         * @return A new thread object
         */
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + counter.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    }



    /**
     * Execute the HTTP request to the DeepSeek API.
     *
     * @param apiUrl The URL of the API
     * @param apiKey The API key for authentication
     * @param requestMap The request parameters map
     * @return A Mono that emits the response string
     */
    private Mono<String> executeRequest(String apiUrl, String apiKey, Map<String, Object> requestMap) {
        try {
            String requestBody = mapper.writeValueAsString(requestMap);
            
            return webClient.post()
                    .uri(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(60))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .maxBackoff(Duration.ofSeconds(5)));
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }

    /**
     * 处理Map格式的请求体，兼容OpenAI格式的API请求
     */
    @GlobalTransactional(name = "deepseek-process-tx", rollbackFor = Exception.class)
    @DS("master")
    @Override
    public Mono<ChatResponse> processRequestReactive(Map<String, Object> requestBody,
                                                     String apiUrl,
                                                     String apiKey,
                                                     String modelName,
                                                     String userId,
                                                     boolean multiTurn) {
        try {
            // 提取消息内容
            if (!requestBody.containsKey("messages")) {
                ChatResponse errorResponse = new ChatResponse();
                errorResponse.setSuccess(false);
                errorResponse.setError(new ChatResponse.Error(400, "Messages are required"));
                return Mono.just(errorResponse);
            }
            
            // 将OpenAI格式的请求转换为内部格式
            StringBuilder contentBuilder = new StringBuilder();
            List<Map<String, Object>> messages = (List<Map<String, Object>>) requestBody.get("messages");
            
            for (Map<String, Object> message : messages) {
                String role = (String) message.get("role");
                String content = (String) message.get("content");
                if (role != null && content != null) {
                    contentBuilder.append(role).append(": ").append(content).append("\n");
                }
            }
            
            // 调用原有的字符串内容处理方法
            return processRequestReactive(
                contentBuilder.toString(),
                apiUrl,
                apiKey,
                modelName,
                userId,
                multiTurn
            );
        } catch (Exception e) {
            log.error("处理Map请求体失败", e);
            ChatResponse errorResponse = new ChatResponse();
            errorResponse.setSuccess(false);
            errorResponse.setError(new ChatResponse.Error(500, e.getMessage()));
            return Mono.just(errorResponse);
        }
    }
}