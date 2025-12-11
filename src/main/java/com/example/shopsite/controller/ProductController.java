package com.example.shopsite.controller;

import com.example.shopsite.model.Product;
import com.example.shopsite.repository.ProductRepository;
import org.springframework.http.ResponseEntity;

import com.example.shopsite.dto.ProductCreationRequest; // 导入 DTO
import com.example.shopsite.service.ProductService; // 导入 Service
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication; // 导入 Authentication
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;
    private final ProductService productService; // 🚨 注入 Service

    public ProductController(ProductRepository productRepository, ProductService productService) {
        this.productRepository = productRepository;
        this.productService = productService;
    }

    /**
     * GET /api/products
     * 所有人可见，用于查看所有已上架商品
     */
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        // 🚨 实际业务中应只返回 isAvailable = true 的商品
        List<Product> products = productRepository.findAll();
        return ResponseEntity.ok(products);
    }

    /**
     * POST /api/products
     * 只有认证用户才能访问 (后续会限制为 MERCHANT/ADMIN)
     */
    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @RequestBody ProductCreationRequest request) {
        // 获取当前认证的用户名（从JWT过滤器加载到SecurityContext中）
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        Product newProduct = productService.createProduct(request, username);
        
        return new ResponseEntity<>(newProduct, HttpStatus.CREATED);
    }
    
    // TODO: PUT /api/products/{id} (修改商品)
    // TODO: DELETE /api/products/{id} (删除/下架商品)
}