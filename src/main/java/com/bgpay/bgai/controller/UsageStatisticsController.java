package com.bgpay.bgai.controller;

import com.bgpay.bgai.entity.UsageCalculationDTO;
import com.bgpay.bgai.entity.UsageInfo;
import com.bgpay.bgai.entity.UsageRecord;
import com.bgpay.bgai.response.PageResponse;
import com.bgpay.bgai.service.BillingService;
import com.bgpay.bgai.service.UsageInfoService;
import com.bgpay.bgai.service.UsageRecordService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用量统计控制器
 * 提供用量查询和计费相关接口
 */
@RestController
@RequestMapping("/api/usage-stats")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "用量统计", description = "用量查询与计费相关接口")
public class UsageStatisticsController {

    private final UsageRecordService usageRecordService;
    private final UsageInfoService usageInfoService;
    private final BillingService billingService;

    /**
     * 分页查询用户的用量记录
     */
    @Operation(summary = "查询用户用量记录", description = "分页查询用户的用量记录，支持按模型类型和时间范围过滤")
    @GetMapping("/records")
    public PageResponse<UsageRecord> getUserUsageRecords(
            @Parameter(description = "用户ID") @RequestParam String userId,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "模型类型") @RequestParam(required = false) String modelType,
            @Parameter(description = "开始日期，格式：yyyy-MM-dd") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        PageHelper.startPage(page, size);
        
        // 转换日期范围为时间范围
        LocalDateTime startDateTime = startDate != null ? 
                LocalDateTime.of(startDate, LocalTime.MIN) : null;
        LocalDateTime endDateTime = endDate != null ? 
                LocalDateTime.of(endDate, LocalTime.MAX) : null;
        
        List<UsageRecord> records = usageRecordService.findUserUsageRecords(
                userId, modelType, startDateTime, endDateTime);
        
        PageInfo<UsageRecord> pageInfo = new PageInfo<>(records);
        return new PageResponse<>(
                pageInfo.getList(),
                pageInfo.getTotal(),
                pageInfo.getPageNum(),
                pageInfo.getPageSize());
    }

    /**
     * 获取用户的用量汇总信息
     */
    @Operation(summary = "获取用户用量汇总", description = "获取用户在指定时间范围内的用量汇总信息")
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getUserUsageSummary(
            @Parameter(description = "用户ID") @RequestParam String userId,
            @Parameter(description = "开始日期，格式：yyyy-MM-dd") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        // 转换日期范围为时间范围
        LocalDateTime startDateTime = startDate != null ? 
                LocalDateTime.of(startDate, LocalTime.MIN) : null;
        LocalDateTime endDateTime = endDate != null ? 
                LocalDateTime.of(endDate, LocalTime.MAX) : null;
        
        // 获取用户在指定时间范围内的用量汇总
        Map<String, Object> summary = usageRecordService.getUserUsageSummary(
                userId, startDateTime, endDateTime);
        
        return ResponseEntity.ok(summary);
    }

    /**
     * 按模型类型获取用户的用量统计
     */
    @Operation(summary = "按模型类型获取用量统计", description = "获取用户在指定时间范围内按模型类型分组的用量统计")
    @GetMapping("/by-model")
    public ResponseEntity<List<Map<String, Object>>> getUserUsageByModel(
            @Parameter(description = "用户ID") @RequestParam String userId,
            @Parameter(description = "开始日期，格式：yyyy-MM-dd") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        // 转换日期范围为时间范围
        LocalDateTime startDateTime = startDate != null ? 
                LocalDateTime.of(startDate, LocalTime.MIN) : null;
        LocalDateTime endDateTime = endDate != null ? 
                LocalDateTime.of(endDate, LocalTime.MAX) : null;
        
        // 获取用户在指定时间范围内按模型分组的用量统计
        List<Map<String, Object>> modelStats = usageRecordService.getUserUsageByModel(
                userId, startDateTime, endDateTime);
        
        return ResponseEntity.ok(modelStats);
    }

    /**
     * 获取用户的每日用量趋势
     */
    @Operation(summary = "获取每日用量趋势", description = "获取用户在指定时间范围内的每日用量趋势数据")
    @GetMapping("/daily-trend")
    public ResponseEntity<List<Map<String, Object>>> getUserDailyUsageTrend(
            @Parameter(description = "用户ID") @RequestParam String userId,
            @Parameter(description = "开始日期，格式：yyyy-MM-dd") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        // 转换日期范围为时间范围
        LocalDateTime startDateTime = startDate != null ? 
                LocalDateTime.of(startDate, LocalTime.MIN) : null;
        LocalDateTime endDateTime = endDate != null ? 
                LocalDateTime.of(endDate, LocalTime.MAX) : null;
        
        // 获取用户在指定时间范围内的每日用量趋势
        List<Map<String, Object>> dailyTrend = usageRecordService.getUserDailyUsageTrend(
                userId, startDateTime, endDateTime);
        
        return ResponseEntity.ok(dailyTrend);
    }

    /**
     * 手动计费接口
     */
    @Operation(summary = "手动计费", description = "手动创建用量记录并进行计费")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "计费成功"),
        @ApiResponse(responseCode = "400", description = "计费失败")
    })
    @PostMapping("/calculate")
    public ResponseEntity<Map<String, Object>> calculateUsage(
            @Parameter(description = "用户ID") @RequestParam String userId,
            @Parameter(description = "模型类型") @RequestParam String modelType,
            @Parameter(description = "提示词token数") @RequestParam Integer promptTokens,
            @Parameter(description = "完成词token数") @RequestParam Integer completionTokens) {
        
        // 创建计算DTO
        UsageCalculationDTO dto = new UsageCalculationDTO();
        dto.setChatCompletionId("manual-" + System.currentTimeMillis());
        dto.setModelType(modelType);
        dto.setPromptCacheMissTokens(promptTokens);
        dto.setPromptCacheHitTokens(0);
        dto.setCompletionTokens(completionTokens);
        
        // 处理用量信息
        boolean success = usageInfoService.processUsageInfo(dto, userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "计费成功" : "计费失败");
        result.put("completionId", dto.getChatCompletionId());
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取当前价格配置
     */
    @Operation(summary = "获取价格配置", description = "获取系统当前的价格配置信息")
    @GetMapping("/price-config")
    public ResponseEntity<Map<String, Object>> getPriceConfig() {
        Map<String, Object> priceConfig = usageRecordService.getCurrentPriceConfig();
        return ResponseEntity.ok(priceConfig);
    }

    /**
     * 获取系统用量统计（管理员接口）
     */
    @Operation(summary = "获取系统用量统计", description = "管理员接口：获取系统级别的用量统计数据")
    @GetMapping("/admin/system-stats")
    public ResponseEntity<Map<String, Object>> getSystemStats(
            @Parameter(description = "开始日期，格式：yyyy-MM-dd") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        // 转换日期范围为时间范围
        LocalDateTime startDateTime = startDate != null ? 
                LocalDateTime.of(startDate, LocalTime.MIN) : null;
        LocalDateTime endDateTime = endDate != null ? 
                LocalDateTime.of(endDate, LocalTime.MAX) : null;
        
        // 获取系统级别的用量统计
        Map<String, Object> systemStats = usageRecordService.getSystemUsageStats(
                startDateTime, endDateTime);
        
        return ResponseEntity.ok(systemStats);
    }
} 