package com.example.shopsite.repository;

import com.example.shopsite.model.CartItem;
import com.example.shopsite.model.Product;
import com.example.shopsite.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    
    // 查询用户的所有购物车项
    List<CartItem> findByUser(User user);
    
    // 查询用户购物车中的特定商品
    Optional<CartItem> findByUserAndProduct(User user, Product product);
    
    // 删除用户的所有购物车项
    void deleteByUser(User user);
    
    // 删除用户的特定购物车项
    void deleteByUserAndProduct(User user, Product product);
}













