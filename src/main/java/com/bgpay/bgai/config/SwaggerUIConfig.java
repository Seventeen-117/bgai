package com.bgpay.bgai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Swagger UI 特定配置
 * 用于确保Swagger UI资源可以被正确访问
 */
@Configuration
public class SwaggerUIConfig {
    
    /**
     * 创建一个高优先级的WebFilter，
     * 专门用于处理Swagger UI相关请求的权限，
     * 确保这些请求能够绕过AuthenticationFilter的验证
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public WebFilter swaggerUIAccessFilter() {
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            String path = exchange.getRequest().getPath().value();
            
            // 定义Swagger UI相关的路径
            boolean isSwaggerUIPath = path.equals("/swagger-ui.html") || 
                                      path.startsWith("/swagger-ui/") ||
                                      path.startsWith("/v3/api-docs") || 
                                      path.startsWith("/swagger-resources") ||
                                      path.startsWith("/webjars/");
            
            // 为了调试，记录是否是Swagger路径
            if (isSwaggerUIPath) {
                exchange.getAttributes().put("isSwaggerUIRequest", true);
                System.out.println("SwaggerUIAccessFilter: Allowing access to " + path);
            }
            
            // 继续过滤器链
            return chain.filter(exchange);
        };
    }
} 