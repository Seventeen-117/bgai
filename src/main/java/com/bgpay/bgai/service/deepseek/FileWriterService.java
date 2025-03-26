package com.bgpay.bgai.service.deepseek;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
public class FileWriterService {

    @Value("${file.output.path:/var/data/ai_responses}")
    private String outputPath;

    @Async("fileWriteExecutor")
    public void writeContentAsync(String rawContent) {
        // 去除所有换行符
        String sanitizedContent = rawContent.replaceAll("\\n", "");

        // 生成唯一文件名
        String fileName = UUID.randomUUID() + ".txt";
        Path filePath = Paths.get(outputPath, fileName);

        try {
            // 创建目录（如果不存在）
            Files.createDirectories(filePath.getParent());

            // 写入文件
            Files.write(filePath, sanitizedContent.getBytes(StandardCharsets.UTF_8));
            log.info("Successfully wrote content to: {}", filePath);
        } catch (Exception e) {
            log.error("Failed to write content to file. Path: {}", filePath, e);
        }
    }
}