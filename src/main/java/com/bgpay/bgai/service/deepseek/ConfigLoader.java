package com.bgpay.bgai.service.deepseek;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigLoader {
    // 配置文件路径常量，方便后续修改
    private static final String CONFIG_FILE = "deepseek-config.properties";
    private static final Logger LOGGER = Logger.getLogger(ConfigLoader.class.getName());
    private static final Properties props = new Properties();

    static {
        loadConfig();
    }

    /**
     * 加载配置文件
     */
    private static void loadConfig() {
        try (InputStream input = ConfigLoader.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                // 记录类路径信息，方便排查问题
                LOGGER.log(Level.SEVERE, "类路径: " + System.getProperty("java.class.path"));
                // 增加当前线程的上下文类加载器信息
                LOGGER.log(Level.SEVERE, "当前线程的上下文类加载器: " + Thread.currentThread().getContextClassLoader());
                throw new RuntimeException("找不到配置文件 " + CONFIG_FILE);
            }
            props.load(input);
            LOGGER.info("配置文件 " + CONFIG_FILE + " 加载成功");
        } catch (IOException ex) {
            // 记录异常信息，方便排查问题
            LOGGER.log(Level.SEVERE, "加载配置文件 " + CONFIG_FILE + " 失败", ex);
            throw new RuntimeException("加载配置失败", ex);
        }
    }

    /**
     * 根据键获取配置项的值，如果不存在则抛出异常
     * @param key 配置项的键
     * @return 配置项的值
     */
    public static String getProperty(String key) {
        String value = props.getProperty(key);
        if (value == null) {
            LOGGER.log(Level.SEVERE, "缺少必要配置项: " + key);
            throw new RuntimeException("缺少必要配置项: " + key);
        }
        return value;
    }

    /**
     * 根据键获取配置项的值，如果不存在则返回默认值
     * @param key 配置项的键
     * @param defaultValue 默认值
     * @return 配置项的值或默认值
     */
    public static String getProperty(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }
}