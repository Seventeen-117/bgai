CREATE TABLE IF NOT EXISTS usage_info (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    chat_completion_id VARCHAR(64) NOT NULL COMMENT '对话完成ID',
    model_type VARCHAR(32) DEFAULT NULL COMMENT '模型类型',
    prompt_tokens INT DEFAULT 0 COMMENT '输入token数',
    completion_tokens INT DEFAULT 0 COMMENT '输出token数',
    total_tokens INT DEFAULT 0 COMMENT '总token数',
    prompt_cache_hit_tokens INT DEFAULT 0 COMMENT '命中缓存的输入token数',
    prompt_cache_miss_tokens INT DEFAULT 0 COMMENT '未命中缓存的输入token数',
    prompt_tokens_cached BOOLEAN DEFAULT FALSE COMMENT '是否使用了缓存',
    completion_reasoning_tokens INT DEFAULT 0 COMMENT '推理输出token数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_user_id (user_id),
    INDEX idx_chat_completion_id (chat_completion_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户使用信息表'; 