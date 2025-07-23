package com.bgpay.bgai.config;

import com.bgpay.bgai.service.deepseek.DeepSeekService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
class RemoveDeepSeekServiceImpConfig {
    @Bean(name = "deepSeekServiceImp")
    @Primary
    public DeepSeekService removeDeepSeekServiceImp() {
        throw new IllegalStateException("deepSeekServiceImp should not be created in test context");
    }
}
