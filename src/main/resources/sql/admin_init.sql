-- 初始管理员账户创建脚本
-- 在系统初始化时执行，创建默认的管理员账户

-- 检查是否已存在管理员账户
INSERT INTO t_user (user_id, username, email, password, create_time, update_time, last_login_time, role_type)
SELECT 'admin-system', '系统管理员', 'admin@example.com', 
       'e10adc3949ba59abbe56e057f20f883e', -- md5('123456')
       NOW(), NOW(), NOW(), 'ADMIN'
WHERE NOT EXISTS (
    SELECT 1 FROM t_user WHERE user_id LIKE 'admin-%'
);

-- 为管理员账户生成初始token
UPDATE t_user 
SET access_token = UUID(), 
    refresh_token = UUID(),
    token_expire_time = DATE_ADD(NOW(), INTERVAL 30 DAY)
WHERE user_id = 'admin-system'
  AND (access_token IS NULL OR access_token = '');

-- 输出管理员token信息，部署后执行脚本可查看
SELECT user_id, access_token, token_expire_time 
FROM t_user 
WHERE user_id = 'admin-system'; 