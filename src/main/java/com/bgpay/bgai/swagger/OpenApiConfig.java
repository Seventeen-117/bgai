package com.bgpay.bgai.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import io.swagger.v3.oas.models.ExternalDocumentation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * OpenAPI 3.0 配置类
 * 用于配置Swagger 3 (OpenAPI)文档信息
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:bgtech-ai}")
    private String applicationName;

    @Value("${springdoc.swagger-ui.path:/swagger-ui.html}")
    private String swaggerPath;

    @Value("${server.port:8688}")
    private String serverPort;
    
    /**
     * 配置OpenAPI基本信息
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .externalDocs(externalDocs())
                .servers(servers())
                .tags(tags())
                .components(securityComponents());
    }
    
    /**
     * API基本信息
     */
    private Info apiInfo() {
        return new Info()
                .title(applicationName + " RESTful API文档")
                .description("这是" + applicationName + "服务的API文档，基于OpenAPI 3.0规范")
                .version("1.0.0")
                .contact(new Contact()
                        .name("BGTech AI团队")
                        .email("support@bgtech.com")
                        .url("https://www.bgtech.com"))
                .license(new License()
                        .name("Apache License 2.0")
                        .url("https://www.apache.org/licenses/LICENSE-2.0"));
    }
    
    /**
     * 外部文档
     */
    private ExternalDocumentation externalDocs() {
        return new ExternalDocumentation()
                .description("项目Wiki文档")
                .url("https://wiki.bgtech.com/ai-platform");
    }
    
    /**
     * 配置服务器信息
     */
    private List<Server> servers() {
        Server localServer = new Server()
                .url("http://localhost:" + serverPort)
                .description("本地开发环境");
                
        Server devServer = new Server()
                .url("https://dev-api.bgtech.com")
                .description("开发环境");
                
        Server testServer = new Server()
                .url("https://test-api.bgtech.com")
                .description("测试环境");
                
        Server prodServer = new Server()
                .url("https://api.bgtech.com")
                .description("生产环境");
                
        return Arrays.asList(localServer, devServer, testServer, prodServer);
    }
    
    /**
     * API标签分组
     */
    private List<Tag> tags() {
        Tag authTag = new Tag().name("认证授权").description("用户认证与授权相关接口");
        Tag chatTag = new Tag().name("聊天服务").description("AI聊天、对话相关接口");
        Tag fileTag = new Tag().name("文件处理").description("文件上传、解析相关接口");
        Tag apiKeyTag = new Tag().name("API密钥").description("API密钥管理相关接口");
        Tag usageTag = new Tag().name("用量统计").description("用量查询与计费相关接口");
        Tag systemTag = new Tag().name("系统管理").description("系统配置、路由等管理接口");
        
        return Arrays.asList(authTag, chatTag, fileTag, apiKeyTag, usageTag, systemTag);
    }
    
    /**
     * 安全配置组件
     */
    private Components securityComponents() {
        return new Components()
                .addSecuritySchemes("BearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT认证令牌，格式: Bearer [token]"))
                .addSecuritySchemes("ApiKeyAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-API-Key")
                        .description("API密钥认证"));
    }
} 