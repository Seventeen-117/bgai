package com.bgpay.bgai.service.impl;

import com.bgpay.bgai.event.RouteRefreshEvent;
import com.bgpay.bgai.service.DynamicRouteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class DynamicRouteServiceImpl implements DynamicRouteService, ApplicationEventPublisherAware {

    @Autowired
    private RouteDefinitionWriter routeDefinitionWriter;

    private ApplicationEventPublisher publisher;

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.publisher = applicationEventPublisher;
    }

    @Override
    public Mono<Void> add(RouteDefinition route) {
        try {
            routeDefinitionWriter.save(Mono.just(route)).subscribe();
            publisher.publishEvent(new RouteRefreshEvent(this, route.getId(), "ADD"));
            return Mono.empty();
        } catch (Exception e) {
            log.error("Add route error: {}", e.getMessage());
            return Mono.error(e);
        }
    }

    @Override
    public Mono<Void> update(RouteDefinition route) {
        try {
            routeDefinitionWriter.delete(Mono.just(route.getId())).subscribe();
            routeDefinitionWriter.save(Mono.just(route)).subscribe();
            publisher.publishEvent(new RouteRefreshEvent(this, route.getId(), "UPDATE"));
            return Mono.empty();
        } catch (Exception e) {
            log.error("Update route error: {}", e.getMessage());
            return Mono.error(e);
        }
    }

    @Override
    public Mono<Void> delete(String routeId) {
        try {
            routeDefinitionWriter.delete(Mono.just(routeId)).subscribe();
            publisher.publishEvent(new RouteRefreshEvent(this, routeId, "DELETE"));
            return Mono.empty();
        } catch (Exception e) {
            log.error("Delete route error: {}", e.getMessage());
            return Mono.error(e);
        }
    }

    @Override
    public Mono<RouteDefinition> getRoute(String routeId) {
        // 这里需要实现从存储中获取路由定义的逻辑
        // 可以使用Redis、数据库等存储
        return Mono.empty();
    }
} 