package com.bgpay.bgai.controller;

import com.bgpay.bgai.context.ReactiveRequestContextHolder;
import com.bgpay.bgai.service.TransactionLogService;
import com.bgpay.bgai.service.deepseek.DeepSeekService;
import com.bgpay.bgai.service.deepseek.FileProcessor;
import com.bgpay.bgai.entity.ApiConfig;
import com.bgpay.bgai.entity.UsageInfo;
import com.bgpay.bgai.response.ChatResponse;
import com.bgpay.bgai.service.ApiConfigService;
import io.seata.core.context.RootContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Base64;

@RestController
@RequestMapping("/Api")
@Slf4j
public class EnhancedChatController {
    private final FileProcessor fileProcessor;
    private final ApiConfigService apiConfigService;
    private final DeepSeekService deepSeekService;
    private final TransactionLogService transactionLogService;
    
    // 用于临时存储事务信息的ThreadLocal
    private static final ThreadLocal<Map<String, Object>> TX_INFO = new ThreadLocal<>();

    @Autowired
    public EnhancedChatController(FileProcessor fileProcessor,
                                  ApiConfigService apiConfigService,
                                  DeepSeekService deepSeekService,
                                  TransactionLogService transactionLogService) {
        this.fileProcessor = fileProcessor;
        this.apiConfigService = apiConfigService;
        this.deepSeekService = deepSeekService;
        this.transactionLogService = transactionLogService;
    }

    @PostMapping(value = "/chat",
            consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ChatResponse>> handleChatRequest(
            @RequestPart(value = "file", required = false) Mono<FilePart> fileMono,
            @RequestParam(value = "question", defaultValue = "请分析该内容") String question,
            @RequestParam(value = "apiUrl", required = false) String apiUrl,
            @RequestParam(value = "apiKey", required = false) String apiKey,
            @RequestParam(value = "modelName", required = false) String modelName,
            @RequestParam(value = "multiTurn", defaultValue = "false") boolean multiTurn,
            ServerWebExchange exchange) {

        log.info("Received request to /Api/chat - question length: {}, multiTurn: {}, modelName: '{}', apiUrl: '{}', apiKey: '{}'", 
                question.length(), multiTurn, modelName, apiUrl, apiKey);
        
        // 设置当前请求上下文
        ReactiveRequestContextHolder.setExchange(exchange);
        
        // 记录请求参数和headers以便调试
        log.info("Request headers: {}", exchange.getRequest().getHeaders());
        log.info("Request query params: {}", exchange.getRequest().getQueryParams());
            
        // 从请求头中获取用户ID
        String userId = getUserIdFromExchange(exchange);
        if (userId == null) {
            return Mono.just(errorResponse(401, "未提供用户ID"));
        }

        // 获取表单数据以确保能获取到所有参数
        return exchange.getFormData()
            .flatMap(formData -> {
                // 尝试从表单数据中获取modelName（如果上面的@RequestParam没有成功获取）
                if (modelName == null && formData.containsKey("modelName")) {
                    String formModelName = formData.getFirst("modelName");
                    log.info("Found modelName in form data: '{}'", formModelName);
                    
                    // 使用从表单中获取的modelName处理请求
                    return processRequestWithFormData(
                        fileMono, 
                        question, 
                        formData.getFirst("apiUrl"), 
                        formData.getFirst("apiKey"), 
                        formModelName, 
                        multiTurn, 
                        userId
                    );
                } else {
                    // 使用通过@RequestParam注解获取的参数处理请求
                    return processRequestWithFormData(
                        fileMono, 
                        question, 
                        apiUrl, 
                        apiKey, 
                        modelName, 
                        multiTurn, 
                        userId
                    );
                }
            })
            .doFinally(signalType -> {
                // 请求结束时清理上下文
                ReactiveRequestContextHolder.clearExchange();
            });
    }

    // 提取请求处理逻辑到单独的方法
    private Mono<ResponseEntity<ChatResponse>> processRequestWithFormData(
            Mono<FilePart> fileMono, 
            String question, 
            String apiUrl, 
            String apiKey, 
            String modelName, 
            boolean multiTurn, 
            String userId) {
        
        log.info("Processing request with params: question length={}, modelName='{}', userId='{}'", 
                question.length(), modelName, userId);
        
        ServerWebExchange exchange = ReactiveRequestContextHolder.getExchange();
        
        // 确保有有效的userId
        if (userId == null || userId.isEmpty()) {
            userId = getUserIdFromExchange(exchange);
        }
        
        // 获取请求路径和客户端IP
        String requestPath = exchange.getRequest().getURI().getPath();
        String sourceIp = getClientIp(exchange);
        
        final String finalUserId = userId; // 用于lambda表达式
        
        // 处理文件上传和内容构建
        return fileMono
                .flatMap(filePart -> processFilePart(filePart))
                .defaultIfEmpty("")
                .flatMap(fileContent -> {
                    try {
                        // 验证请求
                        if (fileContent.isEmpty() && question.isBlank()) {
                            return Mono.just(errorResponse(400, "必须提供问题或文件"));
                        }

                        // 解析API配置
                        ApiConfig apiConfig = resolveApiConfig(apiUrl, apiKey, modelName, finalUserId);
                        
                        log.info("Resolved API config: url={}, model={}", 
                                apiConfig.getApiUrl(), apiConfig.getModelName());

                        // 构建内容
                        String content = buildContent(fileContent, question, multiTurn);
                        
                        // 手动开始记录分布式事务 - 生成一个唯一的XID
                        String xid = "manual-tx-" + UUID.randomUUID().toString();
                        // 在RootContext中设置XID，使其能被Seata感知
                        RootContext.bind(xid);
                        
                        // 手动生成branch_id (模拟RM行为)
                        String branchId = "branch-" + UUID.randomUUID().toString().substring(0, 8);
                        String transactionName = "EnhancedChatController.processRequest";
                        
                        // 记录事务开始
                        log.info("手动记录分布式事务开始: XID={}, 用户={}, 路径={}, IP={}", 
                               xid, finalUserId, requestPath, sourceIp);
                                
                        Long logId = transactionLogService.recordTransactionBegin(
                                xid, transactionName, "AT", requestPath, sourceIp, finalUserId);
                        
                        // 在线程本地变量中保存信息，用于后续处理
                        Map<String, Object> txInfo = new HashMap<>();
                        txInfo.put("xid", xid);
                        txInfo.put("branchId", branchId);
                        txInfo.put("logId", logId);
                        txInfo.put("startTime", System.currentTimeMillis());
                        txInfo.put("userId", finalUserId);
                        txInfo.put("requestPath", requestPath);
                        txInfo.put("sourceIp", sourceIp);
                        TX_INFO.set(txInfo);
                        
                        // 记录分支注册
                        transactionLogService.updateTransactionStatus(xid, "BRANCH_REGISTERED", branchId);

                        // 处理请求
                        return Mono.fromCallable(() -> {
                            try {
                                ChatResponse response = deepSeekService.processRequest(
                                    content,
                                    apiConfig.getApiUrl(),
                                    apiConfig.getApiKey(),
                                    apiConfig.getModelName(),
                                    finalUserId,
                                    multiTurn
                                );
                                
                                // 记录事务成功完成
                                if (TX_INFO.get() != null) {
                                    Map<String, Object> storedTxInfo = TX_INFO.get();
                                    String storedXid = (String) storedTxInfo.get("xid");
                                    String storedBranchId = (String) storedTxInfo.get("branchId");
                                    long startTime = (long) storedTxInfo.get("startTime");
                                    long duration = System.currentTimeMillis() - startTime;

                                    String extraData = String.format(
                                        "{\"duration\":%d,\"result\":\"success\",\"branchId\":\"%s\"}",
                                        duration, storedBranchId);
                                        
                                    transactionLogService.recordTransactionEnd(storedXid, "COMMITTED", extraData);
                                    
                                    log.info("手动记录分布式事务成功完成: XID={}, branchId={}, 耗时={}ms", 
                                           storedXid, storedBranchId, duration);
                                            
                                    TX_INFO.remove();
                                }
                                
                                return response;
                            } catch (Exception e) {
                                // 记录事务失败回滚
                                if (TX_INFO.get() != null) {
                                    Map<String, Object> storedTxInfo = TX_INFO.get();
                                    String storedXid = (String) storedTxInfo.get("xid");
                                    String storedBranchId = (String) storedTxInfo.get("branchId");
                                    long startTime = (long) storedTxInfo.get("startTime");
                                    long duration = System.currentTimeMillis() - startTime;

                                    String extraData = String.format(
                                        "{\"duration\":%d,\"result\":\"failure\",\"branchId\":\"%s\",\"error\":\"%s\"}",
                                        duration, storedBranchId, e.getMessage().replace("\"", "\\\""));
                                        
                                    transactionLogService.recordTransactionEnd(storedXid, "ROLLBACKED", extraData);
                                    
                                    log.warn("手动记录分布式事务回滚: XID={}, branchId={}, 耗时={}ms, 原因={}", 
                                           storedXid, storedBranchId, duration, e.getMessage());
                                            
                                    TX_INFO.remove();
                                }
                                throw e;
                            } finally {
                                // 清理RootContext
                                RootContext.unbind();
                            }
                        })
                        .map(ResponseEntity::ok)
                        .onErrorResume(e -> {
                            log.error("处理请求失败", e);
                            return Mono.just(errorResponse(500, "处理失败: " + e.getMessage()));
                        });
                    } catch (IllegalArgumentException e) {
                        log.error("Invalid argument error: {}", e.getMessage());
                        return Mono.just(errorResponse(400, e.getMessage()));
                    } catch (Exception e) {
                        log.error("处理请求失败", e);
                        return Mono.just(errorResponse(500, "处理失败: " + e.getMessage()));
                    }
                });
    }

    // 添加一个处理普通表单提交的方法，帮助调试
    @PostMapping(value = "/chat-form-data")
    public Mono<ResponseEntity<String>> handleFormDataRequest(ServerWebExchange exchange) {
        log.info("Received form data request");
        
        return exchange.getFormData()
            .map(formData -> {
                StringBuilder sb = new StringBuilder();
                sb.append("Received form data:\n");
                
                formData.forEach((key, values) -> {
                    sb.append(key).append(": ").append(values).append("\n");
                });
                
                // 特别检查modelName参数
                if (formData.containsKey("modelName")) {
                    sb.append("\nmodelName found with value: ").append(formData.getFirst("modelName"));
                } else {
                    sb.append("\nmodelName NOT found in form data");
                }
                
                // 检查Content-Type
                String contentType = exchange.getRequest().getHeaders().getFirst("Content-Type");
                sb.append("\nContent-Type: ").append(contentType);
                
                log.info(sb.toString());
                
                return ResponseEntity.ok(sb.toString());
            });
    }

    private String getUserIdFromExchange(ServerWebExchange exchange) {
        // 尝试从多种通用命名的头部获取
        String[] possibleHeaders = {
            "X-User-Id", 
            "X-UserId",
            "Authorization", 
            "access_token",
            "token",
            "user_id",
            "uid"
        };
        
        for (String header : possibleHeaders) {
            String value = exchange.getRequest().getHeaders().getFirst(header);
            if (value != null && !value.isEmpty()) {
                // 如果是Bearer token格式，只提取token部分
                if (header.equals("Authorization") && value.startsWith("Bearer ")) {
                    String token = value.substring(7); // 移除 "Bearer " 前缀
                    log.debug("从Authorization header获取到token: {}", token);
                    
                    // 尝试解析JWT token获取userId (简化版)
                    try {
                        // 这里是极简的JWT解析，实际项目中应使用JWT库
                        String[] parts = token.split("\\.");
                        if (parts.length == 3) {
                            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
                            // 检查payload中是否包含常见的用户ID字段名
                            if (payload.contains("\"sub\":")) {
                                int start = payload.indexOf("\"sub\":\"") + 7;
                                int end = payload.indexOf("\"", start);
                                if (start > 6 && end > start) {
                                    String userId = payload.substring(start, end);
                                    log.debug("从JWT token中提取userId: {}", userId);
                                    return userId;
                                }
                            }
                            if (payload.contains("\"userId\":")) {
                                int start = payload.indexOf("\"userId\":\"") + 10;
                                int end = payload.indexOf("\"", start);
                                if (start > 9 && end > start) {
                                    String userId = payload.substring(start, end);
                                    log.debug("从JWT token中提取userId: {}", userId);
                                    return userId;
                                }
                            }
                            if (payload.contains("\"user_id\":")) {
                                int start = payload.indexOf("\"user_id\":\"") + 11;
                                int end = payload.indexOf("\"", start);
                                if (start > 10 && end > start) {
                                    String userId = payload.substring(start, end);
                                    log.debug("从JWT token中提取userId: {}", userId);
                                    return userId;
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.debug("JWT token解析失败: {}", e.getMessage());
                    }
                    return token; // 如果无法解析，返回整个token
                }
                log.debug("从{}头部获取到userId: {}", header, value);
                return value;
            }
        }
        
        // 如果头中没有，尝试从交换属性、会话和URL参数中获取
        Object userIdAttr = exchange.getAttribute("userId");
        if (userIdAttr != null) {
            log.debug("从exchange属性获取到userId: {}", userIdAttr);
            return userIdAttr.toString();
        }
        
        // 从URL查询参数中获取
        String userId = exchange.getRequest().getQueryParams().getFirst("userId");
        if (userId != null && !userId.isEmpty()) {
            log.debug("从URL参数获取到userId: {}", userId);
            return userId;
        }

        // 如果所有方法都未获取到userId，则使用默认值
        log.warn("未能从请求中获取userId，使用默认值");
        return "system-user-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private Mono<String> processFilePart(FilePart filePart) {
        if (filePart == null) {
            return Mono.just("");
        }

        try {
            // 创建临时文件
            Path tempFile = Files.createTempFile("upload_", "_" + filePart.filename());
            
            // 将 FilePart 写入临时文件
            return filePart.transferTo(tempFile)
                    .then(Mono.fromCallable(() -> {
                        try {
                            // 处理文件
                            String content = fileProcessor.processFile(tempFile.toFile());
                            // 处理完成后删除临时文件
                            Files.deleteIfExists(tempFile);
                            return content;
                        } catch (Exception e) {
                            log.error("处理文件失败", e);
                            try {
                                Files.deleteIfExists(tempFile);
                            } catch (Exception ex) {
                            }
                            throw new RuntimeException("处理文件失败: " + e.getMessage(), e);
                        }
                    }));
        } catch (Exception e) {
            log.error("创建临时文件失败", e);
            return Mono.error(e);
        }
    }

    private ApiConfig resolveApiConfig(String apiUrl, String apiKey, String modelName, String userId) {
        // 用户提供完整参数
        if (StringUtils.hasText(apiUrl)
                && StringUtils.hasText(apiKey)
                && StringUtils.hasText(modelName)) {
            return new ApiConfig()
                    .setApiUrl(apiUrl)
                    .setApiKey(apiKey)
                    .setModelName(modelName);
        }
        ApiConfig dbConfig;
        String defualtModelName;
        
        // 添加空值检查，确保modelName不为null
        if (modelName == null || modelName.isEmpty()) {
            defualtModelName = "deepseek-chat";
            dbConfig = apiConfigService.findAlternativeConfig(userId, defualtModelName);
        } else {
            dbConfig = apiConfigService.findAlternativeConfig(userId, modelName);
        }

        if (dbConfig == null) {
            throw new IllegalArgumentException("未找到用户API配置且未提供完整参数");
        }
        return dbConfig;
    }

    private String buildContent(String fileContent, String question, boolean multiTurn) {
        StringBuilder content = new StringBuilder();

        // 无论是否多轮对话都处理文件内容
        if (!fileContent.isEmpty()) {
            content.append("【文件内容】\n")
                    .append(fileContent)
                    .append("\n\n");
        }

        content.append("【用户提问】").append(question);
        return content.toString();
    }

    // 修改 errorResponse 方法，使其返回 ResponseEntity<ChatResponse>
    private ResponseEntity<ChatResponse> errorResponse(int code, String message) {
        ChatResponse chatResponse = new ChatResponse();
        String errorJson = String.format("{\"error\":{\"code\":%d,\"message\":\"%s\"}}", code, message);
        chatResponse.setContent(errorJson);
        chatResponse.setUsage(new UsageInfo());
        return ResponseEntity.status(code).body(chatResponse);
    }

    /**
     * 测试手动触发Seata分布式事务记录
     */
    @GetMapping("/test-transaction")
    public ResponseEntity<String> testTransactionLog(
            @RequestParam(value = "userId", required = false) String userId,
            ServerWebExchange exchange) {
        
        try {
            // 获取用户ID，优先使用请求参数中的，如果没有则尝试从请求头等中获取
            if (userId == null || userId.isEmpty()) {
                userId = getUserIdFromExchange(exchange);
            }
            
            // 获取请求路径和客户端IP
            String requestPath = exchange.getRequest().getURI().getPath();
            String sourceIp = getClientIp(exchange);
            
            // 手动开始记录分布式事务 - 生成一个唯一的XID和branch_id
            String xid = "manual-tx-" + UUID.randomUUID().toString();
            // 在RootContext中设置XID，使其能被Seata感知
            RootContext.bind(xid);
            
            // 手动生成一个分支ID，模拟RM行为
            String branchId = "branch-" + UUID.randomUUID().toString().substring(0, 8);
            String transactionName = "EnhancedChatController.testTransactionLog";
            
            // 记录事务开始
            log.info("测试手动记录分布式事务开始: XID={}, 用户={}, 路径={}, IP={}", 
                   xid, userId, requestPath, sourceIp);
                   
            Long logId = transactionLogService.recordTransactionBegin(
                    xid, transactionName, "AT", requestPath, sourceIp, userId);
            
            // 模拟业务处理
            Thread.sleep(100);
            
            // 模拟记录一个分支
            transactionLogService.updateTransactionStatus(xid, "BRANCH_REGISTERED", branchId);
            
            // 模拟更多业务处理
            Thread.sleep(400);
            
            // 记录事务结束
            String extraData = String.format(
                "{\"duration\":%d,\"result\":\"success\",\"test\":true,\"branchId\":\"%s\"}", 
                500, branchId);
                
            transactionLogService.recordTransactionEnd(xid, "COMMITTED", extraData);
            
            log.info("测试手动记录分布式事务结束: XID={}, branchId={}, 耗时=500ms, 用户={}", 
                   xid, branchId, userId);
            
            // 清理RootContext
            RootContext.unbind();
            
            return ResponseEntity.ok(String.format(
                "事务记录测试完成:\nXID=%s\nbranchId=%s\nuserId=%s\nsourceIp=%s\nrequestPath=%s",
                xid, branchId, userId, sourceIp, requestPath));
                
        } catch (Exception e) {
            log.error("测试事务记录失败", e);
            return ResponseEntity.status(500).body("事务记录测试失败: " + e.getMessage());
        }
    }

    /**
     * 获取客户端真实IP地址
     * 尝试从各种代理相关的头信息中获取
     */
    private String getClientIp(ServerWebExchange exchange) {
        String[] headers = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR",
            "X-Real-IP"
        };
        
        for (String header : headers) {
            String ip = exchange.getRequest().getHeaders().getFirst(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // 如果是多个IP，取第一个
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                log.debug("从{}头部获取到客户端IP: {}", header, ip);
                return ip;
            }
        }
        
        // 如果以上方式都无法获取，则从请求远程地址获取
        if (exchange.getRequest().getRemoteAddress() != null) {
            String ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            log.debug("从远程地址获取到客户端IP: {}", ip);
            return ip;
        }
        
        log.warn("无法获取客户端IP，使用默认值");
        return "127.0.0.1";
    }
}