-- 添加分支事务ID列表字段
ALTER TABLE transaction_log 
ADD COLUMN branch_ids JSON DEFAULT NULL COMMENT '分支事务ID列表';

-- 添加额外数据字段（如果不存在）
ALTER TABLE transaction_log 
ADD COLUMN extra_data JSON DEFAULT NULL COMMENT '额外信息(JSON格式)';

-- 将现有的branch_id数据迁移到branch_ids
UPDATE transaction_log 
SET branch_ids = JSON_ARRAY(branch_id)
WHERE branch_id IS NOT NULL; 