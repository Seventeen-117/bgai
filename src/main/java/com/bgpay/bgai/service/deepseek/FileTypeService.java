package com.bgpay.bgai.service.deepseek;


import cn.hutool.core.collection.ConcurrentHashSet;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bgpay.bgai.datasource.DS;
import com.bgpay.bgai.entity.AllowedFileType;
import com.bgpay.bgai.entity.MimeTypeConfig;
import com.bgpay.bgai.mapper.AllowedFileTypeMapper;
import com.bgpay.bgai.mapper.FileTypeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileTypeService {
    private final FileTypeMapper fileTypeMapper;
    private final AllowedFileTypeMapper allowedFileTypeMapper;

    private Map<String, MimeTypeConfig> mimeConfigCache = new ConcurrentHashMap<>();
    private Set<String> allowedTypesCache = new ConcurrentHashSet<>();
    private Map<String, MimeTypeConfig> extensionToMimeTypeConfig = new ConcurrentHashMap<>();

    @PostConstruct
    @Scheduled(fixedRate = 300000) // 5分钟刷新缓存
    public void refreshCache() {
        try {
            log.info("开始刷新文件类型缓存");
            
            // 获取MIME类型配置
            List<MimeTypeConfig> mimeConfigs = fileTypeMapper.selectActiveMimeTypes();
            if (mimeConfigs == null) {
                mimeConfigs = Collections.emptyList();
                log.warn("未能获取MIME类型配置，使用空列表");
            }
            
            mimeConfigCache = mimeConfigs.stream()
                    .collect(Collectors.toMap(
                            MimeTypeConfig::getMimeType,
                            Function.identity(),
                            (existing, replacement) -> existing
                    ));

            // 更新扩展名到MIME类型的映射
            extensionToMimeTypeConfig = new HashMap<>();
            for (MimeTypeConfig config : mimeConfigs) {
                if (config.getExtensions() != null) {
                    extensionToMimeTypeConfig.put(config.getExtensions().toLowerCase(), config);
                }
            }

            // 获取允许的文件类型 - 直接使用AllowedFileTypeMapper
            try {
                LambdaQueryWrapper<AllowedFileType> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.select(AllowedFileType::getMimeType)
                           .eq(AllowedFileType::getIsAllowed, true);
                
                List<String> allowedTypes = allowedFileTypeMapper.selectList(queryWrapper)
                                           .stream()
                                           .map(AllowedFileType::getMimeType)
                                           .collect(Collectors.toList());
                
                allowedTypesCache = new ConcurrentHashSet<>(allowedTypes);
                log.info("文件类型缓存刷新完成，共加载 {} 个MIME类型配置和 {} 个允许的文件类型",
                        mimeConfigCache.size(), allowedTypesCache.size());
            } catch (Exception e) {
                log.error("获取允许的文件类型失败", e);
                allowedTypesCache = new ConcurrentHashSet<>();
            }
        } catch (Exception e) {
            log.error("刷新文件类型缓存失败", e);
        }
    }
    
    /**
     * 获取所有允许的文件类型
     * @return 允许的文件类型列表
     */
    @DS("slave")
    public List<AllowedFileType> getAllowedFileTypes() {
        try {
            return allowedFileTypeMapper.selectList(null);
        } catch (Exception e) {
            log.error("获取允许的文件类型失败", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 获取所有MIME类型配置
     * @return MIME类型配置列表
     */
    @DS("slave")
    public List<MimeTypeConfig> getAllMimeTypeConfigs() {
        try {
            List<MimeTypeConfig> configs = fileTypeMapper.selectActiveMimeTypes();
            return configs != null ? configs : Collections.emptyList();
        } catch (Exception e) {
            log.error("获取MIME类型配置失败", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 添加允许的文件类型
     * @param fileType 文件类型实体
     * @return 保存后的实体
     */
    @Transactional
    @DS("master")
    public AllowedFileType addAllowedFileType(AllowedFileType fileType) {
        try {
            allowedFileTypeMapper.insert(fileType);
            refreshCache(); // 更新缓存
            return fileType;
        } catch (Exception e) {
            log.error("添加允许的文件类型失败: {}", fileType, e);
            throw e;
        }
    }
    
    /**
     * 删除允许的文件类型
     * @param id 文件类型ID
     */
    @Transactional
    @DS("master")
    public void deleteAllowedFileType(Long id) {
        try {
            allowedFileTypeMapper.deleteById(id);
            refreshCache(); // 更新缓存
        } catch (Exception e) {
            log.error("删除允许的文件类型失败: id={}", id, e);
            throw e;
        }
    }

    /**
     * 检查文件类型是否允许
     * @param contentType 内容类型
     * @return 是否允许
     */
    public boolean isAllowedType(String contentType) {
        if (contentType == null) {
            return false;
        }
        return allowedTypesCache.contains(contentType.toLowerCase());
    }

    /**
     * 验证文件的魔数与声明的内容类型是否匹配
     * @param file 文件
     * @param contentType 内容类型
     * @return 是否匹配
     */
    public boolean validateFileMagic(File file, String contentType) {
        if (file == null || contentType == null) {
            return false;
        }
        
        MimeTypeConfig config = mimeConfigCache.get(contentType.toLowerCase());
        if (config == null || config.getMagicNumbers() == null || config.getMagicNumbers().isEmpty()) {
            // 如果没有魔数配置，默认为有效
            return true;
        }

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            byte[] buffer = new byte[Math.min(50, (int)file.length())];
            raf.readFully(buffer);
            String hexString = bytesToHex(buffer);

            // 检查文件的魔数是否与配置的魔数匹配
            return hexString.startsWith(config.getMagicNumbers().toLowerCase());
        } catch (IOException e) {
            log.error("验证文件魔数失败", e);
            return false;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public Map<String, MimeTypeConfig> getExtensionToMimeTypeConfig() {
        return extensionToMimeTypeConfig;
    }
}