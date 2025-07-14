package com.bgpay.bgai.config;

import com.bgpay.bgai.mapper.*;
import org.mockito.Mockito;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 提供所有Mapper接口的Mock实现，用于测试环境
 * 这个配置类会扫描com.bgpay.bgai.mapper包下的所有接口，并为它们创建Mock实现
 */
@TestConfiguration
public class AllMappersMockConfig {
    
    // 以下是已知的Mapper接口，为了确保它们一定有Mock实现
    
    @Bean
    @Primary
    public ApiKeyMapper apiKeyMapper() {
        return Mockito.mock(ApiKeyMapper.class);
    }
    
    @Bean
    @Primary
    public ApiConfigMapper apiConfigMapper() {
        return Mockito.mock(ApiConfigMapper.class);
    }
    
    @Bean
    @Primary
    public TransactionLogMapper transactionLogMapper() {
        return Mockito.mock(TransactionLogMapper.class);
    }
    
    @Bean
    @Primary
    public ApiClientMapper apiClientMapper() {
        return Mockito.mock(ApiClientMapper.class);
    }
    
    @Bean
    @Primary
    public ChatCompletionsMapper chatCompletionsMapper() {
        return Mockito.mock(ChatCompletionsMapper.class);
    }
    
    @Bean
    @Primary
    public ChoicesMapper choicesMapper() {
        return Mockito.mock(ChoicesMapper.class);
    }
    
    @Bean
    @Primary
    public PriceConfigMapper priceConfigMapper() {
        return Mockito.mock(PriceConfigMapper.class);
    }
    
    @Bean
    @Primary
    public PriceVersionMapper priceVersionMapper() {
        return Mockito.mock(PriceVersionMapper.class);
    }
    
    @Bean
    @Primary
    public UserMapper userMapper() {
        return Mockito.mock(UserMapper.class);
    }
    
    @Bean
    @Primary
    public UsageInfoMapper usageInfoMapper() {
        return Mockito.mock(UsageInfoMapper.class);
    }
    
    // 如果还有其他Mapper接口，可以继续添加
} 