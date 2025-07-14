package com.bgpay.bgai.config;

import com.bgpay.bgai.entity.PriceVersion;
import com.bgpay.bgai.mapper.PriceVersionMapper;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Mock configuration for PriceVersionMapper
 */
@Configuration
public class PriceVersionMapperMockConfig {

    @Bean
    @Primary
    public PriceVersionMapper priceVersionMapper() {
        PriceVersionMapper mockMapper = Mockito.mock(PriceVersionMapper.class);
        
        // Mock default behaviors
        Mockito.when(mockMapper.getCurrentVersion())
                .thenReturn(1);
        
        Mockito.when(mockMapper.getByVersion(Mockito.anyInt()))
                .thenReturn(new PriceVersion());
        
        Mockito.when(mockMapper.invalidateCurrentVersion(Mockito.anyInt()))
                .thenReturn(1);
        
        return mockMapper;
    }
} 