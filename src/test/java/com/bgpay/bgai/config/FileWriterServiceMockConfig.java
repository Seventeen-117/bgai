package com.bgpay.bgai.config;

import com.bgpay.bgai.service.deepseek.FileWriterService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class FileWriterServiceMockConfig {
    @Bean
    @Primary
    public FileWriterService fileWriterService() {
        return Mockito.mock(FileWriterService.class);
    }
} 