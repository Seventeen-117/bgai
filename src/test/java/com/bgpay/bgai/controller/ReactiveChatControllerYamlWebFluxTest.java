package com.bgpay.bgai.controller;

import com.bgpay.bgai.response.ChatResponse;
import com.bgpay.bgai.service.ApiConfigService;
import com.bgpay.bgai.service.UserService;
import com.bgpay.bgai.service.deepseek.DeepSeekService;
import com.bgpay.bgai.service.deepseek.ReactiveFileProcessor;
import com.bgpay.bgai.service.impl.FallbackService;
import com.bgpay.bgai.transaction.TransactionCoordinator;
import com.bgpay.bgai.web.RequestAttributesProvider;
import com.bgpay.bgai.service.ApiKeyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MultiValueMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.http.client.MultipartBodyBuilder;
import org.yaml.snakeyaml.Yaml;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@WebFluxTest(
    controllers = ReactiveChatController.class,
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {com.bgpay.bgai.filter.ApiKeyWebFilter.class}),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {com.bgpay.bgai.service.deepseek.DeepSeekServiceImp.class}),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {com.bgpay.bgai.service.deepseek.FileProcessor.class}),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {com.bgpay.bgai.service.deepseek.ConversationHistoryService.class}),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {com.bgpay.bgai.service.deepseek.FileTypeService.class}),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {com.bgpay.bgai.service.deepseek.FileWriterService.class})
    }
)
public class ReactiveChatControllerYamlWebFluxTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ReactiveFileProcessor fileProcessor;
    @MockBean
    private ApiConfigService apiConfigService;
    @MockBean
    private DeepSeekService deepSeekService;
    @MockBean
    private ReactiveCircuitBreakerFactory circuitBreakerFactory;
    @MockBean
    private FallbackService fallbackService;
    @MockBean
    private TransactionCoordinator transactionCoordinator;
    @MockBean
    private RequestAttributesProvider attributesProvider;
    @MockBean
    private UserService userService;
    @MockBean
    private ApiKeyService apiKeyService;

    @BeforeEach
    void setupMocks() {
        // mock API Key Service
        when(apiKeyService.validateApiKeyStatus(any())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            if ("valid-key".equals(key)) {
                return new ApiKeyService.ApiKeyValidationResult(
                        ApiKeyService.ApiKeyStatus.VALID, null, null, "default-client");
            } else {
                // 用 DISABLED 代表无效
                return new ApiKeyService.ApiKeyValidationResult(
                        ApiKeyService.ApiKeyStatus.DISABLED, null, "Invalid API Key", null);
            }
        });
        when(apiKeyService.getApiKeyInfo(any())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            com.bgpay.bgai.entity.ApiKey k = new com.bgpay.bgai.entity.ApiKey();
            if ("valid-key".equals(key)) {
                k.setActive(1);
            } else {
                k.setActive(0);
            }
            return k;
        });
        // mock DeepSeekService 两个重载
        when(deepSeekService.processRequestReactive(anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean()))
                .thenReturn(Mono.just(mockChatResponse()));
        when(deepSeekService.processRequestReactive(anyMap(), anyString(), anyString(), anyString(), anyString(), anyBoolean()))
                .thenReturn(Mono.just(mockChatResponse()));
    }

    private ChatResponse mockChatResponse() {
        ChatResponse resp = new ChatResponse();
        resp.setContent("{\"id\":\"mock-id\",\"choices\":[{\"content\":\"mocked response\"}],\"usage\":{}}");
        return resp;
    }

    static Stream<Map<String, Object>> yamlCases() {
        Yaml yaml = new Yaml();
        InputStream in = ReactiveChatControllerYamlWebFluxTest.class
                .getResourceAsStream("/test-data/api/chatGatWay-internal.yml");
        Map<String, Object> root = yaml.load(in);
        List<Map<String, Object>> cases = (List<Map<String, Object>>) root.get("testCases");
        return cases.stream();
    }

    @ParameterizedTest(name = "{index} - {0}[{1}]")
    @MethodSource("yamlCases")
    void testChatGatWayInternal(Map<String, Object> testCase) {
        String endpoint = (String) testCase.get("endpoint");
        Map<String, Object> request = (Map<String, Object>) testCase.get("request");
        Map<String, Object> expected = (Map<String, Object>) testCase.get("expectedResponse");

        String method = request.getOrDefault("method", "POST").toString();
        Map<String, String> headers = (Map<String, String>) request.get("headers");
        Map<String, Object> body = (Map<String, Object>) request.get("body");
        Map<String, Object> multipart = (Map<String, Object>) request.get("multipart");

        WebTestClient.RequestHeadersSpec<?> reqSpec;

        if (multipart != null) {
            // 处理 multipart/form-data
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            if (multipart.containsKey("file")) {
                String fileName = (String) multipart.get("file");
                String fileContent = (String) multipart.getOrDefault("fileContent", "test file content");
                String contentType = (String) multipart.getOrDefault("contentType", "text/plain");
                builder.part("file", new ByteArrayResource(fileContent.getBytes(StandardCharsets.UTF_8)) {
                    @Override
                    public String getFilename() { return fileName; }
                }).header("Content-Type", contentType);
            }
            multipart.forEach((k, v) -> {
                if (!"file".equals(k) && !"fileContent".equals(k) && !"contentType".equals(k)) {
                    builder.part(k, v);
                }
            });
            reqSpec = webTestClient.method(org.springframework.http.HttpMethod.valueOf(method))
                    .uri("/api" + endpoint)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()));
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    reqSpec = reqSpec.header(entry.getKey(), entry.getValue());
                }
            }
        } else if (body != null && !body.isEmpty()) {
            reqSpec = webTestClient.method(org.springframework.http.HttpMethod.valueOf(method))
                    .uri("/api" + endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body);
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    reqSpec = reqSpec.header(entry.getKey(), entry.getValue());
                }
            }
        } else {
            reqSpec = webTestClient.method(org.springframework.http.HttpMethod.valueOf(method))
                    .uri("/api" + endpoint);
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    reqSpec = reqSpec.header(entry.getKey(), entry.getValue());
                }
            }
        }

        WebTestClient.ResponseSpec resp = reqSpec.exchange();
        resp.expectStatus().isEqualTo((int) expected.get("statusCode"));
        List<String> bodyContains = (List<String>) expected.get("bodyContains");
        if (bodyContains != null && !bodyContains.isEmpty()) {
            resp.expectBody(String.class).value(s -> {
                for (String expect : bodyContains) {
                    assert s.contains(expect) : "响应体未包含: " + expect + "\n实际: " + s;
                }
            });
        }
    }

    /**
     * 通用 YAML 数据驱动测试方法模板，自动遍历 chatGatWay-internal.yml 的所有 testCases 并执行
     */
    @ParameterizedTest(name = "{index} - {0}[{1}]")
    @MethodSource("yamlCases")
    @DisplayName("YAML驱动的ReactiveChatController接口测试")
    void testReactiveChatApiYamlCases(Map<String, Object> testCase) {
        // 复用已有 testChatGatWayInternal 逻辑
        testChatGatWayInternal(testCase);
    }
} 