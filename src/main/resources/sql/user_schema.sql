-- 创建用户表
CREATE TABLE IF NOT EXISTS `t_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` varchar(64) NOT NULL COMMENT '用户唯一标识，来自SSO',
  `username` varchar(64) NOT NULL COMMENT '用户名',
  `email` varchar(128) DEFAULT NULL COMMENT '用户邮箱',
  `avatar_url` varchar(255) DEFAULT NULL COMMENT '用户头像URL',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `access_token` varchar(1024) DEFAULT NULL COMMENT 'SSO访问令牌',
  `refresh_token` varchar(1024) DEFAULT NULL COMMENT 'SSO刷新令牌',
  `token_expire_time` datetime DEFAULT NULL COMMENT '令牌过期时间',
  `status` tinyint(4) DEFAULT '1' COMMENT '用户状态(1:启用,0:禁用)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_email` (`email`),
  KEY `idx_access_token` (`access_token`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SSO用户信息表';

-- 添加索引
CREATE INDEX IF NOT EXISTS `idx_last_login_time` ON `t_user` (`last_login_time`);
CREATE INDEX IF NOT EXISTS `idx_token_expire_time` ON `t_user` (`token_expire_time`); 