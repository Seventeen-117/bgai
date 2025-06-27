package com.bgpay.bgai.service.deepseek;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 文件写入服务，用于将内容保存到文件
 */
@Service
@Slf4j
public class FileWriterService {

    @Value("${app.file-storage.base-path:./file-storage}")
    private String baseStoragePath;

    /**
     * 将内容写入文件并返回文件路径
     *
     * @param content  要写入的内容
     * @param filename 文件名
     * @return 保存的文件路径
     * @throws IOException 如果写入失败
     */
    public String writeContentToFile(String content, String filename) throws IOException {
        // 确保存储目录存在
        Path storagePath = Paths.get(baseStoragePath);
        if (!Files.exists(storagePath)) {
            Files.createDirectories(storagePath);
        }

        // 生成唯一文件名（添加时间戳）
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String sanitizedFilename = sanitizeFilename(filename);
        String uniqueFilename = timestamp + "_" + sanitizedFilename;
        Path filePath = storagePath.resolve(uniqueFilename);

        // 写入内容
        Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));
        log.info("文件已保存: {}", filePath);

        return filePath.toString();
    }

    /**
     * 清理文件名，移除不安全字符
     *
     * @param filename 原始文件名
     * @return 清理后的文件名
     */
    private String sanitizeFilename(String filename) {
        // 移除路径遍历和特殊字符
        return filename.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
    
    /**
     * 获取指定目录下的所有文件
     *
     * @return 文件列表
     */
    public File[] listFiles() {
        File directory = new File(baseStoragePath);
        if (!directory.exists() || !directory.isDirectory()) {
            return new File[0];
        }
        return directory.listFiles();
    }
    
    /**
     * 删除指定文件
     *
     * @param filename 文件名
     * @return 是否删除成功
     */
    public boolean deleteFile(String filename) {
        Path filePath = Paths.get(baseStoragePath, filename);
        try {
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("删除文件失败: {}", filePath, e);
            return false;
        }
    }
}