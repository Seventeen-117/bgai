-- 添加 message_id 列到 usage_record 表
SET @dbname = DATABASE();
SET @tablename = "usage_record";
SET @columnname = "message_id";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND COLUMN_NAME = @columnname
  ) > 0,
  "SELECT 1",
  "ALTER TABLE usage_record ADD COLUMN message_id VARCHAR(64) DEFAULT NULL COMMENT '消息ID'"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists; 