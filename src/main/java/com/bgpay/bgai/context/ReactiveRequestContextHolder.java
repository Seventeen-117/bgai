package com.bgpay.bgai.context;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 用于WebFlux环境下获取当前请求的上下文
 */
public class ReactiveRequestContextHolder {
    private static final Class<ServerWebExchange> EXCHANGE_CONTEXT_KEY = ServerWebExchange.class;
    
    private static final ThreadLocal<ServerWebExchange> EXCHANGE_THREAD_LOCAL = new ThreadLocal<>();

    public static void setExchange(ServerWebExchange exchange) {
        EXCHANGE_THREAD_LOCAL.set(exchange);
    }

    public static ServerWebExchange getExchange() {
        return EXCHANGE_THREAD_LOCAL.get();
    }

    public static void clearExchange() {
        EXCHANGE_THREAD_LOCAL.remove();
    }
    
    public static <T> Mono<T> withExchange(ServerWebExchange exchange, Mono<T> mono) {
        return mono.doOnSubscribe(s -> setExchange(exchange))
                  .doFinally(signalType -> clearExchange());
    }
} 