package com.bgpay.bgai.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试用的chatGatWay-internal接口模拟控制器
 * 实际开发中应替换为真实的接口实现
 */
@RestController
@RequestMapping("/api")
@Slf4j
public class TestChatGatWayInternalController {

    @PostMapping("/chatGatWay-internal")
    public ResponseEntity<?> mockChatGatWayInternal(@RequestBody Map<String, Object> request) {
        log.info("接收到chatGatWay-internal请求: {}", request);
        
        String prompt = request.containsKey("prompt") ? 
            request.get("prompt").toString() : 
            "默认测试用例";
            
        String type = request.containsKey("testCaseType") ? 
            request.get("testCaseType").toString() : 
            "api";
        
        // 根据不同的测试类型生成不同的测试用例内容
        String content = generateMockContent(type, prompt);
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("status", "success");
        
        return ResponseEntity.ok(response);
    }
    
    private String generateMockContent(String type, String prompt) {
        switch (type) {
            case "api":
                return """
                # API接口测试用例
                
                ## 接口名称: 用户登录
                
                ### 测试场景1: 正常登录
                
                **步骤:**
                1. 请求URL: /api/login
                2. 请求方法: POST
                3. 请求参数: {"username": "testuser", "password": "password123"}
                
                **预期结果:**
                - 状态码: 200
                - 返回内容: {"code": 0, "message": "登录成功", "data": {"userId": "123", "token": "abc123"}}
                
                ### 测试场景2: 用户名不存在
                
                **步骤:**
                1. 请求URL: /api/login
                2. 请求方法: POST
                3. 请求参数: {"username": "nonexistent", "password": "password123"}
                
                **预期结果:**
                - 状态码: 400
                - 返回内容: {"code": 1001, "message": "用户名不存在"}
                
                ### 测试场景3: 密码错误
                
                **步骤:**
                1. 请求URL: /api/login
                2. 请求方法: POST
                3. 请求参数: {"username": "testuser", "password": "wrongpassword"}
                
                **预期结果:**
                - 状态码: 400
                - 返回内容: {"code": 1002, "message": "密码错误"}
                """;
                
            case "functional":
                return """
                # 功能测试用例
                
                ## 功能名称: 用户登录
                
                ### 测试场景1: 正常登录
                
                **前置条件:**
                - 用户已注册
                - 登录页面正常加载
                
                **测试步骤:**
                1. 访问登录页面
                2. 在用户名输入框中输入有效用户名
                3. 在密码输入框中输入正确密码
                4. 点击"登录"按钮
                
                **预期结果:**
                - 登录成功，跳转到首页
                - 页面上显示用户名
                - 右上角显示用户头像
                
                ### 测试场景2: 记住密码功能
                
                **前置条件:**
                - 用户已注册
                - 登录页面正常加载
                
                **测试步骤:**
                1. 访问登录页面
                2. 在用户名输入框中输入有效用户名
                3. 在密码输入框中输入正确密码
                4. 勾选"记住密码"复选框
                5. 点击"登录"按钮
                6. 退出登录
                7. 重新访问登录页面
                
                **预期结果:**
                - 用户名和密码字段已自动填充
                - "记住密码"复选框已被勾选
                """;
                
            case "performance":
                return """
                # 性能测试用例
                
                ## 测试目标: 用户登录接口性能测试
                
                ### 基本配置
                
                **接口URL:** /api/login
                **请求方法:** POST
                **请求体:**
                ```json
                {
                  "username": "testuser",
                  "password": "password123"
                }
                ```
                
                ### 测试场景1: 负载测试
                
                **配置:**
                - 并发用户数: 500
                - 持续时间: 10分钟
                - 递增策略: 每30秒增加100用户
                
                **性能指标:**
                - 平均响应时间 < 200ms
                - 95%响应时间 < 500ms
                - 错误率 < 1%
                - TPS > 200/s
                
                ### 测试场景2: 压力测试
                
                **配置:**
                - 并发用户数: 2000
                - 持续时间: 5分钟
                - 递增策略: 每15秒增加200用户
                
                **性能指标:**
                - 平均响应时间 < 500ms
                - 95%响应时间 < 1000ms
                - 错误率 < 5%
                - TPS > 500/s
                """;
                
            case "security":
                return """
                # 安全测试用例
                
                ## 功能名称: 用户登录
                
                ### 测试场景1: SQL注入测试
                
                **测试步骤:**
                1. 访问登录页面
                2. 在用户名输入框中输入: admin' OR '1'='1
                3. 在密码输入框中输入任意内容
                4. 点击"登录"按钮
                
                **预期结果:**
                - 系统不应通过验证
                - 应显示错误信息"无效的用户名或密码"
                - 不应暴露任何SQL错误信息
                
                ### 测试场景2: XSS攻击测试
                
                **测试步骤:**
                1. 访问登录页面
                2. 在用户名输入框中输入: <script>alert('XSS')</script>
                3. 提交表单
                
                **预期结果:**
                - 浏览器不应执行JavaScript代码
                - 输入应被正确转义
                """;
                
            case "compatibility":
                return """
                # 兼容性测试用例
                
                ## 功能名称: 用户登录
                
                ### 测试环境
                
                | 环境ID | 操作系统 | 浏览器 | 版本 |
                |-------|---------|------|------|
                | E1 | Windows 10 | Chrome | 最新版 |
                | E2 | Windows 10 | Firefox | 最新版 |
                | E3 | Windows 10 | Edge | 最新版 |
                | E4 | macOS | Safari | 最新版 |
                | E5 | Android | Chrome | 最新版 |
                | E6 | iOS | Safari | 最新版 |
                
                ### 测试场景
                
                | 用例ID | 环境ID | 测试场景 | 期望结果 |
                |-------|-------|---------|----------|
                | TC001 | E1-E6 | 登录页面加载 | 页面布局正常，所有元素可见 |
                | TC002 | E1-E6 | 输入验证 | 正确显示错误提示信息 |
                | TC003 | E1-E6 | 登录成功 | 正确跳转到首页 |
                | TC004 | E1-E6 | 记住密码功能 | 下次访问时自动填充凭据 |
                | TC005 | E5-E6 | 移动端自适应 | 在手机上显示合适的布局 |
                """;
                
            default:
                return "未支持的测试用例类型: " + type;
        }
    }
} 