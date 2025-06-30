# Seata分布式事务故障排除指南

## 常见问题

### 1. 重复状态机定义导致应用启动失败

**错误信息**:
```
org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'dbStateMachineConfig'... Duplicate entry '000001-SEATA-ChatCompletionSaga-0.0.2' for key 'seata_state_machine_def.uq_tenant_id_app_name_name_ver'
```

**问题原因**:  
当应用程序重启或重新部署时，Seata会尝试自动注册状态机定义到数据库中。如果数据库中已经存在相同版本的状态机定义，就会导致唯一键约束冲突。

**解决方法**:

1. **禁用状态机自动注册**
   
   编辑 `file.conf` 文件，将 `auto-register` 设置为 `false`:

   ```conf
   saga {
     enabled = true
     state-machine {
       auto-register = false  # 修改为false
     }
   }
   ```

2. **Docker环境变量配置**
   
   在Docker运行命令中添加环境变量:

   ```bash
   docker run -p 8688:8688 -e SEATA_SAGA_STATE_MACHINE_AUTO_REGISTER=false jiangyang-ai:latest
   ```

3. **在应用代码中禁用**
   
   在应用启动前添加系统属性:

   ```java
   System.setProperty("seata.saga.state-machine.auto-register", "false");
   ```

4. **在极端情况下完全禁用Seata**

   如果需要临时绕过Seata相关问题，可以完全禁用Seata:

   ```bash
   docker run -p 8688:8688 -e SEATA_ENABLED=false -e SAGA_ENABLED=false jiangyang-ai:latest
   ```

### 2. 启动脚本说明

为简化部署，项目提供以下启动脚本:

- **Linux/Mac**: `run-docker.sh` - 构建并运行Docker容器
- **Windows**: `run-docker.bat` - 在Windows环境下构建并运行Docker容器

这些脚本会自动设置正确的环境变量来避免Seata状态机注册问题。

### 3. 查看Seata状态

访问以下API端点查看Saga状态机状态:

```
GET /api/saga/status
```

## 其他建议

1. 升级到较新版本的Seata，新版本可能解决了部分问题
2. 考虑使用其他分布式事务解决方案，例如基于补偿的方式
3. 对于开发环境，可以考虑使用内存模式而非数据库模式来存储状态机定义 