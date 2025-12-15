package com.example.shopsite.controller.user;

import com.example.shopsite.model.Product;
import com.example.shopsite.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class SearchController {

    private final ProductService productService;

    public SearchController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * GET /search?q=关键词 - 搜索商品
     */
    @GetMapping("/search")
    public String search(@RequestParam(value = "q", required = false) String keyword, Model model) {
        if (keyword == null || keyword.trim().isEmpty()) {
            model.addAttribute("products", List.<Product>of());
            model.addAttribute("keyword", "");
            model.addAttribute("pageTitle", "搜索结果");
            return "user/search_results";
        }

        List<Product> products = productService.searchProducts(keyword);
        model.addAttribute("products", products);
        model.addAttribute("keyword", keyword);
        model.addAttribute("pageTitle", "搜索结果: " + keyword);

        return "user/search_results";
    }
}













