package com.example.shopsite.model;

import lombok.Data; 
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;

// Product.java 示例
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    
    @Lob // 用于存储较大的文本数据
    private String description;

    private BigDecimal price; // 使用 BigDecimal 存储货币值

    private Integer stock;
    private String imageUrl;
    private Boolean isAvailable = true;
}