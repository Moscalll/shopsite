package com.example.shopsite.repository;

import com.example.shopsite.model.Product;
import com.example.shopsite.model.User; // 导入 User 实体
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    
    // 1. 客户和访客使用：查询所有已上架的商品 (isAvailable = true)
    List<Product> findByIsAvailableTrue();

    // 2. 商家管理使用：查询特定商家 (merchant) 的所有商品（无论是否上架）
    List<Product> findByMerchant(User merchant);
    
    // 3. 商家管理使用：查询特定商家 (merchant) 下已上架的商品
    List<Product> findByMerchantAndIsAvailableTrue(User merchant);

    // 4. 精确查找（优化查询）
    Optional<Product> findByIdAndIsAvailableTrue(Long id);

    // 🚨 新增：根据 JPA 命名规范，查找 isAvailable 为 true 且 stock 大于指定值的商品
    List<Product> findByIsAvailableTrueAndStockGreaterThan(Integer stock);
}