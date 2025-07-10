package com.bgpay.bgai.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 禁用OpenFeign的自动配置
 * 导入自定义的Feign客户端Mock配置
 */
@Configuration
@Import({FeignClientMockConfig.class})
@EnableAutoConfiguration(exclude = {
    org.springframework.cloud.openfeign.FeignAutoConfiguration.class,
    org.springframework.cloud.openfeign.encoding.FeignAcceptGzipEncodingAutoConfiguration.class,
    org.springframework.cloud.openfeign.encoding.FeignContentGzipEncodingAutoConfiguration.class,
    org.springframework.cloud.openfeign.loadbalancer.FeignLoadBalancerAutoConfiguration.class
})
public class FeignAutoConfigurationDisabler {
    // 仅用于禁用OpenFeign自动配置
    // 并导入测试专用的FeignClientMockConfig
} 