package com.bgpay.bgai.interceptor;

import com.bgpay.bgai.service.TransactionLogService;
import io.seata.core.context.RootContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

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

    /**
     * 定义切点 - 拦截GlobalTransactional注解的方法
     */
    @Pointcut("@annotation(io.seata.spring.annotation.GlobalTransactional)")
    public void seataTransactionalMethod() {
    }

    /**
     * 事务开始前
     */
    @Before("seataTransactionalMethod()")
    public void beforeTransaction(JoinPoint point) {
        // 获取全局事务XID
        String xid = RootContext.getXID();
        if (xid == null) {
            log.debug("未找到全局事务XID，可能事务尚未开始");
            return;
        }

        // 获取方法信息
        String methodName = point.getSignature().getName();
        String className = point.getTarget().getClass().getName();
        String transactionName = className + "." + methodName;

        // 获取请求信息
        String requestPath = "";
        String sourceIp = "";
        String userId = "";

        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                requestPath = request.getRequestURI();
                sourceIp = getClientIp(request);
                // 从请求或会话中获取用户ID
                userId = getUserId(request);
            }
        } catch (Exception e) {
            log.warn("获取请求信息失败", e);
        }

        // 记录事务开始
        Long logId = transactionLogService.recordTransactionBegin(
                xid, transactionName, "AT", requestPath, sourceIp, userId);

        // 在线程本地变量中保存信息，用于后续处理
        Map<String, Object> txInfo = new HashMap<>();
        txInfo.put("xid", xid);
        txInfo.put("logId", logId);
        txInfo.put("startTime", System.currentTimeMillis());
        TX_INFO.set(txInfo);

        log.info("开始分布式事务: XID={}, 方法={}", xid, transactionName);
    }

    /**
     * 事务成功完成
     */
    @AfterReturning("seataTransactionalMethod()")
    public void afterReturning() {
        Map<String, Object> txInfo = TX_INFO.get();
        if (txInfo == null || !txInfo.containsKey("xid")) {
            return;
        }

        String xid = (String) txInfo.get("xid");
        long startTime = (long) txInfo.get("startTime");
        long duration = System.currentTimeMillis() - startTime;

        String extraData = String.format("{\"duration\":%d,\"result\":\"success\"}", duration);
        transactionLogService.recordTransactionEnd(xid, "COMMITTED", extraData);

        log.info("分布式事务成功完成: XID={}, 耗时={}ms", xid, duration);
        TX_INFO.remove();
    }

    /**
     * 事务异常
     */
    @AfterThrowing(value = "seataTransactionalMethod()", throwing = "ex")
    public void afterThrowing(Throwable ex) {
        Map<String, Object> txInfo = TX_INFO.get();
        if (txInfo == null || !txInfo.containsKey("xid")) {
            return;
        }

        String xid = (String) txInfo.get("xid");
        long startTime = (long) txInfo.get("startTime");
        long duration = System.currentTimeMillis() - startTime;

        String extraData = String.format(
                "{\"duration\":%d,\"result\":\"failure\",\"error\":\"%s\"}",
                duration, ex.getMessage().replace("\"", "\\\""));
        transactionLogService.recordTransactionEnd(xid, "ROLLBACKED", extraData);

        log.warn("分布式事务回滚: XID={}, 耗时={}ms, 原因={}", xid, duration, ex.getMessage());
        TX_INFO.remove();
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 获取用户ID
     */
    private String getUserId(HttpServletRequest request) {
        // 可以从请求头、Session或JWT Token中获取用户ID
        // 这里简单示例，实际应根据项目认证机制实现
        String userId = request.getHeader("X-User-ID");
        if (userId == null || userId.isEmpty()) {
            // 尝试从会话中获取
            try {
                Object userObj = request.getSession().getAttribute("userId");
                if (userObj != null) {
                    userId = userObj.toString();
                }
            } catch (Exception e) {
                log.debug("从会话获取用户ID失败", e);
            }
        }
        return userId != null ? userId : "anonymous";
    }
} 