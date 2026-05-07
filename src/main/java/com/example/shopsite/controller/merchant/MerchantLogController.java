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
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/merchant/logs")
@PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
public class MerchantLogController {

    private final OrderService orderService;
    private final SalesLogRepository salesLogRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public MerchantLogController(OrderService orderService,
                                 SalesLogRepository salesLogRepository,
                                 ProductRepository productRepository,
                                 UserRepository userRepository) {
        this.orderService = orderService;
        this.salesLogRepository = salesLogRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String logs(Model model) {
        // 日志查看入口已迁移到「客户管理 → 客户详情」页面，这里保留兼容跳转
        return "redirect:/merchant/customers";
    }

    /*
     * 旧的“用户日志中心”实现保留在版本历史中。
     * 当前版本统一在 MerchantCustomerController.customerDetail 展示客户行为日志。
     */

    /*
    @GetMapping
    public String logs(Model model) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            model.addAttribute("error", "用户未找到");
            return "merchant/dashboard";
        }

        User merchant = userOpt.get();
        List<Product> merchantProducts = productRepository.findByMerchant(merchant);
        List<Long> productIds = merchantProducts.stream().map(Product::getId).toList();
        List<Order> orders = orderService.findOrdersByMerchant(merchant);
        List<SalesLog> logs = productIds.isEmpty()
                ? List.of()
                : salesLogRepository.findByProductIdIn(productIds).stream()
                .filter(log -> log.getProductId() != null && productIds.contains(log.getProductId()))
                .sorted(Comparator.comparing(SalesLog::getLogTime).reversed())
                .collect(Collectors.toList());

        long totalOrders = orders.size();
        long completedOrders = orders.stream()
                .filter(o -> o.getStatus() != null && "COMPLETED".equals(o.getStatus().name()))
                .count();
        long paidOrders = orders.stream()
                .filter(o -> o.getStatus() != null && ("COMPLETED".equals(o.getStatus().name())
                        || "DELIVERED".equals(o.getStatus().name())
                        || "SHIPPED".equals(o.getStatus().name())
                        || "PROCESSING".equals(o.getStatus().name())))
                .count();
        BigDecimal totalSales = orders.stream()
                .filter(o -> o.getStatus() != null && ("COMPLETED".equals(o.getStatus().name())
                        || "DELIVERED".equals(o.getStatus().name())
                        || "SHIPPED".equals(o.getStatus().name())
                        || "PROCESSING".equals(o.getStatus().name())))
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Long> salesStatus = new LinkedHashMap<>();
        salesStatus.put("待确认", orders.stream().filter(o -> o.getStatus() != null && "PENDING_PAYMENT".equals(o.getStatus().name())).count());
        salesStatus.put("处理中", orders.stream().filter(o -> o.getStatus() != null && "PROCESSING".equals(o.getStatus().name())).count());
        salesStatus.put("已发货", orders.stream().filter(o -> o.getStatus() != null && "SHIPPED".equals(o.getStatus().name())).count());
        salesStatus.put("已完成", completedOrders);
        salesStatus.put("已取消", orders.stream().filter(o -> o.getStatus() != null && "CANCELLED".equals(o.getStatus().name())).count());

        Map<String, Long> trendByDay = logs.stream()
                .collect(Collectors.groupingBy(log -> log.getLogTime().toLocalDate().format(DateTimeFormatter.ISO_DATE),
                        Collectors.counting()));
        Map<String, Long> salesTrend = trendByDay.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));

        Map<Long, Long> productRankMap = logs.stream()
                .filter(log -> log.getActionType() != null && ("PURCHASE".equalsIgnoreCase(log.getActionType()) || "ADD_TO_CART".equalsIgnoreCase(log.getActionType())))
                .collect(Collectors.groupingBy(SalesLog::getProductId, Collectors.counting()));
        List<Map<String, Object>> topProducts = productRankMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(10)
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("productId", entry.getKey());
                    item.put("count", entry.getValue());
                    item.put("productName", merchantProducts.stream()
                            .filter(product -> product.getId().equals(entry.getKey()))
                            .map(Product::getName)
                            .findFirst()
                            .orElse("未知商品"));
                    return item;
                })
                .collect(Collectors.toList());

        List<SalesLog> behaviorLogs = logs.stream()
                .filter(log -> log.getActionType() != null)
                .filter(log -> "VIEW".equalsIgnoreCase(log.getActionType())
                        || "PURCHASE".equalsIgnoreCase(log.getActionType())
                        || "ADD_TO_CART".equalsIgnoreCase(log.getActionType()))
                .limit(20)
                .collect(Collectors.toList());

        long viewCount = logs.stream().filter(log -> "VIEW".equalsIgnoreCase(log.getActionType())).count();
        long purchaseCount = logs.stream().filter(log -> "PURCHASE".equalsIgnoreCase(log.getActionType())).count();
        long addToCartCount = logs.stream().filter(log -> "ADD_TO_CART".equalsIgnoreCase(log.getActionType())).count();
        double conversionRate = viewCount == 0 ? 0D : (purchaseCount * 100.0 / viewCount);

        long todayViews = logs.stream()
                .filter(log -> log.getActionType() != null && "VIEW".equalsIgnoreCase(log.getActionType()))
                .filter(log -> log.getLogTime() != null && log.getLogTime().toLocalDate().isEqual(LocalDate.now()))
                .count();
        long todayPurchases = logs.stream()
                .filter(log -> log.getActionType() != null && "PURCHASE".equalsIgnoreCase(log.getActionType()))
                .filter(log -> log.getLogTime() != null && log.getLogTime().toLocalDate().isEqual(LocalDate.now()))
                .count();

        List<String> alerts = new java.util.ArrayList<>();
        if (conversionRate < 1.5 && viewCount > 30) {
            alerts.add("当前浏览转化率偏低，建议检查商品详情页、价格策略或促销活动。");
        }
        if (todayViews > 200 && todayPurchases == 0) {
            alerts.add("今日浏览量明显偏高但未形成购买，建议关注商品库存、运费与活动配置。");
        }
        if (orders.stream().anyMatch(o -> o.getStatus() != null && "CANCELLED".equals(o.getStatus().name())) && orders.size() >= 10) {
            alerts.add("订单取消率存在波动，建议排查缺货、支付失败或履约异常。");
        }
        if (alerts.isEmpty()) {
            alerts.add("当前未发现明显异常，经营状态整体稳定。");
        }        model.addAttribute("pageTitle", "商家用户日志中心");
        model.addAttribute("merchant", merchant);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("completedOrders", completedOrders);
        model.addAttribute("paidOrders", paidOrders);
        model.addAttribute("totalSales", totalSales);
        model.addAttribute("salesStatus", salesStatus);
        model.addAttribute("salesTrend", salesTrend);
        model.addAttribute("topProducts", topProducts);
        model.addAttribute("behaviorLogs", behaviorLogs);
        model.addAttribute("viewCount", viewCount);
        model.addAttribute("purchaseCount", purchaseCount);
        model.addAttribute("addToCartCount", addToCartCount);
        model.addAttribute("conversionRate", String.format("%.2f", conversionRate));
        model.addAttribute("alerts", alerts);
        model.addAttribute("logs", logs);
        return "merchant/logs";
    }
    */
}