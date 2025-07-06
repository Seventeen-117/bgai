package com.bgpay.bgai.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.bgpay.bgai.base.AbstractDatabaseTest;
import com.bgpay.bgai.entity.ApiConfig;
import com.bgpay.bgai.mapper.ApiConfigMapper;
import com.bgpay.bgai.service.impl.ApiConfigServiceImpl;
import com.bgpay.bgai.utils.DataComparisonUtils;
import com.bgpay.bgai.utils.DataComparisonUtils.KeyExtractor;
import org.apache.ibatis.session.SqlSession;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

import static org.mockito.Mockito.when;

/**
 * API配置服务数据库测试类
 * 演示如何比较服务返回的数据与数据库中的数据是否一致
 */
public class ApiConfigServiceDatabaseTest extends AbstractDatabaseTest {
    
    @Mock
    private ApiConfigMapper mockApiConfigMapper;
    
    @Spy
    private ApiConfigServiceImpl apiConfigService;
    
    private ApiConfigMapper realApiConfigMapper;
    
    @BeforeClass
    public void setUp() {
        log.info("初始化ApiConfigServiceDatabaseTest");
        MockitoAnnotations.openMocks(this);
        
        // 获取真实的Mapper实例用于直接查询数据库
        try (SqlSession session = getSqlSession()) {
            realApiConfigMapper = session.getMapper(ApiConfigMapper.class);
        } catch (Exception e) {
            log.error("获取ApiConfigMapper失败", e);
        }
    }
    
    /**
     * 向MyBatis配置添加需要的Mapper
     */
    @Override
    protected void addMappers(MybatisConfiguration configuration) {
        // 添加需要用到的Mapper
        configuration.addMapper(ApiConfigMapper.class);
    }
    
    /**
     * 测试获取API配置列表，并与数据库中的数据进行比较
     */
    @Test
    public void testGetApiConfigList() {
        log.info("测试获取API配置列表，并与数据库中的数据进行比较");
        
        try (SqlSession session = getSqlSession()) {
            // 1. 获取真实的数据库数据
            ApiConfigMapper dbMapper = session.getMapper(ApiConfigMapper.class);
            List<ApiConfig> dbConfigs = dbMapper.selectList(null);
            
            if (dbConfigs.isEmpty()) {
                log.info("数据库中没有API配置数据，跳过测试");
                return;
            }
            
            log.info("从数据库获取到{}条API配置记录", dbConfigs.size());
            
            // 2. 设置Mock以返回真实数据
            when(mockApiConfigMapper.selectList(null)).thenReturn(dbConfigs);
            
            // 3. 调用服务方法获取数据
            List<ApiConfig> serviceConfigs = apiConfigService.list();
            
            log.info("从服务获取到{}条API配置记录", serviceConfigs.size());
            
            // 4. 使用比较工具比较数据
            KeyExtractor<ApiConfig, Long> keyExtractor = ApiConfig::getId;
            
            // 5. 执行断言比较，如果数据不一致会抛出异常
            DataComparisonUtils.assertCollectionsEqual(
                    serviceConfigs,
                    dbConfigs,
                    keyExtractor,
                    "id", "name", "apiUrl", "modelName", "modelType"
            );
            
            log.info("API配置数据比较一致");
        }
    }
    
    /**
     * 测试根据ID获取单个API配置
     */
    @Test
    public void testGetApiConfigById() {
        log.info("测试通过ID获取单个API配置，并与数据库中的数据进行比较");
        
        try (SqlSession session = getSqlSession()) {
            // 1. 获取数据库中的第一条记录
            ApiConfigMapper dbMapper = session.getMapper(ApiConfigMapper.class);
            List<ApiConfig> dbConfigs = dbMapper.selectList(null);
            
            if (dbConfigs.isEmpty()) {
                log.info("数据库中没有API配置数据，跳过测试");
                return;
            }
            
            ApiConfig dbConfig = dbConfigs.get(0);
            Long configId = dbConfig.getId();
            
            log.info("从数据库获取ID为{}的API配置", configId);
            
            // 2. 设置Mock以返回真实数据
            when(mockApiConfigMapper.selectById(configId)).thenReturn(dbConfig);
            
            // 3. 调用服务方法获取数据
            ApiConfig serviceConfig = apiConfigService.getById(configId);
            
            // 4. 使用比较工具比较单个对象
            DataComparisonUtils.assertObjectsEqual(
                    serviceConfig,
                    dbConfig,
                    "id", "name", "apiUrl", "modelName", "modelType", "userId",
                    "enabled", "isDefault", "priority"
            );
            
            log.info("API配置对象比较一致");
        }
    }
} 