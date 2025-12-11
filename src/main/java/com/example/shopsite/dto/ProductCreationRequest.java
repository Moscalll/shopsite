package com.example.shopsite.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;

@Data
public class ProductCreationRequest {
    
    @NotBlank(message = "商品名称不能为空")
    private String name;

    private String description;
    
    @NotNull(message = "价格不能为空")
    @Min(value = 0, message = "价格必须大于0")
    private BigDecimal price;
    
    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能小于0")
    private Integer stock;
}