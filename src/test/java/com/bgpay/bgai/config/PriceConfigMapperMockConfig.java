package com.bgpay.bgai.config;

import com.bgpay.bgai.entity.PriceConfig;
import com.bgpay.bgai.mapper.PriceConfigMapper;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Mock configuration for PriceConfigMapper
 */
@Configuration
public class PriceConfigMapperMockConfig {

    @Bean
    @Primary
    public PriceConfigMapper priceConfigMapper() {
        PriceConfigMapper mockMapper = Mockito.mock(PriceConfigMapper.class);
        
        // Mock default behaviors
        Mockito.when(mockMapper.findLatestPrice(Mockito.anyString(), Mockito.any(LocalDateTime.class)))
                .thenReturn(new PriceConfig());
        
        Mockito.when(mockMapper.selectByVersion(Mockito.anyInt()))
                .thenReturn(new ArrayList<>());
        
        Mockito.when(mockMapper.findByModelTypeAndPriceType(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(new PriceConfig());
        
        Mockito.when(mockMapper.findByModelTypeAndPriceTypeAndTimePeriod(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(new PriceConfig());
        
        Mockito.when(mockMapper.getAllModelTypes())
                .thenReturn(List.of("gpt-3.5-turbo", "gpt-4"));
        
        Mockito.when(mockMapper.getPriceTypesByModel(Mockito.anyString()))
                .thenReturn(List.of("INPUT", "OUTPUT"));
        
        Mockito.when(mockMapper.findValidPriceConfig(Mockito.any(PriceConfig.class)))
                .thenReturn(new PriceConfig());
        
        return mockMapper;
    }
} 