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
			
			// 禁用Seata Saga状态机自动注册，避免重复注册错误
			System.setProperty("seata.saga.state-machine.auto-register", "false");
			
			// 使用时间戳作为应用会话ID，帮助区分不同启动实例
			String appSessionId = String.valueOf(System.currentTimeMillis());
			System.setProperty("app.session.id", appSessionId);
			
			// 设置一个系统属性标记，表示使用动态版本号
			System.setProperty("saga.state-machine.dynamic-version", "true");
			
			// 禁用Micrometer Metrics，避免关闭时的bean创建错误
			System.setProperty("management.simple.metrics.export.enabled", "false");
			System.setProperty("management.metrics.enable.all", "false");
			
			// 设置Spring懒加载策略，减少启动时的类加载问题
			System.setProperty("spring.main.lazy-initialization", "false");
            
            // 允许循环引用
            System.setProperty("spring.main.allow-circular-references", "true");
            
            // 允许Bean覆盖
            System.setProperty("spring.main.allow-bean-definition-overriding", "true");
            
            // 配置Nacos客户端属性，确保连接正确关闭
            System.setProperty("nacos.client.naming.tls.enable", "false");
            System.setProperty("nacos.client.config.closeTimeoutSeconds", "5");
            System.setProperty("nacos.client.connect.timeout", "10000"); // 增加连接超时时间
            
            // 增加Nacos客户端重试和故障转移配置
            System.setProperty("nacos.client.naming.error.tolerance", "3");
            System.setProperty("nacos.client.naming.max.retry", "5");
            System.setProperty("nacos.client.naming.failover", "true");
            
            // 启用Nacos客户端本地缓存
            System.setProperty("nacos.client.naming.loadCacheAtStart", "true");
            
            // 设置为本地启动模式，不依赖远程Nacos
            System.setProperty("spring.cloud.nacos.config.import-check.enabled", "false");
            System.setProperty("spring.cloud.config.fail-fast", "false");
            
            // 确保gRPC通道正确关闭的设置
            System.setProperty("com.alibaba.nacos.client.grpc.registers.keepalive", "true");
            System.setProperty("com.alibaba.nacos.client.grpc.shutdown.await", "3000");
            System.setProperty("com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.transport.leakDetection.level", "DISABLED");
			
			logger.info("应用启动中，当前应用会话ID: {}", appSessionId);
			logger.info("已启用状态机动态版本号功能，避免版本冲突错误");
			
			// 注册JVM关闭钩子，确保资源正确释放
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			    try {
			        logger.info("执行应用关闭钩子，正在关闭资源...");
                } catch (Exception e) {
                    logger.error("关闭资源时出错", e);
                }
			}));
			
			SpringApplication.run(BgaiApplication.class, args);
			logger.info("Application started successfully");
		} catch (Exception e) {
			// 特别处理Seata相关异常
			if (e.getMessage() != null && e.getMessage().contains("seata_state_machine_def")) {
				logger.error("Seata状态机初始化失败，原因可能是状态机定义存在重复。" +
						"这通常是因为状态机已经注册过。" +
						"请检查file.conf确保saga.state-machine.auto-register=false", e);
				logger.error("如果问题依然存在，请尝试清理数据库中的状态机定义表或重启应用");
			} else {
				logger.error("应用启动失败", e);
			}
			System.exit(1);
		}
	}
}


