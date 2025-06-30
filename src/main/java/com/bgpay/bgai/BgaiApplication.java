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
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.retry.annotation.EnableRetry;
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
@EnableFeignClients(basePackages = "com.bgpay.bgai.feign")
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableCaching
@EnableRetry
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
			// 如果需要禁用Seata，可以通过系统属性
			// System.setProperty("seata.enabled", "false");
			// System.setProperty("saga.enabled", "false");
			
			SpringApplication.run(BgaiApplication.class, args);
			logger.info("Application started successfully");
		} catch (Exception e) {
			// 特别处理Seata相关异常
			if (e.getMessage() != null && e.getMessage().contains("seata_state_machine_def")) {
				logger.error("Seata state machine initialization failed due to duplicate entries. " +
						"This is likely because state machines were already registered. " +
						"Check file.conf to ensure saga.state-machine.auto-register=false", e);
			} else {
				logger.error("Application startup failed", e);
			}
			System.exit(1);
		}
	}
}


