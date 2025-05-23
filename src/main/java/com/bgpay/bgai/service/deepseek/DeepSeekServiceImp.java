package com.bgpay.bgai.service.deepseek;

import com.bgpay.bgai.datasource.DS;
import com.bgpay.bgai.entity.UsageCalculationDTO;
import com.bgpay.bgai.response.ChatResponse;
import com.bgpay.bgai.service.ChatCompletionsService;
import com.bgpay.bgai.service.UsageInfoService;
import com.bgpay.bgai.service.mq.MQCallback;
import com.bgpay.bgai.service.mq.RocketMQProducerService;
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
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import com.bgpay.bgai.entity.ChatCompletions;
import com.bgpay.bgai.entity.UsageInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.reflections.Reflections.log;

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


    private static final int CPU_CORES = Runtime.getRuntime().availableProcessors();

    private final ScheduledExecutorService retryExecutor = new ScheduledThreadPoolExecutor(
            CPU_CORES * 2,
            new CustomThreadFactory("Retry-")
    );


    public DeepSeekServiceImp(
            @Value("${http.max.conn:500}") int maxConn,
            @Value("${http.max.conn.per.route:50}") int maxPerRoute, MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;


        // 配置连接存活性检查
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(
                30, TimeUnit.SECONDS // 存活时间
        );
        connManager.setMaxTotal(maxConn);
        connManager.setDefaultMaxPerRoute(maxPerRoute);
        connManager.setValidateAfterInactivity(30_000); // 30秒空闲检查

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
                .setRetryHandler(new DefaultHttpRequestRetryHandler(3, true)) // 请求重试
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
    @DS("master")
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
                UsageCalculationDTO calculationDTO = new UsageCalculationDTO();
                calculationDTO.setChatCompletionId(usage.getChatCompletionId());
                calculationDTO.setModelType(usage.getModelType());
                calculationDTO.setPromptCacheHitTokens(usage.getPromptCacheHitTokens());
                calculationDTO.setPromptCacheMissTokens(usage.getPromptCacheMissTokens());
                calculationDTO.setCompletionTokens(usage.getCompletionTokens());
                calculationDTO.setCreatedAt(LocalDateTime.now());
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

    @Override
    public Mono<ChatResponse> processRequestReactive(String content,
                                                     String apiUrl,
                                                     String apiKey,
                                                     String modelName,
                                                     String userId,
                                                     boolean multiTurn) {
        log.info("Calling DeepSeek API - URL: {}, Model: {}", apiUrl, modelName);
        return WebClient.create(apiUrl)
                .post()
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(buildRequestJson(content, modelName, multiTurn,userId))
                .retrieve()
                .onStatus(
                        status -> !status.is2xxSuccessful(),
                        response -> {
                            log.error("API returned error status: {}", response.statusCode());
                            return response.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .flatMap(body -> Mono.error(new RuntimeException("API Error: " + body)));
                        }
                )
                .bodyToMono(ChatResponse.class)
                .timeout(Duration.ofSeconds(30))
                .doOnNext(resp -> log.debug("API Response: {}", resp))
                .onErrorResume(e -> {
                    log.error("API call failed", e);
                    return Mono.error(new RuntimeException("API调用失败: " + e.getMessage()));
                });
    }
    private Map<String, Object> buildRequestJson(String content, String model, boolean multiTurn, String userId) {
        List<Map<String, Object>> messages = new ArrayList<>();

        if (multiTurn) {
            // 添加历史消息
            List<Map<String, Object>> history = historyService.getValidHistory(userId);
            messages.addAll(history);
        }

        // 添加当前消息
        messages.add(Map.of(
                "role", "user",
                "content", content
        ));

        return Map.of(
                "model", model,
                "messages", messages,
                "stream", false,
                "temperature", 0.7,
                "max_tokens", 2000
        );
    }

    private Map<String, Object> createMessage(String role, String content) {
        return Map.of(
                "role", role,
                "content", sanitizeContent(content),
                "timestamp", System.currentTimeMillis()
        );
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

        // Execute the task asynchronously
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

            // Synchronously save the chat completions data
            ChatCompletions completion = parseChatCompletion(root);
            chatCompletionsService.insertChatCompletions(completion);

            // Asynchronously save the usage information data
            CompletableFuture.runAsync(() -> {
                try {
                    UsageInfo usage = parseUsageInfo(root);
                    usageInfoService.insertUsageInfo(usage);
                } catch (Exception e) {
                    // Log the exception and throw a runtime exception
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
        // Convert UUID to a 30-bit integer
        UUID uuid = UUID.randomUUID();
        long mostSignificantBits = uuid.getMostSignificantBits();
        long leastSignificantBits = uuid.getLeastSignificantBits();
        long combined = (mostSignificantBits << 32) | (leastSignificantBits & 0xFFFFFFFFL);
        int thirtyBitInt = (int) (combined & 0x3FFFFFFFL); // Take the lower 30 bits
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
}