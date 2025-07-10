package com.bgpay.bgai.config;

import com.bgpay.bgai.controller.ServiceDiscoveryExampleController;
import com.bgpay.bgai.utils.ServiceDiscoveryUtils;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 为ServiceDiscoveryExampleController提供所需依赖的配置类
 */
@TestConfiguration
public class ServiceDiscoveryExampleControllerConfig {

    /**
     * 提供ServiceDiscoveryExampleController的实例
     * 使用所有Mock依赖注入
     */
    @Bean
    @Primary
    public ServiceDiscoveryExampleController serviceDiscoveryExampleController(
            ServiceDiscoveryUtils serviceDiscoveryUtils,
            WebClient loadBalancedWebClient) {
        // 创建一个Mock的RestTemplate
        RestTemplate mockRestTemplate = Mockito.mock(RestTemplate.class);
        
        return new ServiceDiscoveryExampleController(
                serviceDiscoveryUtils,
                loadBalancedWebClient,
                mockRestTemplate,
                mockRestTemplate  // 使用同一个mock实例作为普通RestTemplate
        );
    }
} 