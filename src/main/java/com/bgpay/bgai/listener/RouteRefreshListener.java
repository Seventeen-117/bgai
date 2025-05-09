package com.bgpay.bgai.listener;

import com.bgpay.bgai.event.RouteRefreshEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RouteRefreshListener implements ApplicationEventPublisherAware {

    private ApplicationEventPublisher publisher;

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.publisher = applicationEventPublisher;
    }

    @EventListener
    public void onRouteRefreshEvent(RouteRefreshEvent event) {
        log.info("Received route refresh event: routeId={}, operation={}", 
                event.getRouteId(), event.getOperation());
        
        // 发布Spring Cloud Gateway的路由刷新事件
        publisher.publishEvent(new RefreshRoutesEvent(this));
        
        log.info("Published refresh routes event for route: {}", event.getRouteId());
    }
} 