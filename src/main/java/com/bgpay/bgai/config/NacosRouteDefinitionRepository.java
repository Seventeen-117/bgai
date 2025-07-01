package com.bgpay.bgai.config;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于Nacos的路由定义存储库
 * 用于持久化路由配置并从Nacos中加载路由配置
 */
@Slf4j
@Component
public class NacosRouteDefinitionRepository implements RouteDefinitionRepository {

    private static final String ROUTE_DATA_ID = "gateway-routes";
    private static final String ROUTE_GROUP = "DEFAULT_GROUP";
    
    @Value("${spring.cloud.nacos.config.server-addr}")
    private String serverAddr;
    
    @Value("${spring.cloud.nacos.config.namespace}")
    private String namespace;
    
    // 本地缓存路由信息
    private ConcurrentHashMap<String, RouteDefinition> routeDefinitions = new ConcurrentHashMap<>();
    
    // 标记是否已初始化
    private AtomicBoolean isInitialized = new AtomicBoolean(false);
    
    private ConfigService configService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        log.info("初始化Nacos路由定义存储库");
        try {
            // 初始化Nacos配置服务
            Properties properties = new Properties();
            properties.setProperty("serverAddr", serverAddr);
            if (namespace != null && !namespace.isEmpty()) {
                properties.setProperty("namespace", namespace);
            }
            this.configService = NacosFactory.createConfigService(properties);
            
            // 首次加载配置
            loadRouteDefinitions();
            
            // 注册监听器，监听配置变化
            configService.addListener(ROUTE_DATA_ID, ROUTE_GROUP, new Listener() {
                @Override
                public Executor getExecutor() {
                    return null; // 使用默认的执行器
                }

                @Override
                public void receiveConfigInfo(String configInfo) {
                    log.info("路由定义存储库收到Nacos配置变更通知");
                    parseRouteDefinitions(configInfo);
                }
            });
            
        } catch (NacosException e) {
            log.error("初始化Nacos路由定义存储库失败", e);
        }
    }

    /**
     * 加载路由定义
     */
    private void loadRouteDefinitions() {
        try {
            String configInfo = configService.getConfig(ROUTE_DATA_ID, ROUTE_GROUP, 5000);
            if (configInfo != null && !configInfo.isEmpty()) {
                parseRouteDefinitions(configInfo);
                log.info("从Nacos加载路由定义成功，共{}条", routeDefinitions.size());
            } else {
                log.warn("Nacos中未找到路由定义配置");
            }
        } catch (NacosException e) {
            log.error("从Nacos加载路由定义失败", e);
        }
    }

    /**
     * 解析路由定义
     */
    private void parseRouteDefinitions(String configInfo) {
        try {
            List<RouteDefinition> routes = objectMapper.readValue(
                    configInfo, new TypeReference<List<RouteDefinition>>() {});
            
            // 清空当前缓存
            routeDefinitions.clear();
            
            // 重新加载路由定义
            for (RouteDefinition route : routes) {
                routeDefinitions.put(route.getId(), route);
            }
            
            log.info("解析路由定义成功，共{}条", routeDefinitions.size());
            
        } catch (Exception e) {
            log.error("解析路由定义配置失败", e);
        }
    }

    /**
     * 保存路由定义到Nacos
     */
    private boolean saveRouteDefinitions() {
        try {
            List<RouteDefinition> routes = new ArrayList<>(routeDefinitions.values());
            String configInfo = objectMapper.writeValueAsString(routes);
            boolean result = configService.publishConfig(ROUTE_DATA_ID, ROUTE_GROUP, configInfo);
            
            if (result) {
                log.info("保存路由定义到Nacos成功");
            } else {
                log.error("保存路由定义到Nacos失败");
            }
            
            return result;
        } catch (NacosException | JsonProcessingException e) {
            log.error("保存路由定义到Nacos出错", e);
            return false;
        }
    }

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        // 如果未初始化，先加载路由配置
        if (isInitialized.compareAndSet(false, true)) {
            loadRouteDefinitions();
        }
        
        // 返回所有路由定义
        return Flux.fromIterable(routeDefinitions.values());
    }

    @Override
    public Mono<Void> save(Mono<RouteDefinition> route) {
        return route.flatMap(r -> {
            routeDefinitions.put(r.getId(), r);
            if (saveRouteDefinitions()) {
                return Mono.empty();
            }
            return Mono.error(new RuntimeException("保存路由定义失败: " + r.getId()));
        });
    }

    @Override
    public Mono<Void> delete(Mono<String> routeId) {
        return routeId.flatMap(id -> {
            routeDefinitions.remove(id);
            if (saveRouteDefinitions()) {
                return Mono.empty();
            }
            return Mono.error(new RuntimeException("删除路由定义失败: " + id));
        });
    }
} 