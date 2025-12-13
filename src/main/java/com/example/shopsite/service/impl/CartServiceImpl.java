package com.example.shopsite.service.impl;

import com.example.shopsite.model.CartItem;
import com.example.shopsite.model.Product;
import com.example.shopsite.model.User;
import com.example.shopsite.repository.CartItemRepository;
import com.example.shopsite.repository.ProductRepository;
import com.example.shopsite.service.CartService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartServiceImpl(CartItemRepository cartItemRepository, ProductRepository productRepository) {
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public CartItem addToCart(Long productId, Integer quantity, User user) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));

        if (!product.getIsAvailable() || product.getStock() < quantity) {
            throw new IllegalArgumentException("商品库存不足或已下架");
        }

        // 检查购物车中是否已有该商品
        CartItem existingItem = cartItemRepository.findByUserAndProduct(user, product)
                .orElse(null);

        if (existingItem != null) {
            // 更新数量
            int newQuantity = existingItem.getQuantity() + quantity;
            if (newQuantity > product.getStock()) {
                throw new IllegalArgumentException("购物车中该商品数量已超过库存");
            }
            existingItem.setQuantity(newQuantity);
            return cartItemRepository.save(existingItem);
        } else {
            // 创建新的购物车项
            CartItem cartItem = CartItem.builder()
                    .user(user)
                    .product(product)
                    .quantity(quantity)
                    .build();
            return cartItemRepository.save(cartItem);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartItem> getCartItems(User user) {
        return cartItemRepository.findByUser(user);
    }

    @Override
    @Transactional
    public CartItem updateCartItemQuantity(Long cartItemId, Integer quantity, User user) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("购物车项不存在"));

        if (!cartItem.getUser().getId().equals(user.getId())) {
            throw new SecurityException("无权修改他人的购物车");
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException("数量必须大于0");
        }

        if (quantity > cartItem.getProduct().getStock()) {
            throw new IllegalArgumentException("数量超过商品库存");
        }

        cartItem.setQuantity(quantity);
        return cartItemRepository.save(cartItem);
    }

    @Override
    @Transactional
    public void removeFromCart(Long cartItemId, User user) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("购物车项不存在"));

        if (!cartItem.getUser().getId().equals(user.getId())) {
            throw new SecurityException("无权删除他人的购物车项");
        }

        cartItemRepository.delete(cartItem);
    }

    @Override
    @Transactional
    public void clearCart(User user) {
        cartItemRepository.deleteByUser(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getCartItemCount(User user) {
        List<CartItem> items = cartItemRepository.findByUser(user);
        return items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }
}

