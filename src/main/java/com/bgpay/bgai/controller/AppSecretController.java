package com.bgpay.bgai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bgpay.bgai.entity.AppSecret;
import com.bgpay.bgai.mapper.AppSecretMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 应用密钥管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/app-secret")
@RequiredArgsConstructor
public class AppSecretController {

    private final AppSecretMapper appSecretMapper;

    /**
     * 分页查询应用密钥列表
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String appId,
            @RequestParam(required = false) String appName) {
        
        Page<AppSecret> page = new Page<>(current, size);
        QueryWrapper<AppSecret> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("deleted", 0);
        
        if (appId != null && !appId.isEmpty()) {
            queryWrapper.like("app_id", appId);
        }
        if (appName != null && !appName.isEmpty()) {
            queryWrapper.like("app_name", appName);
        }
        
        queryWrapper.orderByDesc("create_time");
        
        Page<AppSecret> result = appSecretMapper.selectPage(page, queryWrapper);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", result);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 根据ID查询应用密钥
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        AppSecret appSecret = appSecretMapper.selectById(id);
        
        Map<String, Object> response = new HashMap<>();
        if (appSecret != null && appSecret.getDeleted() == 0) {
            response.put("success", true);
            response.put("data", appSecret);
        } else {
            response.put("success", false);
            response.put("message", "应用密钥不存在");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 根据应用ID查询应用密钥
     */
    @GetMapping("/by-app-id/{appId}")
    public ResponseEntity<Map<String, Object>> getByAppId(@PathVariable String appId) {
        AppSecret appSecret = appSecretMapper.selectByAppId(appId);
        
        Map<String, Object> response = new HashMap<>();
        if (appSecret != null) {
            response.put("success", true);
            response.put("data", appSecret);
        } else {
            response.put("success", false);
            response.put("message", "应用密钥不存在");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 创建应用密钥
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody AppSecret appSecret) {
        // 检查应用ID是否已存在
        AppSecret existing = appSecretMapper.selectByAppId(appSecret.getAppId());
        if (existing != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "应用ID已存在");
            return ResponseEntity.badRequest().body(response);
        }
        
        // 设置默认值
        appSecret.setId(null);
        appSecret.setStatus(1);
        appSecret.setDeleted(0);
        appSecret.setCreateTime(LocalDateTime.now());
        appSecret.setUpdateTime(LocalDateTime.now());
        
        int result = appSecretMapper.insert(appSecret);
        
        Map<String, Object> response = new HashMap<>();
        if (result > 0) {
            response.put("success", true);
            response.put("message", "创建成功");
            response.put("data", appSecret);
        } else {
            response.put("success", false);
            response.put("message", "创建失败");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 更新应用密钥
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id, @RequestBody AppSecret appSecret) {
        AppSecret existing = appSecretMapper.selectById(id);
        if (existing == null || existing.getDeleted() == 1) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "应用密钥不存在");
            return ResponseEntity.badRequest().body(response);
        }
        
        // 检查应用ID是否被其他记录使用
        if (!existing.getAppId().equals(appSecret.getAppId())) {
            AppSecret duplicate = appSecretMapper.selectByAppId(appSecret.getAppId());
            if (duplicate != null && !duplicate.getId().equals(id)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "应用ID已被其他记录使用");
                return ResponseEntity.badRequest().body(response);
            }
        }
        
        appSecret.setId(id);
        appSecret.setUpdateTime(LocalDateTime.now());
        
        int result = appSecretMapper.updateById(appSecret);
        
        Map<String, Object> response = new HashMap<>();
        if (result > 0) {
            response.put("success", true);
            response.put("message", "更新成功");
        } else {
            response.put("success", false);
            response.put("message", "更新失败");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 删除应用密钥（逻辑删除）
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        AppSecret appSecret = appSecretMapper.selectById(id);
        if (appSecret == null || appSecret.getDeleted() == 1) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "应用密钥不存在");
            return ResponseEntity.badRequest().body(response);
        }
        
        appSecret.setDeleted(1);
        appSecret.setUpdateTime(LocalDateTime.now());
        
        int result = appSecretMapper.updateById(appSecret);
        
        Map<String, Object> response = new HashMap<>();
        if (result > 0) {
            response.put("success", true);
            response.put("message", "删除成功");
        } else {
            response.put("success", false);
            response.put("message", "删除失败");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 启用/禁用应用密钥
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {
        
        AppSecret appSecret = appSecretMapper.selectById(id);
        if (appSecret == null || appSecret.getDeleted() == 1) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "应用密钥不存在");
            return ResponseEntity.badRequest().body(response);
        }
        
        appSecret.setStatus(status);
        appSecret.setUpdateTime(LocalDateTime.now());
        
        int result = appSecretMapper.updateById(appSecret);
        
        Map<String, Object> response = new HashMap<>();
        if (result > 0) {
            response.put("success", true);
            response.put("message", status == 1 ? "启用成功" : "禁用成功");
        } else {
            response.put("success", false);
            response.put("message", "操作失败");
        }
        
        return ResponseEntity.ok(response);
    }
} 