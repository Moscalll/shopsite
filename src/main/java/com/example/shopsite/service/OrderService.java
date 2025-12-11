package com.example.shopsite.service;

import com.example.shopsite.dto.OrderCreationRequest;
import com.example.shopsite.model.Order;
import java.util.List;

public interface OrderService {
    // 创建订单方法，需要当前登录用户的用户名
    Order createOrder(OrderCreationRequest request, String username);
    List<Order> findMyOrders(String username);
    Order findOrderDetails(Long orderId, String username);

    Order processPayment(Long orderId, String username);
    List<Order> findAllOrders(); // 管理员/商家查看所有订单
    Order findOrderDetailsForAdmin(Long orderId); // 管理员/商家查看任意订单详情
}