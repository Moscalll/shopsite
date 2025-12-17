package com.example.shopsite.controller.user;

import com.example.shopsite.model.Product;
import com.example.shopsite.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class NewArrivalsController {

    private final ProductService productService;

    public NewArrivalsController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * GET /new-arrivals - 新品列表
     */
    @GetMapping("/new-arrivals")
    public String newArrivals(Model model) {
        List<Product> newArrivals = productService.findNewArrivals(50); // 显示前50个新品
        model.addAttribute("products", newArrivals);
        model.addAttribute("pageTitle", "新品上市");
        return "user/new_arrivals";
    }
}














