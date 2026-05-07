package com.example.shopsite.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_behavior_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBehaviorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @Column(name = "session_id", length = 128)
    private String sessionId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(length = 512)
    private String pageUrl;

    @Column(length = 512)
    private String referrer;

    @Column(name = "event_time", nullable = false)
    @Builder.Default
    private LocalDateTime eventTime = LocalDateTime.now();

    @Column(length = 64)
    private String ip;

    @Column(length = 512)
    private String userAgent;
}
