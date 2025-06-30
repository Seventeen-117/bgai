package com.bgpay.bgai.controller;

import com.bgpay.bgai.entity.ApiClient;
import com.bgpay.bgai.entity.ApiConfig;
import com.bgpay.bgai.mapper.ApiClientMapper;
import com.bgpay.bgai.service.ApiConfigService;
import com.bgpay.bgai.service.DynamicRouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

/**
 * 系统配置控制器
 * 提供系统配置、API配置和路由管理功能
 */
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "系统管理", description = "系统配置、路由等管理接口")
public class SystemConfigController {

    private final ApiConfigService apiConfigService;
    private final DynamicRouteService dynamicRouteService;
    private final ApiClientMapper apiClientMapper;

    /**
     * 获取所有API配置
     */
    @Operation(summary = "获取所有API配置", description = "返回系统中所有的API配置信息")
    @GetMapping("/api-configs")
    public ResponseEntity<List<ApiConfig>> getAllApiConfigs() {
        List<ApiConfig> configs = apiConfigService.getAllApiConfigs();
        return ResponseEntity.ok(configs);
    }

    /**
     * 根据ID获取API配置
     */
    @Operation(summary = "获取API配置详情", description = "根据ID获取特定API配置的详细信息")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取配置"),
        @ApiResponse(responseCode = "404", description = "配置不存在")
    })
    @GetMapping("/api-configs/{id}")
    public ResponseEntity<ApiConfig> getApiConfigById(
            @Parameter(description = "API配置ID") @PathVariable Long id) {
        ApiConfig config = apiConfigService.getApiConfigById(id);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(config);
    }

    /**
     * 创建新的API配置
     */
    @Operation(summary = "创建API配置", description = "创建新的API配置")
    @PostMapping("/api-configs")
    public ResponseEntity<ApiConfig> createApiConfig(
            @Parameter(description = "API配置信息") @RequestBody ApiConfig apiConfig) {
        ApiConfig created = apiConfigService.createApiConfig(apiConfig);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * 更新API配置
     */
    @Operation(summary = "更新API配置", description = "更新现有的API配置")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "404", description = "配置不存在")
    })
    @PutMapping("/api-configs/{id}")
    public ResponseEntity<ApiConfig> updateApiConfig(
            @Parameter(description = "API配置ID") @PathVariable Long id,
            @Parameter(description = "API配置信息") @RequestBody ApiConfig apiConfig) {
        apiConfig.setId(id);
        ApiConfig updated = apiConfigService.updateApiConfig(apiConfig);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    /**
     * 删除API配置
     */
    @Operation(summary = "删除API配置", description = "删除指定ID的API配置")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "删除成功"),
        @ApiResponse(responseCode = "404", description = "配置不存在")
    })
    @DeleteMapping("/api-configs/{id}")
    public ResponseEntity<Void> deleteApiConfig(
            @Parameter(description = "API配置ID") @PathVariable Long id) {
        boolean deleted = apiConfigService.deleteApiConfig(id);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * 添加路由
     */
    @Operation(summary = "添加路由", description = "添加新的路由配置")
    @PostMapping("/routes")
    public Mono<ResponseEntity<Void>> addRoute(
            @Parameter(description = "路由定义") @RequestBody RouteDefinition route) {
        return dynamicRouteService.add(route)
                .then(Mono.just(new ResponseEntity<Void>(HttpStatus.CREATED)))
                .onErrorResume(e -> {
                    log.error("添加路由失败: {}", e.getMessage());
                    return Mono.just(new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR));
                });
    }

    /**
     * 更新路由
     */
    @Operation(summary = "更新路由", description = "更新现有的路由配置")
    @PutMapping("/routes")
    public Mono<ResponseEntity<Void>> updateRoute(
            @Parameter(description = "路由定义") @RequestBody RouteDefinition route) {
        return dynamicRouteService.update(route)
                .then(Mono.just(new ResponseEntity<Void>(HttpStatus.OK)))
                .onErrorResume(e -> {
                    log.error("更新路由失败: {}", e.getMessage());
                    return Mono.just(new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR));
                });
    }

    /**
     * 删除路由
     */
    @Operation(summary = "删除路由", description = "删除指定ID的路由")
    @DeleteMapping("/routes/{routeId}")
    public Mono<ResponseEntity<Void>> deleteRoute(
            @Parameter(description = "路由ID") @PathVariable String routeId) {
        return dynamicRouteService.delete(routeId)
                .then(Mono.just(new ResponseEntity<Void>(HttpStatus.NO_CONTENT)))
                .onErrorResume(e -> {
                    log.error("删除路由失败: {}", e.getMessage());
                    return Mono.just(new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR));
                });
    }

    /**
     * 获取路由
     */
    @Operation(summary = "获取路由详情", description = "获取指定ID的路由详细信息")
    @GetMapping("/routes/{routeId}")
    public Mono<ResponseEntity<RouteDefinition>> getRoute(
            @Parameter(description = "路由ID") @PathVariable String routeId) {
        return dynamicRouteService.getRoute(routeId)
                .map(route -> new ResponseEntity<>(route, HttpStatus.OK))
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND))
                .onErrorResume(e -> {
                    log.error("获取路由失败: {}", e.getMessage());
                    return Mono.just(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
                });
    }

    /**
     * 获取系统状态
     */
    @Operation(summary = "获取系统状态", description = "获取当前系统运行状态信息")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        // 这里可以添加系统状态信息，如内存使用、CPU使用、在线用户数等
        Map<String, Object> status = Map.of(
                "status", "running",
                "version", "1.0.0",
                "memoryUsage", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(),
                "totalMemory", Runtime.getRuntime().totalMemory(),
                "availableProcessors", Runtime.getRuntime().availableProcessors()
        );
        return ResponseEntity.ok(status);
    }

    @Operation(summary = "初始化API客户端", description = "在数据库中初始化默认的API客户端")
    @PostMapping("/init-api-client")
    public ResponseEntity<?> initApiClient() {
        try {
            // 检查是否已存在默认客户端
            ApiClient existingClient = apiClientMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ApiClient>()
                    .eq("client_id", "default-client")
            );

            if (existingClient != null) {
                log.info("默认API客户端已存在，无需初始化");
                return ResponseEntity.ok(Map.of("message", "默认API客户端已存在", "clientId", existingClient.getClientId()));
            }

            // 创建默认客户端
            ApiClient defaultClient = new ApiClient();
            defaultClient.setClientId("default-client");
            defaultClient.setClientName("Default Client");
            defaultClient.setDescription("Default API client for system usage");
            defaultClient.setStatus(1);
            defaultClient.setCreateTime(LocalDateTime.now());
            defaultClient.setUpdateTime(LocalDateTime.now());
            
            apiClientMapper.insert(defaultClient);
            log.info("成功初始化默认API客户端: {}", defaultClient.getClientId());
            
            return ResponseEntity.ok(Map.of(
                "message", "成功初始化默认API客户端", 
                "clientId", defaultClient.getClientId()
            ));
        } catch (Exception e) {
            log.error("初始化API客户端失败", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
} 