package com.bgpay.bgai.service;

import com.bgpay.bgai.entity.Choices;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author zly
 * @since 2025-03-08 23:37:28
 */
public interface ChoicesService extends IService<Choices> {
    public void insertChoices(Choices choices);
}
