package com.bgpay.bgai.service.deepseek;


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

    private Map<String, MimeTypeConfig> mimeConfigCache = new ConcurrentHashMap<>();
    private Set<String> allowedTypesCache = new ConcurrentHashSet<>();
    private Map<String, MimeTypeConfig> extensionToMimeTypeConfig = new ConcurrentHashMap<>();

    @PostConstruct
    @DS("master")
    @Scheduled(fixedRate = 300000) // 5分钟刷新缓存
    public void refreshCache() {
        List<MimeTypeConfig> mimeConfigs = fileTypeMapper.selectActiveMimeTypes();
        mimeConfigCache = mimeConfigs.stream()
                .collect(Collectors.toMap(
                        MimeTypeConfig::getMimeType,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        extensionToMimeTypeConfig = new HashMap<>();
        for (MimeTypeConfig config : mimeConfigs) {
            if (config.getExtensions()!=null) {
                extensionToMimeTypeConfig.put(config.getExtensions().toLowerCase(), config);
            }
        }

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
            return true;
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

    public Map<String, MimeTypeConfig> getExtensionToMimeTypeConfig() {
        return Collections.unmodifiableMap(extensionToMimeTypeConfig);
    }
}