package com.example.shopsite.service.impl;

import com.example.shopsite.dto.ProductCreationRequest;
import com.example.shopsite.model.Product;
import com.example.shopsite.model.User;
import com.example.shopsite.repository.ProductRepository;
import com.example.shopsite.repository.UserRepository;
import com.example.shopsite.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public ProductServiceImpl(ProductRepository productRepository, UserRepository userRepository) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public Product createProduct(ProductCreationRequest request, String username) {
        // 1. 根据当前登录用户名查找商家
        User merchant = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("商家用户不存在"));

        // 2. 构建商品实体
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .isAvailable(true) // 默认上架
                .merchant(merchant)
                .build();

        // 3. 保存并返回
        return productRepository.save(product);
    }

    @Override // 🚨 新增方法实现
    public List<Product> findAllAvailableProducts() {
        // 假设我们只显示 isAvailable = true 且 stock > 0 的商品
        return productRepository.findByIsAvailableTrueAndStockGreaterThan(0);
    }
}