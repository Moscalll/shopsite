package com.example.shopsite.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

@Entity
@Table(name = "shop_order") // 映射到你数据库中的 shop_order 表
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order { // 使用 Order 类名
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 订单拥有者
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; 

    @Column(nullable = false)
    private BigDecimal totalAmount; // 订单总额
    
    @Column(nullable = false)
    private LocalDateTime orderDate = LocalDateTime.now(); // 下单时间

    @Enumerated(EnumType.STRING)
    private OrderStatus status; // 订单状态 (待付款, 已发货, 已完成等)
    
    // 订单项：一个订单包含多个商品
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items;
}