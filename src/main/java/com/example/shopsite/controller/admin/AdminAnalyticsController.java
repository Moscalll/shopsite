package com.example.shopsite.controller.admin;

import com.example.shopsite.model.Category;
import com.example.shopsite.model.AuthLoginLog;
import com.example.shopsite.model.Order;
import com.example.shopsite.model.OrderItem;
import com.example.shopsite.model.OrderStatus;
import com.example.shopsite.model.Product;
import com.example.shopsite.model.SalesLog;
import com.example.shopsite.model.RecommendationAlgorithm;
import com.example.shopsite.model.UserBehaviorLog;
import com.example.shopsite.repository.AuthLoginLogRepository;
import com.example.shopsite.repository.CartItemRepository;
import com.example.shopsite.repository.FavoriteRepository;
import com.example.shopsite.repository.OrderItemRepository;
import com.example.shopsite.repository.OrderRepository;
import com.example.shopsite.repository.ProductRepository;
import com.example.shopsite.repository.SalesLogRepository;
import com.example.shopsite.repository.RecommendationResultRepository;
import com.example.shopsite.repository.UserBehaviorLogRepository;
import com.example.shopsite.repository.UserRepository;
import com.example.shopsite.service.CategoryService;
import com.example.shopsite.support.AnalyticsTrendForecast;
import com.example.shopsite.support.RegionInferenceService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/analytics")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsController {

    private final OrderRepository orderRepository;
    private final SalesLogRepository salesLogRepository;
    private final OrderItemRepository orderItemRepository;
    private final AuthLoginLogRepository authLoginLogRepository;
    private final FavoriteRepository favoriteRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final UserBehaviorLogRepository userBehaviorLogRepository;
    private final RegionInferenceService regionInferenceService;
    private final RecommendationResultRepository recommendationResultRepository;

    public AdminAnalyticsController(OrderRepository orderRepository,
                                    SalesLogRepository salesLogRepository,
                                    OrderItemRepository orderItemRepository,
                                    AuthLoginLogRepository authLoginLogRepository,
                                    FavoriteRepository favoriteRepository,
                                    CartItemRepository cartItemRepository,
                                    UserRepository userRepository,
                                    ProductRepository productRepository,
                                    CategoryService categoryService,
                                    UserBehaviorLogRepository userBehaviorLogRepository,
                                    RegionInferenceService regionInferenceService,
                                    RecommendationResultRepository recommendationResultRepository) {
        this.orderRepository = orderRepository;
        this.salesLogRepository = salesLogRepository;
        this.orderItemRepository = orderItemRepository;
        this.authLoginLogRepository = authLoginLogRepository;
        this.favoriteRepository = favoriteRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.categoryService = categoryService;
        this.userBehaviorLogRepository = userBehaviorLogRepository;
        this.regionInferenceService = regionInferenceService;
        this.recommendationResultRepository = recommendationResultRepository;
    }

    @GetMapping
    public String analyticsEntry() {
        return "redirect:/admin/analytics/dashboard";
    }

    @GetMapping("/dashboard")
    public String analyticsDashboard(
            @RequestParam(defaultValue = "gmv") String trendMetric,
            @RequestParam(defaultValue = "30") int trendDays,
            @RequestParam(defaultValue = "DAY") String trendGranularity,
            @RequestParam(required = false) Long trendCategoryId,
            @RequestParam(required = false) Long trendProductId,
            Model model) {
        List<Order> orders = orderRepository.findAll();
        List<SalesLog> salesLogs = salesLogRepository.findAll();

        BigDecimal totalSales = orders.stream()
                .filter(o -> o.getStatus() != null && ("PROCESSING".equals(o.getStatus().name())
                        || "SHIPPED".equals(o.getStatus().name())
                        || "DELIVERED".equals(o.getStatus().name())
                        || "COMPLETED".equals(o.getStatus().name())))
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Long> salesStatus = new LinkedHashMap<>();
        salesStatus.put("待付款", orders.stream().filter(o -> o.getStatus() != null && "PENDING_PAYMENT".equals(o.getStatus().name())).count());
        salesStatus.put("处理中", orders.stream().filter(o -> o.getStatus() != null && "PROCESSING".equals(o.getStatus().name())).count());
        salesStatus.put("已发货", orders.stream().filter(o -> o.getStatus() != null && "SHIPPED".equals(o.getStatus().name())).count());
        salesStatus.put("已完成", orders.stream().filter(o -> o.getStatus() != null && "COMPLETED".equals(o.getStatus().name())).count());
        salesStatus.put("已取消", orders.stream().filter(o -> o.getStatus() != null && "CANCELLED".equals(o.getStatus().name())).count());

        String metric = trendMetric == null ? "gmv" : trendMetric.trim().toLowerCase();
        if (!List.of("gmv", "orders", "units", "conversion").contains(metric)) {
            metric = "gmv";
        }
        if (trendDays != 7 && trendDays != 30 && trendDays != 90) {
            trendDays = 30;
        }
        String gran = trendGranularity == null ? "DAY" : trendGranularity.trim().toUpperCase();
        if (!List.of("DAY", "WEEK", "MONTH").contains(gran)) {
            gran = "DAY";
        }

        List<Category> categories = categoryService.findAllCategories();
        Map<String, Double> trendDaily = buildTrendDailySeries(orders, salesLogs, metric, trendDays, trendCategoryId, trendProductId);
        Map<String, Double> trendBucketed = applyTrendGranularity(trendDaily, gran, metric);
        List<String> trendChartLabels = new ArrayList<>(trendBucketed.keySet());
        List<Double> trendChartValues = new ArrayList<>(trendBucketed.values());
        Map<String, Object> salesForecast = AnalyticsTrendForecast.buildForecast(trendChartValues, yAxisLabelFor(metric));

        List<Map<String, Object>> topProducts = orderItemRepository.findTopSellingProducts().stream().limit(10).map(row -> {
            Product product = (Product) row[0];
            Number sold = (Number) row[1];
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("productName", product.getName());
            item.put("count", sold == null ? 0 : sold.longValue());
            return item;
        }).collect(Collectors.toList());

        // 同购推荐（基于同一订单内商品共现）
        Map<String, Long> pairCount = new HashMap<>();
        Map<Long, String> productNameCache = new HashMap<>();
        orders.stream()
                .filter(o -> o.getItems() != null && !o.getItems().isEmpty())
                .forEach(o -> {
                    List<Long> ids = o.getItems().stream()
                            .map(oi -> oi.getProduct() == null ? null : oi.getProduct().getId())
                            .filter(id -> id != null)
                            .distinct()
                            .sorted()
                            .collect(Collectors.toList());
                    for (int i = 0; i < ids.size(); i++) {
                        for (int j = i + 1; j < ids.size(); j++) {
                            String key = ids.get(i) + "-" + ids.get(j);
                            pairCount.put(key, pairCount.getOrDefault(key, 0L) + 1);
                        }
                    }
                });
        List<Map<String, Object>> coPurchaseRecs = pairCount.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(e -> {
                    String[] parts = e.getKey().split("-");
                    Long aId = Long.valueOf(parts[0]);
                    Long bId = Long.valueOf(parts[1]);
                    String aName = productNameCache.computeIfAbsent(aId,
                            id -> productRepository.findById(id).map(Product::getName).orElse("商品#" + id));
                    String bName = productNameCache.computeIfAbsent(bId,
                            id -> productRepository.findById(id).map(Product::getName).orElse("商品#" + id));
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("aId", aId);
                    row.put("bId", bId);
                    row.put("aName", aName);
                    row.put("bName", bName);
                    row.put("count", e.getValue());
                    return row;
                }).collect(Collectors.toList());

        List<UserBehaviorLog> recentUbl = userBehaviorLogRepository.findAll(
                PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "eventTime"))).getContent();

        List<Map<String, Object>> behaviorRows = new ArrayList<>();
        for (UserBehaviorLog u : recentUbl) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("time", u.getEventTime());
            row.put("type", u.getEventType());
            row.put("productId", u.getProductId());
            row.put("sessionId", u.getSessionId() != null && !u.getSessionId().isBlank() ? u.getSessionId() : "-");
            row.put("pageUrl", u.getPageUrl() != null ? u.getPageUrl() : "-");
            row.put("durationSeconds", u.getDurationSeconds());
            row.put("source", "user_behavior_log");
            behaviorRows.add(row);
        }
        for (SalesLog log : salesLogs) {
            if (log.getActionType() == null) {
                continue;
            }
            String at = log.getActionType();
            if (!"VIEW".equalsIgnoreCase(at) && !"PURCHASE".equalsIgnoreCase(at) && !"ADD_TO_CART".equalsIgnoreCase(at)) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("time", log.getLogTime());
            row.put("type", log.getActionType());
            row.put("productId", log.getProductId());
            row.put("sessionId", "-");
            row.put("pageUrl", "-");
            row.put("durationSeconds", null);
            row.put("source", "sales_log");
            behaviorRows.add(row);
        }
        behaviorRows.sort(Comparator.comparing((Map<String, Object> r) -> (java.time.LocalDateTime) r.get("time"),
                Comparator.nullsLast(Comparator.reverseOrder())));
        if (behaviorRows.size() > 50) {
            behaviorRows = new ArrayList<>(behaviorRows.subList(0, 50));
        }

        long viewCount = salesLogs.stream().filter(log -> "VIEW".equalsIgnoreCase(log.getActionType())).count();
        long purchaseCount = salesLogs.stream().filter(log -> "PURCHASE".equalsIgnoreCase(log.getActionType())).count();
        double conversionRate = viewCount == 0 ? 0D : (purchaseCount * 100.0 / viewCount);

        List<String> alerts = new java.util.ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate start7 = today.minusDays(6);
        long todayPaidOrders = orders.stream()
                .filter(AdminAnalyticsController::isPaidPlus)
                .filter(o -> o.getOrderDate() != null && o.getOrderDate().toLocalDate().equals(today))
                .count();
        long last7PaidOrders = orders.stream()
                .filter(AdminAnalyticsController::isPaidPlus)
                .filter(o -> o.getOrderDate() != null)
                .filter(o -> {
                    LocalDate d = o.getOrderDate().toLocalDate();
                    return !d.isBefore(start7) && !d.isAfter(today);
                })
                .count();
        double avgPaidPerDay7 = last7PaidOrders / 7.0;
        if (avgPaidPerDay7 >= 2.5 && todayPaidOrders < Math.max(1, avgPaidPerDay7 * 0.35)) {
            alerts.add("今日已付款链路订单数相对近7日日均明显偏低，请关注流量与转化。");
        }
        long cancelLast7 = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.CANCELLED)
                .filter(o -> o.getOrderDate() != null)
                .filter(o -> {
                    LocalDate d = o.getOrderDate().toLocalDate();
                    return !d.isBefore(start7) && !d.isAfter(today);
                })
                .count();
        if (cancelLast7 >= 5 && last7PaidOrders > 0 && cancelLast7 * 100.0 / (cancelLast7 + last7PaidOrders) > 25) {
            alerts.add("近7日取消订单占比较高，建议排查缺货、支付失败或用户主动取消原因。");
        }
        if (conversionRate < 1.2 && viewCount > 100) {
            alerts.add("全站浏览转化率偏低，请关注详情页与下单流程。");
        }
        if (alerts.isEmpty()) {
            alerts.add("当前未发现明显异常，平台运行状态稳定。");
        }

        List<String> monitorSnapshots = new java.util.ArrayList<>();
        monitorSnapshots.add(String.format("今日已付款链路订单数：%d", todayPaidOrders));
        monitorSnapshots.add(String.format("近7日已付款链路订单合计：%d（约日均 %.1f）", last7PaidOrders, avgPaidPerDay7));
        monitorSnapshots.add(String.format("近7日取消订单数：%d", cancelLast7));
        monitorSnapshots.add(String.format("全站浏览转化率（SalesLog 粗估）：%s%%", String.format("%.2f", conversionRate)));
        monitorSnapshots.add("说明：以上为当前请求时刻的切片统计，非长连接实时推送；刷新页面即可更新。");

        Map<Long, String> productCategoryNameCache = new HashMap<>();
        List<Map<String, Object>> userPortraits = userRepository.findAll().stream().map(user -> {
            long loginCount = user.getId() == null ? 0L
                    : authLoginLogRepository.countByUser_IdAndSuccess(user.getId(), true);
            long browseCount = salesLogs.stream().filter(l -> l.getUser() != null && l.getUser().getId().equals(user.getId()) && "VIEW".equalsIgnoreCase(l.getActionType())).count();
            long favoriteCount = favoriteRepository.findAll().stream().filter(f -> f.getUser() != null && f.getUser().getId().equals(user.getId())).count();
            long cartCount = cartItemRepository.findAll().stream().filter(c -> c.getUser() != null && c.getUser().getId().equals(user.getId())).count();
            long buyCount = orders.stream().filter(o -> o.getUser() != null && o.getUser().getId().equals(user.getId())).count();

            String spendingTag = buyCount >= 10 ? "高购买力" : (buyCount >= 3 ? "中购买力" : "潜力用户");
            String categoryTag = salesLogs.stream()
                    .filter(l -> l.getUser() != null && l.getUser().getId().equals(user.getId()))
                    .filter(l -> l.getProductId() != null)
                    .map(l -> {
                        Long productId = l.getProductId();
                        if (productCategoryNameCache.containsKey(productId)) {
                            return productCategoryNameCache.get(productId);
                        }
                        String name = productRepository.findById(productId)
                                .map(Product::getCategory)
                                .map(c -> c == null ? null : c.getName())
                                .orElse(null);
                        if (name != null) {
                            productCategoryNameCache.put(productId, name);
                        }
                        return name;
                    })
                    .filter(n -> n != null && !n.isBlank())
                    .collect(Collectors.groupingBy(n -> n, Collectors.counting()))
                    .entrySet()
                    .stream()
                    .max(Map.Entry.comparingByValue())
                    .map(e -> "偏好类目#" + e.getKey())
                    .orElse("偏好多样");

            Map<String, Object> portrait = new LinkedHashMap<>();
            portrait.put("username", user.getUsername());
            String regionTag = "暂无成功登录记录";
            if (user.getId() != null) {
                Optional<AuthLoginLog> lastOk = authLoginLogRepository
                        .findFirstByUser_IdAndSuccessOrderByLoginTimeDesc(user.getId(), true);
                regionTag = lastOk.map(AuthLoginLog::getIp)
                        .map(regionInferenceService::inferRegionTag)
                        .orElse("暂无成功登录记录");
            }
            portrait.put("regionTag", regionTag);
            portrait.put("spendingTag", spendingTag);
            portrait.put("categoryTag", categoryTag);
            portrait.put("loginCount", loginCount);
            portrait.put("browseCount", browseCount);
            portrait.put("favoriteCount", favoriteCount);
            portrait.put("cartCount", cartCount);
            portrait.put("buyCount", buyCount);
            return portrait;
        }).limit(20).collect(Collectors.toList());

        Map<String, Object> recommendationOverview = new LinkedHashMap<>();
        recommendationOverview.put("lastRecompute",
                recommendationResultRepository.findMaxComputedAt()
                        .map(dt -> dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                        .orElse("尚未生成推荐结果"));
        recommendationOverview.put("rowsCoPurchase",
                recommendationResultRepository.countByAlgorithm(RecommendationAlgorithm.CO_PURCHASE));
        recommendationOverview.put("rowsItemCf",
                recommendationResultRepository.countByAlgorithm(RecommendationAlgorithm.ITEM_CF));
        recommendationOverview.put("rowsPopular",
                recommendationResultRepository.countByAlgorithm(RecommendationAlgorithm.POPULAR));
        recommendationOverview.put("noteSimple",
                "商品详情「浏览过此商品的人也买了」基于同订单共现（与同购推荐同源思路）。");
        recommendationOverview.put("noteCf",
                "首页等位置的 ItemCF 推荐依赖定时/手动重算；详见「推荐系统」菜单。");

        model.addAttribute("pageTitle", "管理分析中心");
        model.addAttribute("activeMenu", "analytics");
        model.addAttribute("totalOrders", orders.size());
        model.addAttribute("totalSales", totalSales);
        model.addAttribute("salesStatus", salesStatus);
        model.addAttribute("trendChartLabels", trendChartLabels);
        model.addAttribute("trendChartValues", trendChartValues);
        model.addAttribute("trendMetric", metric);
        model.addAttribute("trendDays", trendDays);
        model.addAttribute("trendGranularity", gran);
        model.addAttribute("trendCategoryId", trendCategoryId);
        model.addAttribute("trendProductId", trendProductId);
        model.addAttribute("trendCategories", categories);
        model.addAttribute("trendYAxisLabel", yAxisLabelFor(metric));
        model.addAttribute("topProducts", topProducts);
        model.addAttribute("coPurchaseRecs", coPurchaseRecs);
        model.addAttribute("behaviorRows", behaviorRows);
        model.addAttribute("conversionRate", String.format("%.2f", conversionRate));
        model.addAttribute("alerts", alerts);
        model.addAttribute("monitorSnapshots", monitorSnapshots);
        model.addAttribute("salesForecast", salesForecast);
        model.addAttribute("userPortraits", userPortraits);
        model.addAttribute("recommendationOverview", recommendationOverview);
        return "admin/analytics_dashboard";
    }

    private static String yAxisLabelFor(String metric) {
        return switch (metric) {
            case "gmv" -> "销售额（元）";
            case "orders" -> "订单数";
            case "units" -> "销量（件）";
            case "conversion" -> "转化率（%）";
            default -> "数值";
        };
    }

    private static boolean isPaidPlus(Order o) {
        if (o == null || o.getStatus() == null) {
            return false;
        }
        OrderStatus s = o.getStatus();
        return s == OrderStatus.PROCESSING || s == OrderStatus.SHIPPED
                || s == OrderStatus.DELIVERED || s == OrderStatus.COMPLETED;
    }

    private static boolean lineMatchesFilter(OrderItem oi, Long categoryId, Long productId) {
        if (oi == null || oi.getProduct() == null) {
            return false;
        }
        if (productId != null) {
            return productId.equals(oi.getProduct().getId());
        }
        if (categoryId != null) {
            return oi.getProduct().getCategory() != null && categoryId.equals(oi.getProduct().getCategory().getId());
        }
        return true;
    }

    private static boolean orderMatchesLineFilter(Order o, Long categoryId, Long productId) {
        if (categoryId == null && productId == null) {
            return true;
        }
        if (o.getItems() == null || o.getItems().isEmpty()) {
            return false;
        }
        return o.getItems().stream().anyMatch(oi -> lineMatchesFilter(oi, categoryId, productId));
    }

    private Map<String, Double> buildTrendDailySeries(List<Order> orders,
                                                      List<SalesLog> salesLogs,
                                                      String metric,
                                                      int trendDays,
                                                      Long categoryId,
                                                      Long productId) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays((long) trendDays - 1);
        DateTimeFormatter dayFmt = DateTimeFormatter.ISO_LOCAL_DATE;
        LinkedHashMap<String, Double> daily = new LinkedHashMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            daily.put(d.format(dayFmt), 0.0);
        }

        switch (metric) {
            case "gmv" -> {
                for (Order o : orders) {
                    if (!isPaidPlus(o) || o.getOrderDate() == null) {
                        continue;
                    }
                    LocalDate od = o.getOrderDate().toLocalDate();
                    if (od.isBefore(start) || od.isAfter(end)) {
                        continue;
                    }
                    String key = od.format(dayFmt);
                    if (!daily.containsKey(key)) {
                        continue;
                    }
                    if (categoryId == null && productId == null) {
                        daily.merge(key, o.getTotalAmount() == null ? 0D : o.getTotalAmount().doubleValue(), Double::sum);
                    } else {
                        if (!orderMatchesLineFilter(o, categoryId, productId)) {
                            continue;
                        }
                        double sum = 0D;
                        for (OrderItem oi : o.getItems()) {
                            if (!lineMatchesFilter(oi, categoryId, productId)) {
                                continue;
                            }
                            BigDecimal line = oi.getPriceAtOrder() == null ? BigDecimal.ZERO
                                    : oi.getPriceAtOrder().multiply(BigDecimal.valueOf(oi.getQuantity() == null ? 0 : oi.getQuantity()));
                            sum += line.doubleValue();
                        }
                        daily.merge(key, sum, Double::sum);
                    }
                }
            }
            case "orders" -> {
                for (Order o : orders) {
                    if (!isPaidPlus(o) || o.getOrderDate() == null) {
                        continue;
                    }
                    LocalDate od = o.getOrderDate().toLocalDate();
                    if (od.isBefore(start) || od.isAfter(end)) {
                        continue;
                    }
                    if (!orderMatchesLineFilter(o, categoryId, productId)) {
                        continue;
                    }
                    String key = od.format(dayFmt);
                    if (daily.containsKey(key)) {
                        daily.merge(key, 1.0, Double::sum);
                    }
                }
            }
            case "units" -> {
                for (Order o : orders) {
                    if (!isPaidPlus(o) || o.getOrderDate() == null || o.getItems() == null) {
                        continue;
                    }
                    LocalDate od = o.getOrderDate().toLocalDate();
                    if (od.isBefore(start) || od.isAfter(end)) {
                        continue;
                    }
                    String key = od.format(dayFmt);
                    if (!daily.containsKey(key)) {
                        continue;
                    }
                    int qty = o.getItems().stream()
                            .filter(oi -> lineMatchesFilter(oi, categoryId, productId))
                            .mapToInt(oi -> oi.getQuantity() == null ? 0 : oi.getQuantity())
                            .sum();
                    if (qty > 0) {
                        daily.merge(key, (double) qty, Double::sum);
                    }
                }
            }
            case "conversion" -> {
                Map<Long, Long> categoryOfProduct = new HashMap<>();
                for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                    long views = 0L;
                    long purchases = 0L;
                    for (SalesLog log : salesLogs) {
                        if (log == null || log.getLogTime() == null || log.getActionType() == null) {
                            continue;
                        }
                        if (!log.getLogTime().toLocalDate().equals(d)) {
                            continue;
                        }
                        String at = log.getActionType();
                        if (!"VIEW".equalsIgnoreCase(at) && !"PURCHASE".equalsIgnoreCase(at)) {
                            continue;
                        }
                        if (!logMatchesProductFilter(log, categoryId, productId, categoryOfProduct)) {
                            continue;
                        }
                        if ("VIEW".equalsIgnoreCase(at)) {
                            views++;
                        } else {
                            purchases++;
                        }
                    }
                    String key = d.format(dayFmt);
                    double rate = views == 0 ? 0D : (purchases * 100.0 / views);
                    daily.put(key, rate);
                }
            }
            default -> {
            }
        }
        return daily;
    }

    private boolean logMatchesProductFilter(SalesLog log,
                                            Long categoryId,
                                            Long productId,
                                            Map<Long, Long> categoryOfProduct) {
        if (productId != null) {
            return productId.equals(log.getProductId());
        }
        if (categoryId == null) {
            return true;
        }
        if (log.getProductId() == null) {
            return false;
        }
        Long cid = categoryOfProduct.computeIfAbsent(log.getProductId(), id ->
                productRepository.findById(id)
                        .map(p -> p.getCategory() == null ? null : p.getCategory().getId())
                        .orElse(null));
        return categoryId.equals(cid);
    }

    private Map<String, Double> applyTrendGranularity(Map<String, Double> daily, String gran, String metric) {
        if (gran == null || "DAY".equals(gran)) {
            return new LinkedHashMap<>(daily);
        }
        boolean useAverage = "conversion".equalsIgnoreCase(metric);
        if ("WEEK".equals(gran)) {
            Map<String, List<Double>> bucketVals = new LinkedHashMap<>();
            Map<String, String> bucketLabel = new LinkedHashMap<>();
            for (Map.Entry<String, Double> e : daily.entrySet()) {
                LocalDate d = LocalDate.parse(e.getKey());
                LocalDate mon = d.with(DayOfWeek.MONDAY);
                String gk = mon.toString();
                bucketVals.computeIfAbsent(gk, k -> new ArrayList<>()).add(e.getValue());
                bucketLabel.putIfAbsent(gk, mon.format(DateTimeFormatter.ISO_LOCAL_DATE) + " 当周");
            }
            return bucketVals.keySet().stream()
                    .sorted()
                    .collect(Collectors.toMap(
                            k -> bucketLabel.getOrDefault(k, k),
                            k -> {
                                List<Double> vals = bucketVals.get(k);
                                if (useAverage) {
                                    return vals.stream().mapToDouble(Double::doubleValue).average().orElse(0D);
                                }
                                return vals.stream().mapToDouble(Double::doubleValue).sum();
                            },
                            (a, b) -> a,
                            LinkedHashMap::new));
        }
        if ("MONTH".equals(gran)) {
            Map<String, List<Double>> bucketVals = new LinkedHashMap<>();
            Map<String, String> bucketLabel = new LinkedHashMap<>();
            for (Map.Entry<String, Double> e : daily.entrySet()) {
                LocalDate d = LocalDate.parse(e.getKey());
                YearMonth ym = YearMonth.from(d);
                String gk = ym.toString();
                bucketVals.computeIfAbsent(gk, k -> new ArrayList<>()).add(e.getValue());
                bucketLabel.putIfAbsent(gk, ym.getYear() + "年" + String.format("%02d", ym.getMonthValue()) + "月");
            }
            return bucketVals.keySet().stream()
                    .sorted()
                    .collect(Collectors.toMap(
                            k -> bucketLabel.getOrDefault(k, k),
                            k -> {
                                List<Double> vals = bucketVals.get(k);
                                if (useAverage) {
                                    return vals.stream().mapToDouble(Double::doubleValue).average().orElse(0D);
                                }
                                return vals.stream().mapToDouble(Double::doubleValue).sum();
                            },
                            (a, b) -> a,
                            LinkedHashMap::new));
        }
        return new LinkedHashMap<>(daily);
    }
}
