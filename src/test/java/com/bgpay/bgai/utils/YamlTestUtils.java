package com.bgpay.bgai.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * YAML测试工具类
 * 提供YAML测试数据处理的辅助方法
 */
public class YamlTestUtils {
    private static final Logger log = LoggerFactory.getLogger(YamlTestUtils.class);
    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private static final String TEST_DATA_DIR = "src/test/resources/test-data";
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    
    /**
     * 加载YAML测试数据文件
     * 
     * @param yamlFilePath YAML文件路径
     * @return 解析后的Map对象
     */
    public static Map<String, Object> loadYamlFile(String yamlFilePath) {
        try {
            Path path = Paths.get(yamlFilePath);
            if (!Files.exists(path)) {
                log.warn("YAML文件不存在: {}", yamlFilePath);
                return new HashMap<>();
            }
            
            return yamlMapper.readValue(path.toFile(), Map.class);
        } catch (IOException e) {
            log.error("加载YAML文件失败: {}", yamlFilePath, e);
            return new HashMap<>();
        }
    }
    
    /**
     * 根据类名和方法名加载对应的测试数据
     * 
     * @param className 测试类名
     * @param methodName 测试方法名
     * @return 解析后的Map对象
     */
    public static Map<String, Object> loadTestData(String className, String methodName) {
        String yamlFilePath = String.format("%s/%s/%s.yml", TEST_DATA_DIR, className, methodName);
        return loadYamlFile(yamlFilePath);
    }
    
    /**
     * 替换字符串中的变量占位符
     * 例如: "${variable}" 会被替换为变量表中的对应值
     * 
     * @param input 输入字符串
     * @param variables 变量表
     * @return 替换后的字符串
     */
    public static String replaceVariables(String input, Map<String, String> variables) {
        if (!StringUtils.hasText(input) || variables == null || variables.isEmpty()) {
            return input;
        }
        
        Matcher matcher = VARIABLE_PATTERN.matcher(input);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String varName = matcher.group(1);
            String replacement = variables.getOrDefault(varName, matcher.group(0));
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        
        matcher.appendTail(result);
        return result.toString();
    }
    
    /**
     * 递归替换Map中所有字符串值的变量占位符
     * 
     * @param data 数据Map
     * @param variables 变量表
     * @return 替换后的Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> replaceVariablesInMap(Map<String, Object> data, Map<String, String> variables) {
        if (data == null || variables == null || variables.isEmpty()) {
            return data;
        }
        
        Map<String, Object> result = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof String) {
                result.put(key, replaceVariables((String) value, variables));
            } else if (value instanceof Map) {
                result.put(key, replaceVariablesInMap((Map<String, Object>) value, variables));
            } else {
                result.put(key, value);
            }
        }
        
        return result;
    }
    
    /**
     * 创建测试数据目录（如果不存在）
     * 
     * @param className 测试类名
     * @return 创建的目录路径
     */
    public static String createTestDataDirectory(String className) {
        String dirPath = String.format("%s/%s", TEST_DATA_DIR, className);
        File dir = new File(dirPath);
        
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                log.info("创建测试数据目录: {}", dirPath);
            } else {
                log.warn("无法创建测试数据目录: {}", dirPath);
            }
        }
        
        return dirPath;
    }
    
    /**
     * 将对象保存为YAML文件
     * 
     * @param data 要保存的对象
     * @param filePath 文件路径
     * @return 是否保存成功
     */
    public static boolean saveToYamlFile(Object data, String filePath) {
        try {
            yamlMapper.writeValue(new File(filePath), data);
            log.info("保存YAML文件成功: {}", filePath);
            return true;
        } catch (IOException e) {
            log.error("保存YAML文件失败: {}", filePath, e);
            return false;
        }
    }
    
    /**
     * 生成测试数据模板
     * 
     * @param className 测试类名
     * @param methodName 测试方法名
     * @param templateData 模板数据
     * @return 是否生成成功
     */
    public static boolean generateTestDataTemplate(String className, String methodName, Map<String, Object> templateData) {
        String dirPath = createTestDataDirectory(className);
        String filePath = String.format("%s/%s.yml", dirPath, methodName);
        
        File file = new File(filePath);
        if (file.exists()) {
            log.warn("测试数据文件已存在，跳过生成: {}", filePath);
            return false;
        }
        
        return saveToYamlFile(templateData, filePath);
    }
} 