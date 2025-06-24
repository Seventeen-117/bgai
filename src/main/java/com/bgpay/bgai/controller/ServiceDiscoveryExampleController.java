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

    @Autowired
    private ServiceDiscoveryUtils serviceDiscoveryUtils;
    
    @Autowired
    @Qualifier("loadBalancedWebClient")
    private WebClient loadBalancedWebClient;
    
    @Autowired
    @Qualifier("loadBalancedRestTemplate")
    private RestTemplate loadBalancedRestTemplate;

    /**
     * 列出已注册的服务
     */
    @GetMapping("/services")
    public Map<String, Object> listServices() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 这里仅使用示例服务ID，实际应用中应该从DiscoveryClient获取
            List<String> sampleServiceIds = List.of("bgtech-ai", "bgtech-gateway", "bgtech-auth");
            
            // 获取每个服务的实例信息
            Map<String, List<Map<String, Object>>> services = new HashMap<>();
            for (String serviceId : sampleServiceIds) {
                List<ServiceInstance> instances = serviceDiscoveryUtils.getServiceInstances(serviceId);
                
                // 将服务实例信息转换为简单的Map结构
                List<Map<String, Object>> instanceList = instances.stream()
                        .map(instance -> {
                            Map<String, Object> instanceMap = new HashMap<>();
                            instanceMap.put("serviceId", instance.getServiceId());
                            instanceMap.put("host", instance.getHost());
                            instanceMap.put("port", instance.getPort());
                            instanceMap.put("uri", instance.getUri().toString());
                            instanceMap.put("metadata", instance.getMetadata());
                            return instanceMap;
                        })
                        .collect(Collectors.toList());
                
                services.put(serviceId, instanceList);
            }
            
            result.put("services", services);
            result.put("status", "success");
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
    }
    
    /**
     * 使用RestTemplate同步调用指定服务
     */
    @GetMapping("/call-service-sync/{serviceId}")
    public ResponseEntity<Map<String, Object>> callServiceSync(
            @PathVariable String serviceId,
            @RequestParam(defaultValue = "/api/health") String path) {
        
        log.info("Synchronously calling service {} at path {}", serviceId, path);
        
        try {
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
        
        try {
            String url = serviceDiscoveryUtils.buildServiceUrl(serviceId, "/");
            result.put("serviceId", serviceId);
            result.put("url", url);
            result.put("status", "success");
        } catch (Exception e) {
            log.error("Error getting service URL for {}", serviceId, e);
            result.put("status", "error");
            result.put("message", e.getMessage());
        }
        
        return result;
    }
} 