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
  `role_type` varchar(20) DEFAULT 'USER' COMMENT '角色类型：USER-普通用户，ADMIN-管理员',
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
CREATE INDEX IF NOT EXISTS `idx_role_type` ON `t_user` (`role_type`);

-- 为表添加密码字段（用于本地登录或作为备份验证方式）
ALTER TABLE `t_user` 
ADD COLUMN IF NOT EXISTS `password` varchar(128) DEFAULT NULL COMMENT '密码（可选，用于本地登录）' AFTER `email`;

-- 确保已有的管理员账户有正确的角色类型
UPDATE `t_user` SET `role_type` = 'ADMIN' WHERE `user_id` LIKE 'admin-%' AND `role_type` IS NULL; 