package com.example.shopsite.controller.user;

import com.example.shopsite.model.Product;
import com.example.shopsite.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class TopSellingController {

    private final ProductService productService;

    public TopSellingController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * GET /top-selling - 排行榜列表
     */
    @GetMapping("/top-selling")
    public String topSelling(Model model) {
        List<Product> topSelling = productService.findTopSellingProducts(50); // 显示前50个
        model.addAttribute("products", topSelling);
        model.addAttribute("pageTitle", "热销排行榜");
        return "user/top_selling";
    }
}

