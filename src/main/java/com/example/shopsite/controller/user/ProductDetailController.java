package com.example.shopsite.controller.user;

import com.example.shopsite.model.Product;
import com.example.shopsite.model.Order;
import com.example.shopsite.model.OrderItem;
import com.example.shopsite.model.OrderStatus;
import com.example.shopsite.model.User;
import com.example.shopsite.repository.OrderRepository;
import com.example.shopsite.repository.UserRepository;
import com.example.shopsite.service.FavoriteService;
import com.example.shopsite.service.ProductService;
import com.example.shopsite.service.SalesLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class ProductDetailController {

    private final ProductService productService;
    private final FavoriteService favoriteService;
    private final UserRepository userRepository;
    private final SalesLogService salesLogService;
    private final OrderRepository orderRepository;

    public ProductDetailController(ProductService productService, FavoriteService favoriteService,
                                   UserRepository userRepository, SalesLogService salesLogService,
                                   OrderRepository orderRepository) {
        this.productService = productService;
        this.favoriteService = favoriteService;
        this.userRepository = userRepository;
        this.salesLogService = salesLogService;
        this.orderRepository = orderRepository;
    }

    /**
     * GET /product/{id} - 商品详情页
     */
    @GetMapping("/product/{id}")
    public String productDetail(@PathVariable Long id,
                                @RequestParam(required = false) String returnUrl,
                                HttpServletRequest request,
                                Model model,
                                Authentication authentication) {
        Optional<Product> productOpt = productService.findAvailableProductById(id);
        
        if (productOpt.isEmpty()) {
            model.addAttribute("error", "商品不存在或已下架");
            return "error/404";
        }

        Product product = productOpt.get();
        model.addAttribute("product", product);
        model.addAttribute("pageTitle", product.getName());
        model.addAttribute("returnUrl", resolveReturnUrl(request, returnUrl, id));
        model.addAttribute("alsoBought", computeAlsoBoughtOrFallback(product, 3));

        // 记录浏览日志：允许匿名浏览（user 为空），以支持商户/平台侧统计
        User user = null;
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            user = userRepository.findByUsername(username).orElse(null);
        }
        salesLogService.logView(id, user);

        // 收藏状态只对登录用户展示
        if (user != null) {
            boolean isFavorite = favoriteService.isFavorite(id, user);
            model.addAttribute("isFavorite", isFavorite);
        } else {
            model.addAttribute("isFavorite", false);
        }

        return "user/product_detail";
    }

    private List<Product> computeAlsoBoughtOrFallback(Product current, int limit) {
        if (current == null || current.getId() == null || limit <= 0) return List.of();
        Long productId = current.getId();
        List<Order> paid = orderRepository.findAll().stream()
                .filter(ProductDetailController::isPaidPlus)
                .toList();

        Map<Long, Long> cnt = new HashMap<>();
        for (Order o : paid) {
            if (o == null || o.getItems() == null || o.getItems().isEmpty()) continue;
            boolean contains = o.getItems().stream().anyMatch(oi -> oi != null && oi.getProduct() != null && productId.equals(oi.getProduct().getId()));
            if (!contains) continue;
            for (OrderItem oi : o.getItems()) {
                if (oi == null || oi.getProduct() == null || oi.getProduct().getId() == null) continue;
                Long otherId = oi.getProduct().getId();
                if (productId.equals(otherId)) continue;
                long q = oi.getQuantity() == null ? 1L : oi.getQuantity().longValue();
                cnt.put(otherId, cnt.getOrDefault(otherId, 0L) + q);
            }
        }

        List<Long> topIds = cnt.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .limit(limit)
                .toList();

        List<Product> out = new ArrayList<>();
        for (Long pid : topIds) {
            productService.findAvailableProductById(pid).ifPresent(out::add);
        }
        out.sort(Comparator.comparing(p -> {
            int idx = topIds.indexOf(p.getId());
            return idx < 0 ? Integer.MAX_VALUE : idx;
        }));
        if (!out.isEmpty()) {
            return out.size() > limit ? out.subList(0, limit) : out;
        }

        // 兜底：没有同购数据时，取同分类商品占位（最多 3 个）
        Long categoryId = current.getCategory() != null ? current.getCategory().getId() : null;
        if (categoryId == null) return List.of();
        return productService.findProductsByCategory(categoryId, limit + 1).stream()
                .filter(p -> p != null && p.getId() != null && !p.getId().equals(productId))
                .limit(limit)
                .toList();
    }

    private static boolean isPaidPlus(Order o) {
        if (o == null || o.getStatus() == null) return false;
        return o.getStatus() == OrderStatus.PROCESSING
                || o.getStatus() == OrderStatus.SHIPPED
                || o.getStatus() == OrderStatus.DELIVERED
                || o.getStatus() == OrderStatus.COMPLETED;
    }

    /**
     * “上一页”固定回到来源功能页（而非浏览器 history.back）。
     * 规则：优先 returnUrl 参数；其次同域 Referer 路径；最后回首页。
     */
    private static String resolveReturnUrl(HttpServletRequest request, String returnUrl, Long productId) {
        if (returnUrl != null && returnUrl.startsWith("/") && !returnUrl.startsWith("//")) {
            return returnUrl;
        }
        String ref = request.getHeader("Referer");
        if (ref != null && !ref.isBlank()) {
            try {
                URI uri = URI.create(ref);
                String host = uri.getHost();
                if (host != null && host.equalsIgnoreCase(request.getServerName())) {
                    String path = uri.getPath();
                    if (path != null && path.startsWith("/") && !path.startsWith("/product/")) {
                        String q = uri.getRawQuery();
                        return path + (q != null ? "?" + q : "");
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return "/";
    }
}

