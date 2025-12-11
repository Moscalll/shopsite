package com.example.shopsite.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class OrderCreationRequest {
    
    @NotEmpty(message = "订单项列表不能为空")
    @Valid // 🚨 确保列表中的每个 OrderItemRequest 也被校验
    private List<OrderItemRequest> items;
}