package com.bgpay.bgai.config;

import com.bgpay.bgai.service.deepseek.FileProcessor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import org.springframework.core.io.buffer.DataBufferUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@Slf4j
public class ReactiveFileProcessor {
    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    
    private final FileProcessor fileProcessor;
    
    public ReactiveFileProcessor(FileProcessor fileProcessor) {
        this.fileProcessor = fileProcessor;
    }

    public Mono<String> processReactiveFile(FilePart filePart) {
        log.info("Processing file: {}, content-type: {}", 
                filePart.filename(), filePart.headers().getContentType());
        
        try {
            // 创建临时文件
            String filename = filePart.filename();
            Path tempFile = Files.createTempFile("upload_", "_" + sanitizeFilename(filename));
            
            // 将文件内容写入临时文件
            return filePart.transferTo(tempFile)
                    .then(Mono.fromCallable(() -> {
                        File file = tempFile.toFile();
                        if (file.length() == 0) {
                            log.warn("Empty file detected");
                            Files.deleteIfExists(tempFile);
                            throw new EmptyFileException("上传的文件为空");
                        }
                        
                        if (file.length() > MAX_FILE_SIZE) {
                            log.warn("File size exceeds limit: current size={}", file.length());
                            Files.deleteIfExists(tempFile);
                            throw new FileSizeLimitExceededException("文件大小超过限制(10MB)");
                        }
                        
                        log.info("File processed successfully: size={} bytes", file.length());
                        
                        try {
                            // 使用 FileProcessor 处理文件
                            return fileProcessor.processFile(file);
                        } finally {
                            // 处理完成后删除临时文件
                            Files.deleteIfExists(tempFile);
                        }
                    }))
                    .onErrorResume(e -> {
                        try {
                            Files.deleteIfExists(tempFile);
                        } catch (IOException ex) {
                            log.warn("Failed to delete temp file: {}", ex.getMessage());
                        }
                        
                        if (e instanceof FileSizeLimitExceededException || e instanceof EmptyFileException) {
                            log.warn("File processing constraint violated: {}", e.getMessage());
                            return Mono.just("文件处理错误: " + e.getMessage());
                        }
                        log.error("Unexpected error processing file: {}", e.getMessage(), e);
                        return Mono.just("文件处理失败: " + e.getMessage());
                    });
        } catch (IOException e) {
            log.error("Failed to create temporary file: {}", e.getMessage());
            return Mono.just("无法创建临时文件: " + e.getMessage());
        }
    }

    private String sanitizeFilename(String filename) {
        // 简单的文件名清理，移除可能导致路径遍历的字符
        return filename.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    public static class FileSizeLimitExceededException extends RuntimeException {
        public FileSizeLimitExceededException(String message) {
            super(message);
        }
    }

    public static class EmptyFileException extends RuntimeException {
        public EmptyFileException(String message) {
            super(message);
        }
    }

    /**
     * 获取原始FileProcessor实例，用于直接文件处理
     * 当常规反应式处理失败时，可以使用此方法获取FileProcessor进行备选处理
     * 
     * @return FileProcessor实例
     */
    public FileProcessor getFileProcessor() {
        return this.fileProcessor;
    }
}