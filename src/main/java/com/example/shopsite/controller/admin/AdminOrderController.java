package com.example.shopsite.controller.admin;

import com.example.shopsite.model.Order;
import com.example.shopsite.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

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
    public String orderList(Model model) {
        List<Order> orders = orderService.findAllOrders();
        model.addAttribute("orders", orders);
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



