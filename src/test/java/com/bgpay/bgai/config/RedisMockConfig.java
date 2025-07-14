package com.bgpay.bgai.config;

import com.bgpay.bgai.entity.PriceConfig;
import com.bgpay.bgai.entity.UserToken;
import com.bgpay.bgai.entity.UsageCalculationDTO;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.retry.support.RetryTemplate;

/**
 * Mock configuration for Redis components in test environment
 */
@Configuration
public class RedisMockConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    @Value("${spring.data.redis.database}")
    private int redisDatabase;

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return Mockito.mock(RedisConnectionFactory.class);
    }

    @Bean
    @Primary
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Bean
    public RedisTemplate<String, Object> mockRedisTemplate() {
        RedisTemplate<String, Object> mockTemplate = Mockito.mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOps = Mockito.mock(ValueOperations.class);
        Mockito.when(mockTemplate.opsForValue()).thenReturn(valueOps);
        return mockTemplate;
    }

    @Bean
    @Primary
    public RedisTemplate<String, String> stringRedisTemplate() {
        RedisTemplate<String, String> mockTemplate = Mockito.mock(RedisTemplate.class);
        ValueOperations<String, String> valueOps = Mockito.mock(ValueOperations.class);
        Mockito.when(mockTemplate.opsForValue()).thenReturn(valueOps);
        Mockito.when(mockTemplate.hasKey(Mockito.anyString())).thenReturn(false);
        return mockTemplate;
    }

    @Bean
    @Primary
    public RedisTemplate<String, PriceConfig> priceConfigRedisTemplate(ObjectMapper redisObjectMapper) {
        RedisTemplate<String, PriceConfig> mockTemplate = Mockito.mock(RedisTemplate.class);
        ValueOperations<String, PriceConfig> valueOps = Mockito.mock(ValueOperations.class);
        Mockito.when(mockTemplate.opsForValue()).thenReturn(valueOps);
        return mockTemplate;
    }

    /**
     * 重试模板配置
     */
    @Bean
    @Primary
    public RetryTemplate redisRetryTemplate() {
        return RetryTemplate.builder()
                .maxAttempts(3)
                .fixedBackoff(100)
                .retryOn(Exception.class)
                .build();
    }

    /**
     * 通用RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplateWithConnectionFactory(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        ObjectMapper redisMapper = new ObjectMapper();
        redisMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        redisMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        redisMapper.registerModule(new JavaTimeModule());

        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(redisMapper, Object.class);
        template.setValueSerializer(serializer);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * UserToken专用RedisTemplate
     */
    @Bean
    @Primary
    public RedisTemplate<String, UserToken> userTokenRedisTemplate(
            RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, UserToken> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        ObjectMapper tokenMapper = new ObjectMapper();
        tokenMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        tokenMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        tokenMapper.registerModule(new JavaTimeModule());

        Jackson2JsonRedisSerializer<UserToken> serializer = new Jackson2JsonRedisSerializer<>(tokenMapper, UserToken.class);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * UsageCalculationDTO专用RedisTemplate
     */
    @Bean
    @Primary
    public RedisTemplate<String, UsageCalculationDTO> usageCalculationRedisTemplate(
            RedisConnectionFactory connectionFactory, ObjectMapper redisObjectMapper) {
        RedisTemplate<String, UsageCalculationDTO> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        Jackson2JsonRedisSerializer<UsageCalculationDTO> serializer =
                new Jackson2JsonRedisSerializer<>(redisObjectMapper, UsageCalculationDTO.class);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }
    
    /**
     * 添加主要的RedisTemplate用于通用对象操作
     * 这个是为了解决PriceCacheServiceImpl中需要的通用RedisTemplate
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> mockTemplate = Mockito.mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOps = Mockito.mock(ValueOperations.class);
        Mockito.when(mockTemplate.opsForValue()).thenReturn(valueOps);
        return mockTemplate;
    }
} 