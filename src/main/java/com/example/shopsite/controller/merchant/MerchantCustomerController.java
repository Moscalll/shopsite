
package com.example.shopsite.controller.merchant;

import com.example.shopsite.model.Order;
import com.example.shopsite.model.Product;
import com.example.shopsite.model.SalesLog;
import com.example.shopsite.model.User;
import com.example.shopsite.repository.ProductRepository;
import com.example.shopsite.repository.SalesLogRepository;
import com.example.shopsite.repository.UserRepository;
import com.example.shopsite.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/merchant/customers")
@PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
public class MerchantCustomerController {

    private final OrderService orderService;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final SalesLogRepository salesLogRepository;

    public MerchantCustomerController(OrderService orderService,
            UserRepository userRepository,
            ProductRepository productRepository,
            SalesLogRepository salesLogRepository) {
        this.orderService = orderService;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.salesLogRepository = salesLogRepository;
    }

    /**
     * GET /merchant/customers - 客户列表
     */
    @GetMapping
    public String customerList(Model model) {
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
                    .map(Order::getTotalAmount)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
            stats.put("lastOrderDate", customerOrders.stream()
                    .map(Order::getOrderDate)
                    .max(java.time.LocalDateTime::compareTo)
                    .orElse(null));

            customerStats.put(customer.getId(), stats);
        }

        model.addAttribute("customerStats", customerStats);
        model.addAttribute("pageTitle", "客户管理");

        return "merchant/customers";
    }

    /**
     * GET /merchant/customers/{id} - 客户详情（包含浏览/购买日志）
     */
    @GetMapping("/{id}")
    public String customerDetail(@PathVariable Long id,
            @RequestParam(required = false) String actionType,
            Model model) {
        // 从 SecurityContext 获取当前登录用户的用户名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        // 从数据库加载 User 实体
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            model.addAttribute("error", "用户未找到");
            return "merchant/dashboard";
        }

        User merchant = userOpt.get();

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

        // 获取商户的所有商品ID
        List<Long> productIds = productRepository.findByMerchant(merchant).stream()
                .map(Product::getId)
                .collect(Collectors.toList());

        // 查询该客户对商户商品的浏览/购买日志
        List<SalesLog> logs;
        if (actionType != null && !actionType.isEmpty()) {
            logs = salesLogRepository.findByUserAndProductIdInAndActionType(customer, productIds, actionType);
        } else {
            logs = salesLogRepository.findByUserAndProductIdIn(customer, productIds);
        }

        // 按时间倒序排列
        logs.sort((a, b) -> b.getLogTime().compareTo(a.getLogTime()));

        // 在 customerDetail 方法中，在设置 model 之前添加：
        java.math.BigDecimal totalSpent = customerOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        model.addAttribute("customer", customer);
        model.addAttribute("orders", customerOrders);
        model.addAttribute("totalSpent", totalSpent); // 添加这一行
        model.addAttribute("logs", logs);
        model.addAttribute("actionType", actionType);
        model.addAttribute("pageTitle", "客户详情: " + customer.getUsername());

        return "merchant/customer_detail";
    }
}