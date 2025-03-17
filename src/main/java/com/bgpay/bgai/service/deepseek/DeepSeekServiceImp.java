package com.bgpay.bgai.service.deepseek;

import com.bgpay.bgai.datasource.DS;
import com.bgpay.bgai.entity.UsageCalculationDTO;
import com.bgpay.bgai.response.ChatResponse;
import com.bgpay.bgai.service.BillingService;
import com.bgpay.bgai.service.ChatCompletionsService;
import com.bgpay.bgai.service.PriceVersionService;
import com.bgpay.bgai.service.UsageInfoService;
import com.bgpay.bgai.service.impl.BillingMessageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
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

    private final BillingMessageService billingMessageService;

    @Autowired
    @Qualifier("asyncTaskExcutor")
    private Executor asyncRequestExecutor;

    @Autowired
    private BillingService billingService;

    private final CloseableHttpClient httpClient;

    private static final int CPU_CORES = Runtime.getRuntime().availableProcessors();

    private final ScheduledExecutorService retryExecutor = new ScheduledThreadPoolExecutor(
            CPU_CORES * 2,
            new CustomThreadFactory("Retry-")
    );

    private final ConcurrentMap<String, CompletableFuture<String>> pendingRequests =
            new ConcurrentHashMap<>(1024);

    public DeepSeekServiceImp(
            @Value("${http.max.conn:500}") int maxConn,
            @Value("${http.max.conn.per.route:50}") int maxPerRoute,
            MeterRegistry meterRegistry,
            BillingMessageService billingMessageService
    ) {
        this.meterRegistry = meterRegistry;
        this.billingMessageService = billingMessageService;
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
        connManager.setMaxTotal(maxConn);
        connManager.setDefaultMaxPerRoute(maxPerRoute);

        this.httpClient = HttpClients.custom()
                .setConnectionManager(connManager)
                .setKeepAliveStrategy((response, context) -> 60_000)
                .evictExpiredConnections()
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
    public ChatResponse processRequest(String content, String apiUrl, String apiKey, String modelName, String userId) {
        ChatResponse chatResponse = new ChatResponse();
        try {
            String processed = sanitizeContent(content);
            String requestBody = buildRequest(processed, modelName);
            CompletableFuture<String> future = executeWithRetry(apiUrl, apiKey, requestBody);
            String response = future.get();

            saveCompletionDataAsync(response);

            JsonNode root = mapper.readTree(response);

            JsonNode choices = root.path("choices");
            if (!choices.isEmpty()) {
                JsonNode message = choices.get(0).path("message");
                if (!message.isEmpty()) {
                    chatResponse.setContent(message.path("content").asText());
                }
            }

            JsonNode usageNode = root.path("usage");
            if (!usageNode.isEmpty()) {
                UsageInfo usage = extractUsageInfo(usageNode, root);
                chatResponse.setUsage(usage);

                // 请求成功后，异步发送消息
                sendBillingMessageAsync(usage, modelName, userId);
            }
        } catch (Exception e) {
            String errorMessage = "Processing failed: " + e.getMessage();
            chatResponse.setContent(buildErrorResponse(500, errorMessage));
            chatResponse.setUsage(new UsageInfo());
        }
        return chatResponse;
    }


    @Async("billingExecutor")
    protected void sendBillingMessageAsync(UsageInfo usage, String modelName, String userId) {
        UsageCalculationDTO calculationDTO = new UsageCalculationDTO();
        calculationDTO.setChatCompletionId(usage.getChatCompletionId());
        calculationDTO.setModelType(modelName);
        calculationDTO.setPromptCacheHitTokens(usage.getPromptCacheHitTokens());
        calculationDTO.setPromptCacheMissTokens(usage.getPromptCacheMissTokens());
        calculationDTO.setCompletionTokens(usage.getCompletionTokens());
        calculationDTO.setCreatedAt(LocalDateTime.now());
        try {
            billingMessageService.sendBillingMessage(calculationDTO, userId);
            meterRegistry.counter("billing.message.sent").increment();
        } catch (Exception e) {
            log.error("Billing message sending failed. completionId: {}",
                    calculationDTO.getChatCompletionId(), e);
            meterRegistry.counter("billing.message.failed").increment();
        }
        billingService.processSingleRecord(calculationDTO, userId);
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
     * @param message   The sanitized message content
     * @param modelName The name of the model
     * @return The JSON string of the request body
     * @throws JsonProcessingException if there is an error in JSON processing
     */
    private String buildRequest(String message, String modelName) throws JsonProcessingException {
        ObjectNode requestNode = mapper.createObjectNode();
        requestNode.put("model", modelName);
        requestNode.put("stream", this.stream);

        ObjectNode messageNode = mapper.createObjectNode();
        messageNode.put("role", "user");
        messageNode.put("content", message);

        requestNode.putArray("messages").add(messageNode);
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
        post.setHeader("Content-Type", "application/json; charset=UTF-8");
        post.setHeader("Authorization", "Bearer " + apiKey);
        post.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));

        try (CloseableHttpClient client = httpClient) {
            HttpResponse response = client.execute(post);
            return parseResponse(response);
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

        if (statusCode == HttpStatus.SC_OK) {
            // Validate the JSON response
            validateJson(body);
            return body;
        }
        // Build an error response if the status code is not OK
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