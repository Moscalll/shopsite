package com.example.shopsite.repository;

import com.example.shopsite.model.Order;
import com.example.shopsite.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    
    // 查询特定用户的所有订单 (供客户使用)
    List<Order> findByUser(User user);
    
    // 查询包含特定商户商品的订单
    @Query("SELECT DISTINCT o FROM Order o " +
           "JOIN o.items oi " +
           "WHERE oi.product.merchant = :merchant")
    List<Order> findOrdersByMerchant(@Param("merchant") User merchant);
}