package com.example.shopsite.repository;

import com.example.shopsite.model.Order;
import com.example.shopsite.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    
    // 查询特定用户的所有订单 (供客户使用)
    List<Order> findByUser(User user);
}