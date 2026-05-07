package com.example.shopsite.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "recommendation_result",
        indexes = {
                @Index(name = "idx_rec_user_algo", columnList = "user_id,algorithm"),
                @Index(name = "idx_rec_computed_at", columnList = "computed_at")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RecommendationAlgorithm algorithm;

    /**
     * 推荐结果：商品ID列表（按推荐顺序），用逗号分隔，避免引入额外JSON依赖/迁移复杂度。
     */
    @Column(name = "product_ids", nullable = false, length = 2000)
    private String productIds;

    @Column(name = "computed_at", nullable = false)
    @Builder.Default
    private LocalDateTime computedAt = LocalDateTime.now();
}

