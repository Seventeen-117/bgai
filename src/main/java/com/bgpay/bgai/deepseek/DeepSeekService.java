package com.bgpay.bgai.deepseek;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class DeepSeekService {
    private final ObjectMapper mapper = new ObjectMapper();
    private final CloseableHttpClient httpClient;

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


    public DeepSeekService() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(this.connectTimeout)
                .setSocketTimeout(this.socketTimeout)
                .build();

        this.httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setRedirectStrategy(new LaxRedirectStrategy())
                .setMaxConnPerRoute(20)
                .setMaxConnTotal(100)
                .build();
    }

    public String processRequest(String content, String apiUrl, String apiKey, String modelName) {
        try {
            String processedContent = sanitizeContent(content);
            String requestBody = buildRequest(processedContent, modelName);
            return executeWithRetry(apiUrl, apiKey, requestBody);
        } catch (Exception e) {
            return buildErrorResponse(500, "处理请求失败: " + e.getMessage());
        }
    }

    private String sanitizeContent(String content) {
        StringBuilder sb = new StringBuilder();
        for (char c : content.toCharArray()) {
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                default -> sb.append(c);
            }
        }
        return truncateUtf8(sb.toString(), maxRequestLength);
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

    private String executeWithRetry(String apiUrl, String apiKey, String requestBody) {
        AtomicInteger retries = new AtomicInteger(0);
        while (retries.get() < maxRetries) {
            try {
                return sendRequest(apiUrl, apiKey, requestBody);
            } catch (Exception e) {
                if (retries.incrementAndGet() == maxRetries) break;
                sleepWithBackoff(retries.get());
            }
        }
        return buildErrorResponse(503, "服务暂时不可用");
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

    private  String parseResponse(HttpResponse response) throws IOException {
        int statusCode = response.getStatusLine().getStatusCode();
        String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

        if (statusCode == HttpStatus.SC_OK) {
            validateJson(body);
            return body;
        }
        return buildErrorResponse(statusCode, "API错误: " + body);
    }

    private  void validateJson(String json) {
        try {
            mapper.readTree(json);
        } catch (IOException e) {
            throw new RuntimeException("无效的JSON响应");
        }
    }

    private  void sleepWithBackoff(int retryCount) {
        try {
            long delay = (long) (initialDelay * Math.pow(backoffFactor, retryCount));
            TimeUnit.MILLISECONDS.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private  String truncateUtf8(String input, int maxBytes) {
        if (input == null || maxBytes <= 0) return "";

        int bytes = 0;
        int lastValidIndex = 0;
        for (int i = 0; i < input.length();) {
            int codePoint = input.codePointAt(i);
            int charLen = Character.charCount(codePoint);
            int byteLen = codePoint <= 0x7F ? 1 :
                    codePoint <= 0x7FF ? 2 :
                            codePoint <= 0xFFFF ? 3 : 4;

            if (bytes + byteLen > maxBytes) break;

            bytes += byteLen;
            lastValidIndex = i + charLen;
            i = lastValidIndex;
        }

        String result = input.substring(0, lastValidIndex);
        return lastValidIndex < input.length() ? result + "[TRUNCATED]" : result;
    }

    private  String buildErrorResponse(int code, String message) {
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

    private  void handleRetry(int retryCount, Exception e) {
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

    private  String processContent(String content) {
        String sanitized = content.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
        return truncateUtf8(sanitized, maxRequestLength);
    }
}