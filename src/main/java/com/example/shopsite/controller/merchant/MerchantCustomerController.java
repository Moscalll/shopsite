
package com.example.shopsite.controller.merchant;

import com.example.shopsite.model.Order;
import com.example.shopsite.model.Product;
import com.example.shopsite.model.SalesLog;
import com.example.shopsite.model.User;
import com.example.shopsite.model.UserBehaviorLog;
import com.example.shopsite.repository.ProductRepository;
import com.example.shopsite.repository.SalesLogRepository;
import com.example.shopsite.repository.UserBehaviorLogRepository;
import com.example.shopsite.repository.UserRepository;
import com.example.shopsite.support.CategoryLabelService;
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
import java.util.LinkedHashMap;
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
    private final UserBehaviorLogRepository userBehaviorLogRepository;
    private final CategoryLabelService categoryLabelService;

    public MerchantCustomerController(OrderService orderService,
            UserRepository userRepository,
            ProductRepository productRepository,
            SalesLogRepository salesLogRepository,
            UserBehaviorLogRepository userBehaviorLogRepository,
            CategoryLabelService categoryLabelService) {
        this.orderService = orderService;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.salesLogRepository = salesLogRepository;
        this.userBehaviorLogRepository = userBehaviorLogRepository;
        this.categoryLabelService = categoryLabelService;
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

        List<SalesLog> salesLogsForStats = productIds.isEmpty()
                ? List.of()
                : salesLogRepository.findByUserAndProductIdIn(customer, productIds);
        salesLogsForStats.sort((a, b) -> b.getLogTime().compareTo(a.getLogTime()));

        List<UserBehaviorLog> behaviorLogs = productIds.isEmpty() || customer.getId() == null
                ? List.of()
                : userBehaviorLogRepository.findByUser_IdAndProductIdInOrderByEventTimeDesc(customer.getId(), productIds);
        Map<Long, String> categoryNames = categoryLabelService.labelsForBehaviorLogs(behaviorLogs);

        // --- 将“日志中心”展示类型迁移到客户详情：销售状态/趋势/商品排名/告警 ---
        Map<String, Long> salesStatus = new LinkedHashMap<>();
        salesStatus.put("待付款", customerOrders.stream().filter(o -> o.getStatus() != null && "PENDING_PAYMENT".equals(o.getStatus().name())).count());
        salesStatus.put("处理中", customerOrders.stream().filter(o -> o.getStatus() != null && "PROCESSING".equals(o.getStatus().name())).count());
        salesStatus.put("已发货", customerOrders.stream().filter(o -> o.getStatus() != null && "SHIPPED".equals(o.getStatus().name())).count());
        salesStatus.put("已完成", customerOrders.stream().filter(o -> o.getStatus() != null && "COMPLETED".equals(o.getStatus().name())).count());
        salesStatus.put("已取消", customerOrders.stream().filter(o -> o.getStatus() != null && "CANCELLED".equals(o.getStatus().name())).count());

        Map<String, Long> trendByDay = behaviorLogs.stream()
                .filter(l -> l.getEventTime() != null)
                .collect(Collectors.groupingBy(l -> l.getEventTime().toLocalDate().toString(), Collectors.counting()));
        Map<String, Long> salesTrend = trendByDay.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));

        Map<Long, Long> productRankMap = salesLogsForStats.stream()
                .filter(l -> l.getActionType() != null && ("PURCHASE".equalsIgnoreCase(l.getActionType()) || "ADD_TO_CART".equalsIgnoreCase(l.getActionType())))
                .filter(l -> l.getProductId() != null)
                .collect(Collectors.groupingBy(SalesLog::getProductId, Collectors.counting()));
        List<Product> merchantProducts = productRepository.findByMerchant(merchant);
        List<Map<String, Object>> topProducts = productRankMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("productId", e.getKey());
                    item.put("count", e.getValue());
                    item.put("productName", merchantProducts.stream()
                            .filter(p -> p.getId().equals(e.getKey()))
                            .map(Product::getName)
                            .findFirst()
                            .orElse("未知商品"));
                    return item;
                })
                .toList();

        long viewCount = salesLogsForStats.stream().filter(l -> "VIEW".equalsIgnoreCase(l.getActionType())).count();
        long purchaseCount = salesLogsForStats.stream().filter(l -> "PURCHASE".equalsIgnoreCase(l.getActionType())).count();
        double conversionRate = viewCount == 0 ? 0D : (purchaseCount * 100.0 / viewCount);
        List<String> alerts = new java.util.ArrayList<>();
        if (conversionRate < 1.5 && viewCount > 10) alerts.add("该客户浏览转化率偏低，可考虑优惠触达或优化商品组合。");
        if (alerts.isEmpty()) alerts.add("当前未发现明显异常，该客户行为较稳定。");

        // 在 customerDetail 方法中，在设置 model 之前添加：
        java.math.BigDecimal totalSpent = customerOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        model.addAttribute("customer", customer);
        model.addAttribute("orders", customerOrders);
        model.addAttribute("totalSpent", totalSpent); 
        model.addAttribute("behaviorLogs", behaviorLogs);
        model.addAttribute("categoryNames", categoryNames);
        model.addAttribute("salesStatus", salesStatus);
        model.addAttribute("salesTrend", salesTrend);
        model.addAttribute("topProducts", topProducts);
        model.addAttribute("alerts", alerts);
        model.addAttribute("conversionRate", String.format("%.2f", conversionRate));
        model.addAttribute("pageTitle", "客户详情: " + customer.getUsername());

        return "merchant/customer_detail";
    }
}