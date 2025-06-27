package com.bgpay.bgai.service.impl;

import com.bgpay.bgai.entity.ApiConfig;
import com.bgpay.bgai.mapper.ApiConfigMapper;
import com.bgpay.bgai.service.ApiConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author zly
 * @since 2025-03-08 20:03:01
 */
@Service
public class ApiConfigServiceImpl extends ServiceImpl<ApiConfigMapper, ApiConfig> implements ApiConfigService {

    private static final Logger log = LoggerFactory.getLogger(ApiConfigServiceImpl.class);

    @Autowired
    private ApiConfigMapper apiConfigMapper;

    @Override
    public ApiConfig getLatestConfig(String userId) {
        LambdaQueryWrapper<ApiConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ApiConfig::getUserId, userId);
        queryWrapper.orderByDesc(ApiConfig::getId);
        return this.getOne(queryWrapper);
    }
    
    @Override
    public ApiConfig findMatchingConfig(String userId, String apiUrl, String apiKey, String modelName) {
        LambdaQueryWrapper<ApiConfig> queryWrapper = new LambdaQueryWrapper<>();

        // 必须包含userId条件
        queryWrapper.eq(ApiConfig::getUserId, userId);
        log.info("查询配置 - userId={}", userId);

        // 根据传入的参数添加条件，如果某个参数为null或空字符串则不添加该条件
        if (StringUtils.hasText(apiUrl)) {
            queryWrapper.eq(ApiConfig::getApiUrl, apiUrl);
            log.info("添加条件 - apiUrl={}", apiUrl);
        }

        if (StringUtils.hasText(apiKey)) {
            queryWrapper.eq(ApiConfig::getApiKey, apiKey);
            log.info("添加条件 - apiKey=*****");
        }

        if (StringUtils.hasText(modelName)) {
            queryWrapper.eq(ApiConfig::getModelName, modelName);
            log.info("添加条件 - modelName={}", modelName);
        }

        // 按ID降序排列，获取最新的配置
        queryWrapper.orderByDesc(ApiConfig::getId);
        log.info("排序方式 - 按ID降序");

        // 添加LIMIT 1限制，确保只返回一条记录
        queryWrapper.last("LIMIT 1");
        log.info("限制结果数 - LIMIT 1");

        // 使用getOne方法，但设置throwEx为false，当有多条记录时不会抛出异常，而是返回第一条
        ApiConfig result = this.getOne(queryWrapper, false);
        if (result != null) {
            log.info("查询结果 - 找到配置: id={}, modelName={}", result.getId(), result.getModelName());
        } else {
            log.info("查询结果 - 未找到匹配配置");
        }
        
        return result;
    }

    @Override
    public ApiConfig findAlternativeConfig(String userId, String currentModelName) {
        // 实现查找替代配置的逻辑
        LambdaQueryWrapper<ApiConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(ApiConfig::getModelName, currentModelName);
        wrapper.eq(ApiConfig::getEnabled, true);
        wrapper.orderByDesc(ApiConfig::getPriority);
        return getOne(wrapper);
    }

    @Override
    public List<ApiConfig> getAllApiConfigs() {
        return list();
    }

    @Override
    public ApiConfig getApiConfigById(Long id) {
        return getById(id);
    }

    @Override
    @Transactional
    public ApiConfig createApiConfig(ApiConfig apiConfig) {
        apiConfig.setCreateTime(LocalDateTime.now());
        apiConfig.setUpdateTime(LocalDateTime.now());
        save(apiConfig);
        return apiConfig;
    }

    @Override
    @Transactional
    public ApiConfig updateApiConfig(ApiConfig apiConfig) {
        ApiConfig existing = getById(apiConfig.getId());
        if (existing == null) {
            return null;
        }
        
        apiConfig.setUpdateTime(LocalDateTime.now());
        updateById(apiConfig);
        return apiConfig;
    }

    @Override
    @Transactional
    public boolean deleteApiConfig(Long id) {
        return removeById(id);
    }

    @Override
    public ApiConfig getApiConfigByModelType(String modelType) {
        LambdaQueryWrapper<ApiConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiConfig::getModelType, modelType);
        wrapper.eq(ApiConfig::getEnabled, true);
        return getOne(wrapper);
    }

    @Override
    public ApiConfig getDefaultApiConfig() {
        LambdaQueryWrapper<ApiConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiConfig::getIsDefault, true);
        wrapper.eq(ApiConfig::getEnabled, true);
        return getOne(wrapper);
    }

    @Override
    @Transactional
    public boolean setDefaultApiConfig(Long id) {
        // 先将所有配置设为非默认
        LambdaUpdateWrapper<ApiConfig> clearDefaultWrapper = new LambdaUpdateWrapper<>();
        clearDefaultWrapper.set(ApiConfig::getIsDefault, false);
        update(clearDefaultWrapper);
        
        // 将指定ID的配置设为默认
        ApiConfig config = getById(id);
        if (config == null) {
            return false;
        }
        
        config.setIsDefault(true);
        config.setUpdateTime(LocalDateTime.now());
        return updateById(config);
    }
}