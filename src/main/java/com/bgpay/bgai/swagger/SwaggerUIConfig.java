package com.bgpay.bgai.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;

/**
 * Swagger UI 特定配置
 * 用于确保Swagger UI资源可以被正确访问
 */
@Configuration
public class SwaggerUIConfig implements WebFluxConfigurer {
    
    @Value("${springdoc.swagger-ui.path:/swagger-ui.html}")
    private String swaggerPath;
    
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
            boolean isSwaggerUIPath = path.equals(swaggerPath) || 
                                      path.startsWith("/swagger-ui/") ||
                                      path.startsWith("/v3/api-docs") || 
                                      path.equals("/v3/api-docs.yaml") ||
                                      path.startsWith("/swagger-resources") ||
                                      path.startsWith("/webjars/");
            
            // 为了调试，记录是否是Swagger路径
            if (isSwaggerUIPath) {
                exchange.getAttributes().put("isSwaggerUIRequest", true);
                System.out.println("SwaggerUIAccessFilter: Allowing access to " + path);
                
                // 设置响应头，确保不缓存Swagger UI资源
                exchange.getResponse().getHeaders().add(HttpHeaders.CACHE_CONTROL, "no-cache");
                exchange.getResponse().getHeaders().add(HttpHeaders.PRAGMA, "no-cache");
                exchange.getResponse().getHeaders().add(HttpHeaders.EXPIRES, "0");
                
                // 如果是API文档请求，确保返回正确的Content-Type
                if (path.startsWith("/v3/api-docs")) {
                    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                }
            }
            
            // 继续过滤器链
            return chain.filter(exchange);
        };
    }
    
    /**
     * 配置CORS，允许Swagger UI跨域访问API
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(Collections.singletonList("*"));
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        corsConfig.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-API-Key"));
        corsConfig.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        
        return new CorsWebFilter(source);
    }
    
    /**
     * 配置API分组 - 认证授权
     */
    @Bean
    public GroupedOpenApi authApiGroup() {
        return GroupedOpenApi.builder()
                .group("认证授权")
                .pathsToMatch("/api/auth/**", "/api/session/**")
                .build();
    }
    
    /**
     * 配置API分组 - 聊天服务
     */
    @Bean
    public GroupedOpenApi chatApiGroup() {
        return GroupedOpenApi.builder()
                .group("聊天服务")
                .pathsToMatch("/api/chat/**", "/api/chatGatWay-internal")
                .build();
    }
    
    /**
     * 配置API分组 - 文件处理
     */
    @Bean
    public GroupedOpenApi fileApiGroup() {
        return GroupedOpenApi.builder()
                .group("文件处理")
                .pathsToMatch("/api/files/**")
                .build();
    }
    
    /**
     * 配置API分组 - API密钥
     */
    @Bean
    public GroupedOpenApi apiKeyApiGroup() {
        return GroupedOpenApi.builder()
                .group("API密钥")
                .pathsToMatch("/api/keys/**")
                .build();
    }
    
    /**
     * 配置API分组 - 用量统计
     */
    @Bean
    public GroupedOpenApi usageApiGroup() {
        return GroupedOpenApi.builder()
                .group("用量统计")
                .pathsToMatch("/api/usage-stats/**")
                .build();
    }
    
    /**
     * 配置API分组 - 系统管理
     */
    @Bean
    public GroupedOpenApi systemApiGroup() {
        return GroupedOpenApi.builder()
                .group("系统管理")
                .pathsToMatch("/api/system/**")
                .build();
    }
    
    /**
     * 添加资源处理器，确保Swagger UI静态资源可以被正确加载
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/4.15.5/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }
} 