-- 为 usage_info 表的字段添加默认值
SET @dbname = DATABASE();
SET @tablename = "usage_info";

-- 为 prompt_tokens_cached 添加默认值
SET @columnname = "prompt_tokens_cached";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND COLUMN_NAME = @columnname
  ) > 0,
  "ALTER TABLE usage_info MODIFY COLUMN prompt_tokens_cached INT DEFAULT 0 COMMENT '缓存的提示词token数'",
  "ALTER TABLE usage_info ADD COLUMN prompt_tokens_cached INT DEFAULT 0 COMMENT '缓存的提示词token数'"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 为 completion_reasoning_tokens 添加默认值
SET @columnname = "completion_reasoning_tokens";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND COLUMN_NAME = @columnname
  ) > 0,
  "ALTER TABLE usage_info MODIFY COLUMN completion_reasoning_tokens INT DEFAULT 0 COMMENT '推理完成token数'",
  "ALTER TABLE usage_info ADD COLUMN completion_reasoning_tokens INT DEFAULT 0 COMMENT '推理完成token数'"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 更新现有的空值记录
UPDATE usage_info SET prompt_tokens_cached = 0 WHERE prompt_tokens_cached IS NULL;
UPDATE usage_info SET completion_reasoning_tokens = 0 WHERE completion_reasoning_tokens IS NULL; 