package com.bgpay.bgai.config;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import org.springframework.core.io.buffer.DataBufferUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class ReactiveFileProcessor {
    private static final int MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

    public Mono<String> processReactiveFile(FilePart filePart) {
        return filePart.content()
                .collect(() -> new ByteArrayOutputStream(),
                        (outputStream, buffer) -> {
                            byte[] bytes = new byte[buffer.readableByteCount()];
                            buffer.read(bytes);
                            try {
                                outputStream.write(bytes);
                                if (outputStream.size() > MAX_FILE_SIZE) {
                                    throw new FileSizeLimitExceededException("文件大小超过限制");
                                }
                            } catch (IOException e) {
                                throw new RuntimeException("文件读取失败", e);
                            } finally {
                                DataBufferUtils.release(buffer);
                            }
                        })
                .map(outputStream -> {
                    if (outputStream.size() == 0) {
                        throw new EmptyFileException("上传文件为空");
                    }
                    return outputStream.toString(StandardCharsets.UTF_8);
                });
    }

    // 自定义异常类
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