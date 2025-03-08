package com.bgpay.bgai.controller;

import com.bgpay.bgai.deepseek.DeepSeekService;
import com.bgpay.bgai.deepseek.FileProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class EnhancedChatController {

    private final FileProcessor fileProcessor;

    // 使用构造函数注入 FileProcessor 实例
    @Autowired
    public EnhancedChatController(FileProcessor fileProcessor) {
        this.fileProcessor = fileProcessor;
    }

    @PostMapping(value = "/chat", consumes = "multipart/form-data")
    public String handleChatRequest(
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "question", defaultValue = "请分析该内容") String question
    ) {
        try {
            // 参数校验
            if (file == null && question.isBlank()) {
                return errorResponse(400, "必须提供问题或文件");
            }

            // 构建提示内容
            StringBuilder content = new StringBuilder();
            if (file != null && !file.isEmpty()) {
                content.append("【文件内容】\n")
                        .append(fileProcessor.processFile(file))
                        .append("\n\n");
            }
            content.append("【用户提问】").append(question);

            return DeepSeekService.processRequest(content.toString());
        } catch (Exception e) {
            return errorResponse(500, "处理失败: " + e.getMessage().replace("\"", "'"));
        }
    }

    private String errorResponse(int code, String message) {
        return String.format("{\"error\":{\"code\":%d,\"message\":\"%s\"}}", code, message);
    }
}