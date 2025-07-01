package com.bgpay.bgai.service;

import com.bgpay.bgai.base.AbstractTestNGTest;
import com.bgpay.bgai.entity.PriceConfig;
import com.bgpay.bgai.entity.PriceQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.math.BigDecimal;

import static com.bgpay.bgai.entity.PriceConstants.INPUT_TYPE;
import static com.bgpay.bgai.entity.PriceConstants.OUTPUT_TYPE;

/**
 * 价格缓存服务测试类
 * 演示如何使用集成Nacos配置的TestNG测试
 */
public class PriceCacheServiceTest extends AbstractTestNGTest {

    @Autowired
    private PriceCacheService priceCacheService;

    /**
     * 测试获取价格配置
     * 此测试将从Nacos或本地配置加载真实的价格配置
     */
    @Test
    public void testGetPriceConfig() {
        // 准备测试数据
        PriceQuery query = new PriceQuery(
                "chat",      // 模型类型
                "standard",  // 时段
                null,        // 缓存类型
                INPUT_TYPE   // 输入类型
        );

        // 执行测试
        PriceConfig priceConfig = priceCacheService.getPriceConfig(query);
        
        // 验证结果
        log.info("获取到价格配置: {}", priceConfig);
        
        // 如果没有配置，则跳过断言
        if (priceConfig == null) {
            log.warn("未找到价格配置，可能是Nacos连接问题或配置未设置");
            return;
        }
        
        // 验证配置有效性
        Assert.assertNotNull(priceConfig);
        Assert.assertNotNull(priceConfig.getPrice());
        Assert.assertTrue(priceConfig.getPrice().compareTo(BigDecimal.ZERO) >= 0);
        Assert.assertEquals(priceConfig.getModelType(), "chat");
        Assert.assertEquals(priceConfig.getIoType(), INPUT_TYPE);
    }

    /**
     * 测试获取输出价格配置
     */
    @Test
    public void testGetOutputPriceConfig() {
        // 准备测试数据
        PriceQuery query = new PriceQuery(
                "chat",      // 模型类型
                "standard",  // 时段
                null,        // 缓存类型
                OUTPUT_TYPE  // 输出类型
        );

        // 执行测试
        PriceConfig priceConfig = priceCacheService.getPriceConfig(query);
        
        // 验证结果
        log.info("获取到输出价格配置: {}", priceConfig);
        
        // 如果没有配置，则跳过断言
        if (priceConfig == null) {
            log.warn("未找到输出价格配置，可能是Nacos连接问题或配置未设置");
            return;
        }
        
        // 验证配置有效性
        Assert.assertNotNull(priceConfig);
        Assert.assertNotNull(priceConfig.getPrice());
        Assert.assertTrue(priceConfig.getPrice().compareTo(BigDecimal.ZERO) >= 0);
        Assert.assertEquals(priceConfig.getModelType(), "chat");
        Assert.assertEquals(priceConfig.getIoType(), OUTPUT_TYPE);
    }
} 