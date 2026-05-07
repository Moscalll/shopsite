package com.example.shopsite.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class UserBehaviorLogRequest {
    private Long userId;
    private String sessionId;
    private Long productId;
    private Long categoryId;
    private String eventType;
    private Integer durationSeconds;
    private String pageUrl;
    private String referrer;
    private LocalDateTime eventTime;
    private String ip;
    private String userAgent;
}
