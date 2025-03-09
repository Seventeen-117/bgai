package com.bgpay.bgai.service;

import com.bgpay.bgai.entity.ChatCompletions;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author zly
 * @since 2025-03-08 23:37:43
 */
public interface ChatCompletionsService extends IService<ChatCompletions> {
    public void insertChatCompletions(ChatCompletions chatCompletions);
}
