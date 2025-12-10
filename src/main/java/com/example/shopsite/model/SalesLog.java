package com.example.shopsite.model;

import lombok.Data; 
import java.time.LocalDateTime;


import jakarta.persistence.*;

// SalesLog.java 示例
@Entity
@Table(name = "sales_log")
@Data
public class SalesLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String actionType; // 例如 "VIEW", "PURCHASE"

    private Long productId; // 如果是与商品相关的行为
    
    private LocalDateTime logTime = LocalDateTime.now();
}
