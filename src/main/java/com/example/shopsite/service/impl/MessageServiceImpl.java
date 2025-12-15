package com.example.shopsite.service.impl;

import com.example.shopsite.model.Message;
import com.example.shopsite.model.User;
import com.example.shopsite.repository.MessageRepository;
import com.example.shopsite.service.MessageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageServiceImpl implements MessageService {
    
    private final MessageRepository messageRepository;

    public MessageServiceImpl(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    @Transactional
    public Message createMessage(User user, String content, Long relatedOrderId) {
        Message message = Message.builder()
                .user(user)
                .content(content)
                .relatedOrderId(relatedOrderId)
                .isRead(false)
                .build();
        return messageRepository.save(message);
    }

    @Override
    @Transactional
    public void markAsRead(Long messageId, User user) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("消息不存在"));
        if (!message.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("无权操作该消息");
        }
        message.setIsRead(true);
        messageRepository.save(message);
    }

    @Override
    @Transactional
    public void markAllAsRead(User user) {
        messageRepository.findByUserAndIsReadFalseOrderByCreateTimeDesc(user)
                .forEach(message -> {
                    message.setIsRead(true);
                    messageRepository.save(message);
                });
    }
}