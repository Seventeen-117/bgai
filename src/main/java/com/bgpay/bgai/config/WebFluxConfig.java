package com.bgpay.bgai.config;

import com.bgpay.bgai.context.ReactiveRequestContextHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * WebFlux配置，添加请求上下文过滤器
 */
@Configuration
public class WebFluxConfig {
    
    /**
     * 存储ServerWebExchange到ReactiveRequestContextHolder的过滤器
     */
    @Bean
    public WebFilter reactiveRequestContextFilter() {
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            ReactiveRequestContextHolder.setExchange(exchange);
            return chain.filter(exchange)
                    .doFinally(signal -> ReactiveRequestContextHolder.clearExchange());
        };
    }
} 