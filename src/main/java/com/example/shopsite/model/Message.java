package com.example.shopsite.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "message")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 消息接收者

    @Column(nullable = false, length = 500)
    private String content; // 消息内容

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createTime = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private Boolean isRead = false; // 是否已读

    private Long relatedOrderId; // 关联的订单ID（如果有）
}