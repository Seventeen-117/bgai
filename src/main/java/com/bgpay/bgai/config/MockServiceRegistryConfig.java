package com.bgpay.bgai.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模拟服务注册配置
 * 在没有真实服务注册中心的情况下，提供本地服务注册
 * 只有在bgai.mock.discovery=true时才启用
 */
@Configuration
@ConditionalOnProperty(name = "bgai.mock.discovery", havingValue = "true", matchIfMissing = false)
public class MockServiceRegistryConfig {

    /**
     * 创建一个模拟的DiscoveryClient，用于本地测试
     */
    @Bean
    public DiscoveryClient mockDiscoveryClient() {
        return new DiscoveryClient() {
            @Override
            public String description() {
                return "Mock Discovery Client";
            }

            @Override
            public List<ServiceInstance> getInstances(String serviceId) {
                // 模拟服务实例
                Map<String, List<ServiceInstance>> serviceInstances = new HashMap<>();
                
                // 将user-service映射到本地
                serviceInstances.put("user-service", Arrays.asList(
                    new DefaultServiceInstance(
                        serviceId + "-1",
                        serviceId,
                        "localhost", 
                        8688,
                        false
                    )
                ));
                
                return serviceInstances.getOrDefault(serviceId, List.of());
            }

            @Override
            public List<String> getServices() {
                return Arrays.asList("user-service", "bgtech-ai");
            }
        };
    }
    
    /**
     * 为user-service创建一个ServiceInstanceListSupplier
     * 用于LoadBalancer的服务发现
     */
    @Bean
    public ServiceInstanceListSupplier userServiceInstanceListSupplier(Environment environment) {
        return new ServiceInstanceListSupplier() {
            @Override
            public String getServiceId() {
                return "user-service";
            }

            @Override
            public Flux<List<ServiceInstance>> get() {
                return Flux.just(Arrays.asList(
                    new DefaultServiceInstance(
                        "user-service-1",
                        "user-service",
                        "localhost", 
                        8688,
                        false
                    )
                ));
            }
        };
    }
} 