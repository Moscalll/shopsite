package com.example.shopsite.handler;

import com.example.shopsite.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice // 🚨 标记为全局异常处理器
public class GlobalExceptionHandler {

    // 捕获所有业务相关的异常，并返回 400 Bad Request
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<String> handleBusinessException(BusinessException ex) {
        // 返回清晰的错误信息和 400 状态码
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    // 💡 可选：捕获通用的 RuntimeException (如果它们是业务相关的)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleGenericRuntimeException(RuntimeException ex) {
        // 如果异常信息是资源不存在 (我们通常用 404)
        if (ex.getMessage() != null && ex.getMessage().contains("不存在")) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        }
        
        // 否则，返回 500
        return new ResponseEntity<>("系统内部错误: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}