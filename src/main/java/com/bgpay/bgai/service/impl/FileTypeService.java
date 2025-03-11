package com.bgpay.bgai.service.impl;


import cn.hutool.core.collection.ConcurrentHashSet;
import com.bgpay.bgai.datasource.DS;
import com.bgpay.bgai.entity.MimeTypeConfig;
import com.bgpay.bgai.mapper.FileTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileTypeService {
    private final FileTypeMapper fileTypeMapper;

    // 内存缓存
    private Map<String, MimeTypeConfig> mimeConfigCache = new ConcurrentHashMap<>();
    private Set<String> allowedTypesCache = new ConcurrentHashSet<>();
    // 新增：扩展名到 MimeTypeConfig 的映射缓存
    private Map<String, MimeTypeConfig> extensionToMimeTypeConfig = new ConcurrentHashMap<>();

    @PostConstruct
    @DS("master")
    @Scheduled(fixedRate = 300000) // 5分钟刷新缓存
    public void refreshCache() {
        // 加载MIME类型配置
        List<MimeTypeConfig> mimeConfigs = fileTypeMapper.selectActiveMimeTypes();
        mimeConfigCache = mimeConfigs.stream()
                .collect(Collectors.toMap(
                        MimeTypeConfig::getMimeType,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        // 构建扩展名到 MimeTypeConfig 的映射
        extensionToMimeTypeConfig = new HashMap<>();
        for (MimeTypeConfig config : mimeConfigs) {
            if (config.getExtensions()!=null) {
                extensionToMimeTypeConfig.put(config.getExtensions().toLowerCase(), config);
            }
        }

        // 加载允许类型
        List<String> allowedTypes = fileTypeMapper.selectAllowedTypes();
        allowedTypesCache = new ConcurrentHashSet<>(allowedTypes);
    }

    public boolean isAllowedType(String mimeType) {
        return allowedTypesCache.contains(mimeType.toLowerCase());
    }

    public MimeTypeConfig getMimeConfig(String mimeType) {
        return mimeConfigCache.get(mimeType.toLowerCase());
    }

    public boolean validateFileMagic(File file, String mimeType) throws IOException {
        MimeTypeConfig config = getMimeConfig(mimeType);
        if (config == null || config.getMagicBytes().length == 0) {
            return true; // 无魔数配置时跳过验证
        }

        byte[] expectedMagic = config.getMagicBytes();
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            byte[] actualMagic = new byte[expectedMagic.length];
            raf.read(actualMagic);
            return Arrays.equals(expectedMagic, actualMagic);
        }
    }

    public Map<String, MimeTypeConfig> getMimeConfigs() {
        return Collections.unmodifiableMap(mimeConfigCache);
    }

    // 新增：获取扩展名到 MimeTypeConfig 的映射
    public Map<String, MimeTypeConfig> getExtensionToMimeTypeConfig() {
        return Collections.unmodifiableMap(extensionToMimeTypeConfig);
    }
}