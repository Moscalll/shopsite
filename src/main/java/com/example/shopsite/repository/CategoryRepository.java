package com.example.shopsite.repository;

import com.example.shopsite.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    // 根据名称查找分类，确保名称唯一性检查
    Optional<Category> findByName(String name);
}