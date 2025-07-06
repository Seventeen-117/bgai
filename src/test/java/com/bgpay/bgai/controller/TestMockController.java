package com.bgpay.bgai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.Map;

/**
 * 测试专用的模拟控制器接口
 * 用于解决泛型问题
 */
public interface TestMockController {
    
    /**
     * 模拟处理请求
     * @param request 请求参数
     * @return 响应
     */
    ResponseEntity<Map<String, Object>> handleRequest(@RequestBody Map<String, Object> request);
} 