package com.bgpay.bgai.exception;

import com.bgpay.bgai.response.CustomErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CustomErrorResponse> handleGenericException(Exception ex) {
        log.error("未捕获的异常", ex);
        CustomErrorResponse response = new CustomErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR",
            ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    @ExceptionHandler(ApiKeyAuthenticationException.class)
    public ResponseEntity<CustomErrorResponse> handleApiKeyAuthenticationException(ApiKeyAuthenticationException ex) {
        log.warn("API密钥认证失败: {}", ex.getMessage());
        CustomErrorResponse response = new CustomErrorResponse(
            HttpStatus.UNAUTHORIZED,
            "UNAUTHORIZED",
            ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }
    
    @ExceptionHandler(BillingException.class)
    public ResponseEntity<CustomErrorResponse> handleBillingException(BillingException ex) {
        log.error("计费错误: {}", ex.getMessage());
        CustomErrorResponse response = new CustomErrorResponse(
            HttpStatus.PAYMENT_REQUIRED,
            "PAYMENT_REQUIRED",
            ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.PAYMENT_REQUIRED);
    }
    
    @ExceptionHandler(MQException.class)
    public ResponseEntity<CustomErrorResponse> handleMQException(MQException ex) {
        log.error("消息队列错误: {}", ex.getMessage(), ex);
        CustomErrorResponse response = new CustomErrorResponse(
            HttpStatus.SERVICE_UNAVAILABLE,
            "SERVICE_UNAVAILABLE",
            ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.SERVICE_UNAVAILABLE);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CustomErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("请求参数无效: {}", ex.getMessage());
        CustomErrorResponse response = new CustomErrorResponse(
            HttpStatus.BAD_REQUEST,
            "BAD_REQUEST",
            ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理缺少必需的请求参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<CustomErrorResponse> handleMissingParameterException(MissingServletRequestParameterException ex) {
        log.warn("缺少必需的请求参数: {}", ex.getMessage());
        CustomErrorResponse response = new CustomErrorResponse(
            HttpStatus.BAD_REQUEST,
            "MISSING_PARAMETER",
            "缺少必需的请求参数: " + ex.getParameterName()
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * 处理缺少必需的请求头异常
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<CustomErrorResponse> handleMissingHeaderException(MissingRequestHeaderException ex) {
        log.warn("缺少必需的请求头: {}", ex.getMessage());
        CustomErrorResponse response = new CustomErrorResponse(
            HttpStatus.BAD_REQUEST,
            "MISSING_HEADER",
            "缺少必需的请求头: " + ex.getHeaderName()
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<CustomErrorResponse> handleAccessDenied() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new CustomErrorResponse(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "权限不足"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CustomErrorResponse> handleValidationError(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());
        return ResponseEntity.badRequest()
                .body(new CustomErrorResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", String.join("; ", errors)));
    }

    /**
     * 处理RuntimeException异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<CustomErrorResponse> handleRuntimeException(RuntimeException ex) {
        log.error("运行时异常", ex);
        CustomErrorResponse response = new CustomErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "RUNTIME_ERROR",
            "刷新令牌失败: " + ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}