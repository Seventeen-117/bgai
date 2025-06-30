# 修复Seata状态机重复注册错误

## 问题描述

使用Docker build并运行应用时出现以下错误:

```
Error creating bean with name 'dbStateMachineConfig'... Duplicate entry '000001-SEATA-ChatCompletionSaga-0.0.2' for key 'seata_state_machine_def.uq_tenant_id_app_name_name_ver'
```

这是因为Seata Saga尝试重新注册已存在的状态机定义到数据库中。

## 解决方案

我们通过以下步骤修复了这个问题:

1. **修改file.conf配置**
   - 将`saga.state-machine.auto-register`设置为`false`

2. **创建自定义配置类**
   - 添加`SeataSagaConfig`类来覆盖默认的Seata配置
   - 提供自定义的`dbStateMachineConfig` Bean

3. **改进SagaStateMachineConfig类**
   - 添加启动日志明确说明自动注册已禁用
   - 依赖file.conf中的配置而非硬编码

4. **增强BgaiApplication类**
   - 添加Seata异常特殊处理
   - 提供错误信息和解决方案提示

5. **创建启动脚本和Dockerfile改进**
   - 添加`entrypoint.sh`脚本自动修复配置
   - 在Dockerfile中设置环境变量禁用自动注册
   - 提供Windows和Linux启动脚本

## 测试方法

1. **使用提供的脚本启动**:

   在Windows环境下:
   ```
   run-docker.bat
   ```

   在Linux/Mac环境下:
   ```
   ./run-docker.sh
   ```

2. **手动构建和运行**:

   ```bash
   docker build -f Dockerfile.quick -t jiangyang-ai:latest .
   docker run -p 8688:8688 --name jiangyang-ai -e SPRING_PROFILES_ACTIVE=dev -e SEATA_SAGA_STATE_MACHINE_AUTO_REGISTER=false jiangyang-ai:latest
   ```

3. **完全禁用Seata方案(适用于开发测试)**:

   ```bash
   docker run -p 8688:8688 --name jiangyang-ai -e SPRING_PROFILES_ACTIVE=dev -e SEATA_ENABLED=false -e SAGA_ENABLED=false jiangyang-ai:latest
   ```

## 查看状态机状态

应用启动后，可以访问以下端点查看状态机加载状态:

```
http://localhost:8688/api/saga/status
```

## 服务连接测试

服务启动后，可以通过以下方式测试API是否正常:

```bash
curl http://localhost:8688/actuator/health
```

如果返回`{"status":"UP"}`，则表示服务已正常启动。 
 