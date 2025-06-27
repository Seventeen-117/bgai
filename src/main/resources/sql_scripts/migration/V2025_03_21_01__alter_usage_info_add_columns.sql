-- 添加user_id列
ALTER TABLE usage_info
ADD COLUMN IF NOT EXISTS user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
ADD COLUMN IF NOT EXISTS model_type VARCHAR(32) DEFAULT NULL COMMENT '模型类型',
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
ADD COLUMN IF NOT EXISTS total_tokens INT DEFAULT 0 COMMENT '总token数',
ADD COLUMN IF NOT EXISTS prompt_tokens INT DEFAULT 0 COMMENT '输入token数',
ADD COLUMN IF NOT EXISTS completion_tokens INT DEFAULT 0 COMMENT '输出token数';

-- 添加索引
ALTER TABLE usage_info
ADD INDEX idx_user_id (user_id),
ADD INDEX idx_chat_completion_id (chat_completion_id),
ADD INDEX idx_created_at (created_at); 