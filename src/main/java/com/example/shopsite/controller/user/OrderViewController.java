package com.example.shopsite.controller.user;

import com.example.shopsite.model.Order;
import com.example.shopsite.service.OrderService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/orders")
public class OrderViewController {

    private final OrderService orderService;

    public OrderViewController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * GET /orders - 订单列表页面
     */
    @GetMapping
    public String orderList(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = authentication.getName();
        List<Order> orders = orderService.findMyOrders(username);

        model.addAttribute("orders", orders);
        model.addAttribute("pageTitle", "我的订单");

        return "user/orders";
    }

    /**
     * GET /orders/{id} - 订单详情页面
     */
    @GetMapping("/{id}")
    public String orderDetail(@PathVariable Long id, Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = authentication.getName();

        try {
            Order order = orderService.findOrderDetails(id, username);
            model.addAttribute("order", order);
            model.addAttribute("pageTitle", "订单详情");
            return "user/order_detail";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error/404";
        }
    }

    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = authentication.getName();

        try {
            orderService.cancelOrderByCustomer(id, username);
            redirectAttributes.addFlashAttribute("success", "订单已取消");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/orders";
    }
}
