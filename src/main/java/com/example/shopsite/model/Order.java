package com.example.shopsite.model;

import lombok.Data; 
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

// Order.java 示例
@Entity
@Table(name = "shop_order") // 避免与 SQL 关键字 'ORDER' 冲突
@Data
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // 多个订单对应一个用户
    @JoinColumn(name = "user_id")
    private User user;

    private BigDecimal totalAmount;
    
    // 建议使用枚举类 OrderStatus
    private String status; 
    private LocalDateTime orderDate = LocalDateTime.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>(); // 包含的商品详情列表
}
