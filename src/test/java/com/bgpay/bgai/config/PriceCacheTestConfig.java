package com.bgpay.bgai.config;

import com.bgpay.bgai.model.es.ChatRecord;
import com.bgpay.bgai.repository.es.ChatRecordRepository;
import com.bgpay.bgai.service.PriceCacheService;
import com.bgpay.bgai.service.PriceConfigService;
import com.bgpay.bgai.service.impl.PriceCacheServiceImpl;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.redisson.api.RedissonClient;

/**
 * 价格缓存服务测试专用配置
 * 仅加载测试必需的组件
 */
@Configuration
@EnableAutoConfiguration(exclude = {
    WebFluxAutoConfiguration.class,
    WebMvcAutoConfiguration.class,
    DataSourceAutoConfiguration.class,
    ElasticsearchDataAutoConfiguration.class,
    ElasticsearchRepositoriesAutoConfiguration.class,
    RedisAutoConfiguration.class
})
@Import({TestNacosConfig.class})
@TestPropertySource(locations = {
        "classpath:bootstrap.yml",
        "classpath:application.yml",
        "classpath:application-dev.yml"
})
public class PriceCacheTestConfig {
    private static final Logger log = LoggerFactory.getLogger(PriceCacheTestConfig.class);
    
    /**
     * 提供模拟的ChatRecordRepository
     */
    @Bean
    @Primary
    public ChatRecordRepository chatRecordRepository() {
        log.info("创建模拟ChatRecordRepository");
        ChatRecordRepository mockRepository = Mockito.mock(ChatRecordRepository.class);
        Mockito.when(mockRepository.save(Mockito.any(ChatRecord.class)))
               .thenAnswer(invocation -> invocation.getArgument(0));
        return mockRepository;
    }
    
    /**
     * 提供模拟的RedisTemplate
     */
    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public <K, V> RedisTemplate<K, V> redisTemplate() {
        log.info("创建模拟RedisTemplate");
        return Mockito.mock(RedisTemplate.class);
    }
    
    /**
     * 提供模拟的RedissonClient
     */
    @Bean
    @Primary
    public RedissonClient redissonClient() {
        log.info("创建模拟RedissonClient");
        return Mockito.mock(RedissonClient.class);
    }
    
    /**
     * 使用完全独立的测试类，不创建实际的Bean
     */
    public PriceCacheTestConfig() {
        log.info("初始化价格缓存测试配置");
        
        // 禁用不必要的功能
        System.setProperty("spring.main.web-application-type", "none");
        System.setProperty("seata.enabled", "false");
        System.setProperty("springdoc.api-docs.enabled", "false");
        System.setProperty("springdoc.swagger-ui.enabled", "false");
    }
} 