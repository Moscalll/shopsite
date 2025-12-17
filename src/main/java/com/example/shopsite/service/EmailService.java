package com.example.shopsite.service;

import com.example.shopsite.model.Order;

public interface EmailService {
    /**
     * 发送订单确认邮件
     */
    void sendOrderConfirmationEmail(Order order);
}




















