package com.bgpay.bgai.filter;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 幂等性过滤器
 * 通过Request-ID识别重复请求，保证接口幂等性
 */
@Component
@Order(1)
@ConditionalOnProperty(name = "idempotence.enabled", havingValue = "true", matchIfMissing = false)
public class IdempotenceWebFilter implements WebFilter {
    private static final Logger log = LoggerFactory.getLogger(IdempotenceWebFilter.class);

    @Value("${idempotence.request-id-header:X-Request-ID}")
    private String requestIdHeader;
    
    @Value("${idempotence.expiration-time-seconds:300}")
    private long expirationTimeSeconds;
    
    @Value("${idempotence.paths:/api/chatGatWay-internal}")
    private String[] idempotencePaths;
    
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ReactiveValueOperations<String, String> reactiveValueOps;
    
    @Autowired
    public IdempotenceWebFilter(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.reactiveValueOps = redisTemplate.opsForValue();
    }
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        
        // 检查是否需要进行幂等性处理的路径
        if (!shouldApplyIdempotence(path)) {
            return chain.filter(exchange);
        }
        
        // 获取请求ID
        String requestId = exchange.getRequest().getHeaders().getFirst(requestIdHeader);
        if (!StringUtils.hasText(requestId)) {
            log.debug("No request ID found in header {}, proceeding without idempotence check", requestIdHeader);
            return chain.filter(exchange);
        }
        
        // 构建Redis缓存键
        String redisKey = "idempotence:" + path + ":" + requestId;
        
        // 检查是否存在缓存的响应
        return reactiveValueOps.get(redisKey)
            .flatMap(cachedResponse -> {
                log.info("Found cached response for request ID {}, returning without processing", requestId);
                ServerHttpResponse response = exchange.getResponse();
                byte[] responseBytes = cachedResponse.getBytes(StandardCharsets.UTF_8);
                DataBuffer buffer = response.bufferFactory().wrap(responseBytes);
                return response.writeWith(Mono.just(buffer));
            })
            .switchIfEmpty(Mono.defer(() -> {
                log.info("No cached response found for request ID {}, processing request", requestId);
                
                // 记录请求体以备后用
                ServerHttpRequest request = exchange.getRequest();
                AtomicReference<String> requestBodyRef = new AtomicReference<>("");
                
                ServerHttpRequestDecorator decoratedRequest = new ServerHttpRequestDecorator(request) {
                    @Override
                    public Flux<DataBuffer> getBody() {
                        return super.getBody().map(buffer -> {
                            byte[] bytes = new byte[buffer.readableByteCount()];
                            buffer.read(bytes);
                            DataBufferUtils.release(buffer);
                            
                            // 追加到请求体引用
                            String chunk = new String(bytes, StandardCharsets.UTF_8);
                            requestBodyRef.accumulateAndGet(chunk, (current, newValue) -> current + newValue);
                            
                            // 返回一个新的缓冲区
                            return exchange.getResponse().bufferFactory().wrap(bytes);
                        });
                    }
                };
                
                // 创建装饰响应以捕获响应体
                AtomicReference<String> responseBodyRef = new AtomicReference<>("");
                ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(exchange.getResponse()) {
                    @Override
                    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                        Flux<DataBuffer> fluxBody = Flux.from(body);
                        
                        return super.writeWith(fluxBody.map(buffer -> {
                            byte[] bytes = new byte[buffer.readableByteCount()];
                            buffer.read(bytes);
                            DataBufferUtils.release(buffer);
                            
                            // 追加到响应体引用
                            String chunk = new String(bytes, StandardCharsets.UTF_8);
                            responseBodyRef.accumulateAndGet(chunk, (current, newValue) -> current + newValue);
                            
                            // 缓存响应
                            return exchange.getResponse().bufferFactory().wrap(bytes);
                        }).doOnComplete(() -> {
                            // 完成后将响应缓存到Redis
                            String responseBody = responseBodyRef.get();
                            reactiveValueOps.set(redisKey, responseBody, Duration.ofSeconds(expirationTimeSeconds))
                                .subscribe(
                                    result -> log.debug("Cached response for request ID {} with result {}", requestId, result),
                                    error -> log.error("Failed to cache response for request ID {}", requestId, error)
                                );
                        }));
                    }
                };
                
                ServerWebExchange decoratedExchange = exchange.mutate()
                    .request(decoratedRequest)
                    .response(decoratedResponse)
                    .build();
                
                return chain.filter(decoratedExchange);
            }));
    }
    
    private boolean shouldApplyIdempotence(String path) {
        return Arrays.stream(idempotencePaths)
                .anyMatch(path::equals);
    }
} 