package com.example.shopsite.repository;

import com.example.shopsite.model.Product;
import com.example.shopsite.model.User; // 导入 User 实体
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    Page<Product> findByIsAvailableTrueAndStockGreaterThan(Integer stock, Pageable pageable);

    Page<Product> findByMerchantOrderByIdDesc(User merchant, Pageable pageable);

    // 根据分类和是否上架查找商品 (用于前台展示)
    List<Product> findByCategory_IdAndIsAvailableTrue(Long categoryId);

    // 新品查询：按ID降序（假设ID越大越新），只查询已上架且有库存的商品
    List<Product> findByIsAvailableTrueAndStockGreaterThanOrderByIdDesc(Integer stock, Pageable pageable);

    // 分类查询：分页支持
    Page<Product> findByCategory_IdAndIsAvailableTrueAndStockGreaterThan(Long categoryId, Integer stock,
            Pageable pageable);

    // 搜索商品：按名称或描述模糊查询
    @Query("SELECT p FROM Product p WHERE p.isAvailable = true AND p.stock > 0 AND " +
            "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Product> searchAvailableProducts(@Param("keyword") String keyword);

    // 排行榜查询：需要从OrderItem统计销量，这里先提供基础查询，实际统计在Service层实现
    // 注意：Product实体需要添加createdAt字段才能按时间排序，或者使用ID排序

    // 统计特定商户的商品数量
    long countByMerchant(User merchant);

    // ProductRepository.java 追加
    Optional<Product> findByIdAndIsAvailableTrueAndStockGreaterThan(Long id, Integer stock);
}