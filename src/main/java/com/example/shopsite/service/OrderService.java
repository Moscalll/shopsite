package com.example.shopsite.service;

import com.example.shopsite.dto.OrderCreationRequest;
import com.example.shopsite.model.Order;
import com.example.shopsite.model.User;
import java.util.List;

public interface OrderService {
    // 创建订单方法，需要当前登录用户的用户名
    Order createOrder(OrderCreationRequest request, String username);

    // 从购物车创建订单
    Order createOrderFromCart(User user);

    List<Order> findMyOrders(String username);

    Order findOrderDetails(Long orderId, String username);

    Order processPayment(Long orderId, String username);

    List<Order> findAllOrders(); // 管理员/商家查看所有订单

    Order findOrderDetailsForAdmin(Long orderId); // 管理员/商家查看任意订单详情

    // 商户查询自己的订单
    List<Order> findOrdersByMerchant(User merchant);

    Order findOrderDetailsForMerchant(Long orderId, User merchant);

    // 商户发货
    Order shipOrder(Long orderId, User merchant);

    // 更新订单（用于修改订单状态等）
    Order updateOrder(Long orderId, User merchant, Order updatedOrder);

    // 客户取消订单
    Order cancelOrderByCustomer(Long orderId, String username);

    List<Order> findOrdersByKeyword(String keyword);
}