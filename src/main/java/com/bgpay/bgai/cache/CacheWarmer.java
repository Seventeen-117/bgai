package com.bgpay.bgai.cache;

import com.bgpay.bgai.service.deepseek.FileTypeService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 应用启动时预热缓存的组件
 */
@Component
public class CacheWarmer {

    private final Logger log = LoggerFactory.getLogger(CacheWarmer.class);
    
    private final FileTypeService fileTypeService;
    
    @Value("${cache.warmer.enabled:true}")
    private boolean enabled;

    @Autowired
    public CacheWarmer(FileTypeService fileTypeService) {
        this.fileTypeService = fileTypeService;
    }

    @PostConstruct
    public void warmUpCaches() {
        if (!enabled) {
            log.info("Cache warming is disabled");
            return;
        }
        
        log.info("Starting to warm up caches...");
        
        try {
            // 预热文件类型缓存
            fileTypeService.getAllowedFileTypes();
            log.info("FileTypes cache warmed up successfully");
            
            // 可以添加其他需要预热的缓存
            
        } catch (Exception e) {
            log.error("Error during cache warming", e);
        }
        
        log.info("Cache warming completed");
    }
}