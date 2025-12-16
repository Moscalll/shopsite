package com.example.shopsite.service;

import com.example.shopsite.model.CartItem;
import com.example.shopsite.model.User;
import java.util.List;

public interface CartService {
    
    // 添加商品到购物车
    CartItem addToCart(Long productId, Integer quantity, User user);
    
    // 获取用户的购物车列表
    List<CartItem> getCartItems(User user);
    
    // 更新购物车项数量
    CartItem updateCartItemQuantity(Long cartItemId, Integer quantity, User user);
    
    // 从购物车移除商品
    void removeFromCart(Long cartItemId, User user);
    
    // 清空购物车
    void clearCart(User user);
    
    // 获取购物车总数量
    Integer getCartItemCount(User user);
}















