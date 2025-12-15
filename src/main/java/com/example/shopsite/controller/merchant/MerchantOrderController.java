package com.example.shopsite.controller.merchant;

import com.example.shopsite.model.Order;
import com.example.shopsite.model.OrderStatus;
import com.example.shopsite.model.User;
import com.example.shopsite.repository.UserRepository;
import com.example.shopsite.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/merchant/orders")
@PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
public class MerchantOrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    public MerchantOrderController(OrderService orderService, UserRepository userRepository) {
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    /**
     * GET /merchant/orders - 商户订单列表
     */
    @GetMapping
    public String orderList(Model model) {
        // 从 SecurityContext 获取当前登录用户的用户名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        
        // 从数据库加载 User 实体
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            model.addAttribute("error", "用户未找到");
            return "merchant/dashboard";
        }
        
        User merchant = userOpt.get();
        
        List<Order> orders = orderService.findOrdersByMerchant(merchant);
        model.addAttribute("orders", orders);
        model.addAttribute("pageTitle", "订单管理");
        return "merchant/order_list";
    }

    /**
     * GET /merchant/orders/{id} - 订单详情
     */
    @GetMapping("/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        // 从 SecurityContext 获取当前登录用户的用户名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        
        // 从数据库加载 User 实体
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            model.addAttribute("error", "用户未找到");
            return "merchant/dashboard";
        }
        
        User merchant = userOpt.get();
        
        try {
            Order order = orderService.findOrderDetailsForMerchant(id, merchant);
            model.addAttribute("order", order);
            model.addAttribute("pageTitle", "订单详情");
            return "merchant/order_detail";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error/404";
        }
    }

    /**
     * POST /merchant/orders/{id}/ship - 发货
     */
    @PostMapping("/{id}/ship")
    public String shipOrder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        // 从 SecurityContext 获取当前登录用户的用户名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        
        // 从数据库加载 User 实体
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "用户未找到");
            return "redirect:/merchant/orders";
        }
        
        User merchant = userOpt.get();
        
        try {
            orderService.shipOrder(id, merchant);
            redirectAttributes.addFlashAttribute("success", "订单已发货");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/merchant/orders";
    }

    /**
     * POST /merchant/orders/{id}/cancel - 取消订单
     */
    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "用户未找到");
            return "redirect:/merchant/orders";
        }
        
        User merchant = userOpt.get();
        
        try {
            Order order = orderService.findOrderDetailsForMerchant(id, merchant);
            if (order.getStatus() == OrderStatus.PENDING_PAYMENT || 
                order.getStatus() == OrderStatus.PROCESSING) {
                Order updatedOrder = new Order();
                updatedOrder.setStatus(OrderStatus.CANCELLED);
                orderService.updateOrder(id, merchant, updatedOrder);
                redirectAttributes.addFlashAttribute("success", "订单已取消");
            } else {
                redirectAttributes.addFlashAttribute("error", "该订单状态不允许取消");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/merchant/orders";
    }

    /**
     * POST /merchant/orders/{id}/update-status - 更新订单状态
     */
    @PostMapping("/{id}/update-status")
    public String updateOrderStatus(@PathVariable Long id,
                                    @RequestParam OrderStatus status,
                                    RedirectAttributes redirectAttributes) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "用户未找到");
            return "redirect:/merchant/orders";
        }
        
        User merchant = userOpt.get();
        
        try {
            Order updatedOrder = new Order();
            updatedOrder.setStatus(status);
            orderService.updateOrder(id, merchant, updatedOrder);
            redirectAttributes.addFlashAttribute("success", "订单状态已更新");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/merchant/orders/" + id;
    }
}





