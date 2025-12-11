package com.example.shopsite.controller;

import com.example.shopsite.dto.OrderCreationRequest;
import com.example.shopsite.model.Order;
import com.example.shopsite.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * POST /api/orders
     * 创建订单（需认证）
     */
    @PostMapping
    public ResponseEntity<Order> createOrder(@Valid @RequestBody OrderCreationRequest request) {
        // 从 SecurityContext 获取当前登录用户
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        
        try {
            Order newOrder = orderService.createOrder(request, username);
            return new ResponseEntity<>(newOrder, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            // 捕获库存不足或商品不存在等业务异常
            return new ResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * GET /api/orders/my
     * 查询当前用户的所有订单 (需认证)
     */
    @GetMapping("/my")
    public ResponseEntity<List<Order>> getMyOrders() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        
        List<Order> myOrders = orderService.findMyOrders(username);
        
        return ResponseEntity.ok(myOrders);
    }
    
    /**
     * GET /api/orders/{id}
     * 查询指定订单详情 (需认证，且只能查询自己的)
     */
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderDetails(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        
        try {
            Order order = orderService.findOrderDetails(id, username);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            // 捕获权限错误或订单不存在
            return new ResponseEntity(e.getMessage(), HttpStatus.NOT_FOUND); // NOT_FOUND 适合隐藏业务逻辑
        }
    }
}