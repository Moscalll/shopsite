package com.example.shopsite.repository;

import com.example.shopsite.model.Favorite;
import com.example.shopsite.model.Product;
import com.example.shopsite.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    
    // 查询用户的所有收藏
    List<Favorite> findByUser(User user);
    
    // 查询用户是否已收藏特定商品
    Optional<Favorite> findByUserAndProduct(User user, Product product);
    
    // 检查用户是否已收藏商品
    boolean existsByUserAndProduct(User user, Product product);
    
    // 删除用户的特定收藏
    void deleteByUserAndProduct(User user, Product product);
}




