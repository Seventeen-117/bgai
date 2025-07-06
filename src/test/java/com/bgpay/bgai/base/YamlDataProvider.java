package com.bgpay.bgai.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.qameta.allure.Allure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.annotations.DataProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * YAML数据提供者工具类
 * 用于从YAML文件加载测试数据，支持数据驱动测试
 */
public class YamlDataProvider {
    private static final Logger log = LoggerFactory.getLogger(YamlDataProvider.class);
    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private static final String TEST_DATA_DIR = "src/test/resources/test-data";
    
    /**
     * 从YAML文件加载测试数据
     * 
     * @param method 测试方法
     * @return 测试数据对象数组
     */
    @DataProvider(name = "yamlData")
    public static Object[][] getYamlData(Method method) {
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();
        String yamlFilePath = String.format("%s/%s/%s.yml", TEST_DATA_DIR, className, methodName);
        
        log.info("从YAML文件加载测试数据: {}", yamlFilePath);
        
        try {
            Path path = Paths.get(yamlFilePath);
            if (!Files.exists(path)) {
                log.warn("YAML文件不存在: {}", yamlFilePath);
                return new Object[0][0];
            }
            
            // 读取YAML文件
            Map<String, Object> yamlData = loadYamlFile(path);
            
            // 获取测试数据列表
            List<Map<String, Object>> testCases = getTestCases(yamlData);
            
            // 添加测试数据到Allure报告
            addYamlDataToAllureReport(yamlFilePath, yamlData);
            
            // 转换为Object[][]格式
            return convertToDataProviderFormat(testCases);
            
        } catch (Exception e) {
            log.error("加载YAML测试数据失败", e);
            return new Object[0][0];
        }
    }
    
    /**
     * 从指定的YAML文件加载测试数据
     * 
     * @param yamlFileName YAML文件名（不含路径和扩展名）
     * @return 测试数据对象数组
     */
    @DataProvider(name = "namedYamlData")
    public static Object[][] getNamedYamlData(Method method, ITestContext context) {
        // 获取测试方法上的YamlSource注解
        YamlSource yamlSource = method.getAnnotation(YamlSource.class);
        if (yamlSource == null) {
            log.warn("测试方法没有@YamlSource注解: {}", method.getName());
            return new Object[0][0];
        }
        
        String yamlFileName = yamlSource.value();
        String yamlFilePath = String.format("%s/%s.yml", TEST_DATA_DIR, yamlFileName);
        
        log.info("从指定YAML文件加载测试数据: {}", yamlFilePath);
        
        try {
            Path path = Paths.get(yamlFilePath);
            if (!Files.exists(path)) {
                log.warn("YAML文件不存在: {}", yamlFilePath);
                return new Object[0][0];
            }
            
            // 读取YAML文件
            Map<String, Object> yamlData = loadYamlFile(path);
            
            // 获取测试数据列表
            List<Map<String, Object>> testCases = getTestCases(yamlData);
            
            // 添加测试数据到Allure报告
            addYamlDataToAllureReport(yamlFilePath, yamlData);
            
            // 转换为Object[][]格式
            return convertToDataProviderFormat(testCases);
            
        } catch (Exception e) {
            log.error("加载YAML测试数据失败", e);
            return new Object[0][0];
        }
    }
    
    /**
     * 从资源目录加载YAML文件
     */
    private static Map<String, Object> loadYamlFile(Path path) throws IOException {
        try (InputStream is = Files.newInputStream(path)) {
            return yamlMapper.readValue(is, Map.class);
        }
    }
    
    /**
     * 从YAML数据中提取测试用例列表
     */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> getTestCases(Map<String, Object> yamlData) {
        // 尝试获取testCases字段
        if (yamlData.containsKey("testCases")) {
            Object testCasesObj = yamlData.get("testCases");
            if (testCasesObj instanceof List) {
                return ((List<Object>) testCasesObj).stream()
                        .filter(item -> item instanceof Map)
                        .map(item -> (Map<String, Object>) item)
                        .collect(Collectors.toList());
            }
        }
        
        // 如果没有testCases字段，则整个YAML文件被视为单个测试用例
        return Collections.singletonList(yamlData);
    }
    
    /**
     * 将测试用例列表转换为TestNG DataProvider格式
     */
    private static Object[][] convertToDataProviderFormat(List<Map<String, Object>> testCases) {
        Object[][] result = new Object[testCases.size()][1];
        for (int i = 0; i < testCases.size(); i++) {
            result[i][0] = testCases.get(i);
        }
        return result;
    }
    
    /**
     * 将YAML数据添加到Allure报告
     */
    private static void addYamlDataToAllureReport(String yamlFilePath, Map<String, Object> yamlData) {
        try {
            String yamlContent = yamlMapper.writeValueAsString(yamlData);
            Allure.addAttachment("测试数据 (" + yamlFilePath + ")", "application/yaml", yamlContent);
        } catch (Exception e) {
            log.error("添加YAML数据到Allure报告失败", e);
        }
    }
    
    /**
     * 扫描测试数据目录，获取所有YAML文件
     * 
     * @return YAML文件路径列表
     */
    public static List<String> scanYamlFiles() {
        List<String> yamlFiles = new ArrayList<>();
        try {
            File testDataDir = new File(TEST_DATA_DIR);
            if (!testDataDir.exists()) {
                testDataDir.mkdirs();
                log.info("创建测试数据目录: {}", TEST_DATA_DIR);
                return yamlFiles;
            }
            
            scanDirectory(testDataDir, yamlFiles);
            
        } catch (Exception e) {
            log.error("扫描YAML文件失败", e);
        }
        return yamlFiles;
    }
    
    /**
     * 递归扫描目录，查找YAML文件
     */
    private static void scanDirectory(File directory, List<String> yamlFiles) {
        File[] files = directory.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, yamlFiles);
            } else if (file.getName().endsWith(".yml") || file.getName().endsWith(".yaml")) {
                yamlFiles.add(file.getAbsolutePath());
            }
        }
    }
} 