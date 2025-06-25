package com.bgpay.bgai.feign;

import feign.Logger;
import feign.Request;
import feign.RequestInterceptor;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * UserServiceClient的特定配置
 */
@Slf4j
@Configuration
public class UserServiceFeignConfig {

    @Value("${bgai.api-key.test-key:test-api-key-123}")
    private String testApiKey;

    /**
     * 配置日志级别
     */
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL; // 记录所有请求和响应的细节
    }

    /**
     * 请求超时配置
     */
    @Bean
    Request.Options requestOptions() {
        // 连接超时(ms)，读取超时(ms)
        return new Request.Options(5000, 10000);
    }

    /**
     * 请求拦截器，添加通用请求头
     */
    @Bean
    public RequestInterceptor userServiceRequestInterceptor() {
        return requestTemplate -> {
            log.debug("为UserService添加请求头");
            requestTemplate.header("X-Request-From", "bgtech-ai");
            requestTemplate.header("X-Service-Call", "bgai-user-service");
            requestTemplate.header("X-API-Key", testApiKey);
        };
    }

    /**
     * 自定义错误解码器
     */
    @Bean
    public ErrorDecoder userServiceErrorDecoder() {
        return (methodKey, response) -> {
            int status = response.status();
            String message = String.format("UserService调用失败: %s %d %s", 
                    methodKey, status, response.reason());
            log.error(message);
            
            // 根据错误状态码返回不同异常
            if (status >= 500) {
                return new RuntimeException("用户服务暂时不可用: " + status);
            } else if (status == 404) {
                return new RuntimeException("用户服务资源不存在");
            } else if (status == 401 || status == 403) {
                return new RuntimeException("用户服务认证失败");
            } else {
                return new RuntimeException("用户服务调用失败: " + response.reason());
            }
        };
    }
} 