package com.bgpay.bgai.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.client.reactive.ReactorResourceFactory;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {
    @Bean
    public WebClient webClient(
            @Value("${deepseek.api.timeout:120000}") long timeout,
            ReactorResourceFactory resourceFactory) {

        HttpClient httpClient = HttpClient.create(resourceFactory.getConnectionProvider())
                .responseTimeout(Duration.ofMillis(timeout))
                .compress(true);

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl("https://api.deepseek.com/v1")
                .build();
    }
}