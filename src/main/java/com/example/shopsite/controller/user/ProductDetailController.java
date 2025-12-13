package com.example.shopsite.controller.user;

import com.example.shopsite.model.Product;
import com.example.shopsite.model.User;
import com.example.shopsite.repository.UserRepository;
import com.example.shopsite.service.FavoriteService;
import com.example.shopsite.service.ProductService;
import com.example.shopsite.service.SalesLogService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@Controller
public class ProductDetailController {

    private final ProductService productService;
    private final FavoriteService favoriteService;
    private final UserRepository userRepository;
    private final SalesLogService salesLogService;

    public ProductDetailController(ProductService productService, FavoriteService favoriteService,
                                   UserRepository userRepository, SalesLogService salesLogService) {
        this.productService = productService;
        this.favoriteService = favoriteService;
        this.userRepository = userRepository;
        this.salesLogService = salesLogService;
    }

    /**
     * GET /product/{id} - 商品详情页
     */
    @GetMapping("/product/{id}")
    public String productDetail(@PathVariable Long id, Model model, Authentication authentication) {
        Optional<Product> productOpt = productService.findAvailableProductById(id);
        
        if (productOpt.isEmpty()) {
            model.addAttribute("error", "商品不存在或已下架");
            return "error/404";
        }

        Product product = productOpt.get();
        model.addAttribute("product", product);
        model.addAttribute("pageTitle", product.getName());

        // 如果用户已登录，检查是否已收藏，并记录浏览日志
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                boolean isFavorite = favoriteService.isFavorite(id, user);
                model.addAttribute("isFavorite", isFavorite);
                // 记录浏览日志
                salesLogService.logView(id, user);
            }
        } else {
            model.addAttribute("isFavorite", false);
        }

        return "user/product_detail";
    }
}

