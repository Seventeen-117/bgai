package com.bgpay.bgai.service;

import org.springframework.cloud.gateway.route.RouteDefinition;
import reactor.core.publisher.Mono;

public interface DynamicRouteService {
    Mono<Void> add(RouteDefinition route);
    Mono<Void> update(RouteDefinition route);
    Mono<Void> delete(String routeId);
    Mono<RouteDefinition> getRoute(String routeId);
} 