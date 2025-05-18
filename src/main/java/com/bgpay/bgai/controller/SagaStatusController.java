package com.bgpay.bgai.controller;

import com.bgpay.bgai.saga.CustomSagaJsonParser;
import io.seata.saga.engine.StateMachineEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Saga状态机状态控制器
 * 提供REST API查看Saga状态机的加载和解析状态
 */
@RestController
@RequestMapping("/api/saga")
public class SagaStatusController {

    @Autowired(required = false)
    private StateMachineEngine stateMachineEngine;
    
    @Autowired
    private CustomSagaJsonParser customSagaJsonParser;
    
    /**
     * 获取Saga状态机加载和解析状态
     * @return 包含状态机加载和解析信息的JSON响应
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSagaStatus() {
        Map<String, Object> result = new HashMap<>();
        
        // 基本状态信息
        result.put("enabled", stateMachineEngine != null);
        result.put("parserType", customSagaJsonParser.getJsonParserType());
        result.put("parsingResults", customSagaJsonParser.getParsingResults());
        result.put("parsingSummary", customSagaJsonParser.getParsingResultSummary());
        
        // 解析结果统计
        int success = 0;
        int failure = 0;
        Map<String, Boolean> parsingResults = customSagaJsonParser.getParsingResults();
        for (Boolean value : parsingResults.values()) {
            if (Boolean.TRUE.equals(value)) {
                success++;
            } else {
                failure++;
            }
        }
        
        Map<String, Integer> statistics = new HashMap<>();
        statistics.put("total", success + failure);
        statistics.put("success", success);
        statistics.put("failure", failure);
        result.put("statistics", statistics);
        
        // 添加状态机定义信息
        result.put("stateMachineDefinitions", customSagaJsonParser.getAllStateMachineDefinitions().keySet());
        
        return ResponseEntity.ok(result);
    }
} 