package com.example.shopsite.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class AdminOperationLogRequest {
    private Long operatorUserId;
    private String operatorRole;
    private LocalDateTime operationTime;
    private String operationType;
    private String content;
    private String ip;
    private String account;
}
