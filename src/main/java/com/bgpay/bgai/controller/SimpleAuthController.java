package com.bgpay.bgai.controller;

import com.bgpay.bgai.entity.UserToken;
import com.bgpay.bgai.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 简单认证控制器，提供更简单的令牌获取方式用于测试
 */
@RestController
@RequestMapping("/api/simple-auth")
@Slf4j
public class SimpleAuthController {

    @Autowired
    private UserService userService;
    
    /**
     * 直接获取测试令牌，无需完整的OAuth流程
     * 仅用于开发测试，生产环境应禁用
     */
    @GetMapping("/token")
    public ResponseEntity<Map<String, Object>> getTestToken() {
        String testToken = UUID.randomUUID().toString();
        
        // 创建用户令牌
        UserToken userToken = UserToken.builder()
//                .userId("test-user-" + UUID.randomUUID().toString().substring(0, 8))
                .userId("689258T")
                .username("测试用户")
                .email("test@example.com")
                .accessToken(testToken)
                .tokenExpireTime(LocalDateTime.now().plusDays(1))
                .loginTime(LocalDateTime.now())
                .valid(true)
                .build();
        
        // 将令牌保存到缓存和数据库
        // 这里依赖于UserService的实现将token存入缓存
        // 在实际实现中需要确保validateToken可以正确识别这个token
        userService.validateToken(testToken);
        
        Map<String, Object> response = new HashMap<>();
        response.put("access_token", testToken);
        response.put("token_type", "Bearer");
        response.put("expires_in", 86400);
        response.put("user_id", userToken.getUserId());
        
        log.info("生成测试令牌: {}", testToken);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取curl示例命令，方便测试
     */
    @GetMapping("/curl-example")
    public ResponseEntity<Map<String, String>> getCurlExample() {
        Map<String, String> examples = new HashMap<>();
        
        examples.put("获取测试令牌", "curl -X GET http://localhost:8080/api/simple-auth/token");
        
        examples.put("使用令牌访问chatGatWay示例", 
                "curl -X POST \\\n" +
                "  http://localhost:8080/api/chatGatWay \\\n" +
                "  -H 'Authorization: Bearer YOUR_TOKEN_HERE' \\\n" +
                "  -F 'question=请分析' \\\n" +
                "  -F 'file=@/path/to/file.pdf'");
        
        return ResponseEntity.ok(examples);
    }
} 