package com.bgpay.bgai.utils;

import org.slf4j.MDC;
import java.util.UUID;

public class LogUtils {
    public static final String TRACE_ID = "traceId";
    public static final String SPAN_ID = "spanId";
    public static final String USER_ID = "userId";

    /**
     * 开始一个新的追踪上下文
     */
    public static void startTrace() {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        String spanId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        MDC.put(TRACE_ID, traceId);
        MDC.put(SPAN_ID, spanId);
    }

    /**
     * 设置用户ID
     */
    public static void setUserId(String userId) {
        MDC.put(USER_ID, userId);
    }

    /**
     * 清理追踪上下文
     */
    public static void clearTrace() {
        MDC.remove(TRACE_ID);
        MDC.remove(SPAN_ID);
        MDC.remove(USER_ID);
    }

    /**
     * 获取当前追踪ID
     */
    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }

    /**
     * 获取当前跨度ID
     */
    public static String getSpanId() {
        return MDC.get(SPAN_ID);
    }

    /**
     * 获取当前用户ID
     */
    public static String getUserId() {
        return MDC.get(USER_ID);
    }
} 