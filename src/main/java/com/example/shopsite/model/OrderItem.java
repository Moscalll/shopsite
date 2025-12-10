package com.example.shopsite.model;


import lombok.Data; 
import java.math.BigDecimal;

import jakarta.persistence.*;

// OrderItem.java 示例
@Entity
@Data
@Table(name = "order_item")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order; // 所属的订单

    private Long productId; // 记录购买的商品ID
    private String productName;
    private Integer quantity;
    private BigDecimal priceAtPurchase; // 购买时的价格
}