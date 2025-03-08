package com.bgpay.bgai.service.impl;

import com.bgpay.bgai.entity.ApiConfig;
import com.bgpay.bgai.mapper.ApiConfigMapper;
import com.bgpay.bgai.service.ApiConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

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

    @Override
    public ApiConfig getLatestConfig(String userId) {
        LambdaQueryWrapper<ApiConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ApiConfig::getUserId, userId);
        queryWrapper.orderByDesc(ApiConfig::getId);
        return this.getOne(queryWrapper);
    }
}