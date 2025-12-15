package com.example.shopsite.controller.admin;

import com.example.shopsite.model.Order;
import com.example.shopsite.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@Controller
@RequestMapping("/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * GET /admin/orders - 全站订单列表
     */
    @GetMapping
    public String orderList(@RequestParam(required = false) String keyword, Model model) {
        List<Order> orders;
        if (keyword != null && !keyword.trim().isEmpty()) {
            orders = orderService.findOrdersByKeyword(keyword);
        } else {
            orders = orderService.findAllOrders();
        }

        // 统计信息
        long totalOrders = orders.size();
        long pendingPayment = orders.stream().filter(o -> o.getStatus().name().equals("PENDING_PAYMENT")).count();
        long processing = orders.stream().filter(o -> o.getStatus().name().equals("PROCESSING")).count();
        long shipped = orders.stream().filter(o -> o.getStatus().name().equals("SHIPPED")).count();
        long completed = orders.stream().filter(o -> o.getStatus().name().equals("COMPLETED")).count();
        long cancelled = orders.stream().filter(o -> o.getStatus().name().equals("CANCELLED")).count();

        java.math.BigDecimal totalAmount = orders.stream()
                .map(Order::getTotalAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        model.addAttribute("orders", orders);
        model.addAttribute("keyword", keyword);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("pendingPayment", pendingPayment);
        model.addAttribute("processing", processing);
        model.addAttribute("shipped", shipped);
        model.addAttribute("completed", completed);
        model.addAttribute("cancelled", cancelled);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("pageTitle", "全站订单管理");
        return "admin/order_list";
    }

    /**
     * GET /admin/orders/{id} - 订单详情
     */
    @GetMapping("/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        try {
            Order order = orderService.findOrderDetailsForAdmin(id);
            model.addAttribute("order", order);
            model.addAttribute("pageTitle", "订单详情");
            return "admin/order_detail";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error/404";
        }
    }
}
