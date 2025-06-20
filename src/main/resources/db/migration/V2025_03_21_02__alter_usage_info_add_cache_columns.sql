-- 添加缓存相关的列
ALTER TABLE usage_info
ADD COLUMN IF NOT EXISTS prompt_cache_hit_tokens INT DEFAULT 0 COMMENT '命中缓存的输入token数',
ADD COLUMN IF NOT EXISTS prompt_cache_miss_tokens INT DEFAULT 0 COMMENT '未命中缓存的输入token数',
ADD COLUMN IF NOT EXISTS prompt_tokens_cached BOOLEAN DEFAULT FALSE COMMENT '是否使用了缓存',
ADD COLUMN IF NOT EXISTS completion_reasoning_tokens INT DEFAULT 0 COMMENT '推理输出token数'; 