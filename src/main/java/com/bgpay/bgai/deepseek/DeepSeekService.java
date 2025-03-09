package com.bgpay.bgai.deepseek;

import com.bgpay.bgai.datasource.DS;
import com.bgpay.bgai.service.ChatCompletionsService;
import com.bgpay.bgai.service.UsageInfoService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
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
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.reflections.Reflections.log;

@Component
public class DeepSeekService {
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

    @Value("${connect.timeout:20000}")
    private int connectTimeout;

    @Value("${socket.timeout:120000}")
    private int socketTimeout;

    @Autowired
    private ChatCompletionsService chatCompletionsService;

    @Autowired
    private UsageInfoService usageInfoService;

    @Autowired
    @Qualifier("asyncTaskExecutor")
    private Executor asyncRequestExecutor;


    // 2. 使用连接池管理的HTTP客户端（提升10倍吞吐量）
    private final CloseableHttpClient httpClient;

    // 3. 优化线程池配置（根据CPU核心数动态调整）
    private static final int CPU_CORES = Runtime.getRuntime().availableProcessors();
    private final ScheduledExecutorService retryExecutor = new ScheduledThreadPoolExecutor(
            CPU_CORES * 2,
            new CustomThreadFactory("Retry-")
    );

    // 4. 异步处理结果Future缓存（避免内存泄漏）
    private final ConcurrentMap<String, CompletableFuture<String>> pendingRequests =
            new ConcurrentHashMap<>(1024);

    public DeepSeekService(
            @Value("${http.max.conn:500}") int maxConn,
            @Value("${http.max.conn.per.route:50}") int maxPerRoute
    ) {
        // 5. 优化HTTP连接池配置
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
        connManager.setMaxTotal(maxConn);
        connManager.setDefaultMaxPerRoute(maxPerRoute);

        this.httpClient = HttpClients.custom()
                .setConnectionManager(connManager)
                .setKeepAliveStrategy((response, context) -> 60_000)
                .evictExpiredConnections()
                .build();
    }

    @DS("master")
    public String processRequest(String content, String apiUrl, String apiKey, String modelName) {
        try {
            String processed = sanitizeContent(content);
            String requestBody = buildRequest(processed, modelName);
            CompletableFuture<String> future = executeWithRetry(apiUrl, apiKey, requestBody);
            String response = future.get(); // 等待 CompletableFuture 完成并获取结果

            // 异步保存（非阻塞）
//            saveCompletionDataAsync(response);

            return response;
        } catch (Exception e) {
            return buildErrorResponse(500, "处理失败: " + e.getMessage());
        }
    }

    private String sanitizeContent(String content) {
        return truncateUtf8(content
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n"), maxRequestLength);
    }


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

    // 在类中添加


    private CompletableFuture<String> executeWithRetry(String apiUrl, String apiKey, String requestBody) {
        CompletableFuture<String> future = new CompletableFuture<>();
        AtomicInteger retries = new AtomicInteger(0);

        Runnable task = new Runnable() {
            @Override
            public void run() {
                if (retries.get() >= maxRetries) {
                    future.complete(buildErrorResponse(503, "服务暂时不可用"));
                    return;
                }

                try {
                    String result = sendRequest(apiUrl, apiKey, requestBody);
                    future.complete(result);
                } catch (Exception e) {
                    if (retries.incrementAndGet() < maxRetries) {
                        long delay = (long) (initialDelay * Math.pow(backoffFactor, retries.get()));
                        retryExecutor.schedule(this, delay, TimeUnit.MILLISECONDS);
                    } else {
                        future.completeExceptionally(e);
                    }
                }
            }
        };

        asyncRequestExecutor.execute(task);
        return future;
    }

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

    private String parseResponse(HttpResponse response) throws IOException {
        int statusCode = response.getStatusLine().getStatusCode();
        String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

        if (statusCode == HttpStatus.SC_OK) {
            validateJson(body);
            return body;
        }
        return buildErrorResponse(statusCode, "API错误: " + body);
    }

    private void validateJson(String json) {
        try {
            mapper.readTree(json);
        } catch (IOException e) {
            throw new RuntimeException("无效的JSON响应");
        }
    }

    private void sleepWithBackoff(int retryCount) {
        try {
            long delay = (long) (initialDelay * Math.pow(backoffFactor, retryCount));
            TimeUnit.MILLISECONDS.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String truncateUtf8(String input, int maxBytes) {
        if (input == null || maxBytes <= 0) return "";

        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= maxBytes) return input;

        // 逆向查找有效字符边界
        int truncLength = maxBytes;
        while (truncLength > 0 && (bytes[truncLength] & 0xC0) == 0x80) {
            truncLength--;
        }
        return new String(bytes, 0, truncLength, StandardCharsets.UTF_8) + "[TRUNCATED]";
    }


    private String buildErrorResponse(int code, String message) {
        try {
            ObjectNode errorNode = mapper.createObjectNode();
            errorNode.putObject("error")
                    .put("code", code)
                    .put("message", message);
            return mapper.writeValueAsString(errorNode);
        } catch (JsonProcessingException e) {
            return "{\"error\":{\"code\":500,\"message\":\"生成错误信息失败\"}}";
        }
    }

    // 异步保存方法
    @Transactional
    @Async
    public void saveCompletionDataAsync(String responseJson) {
        try {
            JsonNode root = mapper.readTree(responseJson);

            // 同步保存关键数据
            ChatCompletions completion = parseChatCompletion(root);
            chatCompletionsService.insertChatCompletions(completion);

            // 异步保存其他数据
            CompletableFuture.runAsync(() -> {
                try {
                    UsageInfo usage = parseUsageInfo(root);
                    usageInfoService.insertUsageInfo(usage);
                } catch (Exception e) {
                    // 记录异常日志
                    log.error("保存UsageInfo时出现异常", e);
                    // 这里可以考虑抛出异常，让事务管理器处理
                    throw new RuntimeException("保存UsageInfo时出现异常", e);
                }
            }).exceptionally(ex -> {
                // 处理异步任务中的异常
                log.error("异步保存UsageInfo时出现异常", ex);
                throw new RuntimeException("异步保存UsageInfo时出现异常", ex);
            });
        } catch (Exception e) {
            // 记录异常日志
            log.error("保存CompletionData时出现异常", e);
            // 抛出异常，让事务管理器处理
            throw new RuntimeException("保存CompletionData时出现异常", e);
        }
    }


    private ChatCompletions parseChatCompletion(JsonNode root) {
        ChatCompletions chatCompletions = new ChatCompletions();
        chatCompletions.setObject(root.path("object").asText());
        chatCompletions.setCreated(root.path("created").asLong());
        chatCompletions.setModel(root.path("model").asText());
        chatCompletions.setSystemFingerprint(root.path("system_fingerprint").asText());
        return chatCompletions;
    }

    private UsageInfo parseUsageInfo(JsonNode root) {
        JsonNode usageNode = root.path("usage");
        JsonNode promptDetails = usageNode.path("prompt_tokens_details");
        JsonNode completionDetails = usageNode.path("completion_tokens_details");
        UsageInfo usageInfo = new UsageInfo();
        usageInfo.setChatCompletionId(root.path("id").asText());
        usageInfo.setPromptTokens(usageNode.path("prompt_tokens").asInt());
        usageInfo.setTotalTokens(usageNode.path("total_tokens").asInt());
        usageInfo.setPromptTokensCached(promptDetails.path("cached_tokens").asInt());
        usageInfo.setCompletionReasoningTokens(completionDetails.path("reasoning_tokens").asInt());
        usageInfo.setPromptCacheHitTokens(usageNode.path("prompt_cache_miss_tokens").asInt());
        usageInfo.setPromptCacheMissTokens(root.path("prompt_cache_miss_tokens").asInt());
        return usageInfo;
    }

    private void handleRetry(int retryCount, Exception e) {
        System.err.printf("[Retry %d/%d] 请求失败: %s%n",
                retryCount + 1, maxRetries, e.getMessage());

        if (retryCount < maxRetries - 1) {
            try {
                long delay = (long) (initialDelay * Math.pow(backoffFactor, retryCount));
                TimeUnit.MILLISECONDS.sleep(delay);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private String processContent(String content) {
        String sanitized = content.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
        return truncateUtf8(sanitized, maxRequestLength);
    }
    static class CustomThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(0);
        private final String namePrefix;

        CustomThreadFactory(String prefix) {
            this.namePrefix = prefix;
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + counter.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    }
}