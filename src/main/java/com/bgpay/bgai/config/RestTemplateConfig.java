package com.bgpay.bgai.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.ClientHttpRequestExecution;

import lombok.extern.slf4j.Slf4j;
import java.net.URI;
import java.util.Collections;
import java.time.Duration;
import java.io.IOException;

/**
 * RestTemplate配置类，支持服务发现和动态路由
 */
@Slf4j
@Configuration
public class RestTemplateConfig {
    
    @Autowired(required = false)
    private LoadBalancerClient loadBalancerClient;
    
    /**
     * 创建RestTemplate Bean
     */
    @Bean
    @Primary
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10 seconds
        factory.setReadTimeout(30000);    // 30 seconds
        
        RestTemplate restTemplate = new RestTemplate(factory);
        
        // 添加日志拦截器
        restTemplate.setInterceptors(
            Collections.singletonList(loggingInterceptor())
        );
        
        log.info("Creating RestTemplate bean using SimpleClientHttpRequestFactory");
        return restTemplate;
    }
    
    /**
     * 支持服务发现和负载均衡的RestTemplate
     * 通过添加@LoadBalanced注解，Spring Cloud会自动增强RestTemplate以支持"服务名"为host的请求
     * 例如：http://service-name/api/resource
     */
    @Bean(name = "loadBalancedRestTemplate")
    @LoadBalanced
    public RestTemplate loadBalancedRestTemplate() {
        RestTemplate restTemplate = new RestTemplate(createRequestFactory());
        // 添加日志拦截器
        restTemplate.setInterceptors(
            Collections.singletonList(loggingInterceptor())
        );
        return restTemplate;
    }
    
    /**
     * 创建ClientHttpRequestFactory
     * 使用BufferingClientHttpRequestFactory包装，允许请求/响应体被多次读取(用于日志记录等)
     */
    private ClientHttpRequestFactory createRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(30000);
        factory.setBufferRequestBody(false);
        return new BufferingClientHttpRequestFactory(factory);
    }
    
    /**
     * 日志拦截器，用于记录请求和响应
     */
    private ClientHttpRequestInterceptor loggingInterceptor() {
        return new ClientHttpRequestInterceptor() {
            @Override
            public ClientHttpResponse intercept(HttpRequest request, byte[] body, 
                                               ClientHttpRequestExecution execution) throws IOException {
                // 请求前记录
                log.debug("发送请求: {} {}", request.getMethod(), request.getURI());
                
                long startTime = System.currentTimeMillis();
                ClientHttpResponse response = execution.execute(request, body);
                long duration = System.currentTimeMillis() - startTime;
                
                // 响应后记录
                log.debug("收到响应: {} {} - 耗时: {}ms", request.getMethod(), 
                          request.getURI(), duration);
                
                return response;
            }
        };
    }
    
    /**
     * 创建服务URL解析器，根据服务ID解析出实际URL
     * 用于手动实现服务发现时使用
     */
    @Bean
    public ServiceUrlResolver serviceUrlResolver() {
        return new ServiceUrlResolver(loadBalancerClient);
    }
    
    /**
     * 服务URL解析器，根据服务ID解析出实际URL
     */
    public static class ServiceUrlResolver {
        private final LoadBalancerClient loadBalancerClient;
        
        public ServiceUrlResolver(LoadBalancerClient loadBalancerClient) {
            this.loadBalancerClient = loadBalancerClient;
        }
        
        /**
         * 根据服务ID和路径构建完整URL
         * @param serviceId 服务ID
         * @param path 请求路径
         * @return 完整URL
         */
        public String buildUrl(String serviceId, String path) {
            if (loadBalancerClient == null) {
                log.warn("LoadBalancerClient is not available, returning fallback URL for service: {}", serviceId);
                return "http://" + serviceId + path;
            }
            
            try {
                URI uri = loadBalancerClient.choose(serviceId).getUri();
                return uri.toString() + path;
            } catch (Exception e) {
                log.warn("Error resolving service URL for {}, using fallback URL. Error: {}", serviceId, e.getMessage());
                return "http://" + serviceId + path;
            }
        }
    }
} 