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
        // 横幅数据（可以后续从数据库或配置文件读取）
        java.util.List<java.util.Map<String, String>> banners = new java.util.ArrayList<>();
        java.util.Map<String, String> banner1 = new java.util.HashMap<>();
        banner1.put("imageUrl", "https://images.unsplash.com/photo-1441986300917-64674bd600d8?w=1200&h=400&fit=crop");
        banner1.put("title", "简约生活，从这里开始");
        banner1.put("description", "发现MUJI风格的生活用品");
        banners.add(banner1);
        
        java.util.Map<String, String> banner2 = new java.util.HashMap<>();
        banner2.put("imageUrl", "https://images.unsplash.com/photo-1556228578-0d85b1a4d571?w=1200&h=400&fit=crop");
        banner2.put("title", "品质生活，自然选择");
        banner2.put("description", "精选优质商品，打造舒适生活");
        banners.add(banner2);
        
        java.util.Map<String, String> banner3 = new java.util.HashMap<>();
        banner3.put("imageUrl", "https://images.unsplash.com/photo-1556912172-45b7abe8b7e1?w=1200&h=400&fit=crop");
        banner3.put("title", "新品上市，限时优惠");
        banner3.put("description", "全场新品7折起，活动截止至本周末");
        banners.add(banner3);
        
        model.addAttribute("banners", banners);

        // 所有分类
        List<Category> categories = categoryService.findAllCategories();
        model.addAttribute("categories", categories);

        // 新品（取前8个）
        List<Product> newArrivals = productService.findNewArrivals(8);
        model.addAttribute("newArrivals", newArrivals);

        // 排行榜（取前8个）
        List<Product> topSelling = productService.findTopSellingProducts(8);
        model.addAttribute("topSelling", topSelling);

        // 每个分类的商品（取前4个），使用Map存储
        java.util.Map<Long, List<Product>> categoryProductsMap = new java.util.HashMap<>();
        for (Category category : categories) {
            List<Product> categoryProducts = productService.findProductsByCategory(category.getId(), 4);
            categoryProductsMap.put(category.getId(), categoryProducts);
        }
        model.addAttribute("categoryProductsMap", categoryProductsMap);

        model.addAttribute("pageTitle", "首页");
        return "user/home";
    }
}

