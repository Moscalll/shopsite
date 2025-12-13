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
import org.springframework.stereotype.Controller;
import java.util.List;
import org.springframework.ui.Model;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.shopsite.model.User;


@RestController
@RequestMapping("/api/products")
public class ProductController {

    

    private final ProductRepository productRepository;
    private final ProductService productService; // 🚨 注入 Service

    public ProductController(ProductRepository productRepository, ProductService productService) {
        this.productRepository = productRepository;
        this.productService = productService;
    }


    // @GetMapping("/") // 首页路由
    // public String listProducts(Model model) { // 🚨 返回 String，接受 Model
    //     List<Product> products = productService.findAllAvailableProducts();
        
    //     model.addAttribute("pageTitle", "所有商品");
    //     model.addAttribute("products", products); // 将数据模型添加到 Model 中
    //     return "product/list"; // 对应 templates/product/list.html
    // }

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
     * POST /api/products (商户创建新商品)
     * URL 修正为 /api/products，权限由 SecurityConfig 限制为 ROLE_MERCHANT
     * 使用 @AuthenticationPrincipal 注入当前商户 User 对象。
     */
    @PostMapping
    public ResponseEntity<Product> createProduct(
        @Valid @RequestBody Product product, 
        @RequestParam Long categoryId,
        @AuthenticationPrincipal User merchant // 假设 User 实现了 UserDetails
    ) {
        if (merchant == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            // 使用 Service 中 (Product, Long, User) 的方法
            Product savedProduct = productService.createProduct(product, categoryId, merchant);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedProduct);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // TODO: PUT /api/products/{id} (修改商品)
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(
        @PathVariable Long id, 
        @Valid @RequestBody Product productDetails,
        @AuthenticationPrincipal User merchant
    ) {
        if (merchant == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // 调用 Service 中带权限校验的更新方法
            Product updatedProduct = productService.updateProduct(id, productDetails, merchant);
            return ResponseEntity.ok(updatedProduct);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 权限不足
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // TODO: DELETE /api/products/{id} (删除/下架商品)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id, @AuthenticationPrincipal User merchant) {
        // 实际业务中，应实现 Service 方法来根据 ID 和 Merchant ID 进行逻辑删除（下架），此处省略 Service 调用
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}