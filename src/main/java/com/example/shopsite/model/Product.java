package com.example.shopsite.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;

@Entity
@Table(name = "product")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;
    
    @Column(nullable = false, precision = 10, scale = 2) // 🚨 数据库精度设置
    private BigDecimal price; 
    
    private Integer stock; // 库存
    
    @Column(nullable = false)
    private Boolean isAvailable = true; // 是否上架

    // 假设商品属于某个商家 (User)
    @ManyToOne
    @JoinColumn(name = "merchant_id", nullable = false)
    private User merchant; 
}