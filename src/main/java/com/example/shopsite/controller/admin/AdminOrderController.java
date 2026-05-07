package com.example.shopsite.controller.admin;

import com.example.shopsite.model.Order;
import com.example.shopsite.model.OrderStatus;
import com.example.shopsite.repository.OrderRepository;
import com.example.shopsite.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
@RequestMapping("/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    public AdminOrderController(OrderService orderService, OrderRepository orderRepository) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
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
        model.addAttribute("activeMenu", "orders");
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
            model.addAttribute("activeMenu", "orders");
            return "admin/order_detail";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error/404";
        }
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam OrderStatus status,
                               RedirectAttributes ra) {
        try {
            Order order = orderRepository.findById(id).orElseThrow(() -> new RuntimeException("订单不存在"));
            order.setStatus(status);
            orderRepository.save(order);
            ra.addFlashAttribute("success", "订单状态已更新");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes ra) {
        try {
            Order order = orderRepository.findById(id).orElseThrow(() -> new RuntimeException("订单不存在"));
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            ra.addFlashAttribute("success", "订单已取消");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            orderRepository.deleteById(id);
            ra.addFlashAttribute("success", "订单已删除");
            return "redirect:/admin/orders";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/orders/" + id;
        }
    }
}
