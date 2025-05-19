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
    
    /**
     * 存储请求上下文并返回Mono<Void>，适用于响应式链中
     * 
     * @param exchange 当前请求上下文
     * @return 完成后的Mono
     */
    public static Mono<Void> storeExchange(ServerWebExchange exchange) {
        return Mono.fromRunnable(() -> EXCHANGE_THREAD_LOCAL.set(exchange));
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
    
    /**
     * 在Reactor上下文中获取ServerWebExchange
     * 
     * @return 包含当前上下文的Mono
     */
    public static Mono<ServerWebExchange> getExchangeMono() {
        return Mono.deferContextual(ctx -> {
            if (ctx.hasKey(EXCHANGE_CONTEXT_KEY)) {
                return Mono.just(ctx.get(EXCHANGE_CONTEXT_KEY));
            }
            ServerWebExchange exchange = EXCHANGE_THREAD_LOCAL.get();
            return exchange != null ? Mono.just(exchange) : Mono.empty();
        });
    }
} 