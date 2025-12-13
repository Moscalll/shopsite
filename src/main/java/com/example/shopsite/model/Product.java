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
    
    @Column(nullable = false, precision = 10, scale = 2) 
    private BigDecimal price; 
    
    private Integer stock; // 库存
    
    @Column(nullable = false)
    private Boolean isAvailable = true; // 是否上架

    // 🚨 新增：商品主图URL
    private String imageUrl; 

    // 🚨 新增：商品类别 (多对一关系)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category; 

    // 维持多商户上新：保留商户关联
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private User merchant; 
}