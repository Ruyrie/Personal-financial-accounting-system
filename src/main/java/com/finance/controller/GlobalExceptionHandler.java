package com.finance.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
/**
 * REST API 全局异常处理器，将业务参数错误转换为统一 JSON 响应。
 */
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    /**
     * 处理业务校验异常，返回 400 和错误信息。
     */
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }
}
