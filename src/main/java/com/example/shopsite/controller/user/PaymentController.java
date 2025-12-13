package com.example.shopsite.controller.user;

import com.example.shopsite.model.Order;
import com.example.shopsite.model.OrderStatus;
import com.example.shopsite.model.User;
import com.example.shopsite.repository.OrderRepository;
import com.example.shopsite.repository.UserRepository;
import com.example.shopsite.service.CartService;
import com.example.shopsite.service.EmailService;
import com.example.shopsite.service.OrderService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/payment")
public class PaymentController {

    private final OrderService orderService;
    private final CartService cartService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final EmailService emailService;

    public PaymentController(OrderService orderService, CartService cartService,
                           UserRepository userRepository, OrderRepository orderRepository,
                           EmailService emailService) {
        this.orderService = orderService;
        this.cartService = cartService;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.emailService = emailService;
    }

    /**
     * POST /checkout/create-order - 创建订单并跳转到付款页面
     */
    @PostMapping("/create-order")
    public String createOrder(Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }

        User user = userOpt.get();
        
        // 从购物车创建订单
        try {
            Order order = orderService.createOrderFromCart(user);
            return "redirect:/payment/" + order.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "创建订单失败: " + e.getMessage());
            return "redirect:/checkout";
        }
    }

    /**
     * GET /payment/{orderId} - 付款页面
     */
    @GetMapping("/{orderId}")
    public String paymentPage(@PathVariable Long orderId,
                              Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }

        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty() || !orderOpt.get().getUser().getUsername().equals(username)) {
            model.addAttribute("error", "订单不存在");
            return "error/404";
        }

        Order order = orderOpt.get();
        model.addAttribute("order", order);
        model.addAttribute("pageTitle", "支付订单");

        return "user/payment";
    }

    /**
     * POST /payment/{orderId}/pay - 处理支付（前端模拟）
     */
    @PostMapping("/{orderId}/pay")
    public String processPayment(@PathVariable Long orderId,
                                 @RequestParam String paymentMethod,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }

        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty() || !orderOpt.get().getUser().getUsername().equals(username)) {
            redirectAttributes.addFlashAttribute("error", "订单不存在");
            return "redirect:/orders";
        }

        Order order = orderOpt.get();
        
        // 前端模拟支付，直接标记为已付款
        order.setStatus(OrderStatus.PROCESSING);
        orderRepository.save(order);

        // 清空购物车
        cartService.clearCart(userOpt.get());

        // 发送确认邮件
        try {
            emailService.sendOrderConfirmationEmail(order);
            redirectAttributes.addFlashAttribute("success", "支付成功！确认邮件已发送到您的邮箱。");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("success", "支付成功！");
            redirectAttributes.addFlashAttribute("warning", "邮件发送失败，但订单已创建。");
        }

        return "redirect:/orders/" + orderId;
    }
}

