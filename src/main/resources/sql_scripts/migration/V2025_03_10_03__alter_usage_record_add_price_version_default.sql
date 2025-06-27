-- 为 price_version 列添加默认值
SET @dbname = DATABASE();
SET @tablename = "usage_record";
SET @columnname = "price_version";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND COLUMN_NAME = @columnname
  ) > 0,
  "ALTER TABLE usage_record MODIFY COLUMN price_version INT DEFAULT 1 COMMENT '价格版本'",
  "ALTER TABLE usage_record ADD COLUMN price_version INT DEFAULT 1 COMMENT '价格版本'"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;
UPDATE usage_record SET price_version = 1 WHERE price_version IS NULL;