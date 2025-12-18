package com.example.shopsite.service;

import com.example.shopsite.model.Category;
import com.example.shopsite.model.Product;
import com.example.shopsite.model.User;
import com.example.shopsite.dto.ProductCreationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface ProductService { 

    // 1. 创建/上架新商品 (商户权限)
    Product createProduct(Product product, Long categoryId, User merchant);

    // 2. 更新商品 (商户权限)
    Product updateProduct(Long productId, Product updatedDetails, User merchant);

    // 3. 按ID查找商品 (前台详情页)
    Optional<Product> findAvailableProductById(Long id);

    // 4. 查找商户自己的所有商品 (商户后台)
    List<Product> findProductsByMerchant(User merchant);

    // 5. 查询所有可售商品 (前台列表)
    List<Product> findAllAvailableProducts();
    
    // 6. (如果您需要保留 DTO 版本的 createProduct)
    Product createProduct(ProductCreationRequest request, String username);
    
    // 7. 查询新品（首页展示）
    List<Product> findNewArrivals(int limit);
    
    // 8. 查询排行榜（按销量排序）
    List<Product> findTopSellingProducts(int limit);
    
    // 9. 按分类查询商品（分页）
    Page<Product> findProductsByCategory(Long categoryId, Pageable pageable);
    
    // 10. 按分类查询商品（不分页，用于首页分类展示）
    List<Product> findProductsByCategory(Long categoryId, int limit);
    
    // 11. 搜索商品（按关键词搜索名称和描述）
    List<Product> searchProducts(String keyword);

    List<Product> findAllProducts();

}