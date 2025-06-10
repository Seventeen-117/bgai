package com.bgpay.bgai.filter;

import com.bgpay.bgai.model.es.ChatRecord;
import com.bgpay.bgai.repository.es.ChatRecordRepository;
import com.bgpay.bgai.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
public class ChatRecordWebFilter implements WebFilter {

    private final ChatRecordRepository chatRecordRepository;
    private final DataBufferFactory bufferFactory = new DefaultDataBufferFactory();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!exchange.getRequest().getURI().getPath().equals("/api/chatGatWay-internal")) {
            return chain.filter(exchange);
        }

        final long startTime = System.currentTimeMillis();
        ServerHttpRequest request = exchange.getRequest();

        // Store request body content
        AtomicReference<String> requestBodyRef = new AtomicReference<>("");

        ServerHttpRequestDecorator decoratedRequest = new ServerHttpRequestDecorator(request) {
            @Override
            public Flux<DataBuffer> getBody() {
                return super.getBody().map(buffer -> {
                    byte[] bytes = new byte[buffer.readableByteCount()];
                    buffer.read(bytes);
                    DataBufferUtils.release(buffer);

                    // Append to request body reference
                    String chunk = new String(bytes, StandardCharsets.UTF_8);
                    requestBodyRef.accumulateAndGet(chunk, (current, newValue) -> current + newValue);

                    // Return a new buffer with the same content
                    return bufferFactory.wrap(bytes);
                });
            }
        };

        ServerWebExchange decoratedExchange = exchange.mutate()
                .request(decoratedRequest)
                .build();

        ChatRecord record = new ChatRecord();
        record.setId(UUID.randomUUID().toString());
        record.setTimestamp(LocalDateTime.now());
        record.setTraceId(LogUtils.getTraceId());
        record.setUserId(LogUtils.getUserId());
        record.setProcessingTime(System.currentTimeMillis() - startTime);

        return chain.filter(decoratedExchange)
                .doOnSuccess(v -> {
                    record.setRequestBody(requestBodyRef.get());
                    record.setStatusCode(exchange.getResponse().getStatusCode().value());
                    chatRecordRepository.save(record);
                })
                .doOnError(error -> {
                    record.setRequestBody(requestBodyRef.get());
                    record.setStatusCode(500);
                    record.setErrorMessage(error.getMessage());
                    chatRecordRepository.save(record);
                });
    }
}