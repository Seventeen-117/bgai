package com.bgpay.bgai.config;

import com.bgpay.bgai.service.DynamicRouteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Mock控制器配置类
 * 提供测试所需的各种Mock控制器
 */
@Configuration
public class MockControllers {
    private static final Logger log = LoggerFactory.getLogger(MockControllers.class);
    
    /**
     * 提供Mock的DynamicRouteController
     * 这个Bean会替代真实的DynamicRouteController，避免自动装配失败
     */
    @Bean
    @Primary
    public Object mockDynamicRouteController(DynamicRouteService dynamicRouteService) {
        log.info("创建Mock DynamicRouteController");
        
        // 返回一个匿名类实现简单的路由控制器功能
        // 注意：此处不能使用@RestController注解，因为这是一个Bean方法
        return new DynamicRouteMockController(dynamicRouteService);
    }
    
    /**
     * 动态路由控制器的Mock实现
     */
    @RestController
    @RequestMapping("/api/routes")
    public static class DynamicRouteMockController {
        private final DynamicRouteService dynamicRouteService;
        
        public DynamicRouteMockController(DynamicRouteService dynamicRouteService) {
            this.dynamicRouteService = dynamicRouteService;
        }
        
        @PostMapping
        public Mono<ResponseEntity<Void>> addRoute(@RequestBody RouteDefinition route) {
            return dynamicRouteService.add(route)
                .then(Mono.just(ResponseEntity.ok().build()));
        }
        
        @PutMapping
        public Mono<ResponseEntity<Void>> updateRoute(@RequestBody RouteDefinition route) {
            return dynamicRouteService.update(route)
                .then(Mono.just(ResponseEntity.ok().build()));
        }
        
        @DeleteMapping("/{routeId}")
        public Mono<ResponseEntity<Void>> deleteRoute(@PathVariable String routeId) {
            return dynamicRouteService.delete(routeId)
                .then(Mono.just(ResponseEntity.ok().build()));
        }
        
        @GetMapping("/{routeId}")
        public Mono<ResponseEntity<RouteDefinition>> getRoute(@PathVariable String routeId) {
            return dynamicRouteService.getRoute(routeId)
                .map(route -> ResponseEntity.ok().body(route))
                .defaultIfEmpty(ResponseEntity.notFound().build());
        }
        
        @GetMapping
        public Flux<RouteDefinition> getRoutes() {
            return dynamicRouteService.getRoutes();
        }
        
        @PostMapping("/refresh")
        public Mono<ResponseEntity<Void>> refreshRoutes() {
            return dynamicRouteService.refreshRoutes()
                .then(Mono.just(ResponseEntity.ok().build()));
        }
    }
} 