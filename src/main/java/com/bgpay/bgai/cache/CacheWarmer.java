package com.bgpay.bgai.cache;

import com.bgpay.bgai.service.FileTypeService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class CacheWarmer implements ApplicationRunner {
    private final FileTypeService fileTypeService;

    public CacheWarmer(FileTypeService fileTypeService) {
        this.fileTypeService = fileTypeService;
    }

    @Override
    public void run(ApplicationArguments args) {
        fileTypeService.refreshCache();
    }
}