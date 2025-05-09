package com.bgpay.bgai.service;

import com.bgpay.bgai.entity.ApiConfig;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author zly
 * @since 2025-03-08 20:03:01
 */
public interface ApiConfigService extends IService<ApiConfig> {
    /**
     * 根据用户ID获取最新的API配置
     * @param userId 用户ID
     * @return API配置
     */
    public ApiConfig getLatestConfig(String userId);
    
    /**
     * 根据用户ID和部分参数查询匹配的API配置
     * 
     * @param userId 用户ID
     * @param apiUrl API URL，可为null
     * @param apiKey API密钥，可为null
     * @param modelName 模型名称，可为null
     * @return 匹配的API配置，如果没有找到则返回null
     */
    public ApiConfig findMatchingConfig(String userId, String apiUrl, String apiKey, String modelName);
    
    /**
     * 查找替代配置，用于API熔断后切换
     * 
     * @param userId 用户ID
     * @param currentModelName 当前使用的模型名称
     * @return 替代的API配置，如果没有找到则返回null
     */
    public ApiConfig findAlternativeConfig(String userId, String currentModelName);
}
