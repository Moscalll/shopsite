package com.example.shopsite.service;

import com.example.shopsite.model.Favorite;
import com.example.shopsite.model.User;
import java.util.List;

public interface FavoriteService {
    
    // 添加收藏
    Favorite addFavorite(Long productId, User user);
    
    // 取消收藏
    void removeFavorite(Long productId, User user);
    
    // 获取用户的所有收藏
    List<Favorite> getUserFavorites(User user);
    
    // 检查是否已收藏
    boolean isFavorite(Long productId, User user);
}



