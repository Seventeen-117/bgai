package com.bgpay.bgai.web;

import com.bgpay.bgai.context.ReactiveRequestContextHolder;
import com.bgpay.bgai.filter.LogTraceWebFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

/**
 * WebFlux配置，添加请求上下文过滤器
 */
@Configuration
@ConditionalOnClass({WebFilter.class, ServerWebExchange.class})
public class WebFluxConfig implements WebFluxConfigurer {
    
    /**
     * 存储ServerWebExchange到ReactiveRequestContextHolder的过滤器
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 10) // 确保在LogTraceWebFilter之后执行
    public WebFilter reactiveRequestContextFilter() {
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            ReactiveRequestContextHolder.setExchange(exchange);
            return chain.filter(exchange)
                    .doFinally(signal -> ReactiveRequestContextHolder.clearExchange());
        };
    }
    
    /**
     * 确保LogTraceWebFilter被注册，并在最高优先级执行
     * 注意：实际的filter实现是通过@Component注解注册的
     */
    @Bean
    public LogTraceWebFilter logTraceWebFilter() {
        return new LogTraceWebFilter();
    }
    
    /**
     * 配置静态资源处理器
     * 特别是为了让Swagger UI能够正常工作
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Swagger UI资源
        registry.addResourceHandler("/swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/");
                
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }
} 