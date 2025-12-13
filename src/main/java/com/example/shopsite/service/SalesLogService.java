package com.example.shopsite.service;

import com.example.shopsite.model.User;

public interface SalesLogService {
    
    // 记录浏览日志
    void logView(Long productId, User user);
    
    // 记录购买日志
    void logPurchase(Long productId, User user);
    
    // 记录加购日志
    void logAddToCart(Long productId, User user);
}

