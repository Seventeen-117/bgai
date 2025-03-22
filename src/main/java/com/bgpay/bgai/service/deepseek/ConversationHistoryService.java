package com.bgpay.bgai.service.deepseek;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service
public class ConversationHistoryService {
    private final Cache<String, List<Map<String, Object>>> historyCache =
            CacheBuilder.newBuilder()
                    .expireAfterWrite(30, TimeUnit.DAYS)
                    .maximumSize(10_000)
                    .build();
    @Value("${conversation.max-rounds:10}") // 默认保留10轮对话
    private int maxRounds;

    public void addMessage(String userId, String role, String content) {
        List<Map<String, Object>> history = getHistory(userId);

        // 自动清理过期记录
        history.removeIf(entry ->
                System.currentTimeMillis() - (Long)entry.get("timestamp") > 30L*24*3600*1000
        );

        // 智能保留核心对话
        if (history.size() >= maxRounds*2) {
            int keepIndex = Math.max(0, history.size() - maxRounds*2);
            history.subList(0, keepIndex).clear();
        }

        // 新增带元数据的消息
        history.add(Map.of(
                "role", role,
                "content", content,
                "timestamp", System.currentTimeMillis(),
                "hasAttachment", content.contains("【文件内容】") // 标记含附件
        ));
    }

    public List<Map<String, Object>> getValidHistory(String userId) {
        List<Map<String, Object>> history = getHistory(userId);
        return history.stream()
                .filter(entry ->
                        System.currentTimeMillis() - (Long)entry.get("timestamp") < 30L*24*3600*1000)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> getHistory(String userId) {
        List<Map<String, Object>> history = historyCache.getIfPresent(userId);
        if (history == null) {
            history = new CopyOnWriteArrayList<>();
            historyCache.put(userId, history);
        }
        return history;
    }
}