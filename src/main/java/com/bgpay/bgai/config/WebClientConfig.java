package com.bgpay.bgai.config;


import org.apache.http.HttpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;

import org.springframework.web.reactive.function.client.WebClient;


@Configuration
@ConditionalOnClass(WebClient.class)
public class WebClientConfig {

        @Bean
        public WebClient webClient(WebClient.Builder builder) {
            return builder
                    .baseUrl("https://api.deepseek.com/v1")
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();
        }
}