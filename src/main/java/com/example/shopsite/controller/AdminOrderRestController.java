package com.example.shopsite.controller;

import com.example.shopsite.model.Order;
import com.example.shopsite.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderRestController {

    private final OrderService orderService;

    public AdminOrderRestController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * GET /api/admin/orders
     * 查询所有订单列表。只允许 MERCHANT 和 ADMIN 访问。
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MERCHANT')")
    public ResponseEntity<List<Order>> getAllOrders() {
        List<Order> orders = orderService.findAllOrders();
        return ResponseEntity.ok(orders);
    }

    /**
     * GET /api/admin/orders/{id}
     * 查询任意订单详情。只允许 MERCHANT 和 ADMIN 访问。
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MERCHANT')")
    public ResponseEntity<Order> getOrderDetails(@PathVariable Long id) {
        try {
            Order order = orderService.findOrderDetailsForAdmin(id);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            // 订单不存在或查询失败
            return ResponseEntity.notFound().build();
        }
    }
}





