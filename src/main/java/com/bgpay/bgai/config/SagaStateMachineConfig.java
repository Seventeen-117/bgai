package com.bgpay.bgai.config;

import com.bgpay.bgai.saga.CustomSagaJsonParser;
import io.seata.saga.engine.StateMachineEngine;
import io.seata.saga.engine.config.DbStateMachineConfig;
import io.seata.saga.engine.impl.ProcessCtrlStateMachineEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Saga状态机配置
 * 用于配置Saga模式的状态机引擎
 */
@Configuration
public class SagaStateMachineConfig implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(SagaStateMachineConfig.class);

    @Autowired(required = false)
    private DataSource dataSource;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @Autowired
    private CustomSagaJsonParser customSagaJsonParser;
    
    @Value("${spring.application.name:bgai-application}")
    private String applicationId;
    
    private final List<String> loadedStateMachines = new ArrayList<>();
    private boolean configLoadSuccess = false;

    @Bean
    public ThreadPoolExecutor sagaThreadPoolExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("saga-executor-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor.getThreadPoolExecutor();
    }

    @Bean
    public StateMachineEngine stateMachineEngine() {
        if (dataSource == null) {
            logger.warn("未配置数据源，Saga状态机将不会启用");
            return null;
        }

        try {
            // 创建状态机配置
            DbStateMachineConfig stateMachineConfig = new DbStateMachineConfig();
            stateMachineConfig.setDataSource(dataSource);
            stateMachineConfig.setThreadPoolExecutor(sagaThreadPoolExecutor());
            stateMachineConfig.setApplicationId(applicationId);
            stateMachineConfig.setTxServiceGroup("bgai-tx-group");
            
            // 使用资源解析器获取状态机定义文件
            PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resourceResolver.getResources("classpath:statelang/*.json");
            
            if (resources != null && resources.length > 0) {
                logger.info("加载到{}个Saga状态机定义文件", resources.length);
                
                // 手动加载和解析每个状态机定义
                for (Resource resource : resources) {
                    try {
                        String resourcePath = resource.getURL().getPath();
                        String content = new String(Files.readAllBytes(Paths.get(resource.getURI())), StandardCharsets.UTF_8);
                        
                        // 使用自定义解析器解析状态机定义
                        boolean success = customSagaJsonParser.parseStateMachineJson(content);
                        if (success) {
                            loadedStateMachines.add(resourcePath);
                            logger.info("成功加载状态机定义: {}", resourcePath);
                        } else {
                            logger.error("解析状态机定义失败: {}", resourcePath);
                        }
                    } catch (Exception e) {
                        logger.error("加载状态机定义失败: {}", resource.getFilename(), e);
                    }
                }
                
                // 转换为资源路径数组
                String[] resourcePaths = new String[resources.length];
                for (int i = 0; i < resources.length; i++) {
                    resourcePaths[i] = resources[i].getURL().toString();
                }
                stateMachineConfig.setResources(resourcePaths);
                configLoadSuccess = loadedStateMachines.size() > 0;
            } else {
                logger.warn("未找到Saga状态机定义文件");
                configLoadSuccess = false;
            }
            
            stateMachineConfig.setEnableAsync(true);
            
            // 创建状态机引擎
            ProcessCtrlStateMachineEngine stateMachineEngine = new ProcessCtrlStateMachineEngine();
            stateMachineEngine.setStateMachineConfig(stateMachineConfig);
            
            return stateMachineEngine;
        } catch (IOException e) {
            logger.error("加载Saga状态机定义文件失败", e);
            configLoadSuccess = false;
            return null;
        }
    }
    
    @Override
    public void run(ApplicationArguments args) {
        logger.info("==================== Saga状态机加载报告 ====================");
        logger.info("Saga状态机配置加载状态: {}", configLoadSuccess ? "成功" : "失败");
        logger.info("已加载的状态机定义文件数量: {}", loadedStateMachines.size());
        
        if (!loadedStateMachines.isEmpty()) {
            logger.info("已加载的状态机定义文件:");
            for (String machine : loadedStateMachines) {
                logger.info(" - {}", machine);
            }
        } else {
            logger.warn("没有成功加载任何状态机定义文件");
        }
        
        logger.info("Saga解析器状态: {}", customSagaJsonParser.getJsonParserType());
        logger.info(customSagaJsonParser.getParsingResultSummary());
        logger.info("=============================================================");
    }
} 