package com.bgpay.bgai.controller;

import com.bgpay.bgai.utils.ServiceDiscoveryUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 服务发现示例控制器，用于演示动态路由功能
 */
@Slf4j
@RestController
@RequestMapping("/api/service-discovery")
public class ServiceDiscoveryExampleController {

    private final ServiceDiscoveryUtils serviceDiscoveryUtils;
    private final WebClient loadBalancedWebClient;
    private final RestTemplate loadBalancedRestTemplate;
    private final RestTemplate restTemplate;

    /**
     * 构造函数注入，所有依赖都是可选的
     */
    @Autowired
    public ServiceDiscoveryExampleController(
            @Autowired(required = false) ServiceDiscoveryUtils serviceDiscoveryUtils,
            @Qualifier("loadBalancedWebClient") @Autowired(required = false) WebClient loadBalancedWebClient,
            @Qualifier("loadBalancedRestTemplate") @Autowired(required = false) RestTemplate loadBalancedRestTemplate,
            @Autowired(required = false) RestTemplate restTemplate) {
        this.serviceDiscoveryUtils = serviceDiscoveryUtils;
        this.loadBalancedWebClient = loadBalancedWebClient;
        this.loadBalancedRestTemplate = loadBalancedRestTemplate;
        this.restTemplate = restTemplate;
        
        // 记录组件可用性
        log.info("ServiceDiscoveryExampleController initialized with: " +
                "serviceDiscoveryUtils={}, " +
                "loadBalancedWebClient={}, " +
                "loadBalancedRestTemplate={}, " +
                "restTemplate={}",
                serviceDiscoveryUtils != null ? "available" : "not available",
                loadBalancedWebClient != null ? "available" : "not available",
                loadBalancedRestTemplate != null ? "available" : "not available",
                restTemplate != null ? "available" : "not available");
    }

    /**
     * 列出已注册的服务
     */
    @GetMapping("/services")
    public Map<String, Object> listServices() {
        Map<String, Object> result = new HashMap<>();
        
        if (serviceDiscoveryUtils == null) {
            result.put("status", "error");
            result.put("message", "ServiceDiscoveryUtils not available");
            return result;
        }
        
        try {
            if (!serviceDiscoveryUtils.isDiscoveryAvailable()) {
                result.put("status", "error");
                result.put("message", "DiscoveryClient not available");
                return result;
            }
            
            List<String> services = serviceDiscoveryUtils.getServiceInstances("user-service")
                    .stream()
                    .map(ServiceInstance::getServiceId)
                    .distinct()
                    .collect(Collectors.toList());
            
            result.put("status", "success");
            result.put("services", services);
            
            // 添加本地服务
            result.put("localServices", List.of("user-service", "bgtech-ai"));
        } catch (Exception e) {
            log.error("Error listing services", e);
            result.put("status", "error");
            result.put("message", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 使用WebClient调用指定服务
     */
    @GetMapping("/call-service/{serviceId}")
    public Mono<ResponseEntity<Map<String, Object>>> callService(
            @PathVariable String serviceId,
            @RequestParam(defaultValue = "/api/health") String path) {
        
        log.info("Calling service {} at path {}", serviceId, path);
        
        if (serviceDiscoveryUtils == null || loadBalancedWebClient == null) {
            log.warn("Required components not available: serviceDiscoveryUtils={}, loadBalancedWebClient={}",
                    serviceDiscoveryUtils != null, loadBalancedWebClient != null);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Required components not available in current environment");
            errorResponse.put("serviceId", serviceId);
            errorResponse.put("path", path);
            return Mono.just(ResponseEntity.status(500).body(errorResponse));
        }
        
        try {
            // 对于user-service，直接调用本地服务
            if ("user-service".equals(serviceId)) {
                return loadBalancedWebClient.method(HttpMethod.GET)
                        .uri("http://localhost:8688" + path)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .map(response -> {
                            Map<String, Object> result = new HashMap<>(response);
                            result.put("_serviceInfo", "Called local " + serviceId + " at " + path);
                            return ResponseEntity.ok(result);
                        })
                        .onErrorResume(e -> {
                            log.error("Error calling local service {} at {}", serviceId, path, e);
                            Map<String, Object> errorResponse = new HashMap<>();
                            errorResponse.put("error", e.getMessage());
                            errorResponse.put("serviceId", serviceId);
                            errorResponse.put("path", path);
                            return Mono.just(ResponseEntity.status(500).body(errorResponse));
                        });
            }
            
            return serviceDiscoveryUtils.callService(serviceId, path, HttpMethod.GET, null, Map.class)
                    .map(response -> {
                        Map<String, Object> result = new HashMap<>(response);
                        result.put("_serviceInfo", "Called " + serviceId + " at " + path);
                        return ResponseEntity.ok(result);
                    })
                    .onErrorResume(e -> {
                        log.error("Error calling service {} at {}", serviceId, path, e);
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("error", e.getMessage());
                        errorResponse.put("serviceId", serviceId);
                        errorResponse.put("path", path);
                        return Mono.just(ResponseEntity.status(500).body(errorResponse));
                    });
        } catch (Exception e) {
            log.error("Error setting up service call to {} at {}", serviceId, path, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("serviceId", serviceId);
            errorResponse.put("path", path);
            return Mono.just(ResponseEntity.status(500).body(errorResponse));
        }
    }
    
    /**
     * 使用RestTemplate调用指定服务
     */
    @GetMapping("/call-service-sync/{serviceId}")
    public ResponseEntity<Map<String, Object>> callServiceSync(
            @PathVariable String serviceId,
            @RequestParam(defaultValue = "/api/health") String path) {
        
        log.info("Synchronously calling service {} at path {}", serviceId, path);
        
        if (serviceDiscoveryUtils == null || loadBalancedRestTemplate == null) {
            log.warn("Required components not available: serviceDiscoveryUtils={}, loadBalancedRestTemplate={}",
                    serviceDiscoveryUtils != null, loadBalancedRestTemplate != null);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Required components not available in test environment");
            errorResponse.put("serviceId", serviceId);
            errorResponse.put("path", path);
            return ResponseEntity.status(500).body(errorResponse);
        }
        
        try {
            // 对于user-service，直接调用本地服务
            if ("user-service".equals(serviceId)) {
                String url = "http://localhost:8688" + path;
                Map<String, Object> response = restTemplate.getForObject(url, Map.class);
                
                Map<String, Object> result = new HashMap<>(response);
                result.put("_serviceInfo", "Called local " + serviceId + " at " + path + " synchronously");
                return ResponseEntity.ok(result);
            }
            
            Map<String, Object> response = serviceDiscoveryUtils.callServiceSync(
                    serviceId, path, HttpMethod.GET, null, Map.class);
            
            Map<String, Object> result = new HashMap<>(response);
            result.put("_serviceInfo", "Called " + serviceId + " at " + path + " synchronously");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error calling service {} at {}", serviceId, path, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("serviceId", serviceId);
            errorResponse.put("path", path);
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 获取服务URL信息
     */
    @GetMapping("/service-url/{serviceId}")
    public Map<String, Object> getServiceUrl(@PathVariable String serviceId) {
        Map<String, Object> result = new HashMap<>();
        
        if (serviceDiscoveryUtils == null) {
            result.put("status", "error");
            result.put("message", "ServiceDiscoveryUtils not available");
            return result;
        }
        
        try {
            // 对于user-service，直接返回本地URL
            if ("user-service".equals(serviceId)) {
                result.put("status", "success");
                result.put("serviceId", serviceId);
                result.put("url", "http://localhost:8688");
                result.put("type", "local");
                return result;
            }
            
            if (!serviceDiscoveryUtils.isLoadBalancerAvailable()) {
                result.put("status", "error");
                result.put("message", "LoadBalancerClient not available");
                return result;
            }
            
            String url = serviceDiscoveryUtils.buildServiceUrl(serviceId, "/");
            result.put("status", "success");
            result.put("serviceId", serviceId);
            result.put("url", url);
            
        } catch (Exception e) {
            log.error("Error getting service URL for {}", serviceId, e);
            result.put("status", "error");
            result.put("message", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 健康检查端点
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("components", Map.of(
            "serviceDiscoveryUtils", serviceDiscoveryUtils != null,
            "loadBalancedWebClient", loadBalancedWebClient != null,
            "loadBalancedRestTemplate", loadBalancedRestTemplate != null,
            "discoveryClient", serviceDiscoveryUtils != null && serviceDiscoveryUtils.isDiscoveryAvailable(),
            "loadBalancerClient", serviceDiscoveryUtils != null && serviceDiscoveryUtils.isLoadBalancerAvailable()
        ));
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
} 