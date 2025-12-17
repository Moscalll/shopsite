package com.example.shopsite.service.impl;

import com.example.shopsite.model.Favorite;
import com.example.shopsite.model.Product;
import com.example.shopsite.model.User;
import com.example.shopsite.repository.FavoriteRepository;
import com.example.shopsite.repository.ProductRepository;
import com.example.shopsite.service.FavoriteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;

    public FavoriteServiceImpl(FavoriteRepository favoriteRepository, ProductRepository productRepository) {
        this.favoriteRepository = favoriteRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public Favorite addFavorite(Long productId, User user) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));

        // 检查是否已收藏
        if (favoriteRepository.existsByUserAndProduct(user, product)) {
            throw new IllegalArgumentException("该商品已在收藏夹中");
        }

        Favorite favorite = Favorite.builder()
                .user(user)
                .product(product)
                .build();

        return favoriteRepository.save(favorite);
    }

    @Override
    @Transactional
    public void removeFavorite(Long productId, User user) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));

        favoriteRepository.deleteByUserAndProduct(user, product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Favorite> getUserFavorites(User user) {
        return favoriteRepository.findByUser(user);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFavorite(Long productId, User user) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));

        return favoriteRepository.existsByUserAndProduct(user, product);
    }
}
















