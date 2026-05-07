package com.example.shopsite.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class AuthLoginLogRequest {
    private Long userId;
    private String role;
    private LocalDateTime loginTime;
    private String ip;
    private String userAgent;
    private Boolean success;
    private String failureReason;
    private String sessionId;
}
