package com.example.shopsite.exception;

// 这是一个通用的业务异常基类
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}