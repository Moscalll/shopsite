package com.example.shopsite.repository;

import com.example.shopsite.model.OrderItem;
import com.example.shopsite.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

       // 统计商品的销量（按商品ID分组，计算总数量）
       @Query("SELECT oi.product, SUM(oi.quantity) as totalSales " +
                     "FROM OrderItem oi " +
                     "WHERE oi.order.status IN (com.example.shopsite.model.OrderStatus.PROCESSING, " +
                     "com.example.shopsite.model.OrderStatus.SHIPPED, " +
                     "com.example.shopsite.model.OrderStatus.DELIVERED, " +
                     "com.example.shopsite.model.OrderStatus.COMPLETED) " +
                     "AND oi.product.isAvailable = true " +
                     "AND oi.product.stock > 0 " +
                     "GROUP BY oi.product " +
                     "ORDER BY totalSales DESC")
       List<Object[]> findTopSellingProducts();

       // 统计特定商品的销量
       @Query("SELECT COALESCE(SUM(oi.quantity), 0) " +
                     "FROM OrderItem oi " +
                     "WHERE oi.product = :product " +
                     "AND oi.order.status IN (com.example.shopsite.model.OrderStatus.PROCESSING, " +
                     "com.example.shopsite.model.OrderStatus.SHIPPED, " +
                     "com.example.shopsite.model.OrderStatus.DELIVERED, " +
                     "com.example.shopsite.model.OrderStatus.COMPLETED)")
       Long getTotalSalesByProduct(@Param("product") Product product);

       // 根据商品查询所有订单项
       List<OrderItem> findByProduct(Product product);
}