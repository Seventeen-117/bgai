package com.bgpay.bgai.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class RouteRefreshEvent extends ApplicationEvent {
    private final String routeId;
    private final String operation; // ADD, UPDATE, DELETE

    public RouteRefreshEvent(Object source, String routeId, String operation) {
        super(source);
        this.routeId = routeId;
        this.operation = operation;
    }
} 