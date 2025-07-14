package com.bgpay.bgai.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Jackson全局配置类
 * 用于配置Jackson的序列化行为
 */
@Configuration
public class JacksonConfig {

    /**
     * 配置全局ObjectMapper，排除所有为null的字段
     * 这将确保在JSON序列化时，所有为null的字段都会被排除
     * 
     * @param builder Jackson2ObjectMapperBuilder
     * @return 配置好的ObjectMapper
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        // 全局设置序列化策略：不包含null值字段
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return objectMapper;
    }
} 