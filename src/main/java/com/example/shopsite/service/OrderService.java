package com.example.shopsite.service;

import com.example.shopsite.dto.OrderCreationRequest;
import com.example.shopsite.model.Order;
import java.util.List;

public interface OrderService {
    // 创建订单方法，需要当前登录用户的用户名
    Order createOrder(OrderCreationRequest request, String username);
    List<Order> findMyOrders(String username);
    Order findOrderDetails(Long orderId, String username);
}