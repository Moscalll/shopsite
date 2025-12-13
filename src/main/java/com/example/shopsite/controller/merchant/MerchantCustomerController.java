package com.example.shopsite.controller.merchant;

import com.example.shopsite.model.Order;
import com.example.shopsite.model.User;
import com.example.shopsite.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/merchant/customers")
@PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
public class MerchantCustomerController {

    private final OrderService orderService;

    public MerchantCustomerController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * GET /merchant/customers - 客户列表
     */
    @GetMapping
    public String customerList(@AuthenticationPrincipal User merchant, Model model) {
        // 获取商户的所有订单
        List<Order> orders = orderService.findOrdersByMerchant(merchant);
        
        // 提取所有客户（去重）
        Set<User> customers = orders.stream()
                .map(Order::getUser)
                .collect(Collectors.toSet());
        
        // 统计每个客户的订单数和总消费
        Map<Long, Map<String, Object>> customerStats = new HashMap<>();
        for (User customer : customers) {
            List<Order> customerOrders = orders.stream()
                    .filter(o -> o.getUser().getId().equals(customer.getId()))
                    .collect(Collectors.toList());
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("customer", customer);
            stats.put("orderCount", customerOrders.size());
            stats.put("totalSpent", customerOrders.stream()
                    .map(o -> o.getTotalAmount())
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
            stats.put("lastOrderDate", customerOrders.stream()
                    .map(o -> o.getOrderDate())
                    .max(java.time.LocalDateTime::compareTo)
                    .orElse(null));
            
            customerStats.put(customer.getId(), stats);
        }
        
        model.addAttribute("customerStats", customerStats);
        model.addAttribute("pageTitle", "客户管理");
        
        return "merchant/customers";
    }

    /**
     * GET /merchant/customers/{id} - 客户详情
     */
    @GetMapping("/{id}")
    public String customerDetail(@PathVariable Long id,
                                 @AuthenticationPrincipal User merchant,
                                 Model model) {
        // 获取该客户的所有订单（只包含商户的商品）
        List<Order> allOrders = orderService.findOrdersByMerchant(merchant);
        List<Order> customerOrders = allOrders.stream()
                .filter(o -> o.getUser().getId().equals(id))
                .collect(Collectors.toList());
        
        if (customerOrders.isEmpty()) {
            model.addAttribute("error", "客户不存在或无相关订单");
            return "error/404";
        }
        
        User customer = customerOrders.get(0).getUser();
        
        model.addAttribute("customer", customer);
        model.addAttribute("orders", customerOrders);
        model.addAttribute("pageTitle", "客户详情: " + customer.getUsername());
        
        return "merchant/customer_detail";
    }
}

