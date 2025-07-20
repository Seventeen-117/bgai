# SQL脚本目录结构

本目录包含系统所需的所有SQL脚本，按照功能和用途分为以下三个子目录：

## 1. schema - 数据库表结构定义

存放所有表结构定义SQL脚本：

- **api_client_api_key_schema.sql**: API客户端和API密钥表结构
- **business_undo_log.sql**: 分布式事务业务撤销日志表
- **seata_tables.sql**: Seata分布式事务相关表
- **undo_log.sql**: 事务撤销日志表
- **usage_info_schema.sql**: 用量信息表结构
- **user_schema.sql**: 用户表结构

## 2. migration - 数据库升级脚本

存放所有数据库结构变更和升级脚本，按照版本号和变更内容命名：

- **V1.0.1__Add_BranchIds_Column.sql**: 添加BranchIds列
- **V2025_03_10_01__alter_usage_record_add_columns.sql**: 为usage_record表添加列
- **V2025_03_10_02__alter_usage_record_add_message_id.sql**: 为usage_record表添加message_id
- **V2025_03_10_03__alter_usage_record_add_price_version_default.sql**: 为usage_record表添加price_version默认值
- **V2025_03_10_04__alter_usage_info_add_defaults.sql**: 为usage_info表添加默认值
- **V2025_03_21_01__alter_usage_info_add_columns.sql**: 为usage_info表添加列
- **V2025_03_21_02__alter_usage_info_add_cache_columns.sql**: 为usage_info表添加缓存相关列

## 3. init - 初始化数据脚本

存放系统初始化数据脚本：

- **admin_init.sql**: 管理员和初始配置数据

## 使用方法

- 初始安装时，先执行schema目录中的表结构创建脚本，再执行init目录中的初始化数据脚本
- 升级现有系统时，按照日期顺序执行migration目录中的升级脚本 