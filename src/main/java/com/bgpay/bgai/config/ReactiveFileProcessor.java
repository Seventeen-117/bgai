package com.bgpay.bgai.config;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import org.springframework.core.io.buffer.DataBufferUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class ReactiveFileProcessor {
    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    public Mono<String> processReactiveFile(FilePart filePart) {
        log.info("Processing file: {}, content-type: {}", 
                filePart.filename(), filePart.headers().getContentType());
                
        return filePart.content()
                .collect(() -> new ByteArrayOutputStream(),
                        (outputStream, buffer) -> {
                            try {
                                byte[] bytes = new byte[buffer.readableByteCount()];
                                buffer.read(bytes);
                                outputStream.write(bytes);
                                if (outputStream.size() > MAX_FILE_SIZE) {
                                    log.warn("File size exceeds limit: current size={}", outputStream.size());
                                    throw new FileSizeLimitExceededException("文件大小超过限制(10MB)");
                                }
                            } catch (IOException e) {
                                log.error("Error reading file buffer: {}", e.getMessage(), e);
                                throw new RuntimeException("读取文件失败: " + e.getMessage(), e);
                            } finally {
                                DataBufferUtils.release(buffer);
                            }
                        })
                .map(outputStream -> {
                    if (outputStream.size() == 0) {
                        log.warn("Empty file detected");
                        throw new EmptyFileException("上传的文件为空");
                    }
                    
                    int size = outputStream.size();
                    log.info("File processed successfully: size={} bytes", size);
                    
                    // 尝试根据文件类型进行特殊处理
                    String filename = filePart.filename().toLowerCase();
                    String content = outputStream.toString(StandardCharsets.UTF_8);
                    
                    // PDF文件可能包含二进制数据，在此简单处理为文本
                    if (filename.endsWith(".pdf")) {
                        log.info("PDF file detected, extracting text content");
                        // 对于PDF，我们返回一个提示，实际应用中应该使用PDF解析库
                        return "【PDF文件: " + filePart.filename() + "】\n" +
                               "文件大小: " + size + " 字节\n" +
                               "注意: PDF内容已被接收，但未进行详细解析。";
                    }
                    
                    return content;
                })
                .onErrorResume(e -> {
                    if (e instanceof FileSizeLimitExceededException || e instanceof EmptyFileException) {
                        log.warn("File processing constraint violated: {}", e.getMessage());
                        return Mono.just("文件处理错误: " + e.getMessage());
                    }
                    log.error("Unexpected error processing file: {}", e.getMessage(), e);
                    return Mono.just("文件处理失败: " + e.getMessage());
                });
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
}