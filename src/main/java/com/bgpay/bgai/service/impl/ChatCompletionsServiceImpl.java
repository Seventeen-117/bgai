package com.bgpay.bgai.service.impl;

import com.bgpay.bgai.entity.ChatCompletions;
import com.bgpay.bgai.mapper.ChatCompletionsMapper;
import com.bgpay.bgai.service.ChatCompletionsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author zly
 * @since 2025-03-08 23:37:43
 */
@Service
public class ChatCompletionsServiceImpl extends ServiceImpl<ChatCompletionsMapper, ChatCompletions> implements ChatCompletionsService {

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void insertChatCompletions(ChatCompletions chatCompletions) {
        this.save(chatCompletions);
    }
}
