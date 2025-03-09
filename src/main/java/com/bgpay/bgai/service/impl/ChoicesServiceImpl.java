package com.bgpay.bgai.service.impl;

import com.bgpay.bgai.entity.Choices;
import com.bgpay.bgai.mapper.ChoicesMapper;
import com.bgpay.bgai.service.ChoicesService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author zly
 * @since 2025-03-08 23:37:28
 */
@Service
public class ChoicesServiceImpl extends ServiceImpl<ChoicesMapper, Choices> implements ChoicesService {
    public void insertChoices(Choices choices){
        baseMapper.insert(choices);
    }
}
