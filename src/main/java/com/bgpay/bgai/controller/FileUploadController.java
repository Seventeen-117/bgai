package com.bgpay.bgai.controller;

import com.bgpay.bgai.entity.AllowedFileType;
import com.bgpay.bgai.entity.MimeTypeConfig;
import com.bgpay.bgai.response.PageResponse;
import com.bgpay.bgai.service.deepseek.FileProcessor;
import com.bgpay.bgai.service.deepseek.FileTypeService;
import com.bgpay.bgai.service.deepseek.FileWriterService;
import com.bgpay.bgai.service.deepseek.ReactiveFileProcessor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件上传和管理控制器
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "文件处理", description = "文件上传、解析相关接口")
public class FileUploadController {

    private final FileProcessor fileProcessor;
    private final ReactiveFileProcessor reactiveFileProcessor;
    private final FileTypeService fileTypeService;
    private final FileWriterService fileWriterService;

    /**
     * 同步文件上传和解析接口
     */
    @Operation(summary = "上传并解析文件", description = "同步上传文件并返回解析内容")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "上传成功"),
        @ApiResponse(responseCode = "400", description = "文件格式不支持或解析失败")
    })
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @Parameter(description = "要上传的文件") @RequestParam("file") MultipartFile file) {
        try {
            String content = fileProcessor.processFile(file);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("content", content);
            response.put("filename", file.getOriginalFilename());
            response.put("size", file.getSize());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("文件上传处理失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * 响应式文件上传和解析接口
     */
    @Operation(summary = "响应式上传并解析文件", description = "使用响应式方式上传文件并返回解析内容")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "上传成功"),
        @ApiResponse(responseCode = "400", description = "文件格式不支持或解析失败")
    })
    @PostMapping(value = "/upload/reactive", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> uploadFileReactive(
            @Parameter(description = "要上传的文件") @RequestPart("file") FilePart filePart) {
        return reactiveFileProcessor.processReactiveFile(filePart)
                .map(content -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("content", content);
                    response.put("filename", filePart.filename());
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(e -> {
                    log.error("响应式文件上传处理失败", e);
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("error", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response));
                });
    }

    /**
     * 获取允许的文件类型列表
     */
    @Operation(summary = "获取允许的文件类型", description = "返回系统支持的所有文件类型列表")
    @GetMapping("/allowed-types")
    public ResponseEntity<List<AllowedFileType>> getAllowedFileTypes() {
        List<AllowedFileType> allowedTypes = fileTypeService.getAllowedFileTypes();
        return ResponseEntity.ok(allowedTypes);
    }

    /**
     * 获取MIME类型配置列表
     */
    @Operation(summary = "获取MIME类型配置", description = "返回系统中所有MIME类型的配置信息")
    @GetMapping("/mime-types")
    public ResponseEntity<List<MimeTypeConfig>> getMimeTypeConfigs() {
        List<MimeTypeConfig> mimeTypes = fileTypeService.getAllMimeTypeConfigs();
        return ResponseEntity.ok(mimeTypes);
    }

    /**
     * 添加新的允许文件类型
     */
    @Operation(summary = "添加允许的文件类型", description = "添加新的允许上传的文件类型")
    @PostMapping("/allowed-types")
    public ResponseEntity<AllowedFileType> addAllowedFileType(
            @Parameter(description = "文件类型信息") @RequestBody AllowedFileType fileType) {
        AllowedFileType saved = fileTypeService.addAllowedFileType(fileType);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * 删除允许的文件类型
     */
    @Operation(summary = "删除允许的文件类型", description = "根据ID删除允许上传的文件类型")
    @DeleteMapping("/allowed-types/{id}")
    public ResponseEntity<Void> deleteAllowedFileType(
            @Parameter(description = "文件类型ID") @PathVariable Long id) {
        fileTypeService.deleteAllowedFileType(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 保存文件内容到本地存储
     */
    @Operation(summary = "保存文件内容", description = "将文本内容保存为文件")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "保存成功"),
        @ApiResponse(responseCode = "500", description = "保存失败")
    })
    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveFileContent(
            @Parameter(description = "文件内容") @RequestParam("content") String content,
            @Parameter(description = "文件名") @RequestParam("filename") String filename) {
        try {
            String filePath = fileWriterService.writeContentToFile(content, filename);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("filePath", filePath);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("保存文件内容失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
} 