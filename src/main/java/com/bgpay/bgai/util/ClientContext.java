package com.bgpay.bgai.util;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class ClientContext {
    private static final String CLIENT_ID_ATTRIBUTE = "clientId";

    public static Mono<String> getCurrentClientId(ServerWebExchange exchange) {
        return Mono.justOrEmpty(exchange.getAttribute(CLIENT_ID_ATTRIBUTE));
    }

    public static void setCurrentClientId(ServerWebExchange exchange, String clientId) {
        exchange.getAttributes().put(CLIENT_ID_ATTRIBUTE, clientId);
    }
} 