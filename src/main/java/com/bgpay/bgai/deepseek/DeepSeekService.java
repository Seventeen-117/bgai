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
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class DeepSeekService {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final int MAX_RETRIES;
    private static final long INITIAL_DELAY;
    private static final double BACKOFF_FACTOR;
    private static final int MAX_LENGTH;
    private static final RequestConfig REQUEST_CONFIG;
    private static final CloseableHttpClient CLIENT;
    private static final String MODEL_NAME;
    private static final String API_URL;
    private static final String API_KEY;

    static {
        MAX_RETRIES = Integer.parseInt(ConfigLoader.getProperty("retry.count", "3"));
        INITIAL_DELAY = Long.parseLong(ConfigLoader.getProperty("retry.delay.ms", "1000"));
        BACKOFF_FACTOR = Double.parseDouble(ConfigLoader.getProperty("retry.backoff.factor", "2.0"));
        MAX_LENGTH = Integer.parseInt(ConfigLoader.getProperty("max.request.length", "4096"));
        MODEL_NAME = ConfigLoader.getProperty("model.name");
        API_URL = ConfigLoader.getProperty("api.url");
        API_KEY = ConfigLoader.getProperty("api.key");

        REQUEST_CONFIG = RequestConfig.custom()
                .setConnectTimeout(Integer.parseInt(ConfigLoader.getProperty("connect.timeout", "15000")))
                .setSocketTimeout(Integer.parseInt(ConfigLoader.getProperty("socket.timeout", "60000")))
                .build();

        CLIENT = HttpClients.custom()
                .setDefaultRequestConfig(REQUEST_CONFIG)
                .setRedirectStrategy(new LaxRedirectStrategy())
                .setMaxConnPerRoute(20)
                .setMaxConnTotal(100)
                .build();
    }

    public static String processRequest(String content) {
        try {
            String processedContent = sanitizeContent(content);
            String requestBody = buildRequest(processedContent);
            return executeWithRetry(requestBody);
        } catch (Exception e) {
            return buildErrorResponse(500, "处理请求失败: " + e.getMessage());
        }
    }

    private static String sanitizeContent(String content) {
        StringBuilder sb = new StringBuilder();
        for (char c : content.toCharArray()) {
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                default -> sb.append(c);
            }
        }
        return truncateUtf8(sb.toString(), MAX_LENGTH);
    }

    private static String buildRequest(String message) throws JsonProcessingException {
        ObjectNode requestNode = mapper.createObjectNode();
        requestNode.put("model", MODEL_NAME);
        requestNode.put("stream", false);

        ObjectNode messageNode = mapper.createObjectNode();
        messageNode.put("role", "user");
        messageNode.put("content", message);

        requestNode.putArray("messages").add(messageNode);
        return mapper.writeValueAsString(requestNode);
    }

    private static String executeWithRetry(String requestBody) {
        AtomicInteger retries = new AtomicInteger(0);
        while (retries.get() < MAX_RETRIES) {
            try {
                return sendRequest(requestBody);
            } catch (Exception e) {
                if (retries.incrementAndGet() == MAX_RETRIES) break;
                sleepWithBackoff(retries.get());
            }
        }
        return buildErrorResponse(503, "服务暂时不可用");
    }

    private static String sendRequest(String requestBody) throws IOException {
        HttpPost post = new HttpPost(API_URL);
        post.setHeader("Content-Type", "application/json; charset=UTF-8");
        post.setHeader("Authorization", "Bearer " + API_KEY);
        post.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));

        try (CloseableHttpClient client = CLIENT) {
            HttpResponse response = client.execute(post);
            return parseResponse(response);
        }
    }

    private static String parseResponse(HttpResponse response) throws IOException {
        int statusCode = response.getStatusLine().getStatusCode();
        String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

        if (statusCode == HttpStatus.SC_OK) {
            validateJson(body);
            return body;
        }
        return buildErrorResponse(statusCode, "API错误: " + body);
    }

    private static void validateJson(String json) {
        try {
            mapper.readTree(json);
        } catch (IOException e) {
            throw new RuntimeException("无效的JSON响应");
        }
    }

    private static void sleepWithBackoff(int retryCount) {
        try {
            long delay = (long) (INITIAL_DELAY * Math.pow(BACKOFF_FACTOR, retryCount));
            TimeUnit.MILLISECONDS.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String truncateUtf8(String input, int maxBytes) {
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

    private static String buildErrorResponse(int code, String message) {
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

    private static void handleRetry(int retryCount, Exception e) {
        System.err.printf("[Retry %d/%d] 请求失败: %s%n",
                retryCount + 1, MAX_RETRIES, e.getMessage());

        if (retryCount < MAX_RETRIES - 1) {
            try {
                long delay = (long) (INITIAL_DELAY * Math.pow(BACKOFF_FACTOR, retryCount));
                TimeUnit.MILLISECONDS.sleep(delay);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static String processContent(String content) {
        String sanitized = content.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
        return truncateUtf8(sanitized, MAX_LENGTH);
    }
}