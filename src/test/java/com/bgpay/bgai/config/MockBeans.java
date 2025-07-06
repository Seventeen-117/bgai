package com.bgpay.bgai.config;

import com.bgpay.bgai.entity.ApiKey;
import com.bgpay.bgai.entity.ApiKeyInfo;
import com.bgpay.bgai.entity.MimeTypeConfig;
import com.bgpay.bgai.entity.UserToken;
import com.bgpay.bgai.service.ApiKeyService;
import com.bgpay.bgai.service.DynamicRouteService;
import com.bgpay.bgai.service.UserService;
import com.bgpay.bgai.service.deepseek.FileProcessor;
import com.bgpay.bgai.service.deepseek.FileTypeService;
import com.bgpay.bgai.service.deepseek.FileWriterService;
import com.bgpay.bgai.service.deepseek.ReactiveFileProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Mock Beans配置类
 * 提供直接的Bean实例而不是使用Mockito.mock()
 */
@Configuration
public class MockBeans {
    private static final Logger log = LoggerFactory.getLogger(MockBeans.class);
    
    @Bean
    @Primary
    public ApiKeyService apiKeyService() {
        log.info("创建直接实现的ApiKeyService Bean");
        
        // 返回一个匿名内部类实现，而不是使用Mockito.mock()
        return new ApiKeyService() {
            @Override
            public ApiKeyInfo generateApiKey(String clientId, String clientName, String description) {
                return ApiKeyInfo.builder()
                    .apiKey(UUID.randomUUID().toString())
                    .clientId(clientId)
                    .clientName(clientName != null ? clientName : "Test Client")
                    .description(description)
                    .createdAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusYears(1))
                    .active(true)
                    .build();
            }
            
            @Override
            public void revokeApiKey(String apiKey) {
                // 空实现
            }
            
            @Override
            public ApiKeyValidationResult validateApiKeyStatus(String apiKey) {
                return new ApiKeyValidationResult(
                    ApiKeyStatus.VALID, 
                    LocalDateTime.now().plusYears(1), 
                    null,
                    "default-client"
                );
            }
            
            @Override
            public List<ApiKey> getAllApiKeys() {
                return Collections.emptyList();
            }
            
            @Override
            public ApiKey getApiKeyInfo(String apiKey) {
                return null;
            }
            
            @Override
            public void updateApiKeyStatus(String apiKey, boolean active) {
                // 空实现
            }
        };
    }
    
    @Bean
    @Primary
    public UserService userService() {
        log.info("创建直接实现的UserService Bean");
        
        return new UserService() {
            @Override
            public UserToken loginWithSSO(String code) {
                return null;
            }
            
            @Override
            public UserToken validateToken(String accessToken) {
                UserToken token = new UserToken();
                token.setUserId("test-user");
                token.setValid(true);
                token.setTokenExpireTime(LocalDateTime.now().plusDays(1));
                return token;
            }
            
            @Override
            public com.bgpay.bgai.entity.User getUserInfo(String userId) {
                return null;
            }
            
            @Override
            public UserToken refreshToken(String refreshToken) {
                return null;
            }
            
            @Override
            public UserToken refreshTokenByUserId(String userId) {
                return null;
            }
            
            @Override
            public void logout(String accessToken) {
                // 空实现
            }
        };
    }
    
    /**
     * 提供DynamicRouteService的Mock实现
     */
    @Bean
    @Primary
    public DynamicRouteService dynamicRouteService() {
        log.info("创建直接实现的DynamicRouteService Bean");
        
        // 用于存储模拟路由的Map
        Map<String, RouteDefinition> routes = new HashMap<>();
        
        return new DynamicRouteService() {
            @Override
            public Mono<Void> add(RouteDefinition route) {
                log.info("模拟添加路由: {}", route.getId());
                routes.put(route.getId(), route);
                return Mono.empty();
            }
            
            @Override
            public Mono<Void> update(RouteDefinition route) {
                log.info("模拟更新路由: {}", route.getId());
                routes.put(route.getId(), route);
                return Mono.empty();
            }
            
            @Override
            public Mono<Void> delete(String routeId) {
                log.info("模拟删除路由: {}", routeId);
                routes.remove(routeId);
                return Mono.empty();
            }
            
            @Override
            public Mono<RouteDefinition> getRoute(String routeId) {
                log.info("模拟获取路由: {}", routeId);
                return Mono.justOrEmpty(routes.get(routeId));
            }
            
            @Override
            public Flux<RouteDefinition> getRoutes() {
                log.info("模拟获取所有路由");
                return Flux.fromIterable(routes.values());
            }
            
            @Override
            public Mono<Void> refreshRoutes() {
                log.info("模拟刷新路由");
                return Mono.empty();
            }
        };
    }
    
    /**
     * 提供FileTypeService的Mock实现
     */
    @Bean
    @Primary
    public FileTypeService fileTypeService() {
        log.info("创建直接实现的FileTypeService Bean");
        
        return new FileTypeService(null, null) {
            private final Map<String, MimeTypeConfig> extensionToMimeTypeConfig = new HashMap<>();
            private final Set<String> allowedTypesCache = new HashSet<>();
            
            // 重写refreshCache方法，避免调用父类的实现导致NullPointerException
            @Override
            public void refreshCache() {
                log.info("模拟刷新文件类型缓存");
                // 不调用父类方法，避免NullPointerException
            }
            
            @Override
            public boolean isAllowedType(String contentType) {
                log.info("模拟文件类型检查: {}", contentType);
                // 默认允许所有文件类型
                return true;
            }
            
            @Override
            public boolean validateFileMagic(File file, String contentType) {
                log.info("模拟魔数验证: {} - {}", file.getName(), contentType);
                // 默认通过魔数验证
                return true;
            }
            
            @Override
            public Map<String, MimeTypeConfig> getExtensionToMimeTypeConfig() {
                return extensionToMimeTypeConfig;
            }
        };
    }
    
    /**
     * 提供FileProcessor的Mock实现
     */
    @Bean
    @Primary
    public FileProcessor fileProcessor(FileTypeService fileTypeService) {
        log.info("创建直接实现的FileProcessor Bean");
        
        return new FileProcessor(fileTypeService) {
            @Override
            public String processFile(MultipartFile file) throws Exception {
                log.info("模拟处理MultipartFile: {}", file.getOriginalFilename());
                return "模拟文件处理内容: " + file.getOriginalFilename();
            }
            
            @Override
            public String processFile(File file) throws Exception {
                log.info("模拟处理File: {}", file.getName());
                return "模拟文件处理内容: " + file.getName();
            }
        };
    }
    
    /**
     * 提供ReactiveFileProcessor的Mock实现
     */
    @Bean
    @Primary
    public ReactiveFileProcessor reactiveFileProcessor(FileProcessor fileProcessor) {
        log.info("创建直接实现的ReactiveFileProcessor Bean");
        
        return new ReactiveFileProcessor(fileProcessor);
    }
    
    /**
     * 提供FileWriterService的Mock实现
     */
    @Bean
    @Primary
    public FileWriterService fileWriterService() {
        log.info("创建直接实现的FileWriterService Bean");
        
        return new FileWriterService() {
            private final String testStoragePath = "./test-file-storage";
            
            @Override
            public String writeContentToFile(String content, String filename) throws IOException {
                log.info("模拟写入文件内容: {} -> {}", filename, content.length() + " bytes");
                // 返回模拟的文件路径
                Path filePath = Paths.get(testStoragePath, "test_" + System.currentTimeMillis() + "_" + filename);
                return filePath.toString();
            }
            
            @Override
            public File[] listFiles() {
                log.info("模拟列出文件");
                return new File[0];
            }
            
            @Override
            public boolean deleteFile(String filename) {
                log.info("模拟删除文件: {}", filename);
                return true;
            }
        };
    }
} 