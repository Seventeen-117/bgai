package com.bgpay.bgai.controller;

import com.bgpay.bgai.service.DynamicRouteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/gateway/routes")
public class DynamicRouteController {

    @Autowired
    private DynamicRouteService dynamicRouteService;

    @PostMapping
    public Mono<Void> add(@RequestBody RouteDefinition route) {
        log.info("Adding route: {}", route);
        return dynamicRouteService.add(route);
    }

    @PutMapping
    public Mono<Void> update(@RequestBody RouteDefinition route) {
        log.info("Updating route: {}", route);
        return dynamicRouteService.update(route);
    }

    @DeleteMapping("/{routeId}")
    public Mono<Void> delete(@PathVariable String routeId) {
        log.info("Deleting route: {}", routeId);
        return dynamicRouteService.delete(routeId);
    }

    @GetMapping("/{routeId}")
    public Mono<RouteDefinition> getRoute(@PathVariable String routeId) {
        log.info("Getting route: {}", routeId);
        return dynamicRouteService.getRoute(routeId);
    }
} 