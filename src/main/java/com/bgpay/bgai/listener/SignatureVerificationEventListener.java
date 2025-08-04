package com.bgpay.bgai.listener;

import com.bgpay.bgai.event.SignatureVerificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 签名验证事件监听器
 * 用于监控、告警和审计
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SignatureVerificationEventListener {

    // 统计计数器
    private final ConcurrentHashMap<String, AtomicLong> successCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> failureCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> replayAttackCounters = new ConcurrentHashMap<>();
    
    // 性能统计
    private final ConcurrentHashMap<String, AtomicLong> totalVerificationTime = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> verificationCount = new ConcurrentHashMap<>();

    /**
     * 处理签名验证事件
     * 支持监控、告警和审计功能
     */
    @EventListener
    @Async
    public void handleSignatureVerificationEvent(SignatureVerificationEvent event) {
        try {
            String appId = event.getAppId();
            
            // 更新统计计数器
            updateCounters(appId, event);
            
            // 记录审计日志
            logAuditEvent(event);
            
            // 检查告警条件
            checkAlertConditions(appId, event);
            
            // 记录性能指标
            recordPerformanceMetrics(appId, event);
            
        } catch (Exception e) {
            log.error("Error handling signature verification event: {}", event.getEventId(), e);
        }
    }

    /**
     * 更新统计计数器
     */
    private void updateCounters(String appId, SignatureVerificationEvent event) {
        switch (event.getEventType()) {
            case VERIFICATION_SUCCESS:
                successCounters.computeIfAbsent(appId, k -> new AtomicLong(0)).incrementAndGet();
                break;
            case VERIFICATION_FAILED:
                failureCounters.computeIfAbsent(appId, k -> new AtomicLong(0)).incrementAndGet();
                break;
            case REPLAY_ATTACK_DETECTED:
                replayAttackCounters.computeIfAbsent(appId, k -> new AtomicLong(0)).incrementAndGet();
                break;
            default:
                // 其他事件类型
                break;
        }
    }

    /**
     * 记录审计日志
     */
    private void logAuditEvent(SignatureVerificationEvent event) {
        String auditMessage = String.format(
                "Signature verification event - AppId: %s, Path: %s, ClientIP: %s, Success: %s, Time: %dms, Error: %s",
                event.getAppId(),
                event.getPath(),
                event.getClientIp(),
                event.isSuccess(),
                event.getVerificationTime(),
                event.getErrorMessage()
        );
        
        if (event.isSuccess()) {
            log.info(auditMessage);
        } else {
            log.warn(auditMessage);
        }
    }

    /**
     * 检查告警条件
     */
    private void checkAlertConditions(String appId, SignatureVerificationEvent event) {
        // 检查失败率告警
        checkFailureRateAlert(appId);
        
        // 检查重放攻击告警
        if (event.getEventType() == SignatureVerificationEvent.EventType.REPLAY_ATTACK_DETECTED) {
            checkReplayAttackAlert(appId);
        }
        
        // 检查性能告警
        checkPerformanceAlert(appId, event);
    }

    /**
     * 检查失败率告警
     */
    private void checkFailureRateAlert(String appId) {
        AtomicLong successCount = successCounters.get(appId);
        AtomicLong failureCount = failureCounters.get(appId);
        
        if (successCount != null && failureCount != null) {
            long total = successCount.get() + failureCount.get();
            if (total > 100) { // 至少100次请求才开始计算失败率
                double failureRate = (double) failureCount.get() / total;
                if (failureRate > 0.1) { // 失败率超过10%
                    log.error("High signature verification failure rate for appId: {}, failure rate: {:.2%}", 
                            appId, failureRate);
                    // 这里可以集成告警系统，如发送邮件、短信等
                }
            }
        }
    }

    /**
     * 检查重放攻击告警
     */
    private void checkReplayAttackAlert(String appId) {
        AtomicLong replayCount = replayAttackCounters.get(appId);
        if (replayCount != null && replayCount.get() > 5) { // 重放攻击次数超过5次
            log.error("Multiple replay attacks detected for appId: {}, count: {}", 
                    appId, replayCount.get());
            // 这里可以集成告警系统
        }
    }

    /**
     * 检查性能告警
     */
    private void checkPerformanceAlert(String appId, SignatureVerificationEvent event) {
        if (event.getVerificationTime() > 1000) { // 验证时间超过1秒
            log.warn("Slow signature verification for appId: {}, time: {}ms", 
                    appId, event.getVerificationTime());
        }
    }

    /**
     * 记录性能指标
     */
    private void recordPerformanceMetrics(String appId, SignatureVerificationEvent event) {
        totalVerificationTime.computeIfAbsent(appId, k -> new AtomicLong(0))
                .addAndGet(event.getVerificationTime());
        verificationCount.computeIfAbsent(appId, k -> new AtomicLong(0))
                .incrementAndGet();
    }

    /**
     * 获取应用统计信息
     */
    public SignatureVerificationStats getAppStats(String appId) {
        AtomicLong successCount = successCounters.get(appId);
        AtomicLong failureCount = failureCounters.get(appId);
        AtomicLong replayCount = replayAttackCounters.get(appId);
        AtomicLong totalTime = totalVerificationTime.get(appId);
        AtomicLong count = verificationCount.get(appId);
        
        long success = successCount != null ? successCount.get() : 0;
        long failure = failureCount != null ? failureCount.get() : 0;
        long replay = replayCount != null ? replayCount.get() : 0;
        long total = success + failure;
        long avgTime = (totalTime != null && count != null && count.get() > 0) ? 
                totalTime.get() / count.get() : 0;
        
        return SignatureVerificationStats.builder()
                .appId(appId)
                .successCount(success)
                .failureCount(failure)
                .replayAttackCount(replay)
                .totalCount(total)
                .failureRate(total > 0 ? (double) failure / total : 0)
                .averageVerificationTime(avgTime)
                .build();
    }

    /**
     * 签名验证统计信息
     */
    public static class SignatureVerificationStats {
        private String appId;
        private long successCount;
        private long failureCount;
        private long replayAttackCount;
        private long totalCount;
        private double failureRate;
        private long averageVerificationTime;
        
        // Builder pattern
        public static SignatureVerificationStatsBuilder builder() {
            return new SignatureVerificationStatsBuilder();
        }
        
        public static class SignatureVerificationStatsBuilder {
            private SignatureVerificationStats stats = new SignatureVerificationStats();
            
            public SignatureVerificationStatsBuilder appId(String appId) {
                stats.appId = appId;
                return this;
            }
            
            public SignatureVerificationStatsBuilder successCount(long successCount) {
                stats.successCount = successCount;
                return this;
            }
            
            public SignatureVerificationStatsBuilder failureCount(long failureCount) {
                stats.failureCount = failureCount;
                return this;
            }
            
            public SignatureVerificationStatsBuilder replayAttackCount(long replayAttackCount) {
                stats.replayAttackCount = replayAttackCount;
                return this;
            }
            
            public SignatureVerificationStatsBuilder totalCount(long totalCount) {
                stats.totalCount = totalCount;
                return this;
            }
            
            public SignatureVerificationStatsBuilder failureRate(double failureRate) {
                stats.failureRate = failureRate;
                return this;
            }
            
            public SignatureVerificationStatsBuilder averageVerificationTime(long averageVerificationTime) {
                stats.averageVerificationTime = averageVerificationTime;
                return this;
            }
            
            public SignatureVerificationStats build() {
                return stats;
            }
        }
        
        // Getters
        public String getAppId() { return appId; }
        public long getSuccessCount() { return successCount; }
        public long getFailureCount() { return failureCount; }
        public long getReplayAttackCount() { return replayAttackCount; }
        public long getTotalCount() { return totalCount; }
        public double getFailureRate() { return failureRate; }
        public long getAverageVerificationTime() { return averageVerificationTime; }
    }
} 