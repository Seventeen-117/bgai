package com.bgpay.bgai.config;

import com.bgpay.bgai.utils.ServiceDiscoveryUtils;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 服务发现相关的mock配置类
 * 提供ServiceDiscoveryUtils及其依赖的mock实现
 */
@TestConfiguration
public class ServiceDiscoveryMockConfig {

    /**
     * 提供ServiceDiscoveryUtils的mock实现
     */
    @Bean
    @Primary
    public ServiceDiscoveryUtils serviceDiscoveryUtils() {
        return Mockito.mock(ServiceDiscoveryUtils.class);
    }
    
    /**
     * 提供loadBalancedWebClient的mock实现
     */
    @Bean
    @Primary
    @org.springframework.beans.factory.annotation.Qualifier("loadBalancedWebClient")
    public WebClient loadBalancedWebClient() {
        return Mockito.mock(WebClient.class);
    }
    
    /**
     * 提供loadBalancedRestTemplate的mock实现
     */
    @Bean
    @Primary
    @org.springframework.beans.factory.annotation.Qualifier("loadBalancedRestTemplate")
    public RestTemplate loadBalancedRestTemplate() {
        return Mockito.mock(RestTemplate.class);
    }
} 