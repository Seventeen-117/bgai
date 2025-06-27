-- 创建MIME类型配置表
CREATE TABLE IF NOT EXISTS `mime_type_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `mime_type` varchar(100) NOT NULL COMMENT 'MIME类型',
  `extensions` varchar(50) DEFAULT NULL COMMENT '文件扩展名',
  `magic_numbers` varchar(255) DEFAULT NULL COMMENT '文件魔数',
  `is_active` tinyint(1) DEFAULT 1 COMMENT '是否启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mime_type` (`mime_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MIME类型配置表';

-- 创建允许的文件类型表
CREATE TABLE IF NOT EXISTS `allowed_file_type` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `mime_type` varchar(100) NOT NULL COMMENT 'MIME类型',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `is_allowed` tinyint(1) DEFAULT 1 COMMENT '是否允许',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mime_type` (`mime_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='允许的文件类型表';

-- 插入常用MIME类型配置
INSERT INTO `mime_type_config` (`mime_type`, `extensions`, `magic_numbers`, `is_active`) VALUES
('text/plain', 'txt', NULL, 1),
('text/html', 'html', '3c68746d6c', 1),
('text/css', 'css', NULL, 1),
('text/javascript', 'js', NULL, 1),
('application/json', 'json', NULL, 1),
('application/xml', 'xml', '3c3f786d6c', 1),
('application/pdf', 'pdf', '25504446', 1),
('application/msword', 'doc', 'd0cf11e0', 1),
('application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'docx', '504b0304', 1),
('application/vnd.ms-excel', 'xls', 'd0cf11e0', 1),
('application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 'xlsx', '504b0304', 1),
('application/vnd.ms-powerpoint', 'ppt', 'd0cf11e0', 1),
('application/vnd.openxmlformats-officedocument.presentationml.presentation', 'pptx', '504b0304', 1),
('image/jpeg', 'jpg', 'ffd8ff', 1),
('image/png', 'png', '89504e47', 1),
('image/gif', 'gif', '474946', 1),
('image/svg+xml', 'svg', '3c737667', 1),
('audio/mpeg', 'mp3', '494433', 1),
('video/mp4', 'mp4', '00000020', 1),
('application/zip', 'zip', '504b0304', 1);

-- 插入允许的文件类型
INSERT INTO `allowed_file_type` (`mime_type`, `description`, `is_allowed`) VALUES
('text/plain', '纯文本文件', 1),
('text/html', 'HTML文件', 1),
('application/json', 'JSON文件', 1),
('application/xml', 'XML文件', 1),
('application/pdf', 'PDF文件', 1),
('application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'Word文档', 1),
('application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 'Excel表格', 1),
('application/vnd.openxmlformats-officedocument.presentationml.presentation', 'PowerPoint演示文稿', 1),
('image/jpeg', 'JPEG图片', 1),
('image/png', 'PNG图片', 1); 