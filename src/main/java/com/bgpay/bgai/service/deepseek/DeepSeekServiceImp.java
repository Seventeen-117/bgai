package com.bgpay.bgai.service.deepseek;import com.bgpay.bgai.datasource.DS;import com.bgpay.bgai.entity.UsageCalculationDTO;import com.bgpay.bgai.response.ChatResponse;import com.bgpay.bgai.service.ChatCompletionsService;import com.bgpay.bgai.service.UsageInfoService;import com.bgpay.bgai.service.mq.MQCallback;import com.bgpay.bgai.service.mq.RocketMQProducerService;import io.seata.spring.annotation.GlobalTransactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.MeterRegistry;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.seata.spring.annotation.GlobalTransactional;
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
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import com.bgpay.bgai.entity.ChatCompletions;
import com.bgpay.bgai.entity.UsageInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;
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
import static org.reflections.Reflections.log;
import com.bgpay.bgai.service.impl.FallbackService;
import com.bgpay.bgai.transaction.TransactionCoordinator;

/**
 * This class implements the DeepSeekService interface, providing methods to process requests
 * to the DeepSeek API, including content sanitization, request building, retry mechanisms,
 * and asynchronous data saving.
 */
@Component
@Service
public class DeepSeekServiceImp implements DeepSeekService {
    private static final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

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

    private final WebClient webClient;

    private static final int CPU_CORES = Runtime.getRuntime().availableProcessors();

    private final ScheduledExecutorService retryExecutor = new ScheduledThreadPoolExecutor(
            CPU_CORES * 2,
            new CustomThreadFactory("Retry-")
    );

    @Autowired
    private FallbackService fallbackService;

    @Autowired
    private TransactionCoordinator transactionCoordinator;

    /**
     * Constructor for DeepSeekServiceImp.
     * Initializes the WebClient and CloseableHttpClient with the given configurations,
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
        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                .responseTimeout(Duration.ofSeconds(120))
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
                                .doOnConnected(conn ->
                                        conn.addHandlerLast(new ReadTimeoutHandler(120, TimeUnit.SECONDS)))
                ))
                .build();
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
                    fileWriterService.writeContentAsync(responseContent);
                }
            }

            JsonNode usageNode = root.path("usage");
            if (!usageNode.isEmpty()) {
                UsageInfo usage = extractUsageInfo(usageNode, root);
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

        WebClient client = webClient.mutate()
                .baseUrl(apiUrl)
                .build();

        Map<String, Object> map = buildRequestJson(content, modelName, multiTurn, userId);
        String jsonRequest = "";
        
        try {
            // 第一阶段：准备事务，生成chatCompletionId
            String chatCompletionId = transactionCoordinator.prepare(userId);
            
            // 将chatCompletionId添加到请求中
            map.put("id", chatCompletionId);
            
            jsonRequest = mapper.writeValueAsString(map);
            log.info("Generated request JSON with 2PC transaction: {}", 
                    jsonRequest.length() > 100 ? jsonRequest.substring(0, 100) + "..." : jsonRequest);
            
            return client.post()
                    .uri(apiUrl)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(jsonRequest)
                .retrieve()
                .bodyToMono(String.class)
                    .doOnNext(response -> {
                        log.info("Raw response received, length: {}", response.length());
                        // 尝试从响应中提取实际返回的chatCompletionId
                        try {
                            JsonNode root = mapper.readTree(response);
                            String responseId = root.path("id").asText();
                            if (responseId != null && !responseId.isEmpty() && !responseId.equals(chatCompletionId)) {
                                log.warn("Received different chatCompletionId from response: {} vs prepared: {}", 
                                        responseId, chatCompletionId);
                            }
                        } catch (Exception e) {
                            log.warn("Failed to parse response for ID verification: {}", e.getMessage());
                        }
                    })
                    .flatMap(response -> {
                        log.debug("Response received from DeepSeek API: {}", 
                                response.length() > 100 ? response.substring(0, 100) + "..." : response);
                        
                        // 第二阶段：提交事务
                        boolean committed = transactionCoordinator.commit(userId, chatCompletionId,modelName);
                        if (!committed) {
                            log.warn("Failed to commit transaction, using compensation mechanism");
                            // 使用Saga补偿模式处理失败情况，但仍保持ID一致性
                            transactionCoordinator.compensate(userId, chatCompletionId);
                        }
                        
                        return parseResponse(response)
                                .flatMap(chatResponse -> {
                                    // 确保使用事务中的chatCompletionId
                                    if (chatResponse.getUsage() != null) {
                                        chatResponse.getUsage().setChatCompletionId(chatCompletionId);
                                    }
                                    
                                    // 首先同步保存完成数据，确保数据在发送消息前已经持久化
                                    return Mono.fromCallable(() -> {
                                        // 同步执行数据保存，不使用异步方法
                                        saveCompletionDataSync(response, chatCompletionId);
                                        return chatResponse;
                                    })
                                    .subscribeOn(Schedulers.boundedElastic())
                                    // 然后发送账单消息，此时BillingTransactionListener检查时会发现记录已存在
                                    .flatMap(resp -> sendBillingMessage(resp, userId)
                                        .doOnError(e -> log.error("Failed to send billing message: {}", e.getMessage(), e))
                                        .onErrorResume(e -> Mono.empty())  // 错误恢复，不中断流程
                                        .thenReturn(resp))
                                    // 最后发送聊天日志
                                    .flatMap(resp -> sendChatLogAsync(resp, userId, map)
                                        .thenReturn(resp));
                                });
                    })
                .timeout(Duration.ofSeconds(120))
                .doOnNext(res ->
                        log.info("Complete processing chain completed: {}", res.getContent()))
                .onErrorResume(e -> {
                        log.error("Error in API request processing: {}", e.getMessage(), e);
                        
                        // 回滚事务，但保持chatCompletionId一致性
                        String rollbackId = transactionCoordinator.rollback(userId);
                        
                        // 创建包含一致ID的错误响应
                        ChatResponse errorResponse = createErrorResponseWithId(
                                e.getMessage(), rollbackId);
                        
                        return Mono.just(errorResponse);
                    });
        } catch (Exception e) {
            log.error("Error preparing request: {}", e.getMessage(), e);
            
            // 如果在准备阶段就失败，尝试查找已有事务或创建新的回滚事务
            String emergencyId = transactionCoordinator.hasActiveTransaction(userId) ?
                    transactionCoordinator.rollback(userId) :
                    "chat-emergency-" + UUID.randomUUID().toString();
            
            return Mono.just(createErrorResponseWithId(e.getMessage(), emergencyId));
        }
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
                    new MQCallback() {
                        @Override
                        public void onSuccess(String msgId) {
                            meterRegistry.counter("mq.message.success").increment();
                        }

                        @Override
                        public void onFailure(String msgId, Throwable e) {
                            meterRegistry.counter("mq.message.failure").increment();
                        }
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
        return Mono.defer(() -> {
                    UsageCalculationDTO dto = convertToDTO(response.getUsage());
                    String chatCompletionId = dto.getChatCompletionId();
                    log.info("开始发送账单消息，chatCompletionId: {}, userId: {}", chatCompletionId, userId);
                    return rocketMQProducer.sendBillingMessageReactive(dto, userId)
                            .retryWhen(Retry.backoff(3, Duration.ofMillis(100))
                                    .maxBackoff(Duration.ofSeconds(1))
                                    .doBeforeRetry(ctx ->
                                            log.warn("账单消息发送第{}次重试，原因：{}, chatCompletionId: {}",
                                                    ctx.totalRetries(), ctx.failure(), chatCompletionId))
                            );
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSubscribe(s ->
                        log.debug("开始发送账单消息，线程：{}", Thread.currentThread().getName()))
                .doOnSuccess(v -> {
                        meterRegistry.counter("billing.sent.success").increment();
                        log.info("账单消息发送成功，chatCompletionId: {}, userId: {}", 
                                response.getUsage().getChatCompletionId(), userId);
                })
                .doOnError(e -> {
                        meterRegistry.counter("billing.sent.failure").increment();
                        log.error("账单消息发送失败，chatCompletionId: {}, userId: {}, 错误: {}", 
                                response.getUsage().getChatCompletionId(), userId, e.getMessage(), e);
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
        calculationDTO.setPromptCacheHitTokens(usage.getPromptCacheHitTokens());
        calculationDTO.setPromptCacheMissTokens(usage.getPromptCacheMissTokens());
        calculationDTO.setCompletionTokens(usage.getCompletionTokens());
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
        return Mono.fromCallable(() -> {
            JsonNode root = mapper.readTree(responseBody);
            ChatResponse chatResponse = new ChatResponse();

            // 解析 content
            JsonNode choices = root.path("choices");
            if (!choices.isEmpty() && choices.get(0).has("message")) {
                chatResponse.setContent(choices.get(0).path("message").path("content").asText());
                JsonNode message = choices.get(0).path("message");
                if (!message.isEmpty()) {
                    String responseContent = message.path("content").asText();
                    chatResponse.setContent(responseContent);

                    fileWriterService.writeContentAsync(responseContent);
                }
            }
            JsonNode usageNode = root.path("usage");
            if (!usageNode.isEmpty()) {
                UsageInfo usage = extractUsageInfo(usageNode, root);
                chatResponse.setUsage(usage);
            }
            return chatResponse;
        });
    }


    /**
     * Create an error response ChatResponse object with a default error message and empty usage information.
     *
     * @return A ChatResponse object representing an error response.
     */
    private ChatResponse createErrorResponse() {
        ChatResponse response = new ChatResponse();
        response.setContent("服务暂时不可用，请稍后重试");
        response.setUsage(new UsageInfo());
        return response;
    }
    
    /**
     * Create an error response ChatResponse object with a custom error message and empty usage information.
     *
     * @param errorMessage The custom error message to include
     * @return A ChatResponse object representing an error response.
     */
    private ChatResponse createErrorResponse(String errorMessage) {
        ChatResponse response = new ChatResponse();
        response.setContent("服务暂时不可用，请稍后重试: " + errorMessage);
        response.setUsage(new UsageInfo());
        return response;
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
    private UsageInfo extractUsageInfo(JsonNode usageNode, JsonNode root) {
        JsonNode promptDetails = usageNode.path("prompt_tokens_details");
        JsonNode completionDetails = usageNode.path("completion_tokens_details");
        UsageInfo usage = new UsageInfo();
        UUID uuid = UUID.randomUUID();
        long mostSignificantBits = uuid.getMostSignificantBits();
        long leastSignificantBits = uuid.getLeastSignificantBits();
        long combined = (mostSignificantBits << 32) | (leastSignificantBits & 0xFFFFFFFFL);
        int thirtyBitInt = (int) (combined & 0x3FFFFFFFL);
        usage.setId(thirtyBitInt);
        usage.setChatCompletionId(root.path("id").asText());
        usage.setPromptTokens(usageNode.path("prompt_tokens").asInt());
        usage.setTotalTokens(usageNode.path("total_tokens").asInt());
        usage.setCompletionTokens(usageNode.path("completion_tokens").asInt());
        usage.setPromptTokensCached(promptDetails.path("cached_tokens").asInt());
        usage.setCompletionReasoningTokens(completionDetails.path("reasoning_tokens").asInt());
        usage.setPromptCacheHitTokens(usageNode.path(" prompt_cache_hit_tokens").asInt());
        usage.setPromptCacheMissTokens(usageNode.path("prompt_cache_miss_tokens").asInt());
        usage.setCreatedAt(LocalDateTime.now());
        usage.setModelType(root.path("model").asText());
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
     * 创建带有指定chatCompletionId的错误响应
     */
    private ChatResponse createErrorResponseWithId(String errorMessage, String chatCompletionId) {
        ChatResponse response = createErrorResponse(errorMessage);
        UsageInfo usage = new UsageInfo();
        usage.setChatCompletionId(chatCompletionId);
        response.setUsage(usage);
        return response;
    }

    /**
     * 同步保存完成数据，包括chat completions和usage information
     * 
     * @param responseJson DeepSeek API的JSON响应
     * @param chatCompletionId 指定的chatCompletionId，确保一致性
     */
    @Transactional
    public void saveCompletionDataSync(String responseJson, String chatCompletionId) {
        try {
            JsonNode root = mapper.readTree(responseJson);
            
            // 解析并保存ChatCompletions
            ChatCompletions completion = parseChatCompletion(root);
            // 确保使用事务一致的chatCompletionId
            completion.setApiKeyId(chatCompletionId);
            
            // 同步保存完成记录
            chatCompletionsService.insertChatCompletions(completion);
            
            // 解析并保存UsageInfo
            UsageInfo usage = parseUsageInfo(root);
            // 确保使用事务一致的chatCompletionId
            usage.setChatCompletionId(chatCompletionId);
            
            // 同步保存使用信息
            usageInfoService.insertUsageInfo(usage);
            
            log.info("Completion data saved synchronously for chatCompletionId: {}", chatCompletionId);
        } catch (Exception e) {
            log.error("Failed to save completion data synchronously: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save completion data", e);
        }
    }
}