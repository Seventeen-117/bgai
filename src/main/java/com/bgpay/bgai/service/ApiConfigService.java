package com.bgpay.bgai.service;

import com.bgpay.bgai.entity.ApiConfig;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

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

    /**
     * 获取所有API配置
     * @return API配置列表
     */
    List<ApiConfig> getAllApiConfigs();
    
    /**
     * 根据ID获取API配置
     * @param id API配置ID
     * @return API配置对象
     */
    ApiConfig getApiConfigById(Long id);
    
    /**
     * 创建API配置
     * @param apiConfig API配置对象
     * @return 创建后的API配置
     */
    ApiConfig createApiConfig(ApiConfig apiConfig);
    
    /**
     * 更新API配置
     * @param apiConfig API配置对象
     * @return 更新后的API配置
     */
    ApiConfig updateApiConfig(ApiConfig apiConfig);
    
    /**
     * 删除API配置
     * @param id API配置ID
     * @return 是否删除成功
     */
    boolean deleteApiConfig(Long id);
    
    /**
     * 根据模型类型获取API配置
     * @param modelType 模型类型
     * @return API配置对象
     */
    ApiConfig getApiConfigByModelType(String modelType);
    
    /**
     * 获取默认API配置
     * @return 默认API配置
     */
    ApiConfig getDefaultApiConfig();
    
    /**
     * 设置默认API配置
     * @param id API配置ID
     * @return 是否设置成功
     */
    boolean setDefaultApiConfig(Long id);
}
