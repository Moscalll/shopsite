package com.example.shopsite.controller.merchant;

import com.example.shopsite.model.Order;
import com.example.shopsite.model.Product;
import com.example.shopsite.model.User;
import com.example.shopsite.repository.OrderItemRepository;
import com.example.shopsite.repository.UserRepository;
import com.example.shopsite.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/merchant/statistics")
@PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
public class MerchantStatisticsController {

    private final OrderService orderService;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    public MerchantStatisticsController(OrderService orderService, 
                                       OrderItemRepository orderItemRepository,
                                       UserRepository userRepository) {
        this.orderService = orderService;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
    }

    /**
     * GET /merchant/statistics - 销售统计报表
     */
    @GetMapping
    public String statistics(Model model) {
        // 从 SecurityContext 获取当前登录用户的用户名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        
        // 从数据库加载 User 实体
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            model.addAttribute("error", "用户未找到");
            return "merchant/dashboard";
        }
        
        User merchant = userOpt.get();
        
        // 获取商户的所有订单
        List<Order> orders = orderService.findOrdersByMerchant(merchant);
        
        // 统计总销售额
        BigDecimal totalSales = orders.stream()
                .filter(o -> o.getStatus().name().equals("COMPLETED") || 
                           o.getStatus().name().equals("DELIVERED") ||
                           o.getStatus().name().equals("SHIPPED") ||
                           o.getStatus().name().equals("PROCESSING"))
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 统计订单数量
        long totalOrders = orders.size();
        long completedOrders = orders.stream()
                .filter(o -> o.getStatus().name().equals("COMPLETED"))
                .count();
        
        // 统计商品销量（前10名）- 只统计该商户的商品
        List<Object[]> allTopProducts = orderItemRepository.findTopSellingProducts();
        List<Object[]> topProducts = allTopProducts.stream()
                .filter(result -> {
                    Product product = (Product) result[0];
                    // 添加 null 检查，避免 NullPointerException
                    return product.getMerchant() != null && 
                           product.getMerchant().getId() != null &&
                           product.getMerchant().getId().equals(merchant.getId());
                })
                .limit(10)
                .collect(Collectors.toList());
        
        // 统计各状态订单数量
        Map<String, Long> orderStatusCount = orders.stream()
                .collect(Collectors.groupingBy(
                    o -> o.getStatus().name(),
                    Collectors.counting()
                ));
        
        model.addAttribute("totalSales", totalSales);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("completedOrders", completedOrders);
        model.addAttribute("topProducts", topProducts);
        model.addAttribute("orderStatusCount", orderStatusCount);
        model.addAttribute("pageTitle", "销售统计");
        
        return "merchant/statistics";
    }
}

