package com.bgpay.bgai.service;

import com.bgpay.bgai.base.NoSpringContextTest;
import com.bgpay.bgai.entity.PriceConfig;
import com.bgpay.bgai.entity.PriceQuery;
import com.bgpay.bgai.service.impl.PriceCacheServiceImpl;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static com.bgpay.bgai.entity.PriceConstants.INPUT_TYPE;
import static com.bgpay.bgai.entity.PriceConstants.OUTPUT_TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 价格缓存服务测试类
 * 完全独立测试，不依赖Spring上下文
 */
public class PriceCacheServiceTest extends NoSpringContextTest {

    @Mock
    private PriceConfigService priceConfigService;

    @Mock
    private RedisTemplate<String, PriceConfig> priceConfigTemplate;
    
    @Mock
    private RedisTemplate<String, String> stringTemplate;
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private RedissonClient redissonClient;
    
    @Mock
    private ValueOperations<String, PriceConfig> valueOperations;
    
    @Mock
    private ValueOperations<String, String> stringValueOps;
    
    @Mock
    private RLock lock;

    // 使用实现类而不是接口
    private PriceCacheServiceImpl priceCacheService;

    private PriceConfig mockInputPriceConfig;
    private PriceConfig mockOutputPriceConfig;

    @BeforeClass
    public void setUp() {
        log.info("初始化PriceCacheServiceTest测试环境");
        MockitoAnnotations.openMocks(this);
        
        // 创建PriceCacheServiceImpl实例并注入模拟的依赖
        priceCacheService = new PriceCacheServiceImpl(
            priceConfigTemplate,
            stringTemplate,
            redisTemplate,
            priceConfigService,
            redissonClient
        );
        
        // 设置Redis模拟行为
        when(priceConfigTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringTemplate.opsForValue()).thenReturn(stringValueOps);
        when(stringTemplate.hasKey(anyString())).thenReturn(false);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        try {
            when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        } catch (InterruptedException e) {
            // This will never happen in the mock setup
            Thread.currentThread().interrupt();
        }
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        
        // 模拟Redis缓存不命中，直接访问数据库
        when(valueOperations.get(anyString())).thenReturn(null);
        
        // 创建输入价格配置
        mockInputPriceConfig = new PriceConfig();
        mockInputPriceConfig.setModelType("chat");
        mockInputPriceConfig.setIoType(INPUT_TYPE);
        mockInputPriceConfig.setPrice(new BigDecimal("0.05"));
        
        // 创建输出价格配置
        mockOutputPriceConfig = new PriceConfig();
        mockOutputPriceConfig.setModelType("chat");
        mockOutputPriceConfig.setIoType(OUTPUT_TYPE);
        mockOutputPriceConfig.setPrice(new BigDecimal("0.10"));
        
        // 设置Mock行为
        when(priceConfigService.findValidPriceConfig(any(PriceQuery.class)))
            .thenAnswer(invocation -> {
                PriceQuery query = invocation.getArgument(0);
                if (INPUT_TYPE.equals(query.getIoType())) {
                    return mockInputPriceConfig;
                } else if (OUTPUT_TYPE.equals(query.getIoType())) {
                    return mockOutputPriceConfig;
                }
                return null;
            });
    }

    /**
     * 测试获取价格配置
     * 使用Mock数据进行测试
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
        
        // 验证配置有效性
        Assert.assertNotNull(priceConfig);
        Assert.assertNotNull(priceConfig.getPrice());
        Assert.assertTrue(priceConfig.getPrice().compareTo(BigDecimal.ZERO) > 0);
        Assert.assertEquals(priceConfig.getModelType(), "chat");
        Assert.assertEquals(priceConfig.getIoType(), INPUT_TYPE);
        Assert.assertEquals(priceConfig.getPrice(), new BigDecimal("0.05"));
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
        
        // 验证配置有效性
        Assert.assertNotNull(priceConfig);
        Assert.assertNotNull(priceConfig.getPrice());
        Assert.assertTrue(priceConfig.getPrice().compareTo(BigDecimal.ZERO) > 0);
        Assert.assertEquals(priceConfig.getModelType(), "chat");
        Assert.assertEquals(priceConfig.getIoType(), OUTPUT_TYPE);
        Assert.assertEquals(priceConfig.getPrice(), new BigDecimal("0.10"));
    }
} 