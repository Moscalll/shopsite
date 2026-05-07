package com.example.shopsite.controller.user;

import com.example.shopsite.model.Category;
import com.example.shopsite.model.Product;
import com.example.shopsite.model.RecommendationAlgorithm;
import com.example.shopsite.model.User;
import com.example.shopsite.repository.UserRepository;
import com.example.shopsite.service.CategoryService;
import com.example.shopsite.service.ProductService;
import com.example.shopsite.service.RecommendationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.FileWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class HomeController {

        private final ProductService productService;
        private final CategoryService categoryService;
        private final RecommendationService recommendationService;
        private final UserRepository userRepository;

        public HomeController(ProductService productService,
                              CategoryService categoryService,
                              RecommendationService recommendationService,
                              UserRepository userRepository) {
                this.productService = productService;
                this.categoryService = categoryService;
                this.recommendationService = recommendationService;
                this.userRepository = userRepository;
        }

        /**
         * GET / - 首页
         * 展示横幅、分类、新品、排行榜
         */
        @GetMapping("/")
        public String home(Model model, Authentication authentication, HttpServletRequest request) {
                String requestUri = request != null ? request.getRequestURI() : null;
                String queryString = request != null ? request.getQueryString() : null;
                String currentUrl = (requestUri == null ? "/" : requestUri) + (queryString != null && !queryString.isBlank() ? "?" + queryString : "");
                model.addAttribute("currentUrl", currentUrl);
                // #region agent log
                debugLog("pre-fix", "H1", "HomeController.java:home", "computed currentUrl", Map.of(
                        "requestUri", requestUri,
                        "hasQueryString", queryString != null && !queryString.isBlank(),
                        "currentUrl", currentUrl
                ));
                // #endregion
                // 横幅数据
                java.util.List<java.util.Map<String, String>> banners = new java.util.ArrayList<>();
                java.util.Map<String, String> banner1 = new java.util.HashMap<>();
                banner1.put("imageUrl",
                                "/images/banners/kitchen.jpg");
                banner1.put("title", "简约生活，从这里开始");
                banner1.put("description", "发现KIKA风格的生活用品");
                banners.add(banner1);

                java.util.Map<String, String> banner2 = new java.util.HashMap<>();
                banner2.put("imageUrl",
                                "/images/banners/wind.jpg");
                banner2.put("title", "品质生活，自然选择");
                banner2.put("description", "精选优质商品，打造舒适生活");
                banners.add(banner2);

                java.util.Map<String, String> banner3 = new java.util.HashMap<>();
                banner3.put("imageUrl",
                                "/images/banners/breakfast.jpg");
                banner3.put("title", "新品上市，限时优惠");
                banner3.put("description", "全场新品7折起，活动截止至本周末");
                banners.add(banner3);

                model.addAttribute("banners", banners);

                // 品类图映射（使用 /images/categories 下的本地图）
                java.util.Map<String, String> categoryImageMap = new java.util.HashMap<>();
                categoryImageMap.put("服装", "/images/categories/cloth-style.jpg");
                categoryImageMap.put("家居用品", "/images/categories/household-style.png");
                categoryImageMap.put("文具", "/images/categories/stationery-style.png");
                categoryImageMap.put("食品", "/images/categories/food-style.png");
                categoryImageMap.put("美妆护理", "/images/categories/beauty-style.png");
                categoryImageMap.put("收纳整理", "/images/categories/storage-style.png");
                categoryImageMap.put("旅行用品", "/images/categories/travel-style.png");
                categoryImageMap.put("厨房用品", "/images/categories/kitchen-style.png");
                model.addAttribute("categoryImageMap", categoryImageMap);

                // 所有分类
                List<Category> categories = categoryService.findAllCategories();
                // 分类列表已获取
                model.addAttribute("categories", categories);

                // 首页数据兜底过滤，确保只显示上架且有库存
                List<Product> newArrivals = productService.findNewArrivals(8).stream()
                                .filter(p -> Boolean.TRUE.equals(p.getIsAvailable()) && p.getStock() != null
                                                && p.getStock() > 0)
                                .toList();
                model.addAttribute("newArrivals", newArrivals);

                List<Product> topSelling = productService.findTopSellingProducts(8).stream()
                                .filter(p -> Boolean.TRUE.equals(p.getIsAvailable()) && p.getStock() != null
                                                && p.getStock() > 0)
                                .toList();
                model.addAttribute("topSelling", topSelling);

                java.util.Map<Long, List<Product>> categoryProductsMap = new java.util.HashMap<>();
                for (Category category : categories) {
                        List<Product> categoryProducts = productService.findProductsByCategory(category.getId(), 4)
                                        .stream()
                                        .filter(p -> Boolean.TRUE.equals(p.getIsAvailable()) && p.getStock() != null
                                                        && p.getStock() > 0)
                                        .toList();
                        categoryProductsMap.put(category.getId(), categoryProducts);
                }
                model.addAttribute("categoryProductsMap", categoryProductsMap);

                // 猜你喜欢：固定 2×4=8 个商品
                List<Product> guessYouLike = buildGuessYouLike(categories, authentication);
                model.addAttribute("guessYouLike", guessYouLike);

                model.addAttribute("pageTitle", "首页");
                return "user/home";
        }

        private static void debugLog(String runId, String hypothesisId, String location, String message, Map<String, Object> data) {
                try (FileWriter fw = new FileWriter("debug-a845d9.log", true)) {
                        String json = "{"
                                + "\"sessionId\":\"a845d9\""
                                + ",\"runId\":\"" + escape(runId) + "\""
                                + ",\"hypothesisId\":\"" + escape(hypothesisId) + "\""
                                + ",\"location\":\"" + escape(location) + "\""
                                + ",\"message\":\"" + escape(message) + "\""
                                + ",\"data\":" + toJson(data)
                                + ",\"timestamp\":" + Instant.now().toEpochMilli()
                                + "}";
                        fw.write(json);
                        fw.write("\n");
                } catch (Exception ignored) {
                }
        }

        private static String toJson(Object v) {
                if (v == null) return "null";
                if (v instanceof String) return "\"" + escape((String) v) + "\"";
                if (v instanceof Number || v instanceof Boolean) return String.valueOf(v);
                if (v instanceof Map<?, ?> m) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("{");
                        boolean first = true;
                        for (Map.Entry<?, ?> e : m.entrySet()) {
                                if (!first) sb.append(",");
                                first = false;
                                sb.append(toJson(String.valueOf(e.getKey()))).append(":").append(toJson(e.getValue()));
                        }
                        sb.append("}");
                        return sb.toString();
                }
                return "\"" + escape(String.valueOf(v)) + "\"";
        }

        private static String escape(String s) {
                return s.replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\r", "\\r")
                        .replace("\n", "\\n")
                        .replace("\t", "\\t");
        }

        private List<Product> buildGuessYouLike(List<Category> categories, Authentication authentication) {
                // 1) 登录用户：优先读取推荐结果
                if (authentication != null && authentication.isAuthenticated()) {
                        Optional<User> userOpt = userRepository.findByUsername(authentication.getName());
                        if (userOpt.isPresent()) {
                                User user = userOpt.get();
                                List<Long> ids = recommendationService.getRecommendations(user, RecommendationAlgorithm.ITEM_CF, 24);
                                if (ids.isEmpty()) {
                                        ids = recommendationService.getRecommendations(user, RecommendationAlgorithm.CO_PURCHASE, 24);
                                }
                                if (ids.isEmpty()) {
                                        ids = recommendationService.getRecommendations(user, RecommendationAlgorithm.POPULAR, 24);
                                }
                                if (!ids.isEmpty()) {
                                        // 用 allProducts 做一次性取数（避免给 ProductService 加新方法），再按推荐顺序排序
                                        List<Product> all = productService.findAllProducts();
                                        Map<Long, Product> byId = all.stream()
                                                .filter(p -> p.getId() != null)
                                                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));

                                        List<Product> products = new ArrayList<>();
                                        for (Long pid : ids) {
                                                Product p = byId.get(pid);
                                                if (p == null) continue;
                                                if (!Boolean.TRUE.equals(p.getIsAvailable()) || p.getStock() == null || p.getStock() <= 0) continue;
                                                products.add(p);
                                        }
                                        if (!products.isEmpty()) {
                                                return products.stream().limit(8).collect(Collectors.toList());
                                        }
                                }
                        }
                }

                // 2) 兜底：每个类目取 3 个（上架且有库存）
                List<Product> fallback = new ArrayList<>();
                if (categories != null) {
                        for (Category c : categories) {
                                if (c == null || c.getId() == null) continue;
                                List<Product> picks = productService.findProductsByCategory(c.getId(), 3).stream()
                                        .filter(p -> Boolean.TRUE.equals(p.getIsAvailable()) && p.getStock() != null && p.getStock() > 0)
                                        .limit(3)
                                        .collect(Collectors.toList());
                                fallback.addAll(picks);
                        }
                }
                return fallback.stream().limit(8).collect(Collectors.toList());
        }
}
