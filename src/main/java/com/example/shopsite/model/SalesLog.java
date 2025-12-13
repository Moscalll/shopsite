package com.example.shopsite.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "sales_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String actionType; // 例如 "VIEW", "PURCHASE", "ADD_TO_CART"

    private Long productId; // 如果是与商品相关的行为
    
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime logTime = LocalDateTime.now();
}
