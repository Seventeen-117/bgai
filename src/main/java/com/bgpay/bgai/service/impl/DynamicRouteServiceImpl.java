package com.bgpay.bgai.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.bgpay.bgai.event.RouteRefreshEvent;
import com.bgpay.bgai.service.DynamicRouteService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class DynamicRouteServiceImpl implements DynamicRouteService, ApplicationEventPublisherAware, InitializingBean, DisposableBean {

    private static final String ROUTE_DATA_ID = "gateway-routes";
    private static final String ROUTE_GROUP = "DEFAULT_GROUP";
    
    @Value("${spring.cloud.nacos.config.server-addr}")
    private String serverAddr;
    
    @Value("${spring.cloud.nacos.config.namespace}")
    private String namespace;
    
    @Autowired
    private RouteDefinitionWriter routeDefinitionWriter;
    
    @Autowired(required = false)
    private RouteDefinitionRepository routeDefinitionRepository;
    
    private ApplicationEventPublisher publisher;
    
    private ConfigService configService;
    
    // 本地缓存路由信息，避免每次查询Nacos
    private ConcurrentHashMap<String, RouteDefinition> routeDefinitionMap = new ConcurrentHashMap<>();
    
    // 标记是否已初始化
    private AtomicBoolean isInitialized = new AtomicBoolean(false);
    
    // Nacos配置监听器
    private Listener nacosListener;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.publisher = applicationEventPublisher;
    }
    
    @PostConstruct
    public void init() {
        log.info("初始化动态路由服务");
        try {
            // 初始化Nacos配置服务
            Properties properties = new Properties();
            properties.setProperty("serverAddr", serverAddr);
            if (namespace != null && !namespace.isEmpty()) {
                properties.setProperty("namespace", namespace);
            }
            this.configService = NacosFactory.createConfigService(properties);
            
            // 创建Nacos监听器
            this.nacosListener = new Listener() {
                @Override
                public Executor getExecutor() {
                    return null; // 使用默认的执行器
                }

                @Override
                public void receiveConfigInfo(String configInfo) {
                    log.info("收到Nacos路由配置变更通知");
                    updateRouteDefinitions(configInfo);
                }
            };
            
        } catch (NacosException e) {
            log.error("初始化Nacos配置服务失败", e);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // 应用启动时从Nacos加载路由配置
        if (isInitialized.compareAndSet(false, true)) {
            loadRouteConfig();
        }
    }
    
    @Override
    public void destroy() throws Exception {
        // 关闭资源或执行清理操作
        log.info("动态路由服务关闭，执行清理操作");
    }
    
    /**
     * 从Nacos加载路由配置
     */
    private void loadRouteConfig() {
        try {
            String configInfo = configService.getConfig(ROUTE_DATA_ID, ROUTE_GROUP, 5000);
            if (configInfo != null && !configInfo.isEmpty()) {
                log.info("从Nacos加载路由配置: {}", configInfo);
                updateRouteDefinitions(configInfo);
            } else {
                log.warn("Nacos中没有找到路由配置，使用默认配置");
            }
            
            // 注册Nacos配置监听器
            configService.addListener(ROUTE_DATA_ID, ROUTE_GROUP, nacosListener);
            log.info("注册Nacos配置监听器成功");
            
        } catch (NacosException e) {
            log.error("从Nacos加载路由配置失败", e);
        }
    }
    
    /**
     * 更新路由定义
     */
    private void updateRouteDefinitions(String configInfo) {
        try {
            // 将JSON字符串转换为路由定义列表
            List<RouteDefinition> routeDefinitions = objectMapper.readValue(
                    configInfo, new TypeReference<List<RouteDefinition>>() {});
            
            // 清空缓存
            routeDefinitionMap.clear();
            
            // 更新路由
            for (RouteDefinition routeDefinition : routeDefinitions) {
                routeDefinitionMap.put(routeDefinition.getId(), routeDefinition);
                // 保存到Gateway中
                routeDefinitionWriter.delete(Mono.just(routeDefinition.getId())).subscribe();
                routeDefinitionWriter.save(Mono.just(routeDefinition)).subscribe();
            }
            
            // 发布路由刷新事件
            publisher.publishEvent(new RefreshRoutesEvent(this));
            log.info("路由配置已更新，共{}条路由", routeDefinitions.size());
            
        } catch (Exception e) {
            log.error("解析路由配置失败", e);
        }
    }
    
    /**
     * 保存路由配置到Nacos
     */
    private Mono<Void> saveRoutesToNacos() {
        try {
            if (configService != null) {
                List<RouteDefinition> routes = new ArrayList<>(routeDefinitionMap.values());
                String configInfo = objectMapper.writeValueAsString(routes);
                boolean result = configService.publishConfig(ROUTE_DATA_ID, ROUTE_GROUP, configInfo);
                
                if (result) {
                    log.info("保存路由配置到Nacos成功");
                    return Mono.empty();
                } else {
                    log.error("保存路由配置到Nacos失败");
                    return Mono.error(new RuntimeException("保存路由配置到Nacos失败"));
                }
            }
        } catch (NacosException | JsonProcessingException e) {
            log.error("保存路由配置到Nacos出错", e);
            return Mono.error(e);
        }
        return Mono.empty();
    }

    @Override
    public Mono<Void> add(RouteDefinition route) {
        try {
            // 保存路由到本地缓存
            routeDefinitionMap.put(route.getId(), route);
            
            // 更新Gateway中的路由
            routeDefinitionWriter.save(Mono.just(route)).subscribe();
            
            // 发布路由刷新事件
            publisher.publishEvent(new RouteRefreshEvent(this, route.getId(), "ADD"));
            
            // 保存到Nacos
            return saveRoutesToNacos();
        } catch (Exception e) {
            log.error("添加路由失败: {}", e.getMessage());
            return Mono.error(e);
        }
    }

    @Override
    public Mono<Void> update(RouteDefinition route) {
        try {
            // 从Gateway中删除旧路由
            routeDefinitionWriter.delete(Mono.just(route.getId())).subscribe();
            
            // 添加新路由
            routeDefinitionWriter.save(Mono.just(route)).subscribe();
            
            // 更新本地缓存
            routeDefinitionMap.put(route.getId(), route);
            
            // 发布路由刷新事件
            publisher.publishEvent(new RouteRefreshEvent(this, route.getId(), "UPDATE"));
            
            // 保存到Nacos
            return saveRoutesToNacos();
        } catch (Exception e) {
            log.error("更新路由失败: {}", e.getMessage());
            return Mono.error(e);
        }
    }

    @Override
    public Mono<Void> delete(String routeId) {
        try {
            // 从Gateway中删除路由
            routeDefinitionWriter.delete(Mono.just(routeId)).subscribe();
            
            // 从本地缓存中删除
            routeDefinitionMap.remove(routeId);
            
            // 发布路由刷新事件
            publisher.publishEvent(new RouteRefreshEvent(this, routeId, "DELETE"));
            
            // 保存到Nacos
            return saveRoutesToNacos();
        } catch (Exception e) {
            log.error("删除路由失败: {}", e.getMessage());
            return Mono.error(e);
        }
    }

    @Override
    public Mono<RouteDefinition> getRoute(String routeId) {
        // 从本地缓存获取路由定义
        RouteDefinition route = routeDefinitionMap.get(routeId);
        return route != null ? Mono.just(route) : Mono.empty();
    }
    
    @Override
    public Flux<RouteDefinition> getRoutes() {
        // 返回所有路由定义
        return Flux.fromIterable(routeDefinitionMap.values());
    }
    
    @Override
    public Mono<Void> refreshRoutes() {
        // 发布路由刷新事件
        publisher.publishEvent(new RefreshRoutesEvent(this));
        log.info("手动刷新路由完成");
        return Mono.empty();
    }
} 