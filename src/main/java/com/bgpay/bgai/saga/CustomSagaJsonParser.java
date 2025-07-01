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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

/**
 * 自定义Saga状态机JSON解析器
 * 提供解析状态监控和结果跟踪功能
 */
@Component("customSagaJsonParser")
public class CustomSagaJsonParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomSagaJsonParser.class);
    
    private final Map<String, Boolean> parsingResults = new ConcurrentHashMap<>();
    private final Map<String, JSONObject> stateMachineDefinitions = new ConcurrentHashMap<>();
    private final DateTimeFormatter versionFormatter = DateTimeFormatter.ofPattern("yyMMdd.HHmmss");
    
    /**
     * 解析状态机JSON定义，并动态生成版本号
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
            
            // 检查是否启用动态版本号功能
            boolean useDynamicVersion = Boolean.parseBoolean(
                    System.getProperty("saga.state-machine.dynamic-version", "true"));
            
            if (useDynamicVersion) {
                // 动态生成版本号，替换原有的静态版本号
                String dynamicVersion = generateDynamicVersion();
                String originalVersion = jsonObj.getString("Version");
                LOGGER.info("状态机 [{}] 原始版本: {}, 动态生成的新版本: {}", 
                        stateMachineName, originalVersion, dynamicVersion);
                
                // 更新状态机定义中的版本号
                jsonObj.put("Version", dynamicVersion);
                LOGGER.info("状态机定义解析成功: {}, 已更新版本号: {}", stateMachineName, dynamicVersion);
            } else {
                LOGGER.info("动态版本号功能未启用，使用原始版本号");
                LOGGER.info("状态机定义解析成功: {}, 使用原版本号: {}", 
                        stateMachineName, jsonObj.getString("Version"));
            }
            
            // 存储解析后的状态机定义
            stateMachineDefinitions.put(stateMachineName, jsonObj);
            
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
     * 生成动态版本号，基于时间戳和随机部分
     * 格式: yyMMdd.HHmmss.randomSuffix
     * @return 动态生成的版本号
     */
    private String generateDynamicVersion() {
        LocalDateTime now = LocalDateTime.now();
        String timeBasedVersion = now.format(versionFormatter);
        
        // 添加一个短的随机后缀，确保唯一性
        String randomSuffix = UUID.randomUUID().toString().substring(0, 4);
        
        return timeBasedVersion + "." + randomSuffix;
    }
    
    /**
     * 从文件解析状态机定义，并更新文件内容
     * @param filePath 文件路径
     * @param updateFile 是否更新文件内容
     * @return 解析结果
     */
    public boolean parseStateMachineFile(String filePath, boolean updateFile) {
        try {
            Path path = Paths.get(filePath);
            String content = new String(Files.readAllBytes(path));
            boolean success = parseStateMachineJson(content);
            
            if (success && updateFile) {
                // 获取更新后的JSON内容
                JSONObject jsonObj = JSON.parseObject(content);
                String stateMachineName = jsonObj.getString("Name");
                JSONObject updatedJson = stateMachineDefinitions.get(stateMachineName);
                
                if (updatedJson != null) {
                    // 格式化JSON并写回文件
                    String updatedContent = JSON.toJSONString(updatedJson);
                    Files.write(path, updatedContent.getBytes());
                    LOGGER.info("已更新状态机定义文件: {}", filePath);
                }
            }
            
            return success;
        } catch (IOException e) {
            LOGGER.error("读取或更新状态机定义文件失败: {}", filePath, e);
            return false;
        }
    }
    
    /**
     * 从文件解析状态机定义
     * @param filePath 文件路径
     * @return 解析结果
     */
    public boolean parseStateMachineFile(String filePath) {
        return parseStateMachineFile(filePath, false);
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