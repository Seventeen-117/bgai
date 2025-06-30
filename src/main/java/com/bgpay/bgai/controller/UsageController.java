package com.bgpay.bgai.controller;

import com.bgpay.bgai.entity.BatchRequest;
import com.bgpay.bgai.entity.UsageRecord;
import com.bgpay.bgai.mapper.UsageRecordMapper;
import com.bgpay.bgai.response.PageResponse;
import com.bgpay.bgai.service.BillingService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/usage")
@RequiredArgsConstructor
public class UsageController {
    private final RocketMQTemplate rocketMQTemplate;
    private final BillingService billingService;
    private final UsageRecordMapper recordMapper;

    @PostMapping("/batch")
    public ResponseEntity<Void> processBatch(
            @Valid @RequestBody BatchRequest request,
            @RequestParam(required = false) String userId,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {
        
        // 如果请求参数中没有提供userId，则尝试从请求头中获取
        String effectiveUserId = userId;
        if (effectiveUserId == null || effectiveUserId.isEmpty()) {
            effectiveUserId = headerUserId;
        }
        
        // 如果两者都为空，使用默认值
        if (effectiveUserId == null || effectiveUserId.isEmpty()) {
            effectiveUserId = "anonymous";
        }
        
        billingService.processBatch(request.getRecords(), effectiveUserId);
        return ResponseEntity.accepted().build();
    }

    @GetMapping
    public PageResponse<UsageRecord> queryRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String modelType) {
        PageHelper.startPage(page, size);
        List<UsageRecord> records = recordMapper.findByModel(modelType);
        PageInfo<UsageRecord> pageInfo = new PageInfo<>(records);
        return new PageResponse<>(pageInfo.getList(),
                pageInfo.getTotal(),
                pageInfo.getPageNum(),
                pageInfo.getPageSize());
    }


    @GetMapping("/send")
    public String sendMessage() {
        rocketMQTemplate.convertAndSend("test-topic", "Hello RocketMQ!");
        return "Message sent!";
    }
}