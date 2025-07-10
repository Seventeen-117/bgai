package com.bgpay.bgai.config;

import com.bgpay.bgai.mapper.FileTypeMapper;
import com.bgpay.bgai.service.deepseek.FileTypeService;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;

/**
 * 为测试提供FileTypeService和FileTypeMapper的Mock实现
 * 解决CacheWarmer需要这些Bean的问题
 */
@Configuration
public class FileTypeServiceMockConfig {

    /**
     * 提供FileTypeMapper的Mock实现
     */
    @Bean
    @Primary
    public FileTypeMapper fileTypeMapper() {
        return Mockito.mock(FileTypeMapper.class);
    }
    
    /**
     * 提供FileTypeService的Mock实现
     */
    @Bean
    @Primary
    public FileTypeService fileTypeService() {
        FileTypeService mockService = Mockito.mock(FileTypeService.class);
        // 设置基础行为，避免NPE
        Mockito.when(mockService.getAllowedFileTypes()).thenReturn(new ArrayList<>());
        return mockService;
    }
} 