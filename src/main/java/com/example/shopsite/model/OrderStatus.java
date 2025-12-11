package com.example.shopsite.model;

public enum OrderStatus {
    PENDING_PAYMENT, // 待付款
    PROCESSING,      // 处理中 (已付款)
    SHIPPED,         // 已发货
    DELIVERED,       // 已送达
    COMPLETED,       // 已完成
    CANCELLED        // 已取消
}