package com.bgpay.bgai.saga;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自定义Saga状态机JSON解析器
 * 提供解析状态监控和结果跟踪功能
 */
@Component("customSagaJsonParser")
public class CustomSagaJsonParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomSagaJsonParser.class);
    
    private final Map<String, Boolean> parsingResults = new ConcurrentHashMap<>();
    private final Map<String, JSONObject> stateMachineDefinitions = new ConcurrentHashMap<>();
    
    /**
     * 解析状态机JSON定义
     * @param json 状态机JSON字符串
     * @return 解析结果
     */
    public boolean parseStateMachineJson(String json) {
        try {
            JSONObject jsonObj = JSON.parseObject(json);
            String stateMachineName = jsonObj.getString("Name");
            LOGGER.info("开始解析状态机定义: {}", stateMachineName);
            
            // 验证状态机定义的基本结构
            validateStateMachineJson(jsonObj);
            
            // 存储解析后的状态机定义
            stateMachineDefinitions.put(stateMachineName, jsonObj);
            
            LOGGER.info("状态机定义解析成功: {}", stateMachineName);
            parsingResults.put(stateMachineName, true);
            return true;
        } catch (Exception e) {
            LOGGER.error("状态机定义解析失败", e);
            try {
                JSONObject jsonObj = JSON.parseObject(json);
                if (jsonObj != null && jsonObj.containsKey("Name")) {
                    parsingResults.put(jsonObj.getString("Name"), false);
                }
            } catch (Exception ignored) {
                // 忽略解析错误
            }
            return false;
        }
    }
    
    /**
     * 从文件解析状态机定义
     * @param filePath 文件路径
     * @return 解析结果
     */
    public boolean parseStateMachineFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            String content = new String(Files.readAllBytes(path));
            return parseStateMachineJson(content);
        } catch (IOException e) {
            LOGGER.error("读取状态机定义文件失败: {}", filePath, e);
            return false;
        }
    }
    
    /**
     * 验证状态机JSON定义的基本结构
     * @param jsonObj 状态机JSON对象
     * @throws IllegalArgumentException 如果验证失败
     */
    private void validateStateMachineJson(JSONObject jsonObj) {
        if (!jsonObj.containsKey("Name")) {
            throw new IllegalArgumentException("状态机定义缺少Name字段");
        }
        
        if (!jsonObj.containsKey("StartState")) {
            throw new IllegalArgumentException("状态机定义缺少StartState字段");
        }
        
        if (!jsonObj.containsKey("States") || !jsonObj.getJSONObject("States").isEmpty()) {
            // 验证States对象不为空
        } else {
            throw new IllegalArgumentException("状态机定义States为空");
        }
    }
    
    /**
     * 获取解析器类型
     * @return 解析器类型标识
     */
    public String getJsonParserType() {
        return "CUSTOM_FASTJSON2";
    }
    
    /**
     * 获取状态机定义
     * @param stateMachineName 状态机名称
     * @return 状态机定义JSON对象，如果不存在返回null
     */
    public JSONObject getStateMachineDefinition(String stateMachineName) {
        return stateMachineDefinitions.get(stateMachineName);
    }
    
    /**
     * 获取所有状态机定义
     * @return 状态机名称到定义的映射
     */
    public Map<String, JSONObject> getAllStateMachineDefinitions() {
        return new HashMap<>(stateMachineDefinitions);
    }
    
    /**
     * 获取状态机解析结果
     * @return 状态机名称与解析结果的映射
     */
    public Map<String, Boolean> getParsingResults() {
        return new HashMap<>(parsingResults);
    }
    
    /**
     * 清除解析结果记录
     */
    public void clearParsingResults() {
        parsingResults.clear();
    }
    
    /**
     * 获取解析结果摘要
     * @return 包含成功和失败信息的字符串
     */
    public String getParsingResultSummary() {
        int success = 0;
        int failure = 0;
        StringBuilder failedStateMachines = new StringBuilder();
        
        for (Map.Entry<String, Boolean> entry : parsingResults.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) {
                success++;
            } else {
                failure++;
                failedStateMachines.append(entry.getKey()).append(", ");
            }
        }
        
        StringBuilder summary = new StringBuilder();
        summary.append("状态机解析结果: 成功 ").append(success).append(" 个, 失败 ").append(failure).append(" 个");
        
        if (failure > 0 && failedStateMachines.length() > 0) {
            summary.append("\n失败的状态机: ")
                    .append(failedStateMachines.substring(0, failedStateMachines.length() - 2));
        }
        
        return summary.toString();
    }
} 