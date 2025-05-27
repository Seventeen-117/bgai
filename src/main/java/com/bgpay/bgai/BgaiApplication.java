package com.bgpay.bgai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {
		DataSourceAutoConfiguration.class,
		DataSourceTransactionManagerAutoConfiguration.class,
		JdbcTemplateAutoConfiguration.class,
		ThymeleafAutoConfiguration.class
})
@EnableScheduling
@Configuration
@EnableDiscoveryClient
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableCaching
@ComponentScan(basePackages = {
		"org.apache.rocketmq.spring.autoconfigure",
		"org.apache.rocketmq.spring.core",
		"org.apache.rocketmq.spring.support",
		"com.bgpay.bgai"
})
public class BgaiApplication {

	private static final Logger logger = LoggerFactory.getLogger(BgaiApplication.class);

	public static void main(String[] args) {
		try {
			SpringApplication.run(BgaiApplication.class, args);
		} catch (Exception e) {
			logger.error("Application startup failed", e);
			throw e;
		}
	}
}


