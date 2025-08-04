-- 应用密钥表
CREATE TABLE IF NOT EXISTS `app_secret` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `app_id` varchar(64) NOT NULL COMMENT '应用ID',
  `app_secret` varchar(128) NOT NULL COMMENT '应用密钥',
  `app_name` varchar(100) DEFAULT NULL COMMENT '应用名称',
  `description` varchar(500) DEFAULT NULL COMMENT '应用描述',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：1-启用，0-禁用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除：1-已删除，0-未删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_id` (`app_id`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用密钥表';

-- 插入测试数据
INSERT INTO `app_secret` (`app_id`, `app_secret`, `app_name`, `description`, `status`, `create_by`) VALUES
('test-app-001', 'secret_test_app_001', '测试应用001', '用于测试签名验证功能的应用', 1, 'system'),
('test-app-002', 'secret_test_app_002', '测试应用002', '用于测试签名验证功能的应用', 1, 'system'),
('demo-app-001', 'secret_demo_app_001', '演示应用001', '用于演示签名验证功能的应用', 1, 'system'); 