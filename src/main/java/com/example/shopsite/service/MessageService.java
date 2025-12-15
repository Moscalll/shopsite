package com.example.shopsite.service;

import com.example.shopsite.model.Message;
import com.example.shopsite.model.User;

public interface MessageService {
    Message createMessage(User user, String content, Long relatedOrderId);
    void markAsRead(Long messageId, User user);
    void markAllAsRead(User user);
}