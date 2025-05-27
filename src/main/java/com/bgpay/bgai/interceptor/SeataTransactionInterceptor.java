package com.bgpay.bgai.interceptor;

import com.bgpay.bgai.service.TransactionLogService;
import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;
import io.seata.spring.annotation.GlobalTransactional;
import io.seata.tm.api.GlobalTransaction;
import io.seata.tm.api.GlobalTransactionContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * Seata分布式事务拦截器
 * 拦截GlobalTransactional注解的方法，记录事务信息
 */
@Aspect
@Component
@Slf4j
public class SeataTransactionInterceptor {

    @Autowired
    private TransactionLogService transactionLogService;

    private static final ThreadLocal<Map<String, Object>> TX_INFO = new ThreadLocal<>();

    @Around("@annotation(io.seata.spring.annotation.GlobalTransactional)")
    public Object aroundTransaction(ProceedingJoinPoint point) throws Throwable {
        String xid = RootContext.getXID();
        if (xid == null) {
            return point.proceed();
        }

        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        String requestPath = request != null ? request.getRequestURI() : "";
        String sourceIp = getLocalIp();
        String userId = "system"; // 这里可以根据实际情况获取用户ID

        // 记录事务开始
        Long transactionId = recordTransactionBegin(xid, point, requestPath, sourceIp, userId);
        
        // 保存事务ID到ThreadLocal
        Map<String, Object> txInfo = new HashMap<>();
        txInfo.put("transactionId", transactionId);
        txInfo.put("xid", xid);
        txInfo.put("startTime", System.currentTimeMillis());
        TX_INFO.set(txInfo);
        
        try {
            // 执行业务方法
            Object result = point.proceed();
            
            // 记录事务成功
            recordTransactionEnd(xid, "Committed", buildSuccessExtraData(result, transactionId));
            return result;
        } catch (Throwable e) {
            // 记录事务失败
            recordTransactionEnd(xid, "Rollbacked", buildFailureExtraData(e, transactionId));
            throw e;
        } finally {
            TX_INFO.remove();
        }
    }

    private Long recordTransactionBegin(String xid, ProceedingJoinPoint point, String requestPath, String sourceIp, String userId) {
        try {
            String transactionName = point.getSignature().getDeclaringTypeName() + "." + point.getSignature().getName();
            String transactionMode = BranchType.AT.name();
            
            return transactionLogService.recordTransactionBegin(
                xid, transactionName, transactionMode, requestPath, sourceIp, userId);
        } catch (Exception e) {
            log.error("Failed to record transaction begin: {}", e.getMessage(), e);
            return null;
        }
    }

    private void recordTransactionEnd(String xid, String status, String extraData) {
        try {
            // 获取当前事务的信息
            GlobalTransaction globalTransaction = GlobalTransactionContext.getCurrentOrCreate();
            if (globalTransaction != null) {
                // 更新事务状态
                transactionLogService.updateTransactionStatus(xid, status, extraData);
            }
            
            // 记录事务结束
            transactionLogService.recordTransactionEnd(xid, status, extraData);
        } catch (Exception e) {
            log.error("记录事务结束失败: {}", e.getMessage(), e);
        }
    }

    private String buildSuccessExtraData(Object result, Long transactionId) {
        Map<String, Object> txInfo = TX_INFO.get();
        if (txInfo == null) {
            txInfo = new HashMap<>();
        }

        Map<String, Object> extraData = new HashMap<>();
        extraData.put("transactionId", transactionId);
        extraData.put("startTime", txInfo.getOrDefault("startTime", System.currentTimeMillis()));
        extraData.put("endTime", System.currentTimeMillis());
        extraData.put("duration", System.currentTimeMillis() - (Long) txInfo.getOrDefault("startTime", System.currentTimeMillis()));
        extraData.put("result", "success");
        
        // 添加事务信息
        try {
            GlobalTransaction globalTransaction = GlobalTransactionContext.getCurrentOrCreate();
            if (globalTransaction != null) {
                extraData.put("xid", globalTransaction.getXid());
                extraData.put("status", globalTransaction.getStatus().name());
            }
        } catch (Exception e) {
            log.warn("获取事务信息失败", e);
        }
        
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(extraData);
        } catch (Exception e) {
            log.error("构建成功extraData失败", e);
            return "{}";
        }
    }

    private String buildFailureExtraData(Throwable e, Long transactionId) {
        Map<String, Object> txInfo = TX_INFO.get();
        if (txInfo == null) {
            txInfo = new HashMap<>();
        }

        Map<String, Object> extraData = new HashMap<>();
        extraData.put("transactionId", transactionId);
        extraData.put("startTime", txInfo.getOrDefault("startTime", System.currentTimeMillis()));
        extraData.put("endTime", System.currentTimeMillis());
        extraData.put("duration", System.currentTimeMillis() - (Long) txInfo.getOrDefault("startTime", System.currentTimeMillis()));
        extraData.put("result", "failure");
        extraData.put("errorMessage", e.getMessage());
        extraData.put("errorType", e.getClass().getName());
        
        // 添加事务信息
        try {
            GlobalTransaction globalTransaction = GlobalTransactionContext.getCurrentOrCreate();
            if (globalTransaction != null) {
                extraData.put("xid", globalTransaction.getXid());
                extraData.put("status", globalTransaction.getStatus().name());
            }
        } catch (Exception ex) {
            log.warn("获取事务信息失败", ex);
        }
        
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(extraData);
        } catch (Exception ex) {
            log.error("构建失败extraData失败", ex);
            return "{}";
        }
    }

    /**
     * 获取本地IP地址
     */
    private String getLocalIp() {
        try {
            java.net.InetAddress localHost = java.net.InetAddress.getLocalHost();
            String localIp = localHost.getHostAddress();
            if (localIp != null && !localIp.isEmpty() && !"127.0.0.1".equals(localIp)) {
                return localIp;
            }

            // 如果获取到的是本地回环地址，则遍历网卡获取第一个非回环地址
            java.util.Enumeration<java.net.NetworkInterface> networkInterfaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                java.net.NetworkInterface networkInterface = networkInterfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
                    continue;
                }
                java.util.Enumeration<java.net.InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress address = addresses.nextElement();
                    if (address instanceof java.net.Inet4Address && !address.isLoopbackAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
            log.warn("无法获取本地IP地址，将使用localhost");
            return "127.0.0.1";
        } catch (Exception e) {
            log.warn("获取本地IP失败: {}", e.getMessage());
            return "127.0.0.1";
        }
    }
} 