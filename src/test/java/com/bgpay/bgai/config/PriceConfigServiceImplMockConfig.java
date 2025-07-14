package com.bgpay.bgai.config;

import com.bgpay.bgai.entity.PriceConfig;
import com.bgpay.bgai.entity.PriceQuery;
import com.bgpay.bgai.entity.PriceVersion;
import com.bgpay.bgai.priceEnum.ModelType;
import com.bgpay.bgai.priceEnum.PriceType;
import com.bgpay.bgai.priceEnum.TimePeriod;
import com.bgpay.bgai.service.PriceConfigService;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Mock configuration for PriceConfigServiceImpl
 */
@Configuration
public class PriceConfigServiceImplMockConfig {

    @Bean
    @Primary
    public PriceConfigService priceConfigService() {
        PriceConfigService mockService = Mockito.mock(PriceConfigService.class);
        
        // Mock default behaviors
        Mockito.when(mockService.getPrice(Mockito.any(PriceQuery.class)))
                .thenReturn(BigDecimal.valueOf(0.01));
        
        Mockito.when(mockService.getCurrentPriceVersion())
                .thenReturn(1);
        
        Mockito.when(mockService.getPriceConfigsByVersion(Mockito.anyInt()))
                .thenReturn(new ArrayList<>());
        
        Mockito.when(mockService.getPriceConfig(
                Mockito.any(ModelType.class), Mockito.any(PriceType.class), Mockito.anyInt()))
                .thenReturn(new PriceConfig());
        
        Mockito.when(mockService.getPriceByTimePeriod(
                Mockito.any(ModelType.class), Mockito.any(PriceType.class), 
                Mockito.any(TimePeriod.class), Mockito.anyInt()))
                .thenReturn(BigDecimal.valueOf(0.01));
        
        Mockito.when(mockService.createPriceVersion(Mockito.any(PriceVersion.class)))
                .thenReturn(new PriceVersion());
        
        Mockito.when(mockService.updatePriceConfig(Mockito.any(PriceConfig.class)))
                .thenReturn(new PriceConfig());
        
        Mockito.when(mockService.saveBatchPriceConfigs(Mockito.anyList()))
                .thenReturn(true);
        
        Mockito.when(mockService.findValidPriceConfig(Mockito.any(PriceQuery.class)))
                .thenReturn(new PriceConfig());
        
        return mockService;
    }
} 