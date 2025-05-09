package com.bgpay.bgai.service.impl;

import com.bgpay.bgai.entity.ApiConfig;
import com.bgpay.bgai.mapper.ApiConfigMapper;
import com.bgpay.bgai.service.ApiConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        LambdaQueryWrapper<ApiConfig> queryWrapper = new LambdaQueryWrapper<>();
        
        // 查找同一用户的其他模型配置（即排除当前模型名称）
        queryWrapper.eq(ApiConfig::getUserId, userId);
        if (StringUtils.hasText(currentModelName)) {
            queryWrapper.ne(ApiConfig::getModelName, currentModelName);
        }
        
        // 按ID降序排列，获取最新的配置
        queryWrapper.orderByDesc(ApiConfig::getId);
        queryWrapper.last("LIMIT 1");
        
        return this.getOne(queryWrapper, false);
    }
}