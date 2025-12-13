package com.example.shopsite.controller;

import com.example.shopsite.model.Product;
import com.example.shopsite.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller // 🚨 仅使用 @Controller
public class ProductViewController {
    
    private final ProductService productService;

    @Autowired
    public ProductViewController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * GET /products - 商品列表视图
     * 对应 templates/layout/products_list.html (硬编码布局)
     */
    @GetMapping("/products") // 🚨 使用 /products 作为视图路由
    public String listProducts(Model model) {
        List<Product> products = productService.findAllAvailableProducts();
        
        model.addAttribute("pageTitle", "所有商品");
        model.addAttribute("products", products); // 将数据模型添加到 Model 中
        
        // 假设您已经按照前面的建议创建了 layout/products_list.html 布局
        return "layout/products_list"; 
    }
    
    // ... 其他视图方法 (如 /product/{id} 详情页)
}