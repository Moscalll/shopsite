package com.example.shopsite.controller.user;

import com.example.shopsite.model.Category;
import com.example.shopsite.model.Product;
import com.example.shopsite.service.CategoryService;
import com.example.shopsite.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

        private final ProductService productService;
        private final CategoryService categoryService;

        public HomeController(ProductService productService, CategoryService categoryService) {
                this.productService = productService;
                this.categoryService = categoryService;
        }

        /**
         * GET / - 首页
         * 展示横幅、分类、新品、排行榜
         */
        @GetMapping("/")
        public String home(Model model) {
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

                model.addAttribute("pageTitle", "首页");
                return "user/home";
        }
}
