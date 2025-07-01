package com.bgpay.bgai.service;

import org.springframework.cloud.gateway.route.RouteDefinition;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 动态路由服务接口
 * 提供路由的增删改查功能
 */
public interface DynamicRouteService {
    
    /**
     * 添加路由
     * @param route 路由定义
     * @return 操作结果
     */
    Mono<Void> add(RouteDefinition route);
    
    /**
     * 更新路由
     * @param route 路由定义
     * @return 操作结果
     */
    Mono<Void> update(RouteDefinition route);
    
    /**
     * 删除路由
     * @param routeId 路由ID
     * @return 操作结果
     */
    Mono<Void> delete(String routeId);
    
    /**
     * 获取指定路由
     * @param routeId 路由ID
     * @return 路由定义
     */
    Mono<RouteDefinition> getRoute(String routeId);
    
    /**
     * 获取所有路由
     * @return 所有路由定义
     */
    Flux<RouteDefinition> getRoutes();
    
    /**
     * 刷新路由
     * 强制刷新Gateway路由配置
     * @return 操作结果
     */
    Mono<Void> refreshRoutes();
} 