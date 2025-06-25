package com.bgpay.bgai.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.bgpay.bgai.config.RestTemplateConfig.ServiceUrlResolver;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务发现工具类，提供动态路由能力
 * 用于服务间通信，支持同步和响应式两种方式
 */
@Slf4j
@Component
public class ServiceDiscoveryUtils {

    private final Map<String, Long> serviceLastRefreshTime = new ConcurrentHashMap<>();
    private final Map<String, List<ServiceInstance>> serviceInstancesCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 30_000; // 30秒缓存有效期

    @Autowired(required = false)
    private DiscoveryClient discoveryClient;

    @Autowired(required = false)
    private LoadBalancerClient loadBalancerClient;

    @Autowired(required = false)
    @Qualifier("loadBalancedWebClient")
    private WebClient loadBalancedWebClient;

    @Autowired(required = false)
    @Qualifier("loadBalancedRestTemplate")
    private RestTemplate loadBalancedRestTemplate;
    
    @Autowired(required = false)
    private ServiceUrlResolver serviceUrlResolver;
    
    /**
     * 检查服务发现组件是否可用
     */
    public boolean isDiscoveryAvailable() {
        return discoveryClient != null;
    }
    
    /**
     * 检查负载均衡组件是否可用
     */
    public boolean isLoadBalancerAvailable() {
        return loadBalancerClient != null;
    }

    /**
     * 获取服务实例列表，支持缓存
     *
     * @param serviceId 服务ID
     * @return 服务实例列表
     */
    public List<ServiceInstance> getServiceInstances(String serviceId) {
        if (discoveryClient == null) {
            log.warn("DiscoveryClient is not available, returning empty instance list for service: {}", serviceId);
            return List.of();
        }
        
        long now = System.currentTimeMillis();
        Long lastRefresh = serviceLastRefreshTime.getOrDefault(serviceId, 0L);

        if (now - lastRefresh > CACHE_TTL_MS || !serviceInstancesCache.containsKey(serviceId)) {
            List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
            if (!instances.isEmpty()) {
                serviceInstancesCache.put(serviceId, instances);
                serviceLastRefreshTime.put(serviceId, now);
            }
        }

        return serviceInstancesCache.getOrDefault(serviceId, List.of());
    }

    /**
     * 获取服务实例，使用负载均衡
     *
     * @param serviceId 服务ID
     * @return 服务实例Optional
     */
    public Optional<ServiceInstance> getServiceInstance(String serviceId) {
        if (loadBalancerClient == null) {
            log.warn("LoadBalancerClient is not available for service: {}", serviceId);
            return Optional.empty();
        }
        
        try {
            ServiceInstance instance = loadBalancerClient.choose(serviceId);
            return Optional.ofNullable(instance);
        } catch (Exception e) {
            log.error("Error getting service instance for {}: {}", serviceId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 构建服务URL
     *
     * @param serviceId 服务ID
     * @param path 请求路径
     * @return 完整URL
     */
    public String buildServiceUrl(String serviceId, String path) {
        // 如果是本地服务，直接返回本地URL
        if ("user-service".equals(serviceId)) {
            return "http://localhost:8688" + (path.startsWith("/") ? path : "/" + path);
        }
        
        return getServiceInstance(serviceId)
                .map(instance -> UriComponentsBuilder.fromUri(instance.getUri())
                        .path(path.startsWith("/") ? path : "/" + path)
                        .build()
                        .toUriString())
                .orElseGet(() -> {
                    log.warn("No instances available for service: {}, using fallback URL", serviceId);
                    return "http://" + serviceId + (path.startsWith("/") ? path : "/" + path);
                });
    }

    /**
     * 使用WebClient调用服务 (响应式)
     *
     * @param serviceId 服务ID
     * @param path 请求路径
     * @param method HTTP方法
     * @param requestBody 请求体
     * @param responseType 响应类型
     * @return 响应Mono
     */
    public <T, R> Mono<R> callService(String serviceId, String path, HttpMethod method, 
                                     T requestBody, Class<R> responseType) {
        if (loadBalancedWebClient == null) {
            log.error("LoadBalancedWebClient is not available for service call: {} {}", serviceId, path);
            return Mono.error(new IllegalStateException("LoadBalancedWebClient not available"));
        }
        
        // 对于本地测试服务，使用直接URL
        String url;
        if ("user-service".equals(serviceId)) {
            url = "http://localhost:8688" + (path.startsWith("/") ? path : "/" + path);
        } else {
            url = "lb://" + serviceId + (path.startsWith("/") ? path : "/" + path);
        }
        
        WebClient.RequestBodySpec requestSpec = loadBalancedWebClient.method(method)
                .uri(url);
                
        WebClient.RequestHeadersSpec<?> headersSpec;
        if (requestBody != null && 
            (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH)) {
            headersSpec = requestSpec.bodyValue(requestBody);
        } else {
            headersSpec = requestSpec;
        }
        
        return headersSpec.retrieve()
                .bodyToMono(responseType)
                .doOnError(e -> log.error("Error calling service {} at {}: {}", 
                        serviceId, path, e.getMessage()));
    }

    /**
     * 使用RestTemplate调用服务 (同步)
     *
     * @param serviceId 服务ID
     * @param path 请求路径
     * @param method HTTP方法
     * @param requestBody 请求体
     * @param responseType 响应类型
     * @return 响应对象
     */
    public <T, R> R callServiceSync(String serviceId, String path, HttpMethod method, 
                                  T requestBody, Class<R> responseType) {
        if (loadBalancedRestTemplate == null) {
            log.error("LoadBalancedRestTemplate is not available for service call: {} {}", serviceId, path);
            throw new IllegalStateException("LoadBalancedRestTemplate not available");
        }
        
        // 对于本地测试服务，使用直接URL
        String url;
        if ("user-service".equals(serviceId)) {
            url = "http://localhost:8688" + (path.startsWith("/") ? path : "/" + path);
        } else {
            url = "http://" + serviceId + (path.startsWith("/") ? path : "/" + path);
        }
        
        try {
            if (HttpMethod.GET.equals(method)) {
                return loadBalancedRestTemplate.getForObject(url, responseType);
            } else if (HttpMethod.POST.equals(method)) {
                return loadBalancedRestTemplate.postForObject(url, requestBody, responseType);
            } else if (HttpMethod.PUT.equals(method)) {
                loadBalancedRestTemplate.put(url, requestBody);
                return null;
            } else if (HttpMethod.DELETE.equals(method)) {
                loadBalancedRestTemplate.delete(url);
                return null;
            } else {
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
            }
        } catch (Exception e) {
            log.error("Error calling service {} at {}: {}", serviceId, path, e.getMessage());
            throw e;
        }
    }
    
    /**
     * 使用direct URL调用服务 (同步)
     * 先通过服务发现找到服务实例，然后直接使用实例URL
     *
     * @param serviceId 服务ID
     * @param path 请求路径
     * @param method HTTP方法
     * @param requestBody 请求体
     * @param responseType 响应类型
     * @return 响应对象
     */
    public <T, R> R callServiceDirectSync(String serviceId, String path, HttpMethod method, 
                                        T requestBody, Class<R> responseType) {
        if (loadBalancedRestTemplate == null) {
            log.error("LoadBalancedRestTemplate is not available for direct service call: {} {}", serviceId, path);
            throw new IllegalStateException("LoadBalancedRestTemplate not available");
        }
        
        String url;
        
        // 对于本地测试服务，使用直接URL
        if ("user-service".equals(serviceId)) {
            url = "http://localhost:8688" + (path.startsWith("/") ? path : "/" + path);
        } else if (serviceUrlResolver != null) {
            url = serviceUrlResolver.buildUrl(serviceId, path);
        } else {
            url = "http://" + serviceId + (path.startsWith("/") ? path : "/" + path);
            log.warn("ServiceUrlResolver is not available, using fallback URL: {}", url);
        }
        
        try {
            if (HttpMethod.GET.equals(method)) {
                return loadBalancedRestTemplate.getForObject(url, responseType);
            } else if (HttpMethod.POST.equals(method)) {
                return loadBalancedRestTemplate.postForObject(url, requestBody, responseType);
            } else if (HttpMethod.PUT.equals(method)) {
                loadBalancedRestTemplate.put(url, requestBody);
                return null;
            } else if (HttpMethod.DELETE.equals(method)) {
                loadBalancedRestTemplate.delete(url);
                return null;
            } else {
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
            }
        } catch (Exception e) {
            log.error("Error calling service directly {} at {}: {}", serviceId, path, e.getMessage());
            throw e;
        }
    }
} 