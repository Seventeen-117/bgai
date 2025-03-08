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
    public ApiConfig getLatestConfig(String userId);
}
