package com.example.shopsite.config;

import com.example.shopsite.model.Category;
import com.example.shopsite.repository.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

import java.util.Arrays;
import java.util.List;

@Component
@Profile("init-data") // 仅在 init-data profile 下执行，避免默认启动自动插入
@Order(1) 
public class CategoryInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    public CategoryInitializer(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) {
        // 如果数据库中已有分类，跳过初始化
        if (categoryRepository.count() > 0) {
            System.out.println("分类数据已存在，跳过初始化");
            return;
        }

        // MUJI风格的商品分类
        List<Category> categories = Arrays.asList(
            Category.builder()
                .name("服装")
                .description("简约舒适的日常服装")
                .imageUrl("https://images.unsplash.com/photo-1441986300917-64674bd600d8?w=300&h=200&fit=crop")
                .build(),
            Category.builder()
                .name("家居用品")
                .description("打造舒适家居生活的日用品")
                .imageUrl("https://images.unsplash.com/photo-1556912172-45b7abe8b7e1?w=300&h=200&fit=crop")
                .build(),
            Category.builder()
                .name("文具")
                .description("简约实用的办公文具")
                .imageUrl("https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=300&h=200&fit=crop")
                .build(),
            Category.builder()
                .name("食品")
                .description("健康自然的食品和零食")
                .imageUrl("https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=300&h=200&fit=crop")
                .build(),
            Category.builder()
                .name("美妆护理")
                .description("温和无添加的护肤美妆产品")
                .imageUrl("https://images.unsplash.com/photo-1556229010-6c3f2c9ca5f8?w=300&h=200&fit=crop")
                .build(),
            Category.builder()
                .name("收纳整理")
                .description("让生活更有序的收纳用品")
                .imageUrl("https://images.unsplash.com/photo-1556228578-0d85b1a4d571?w=300&h=200&fit=crop")
                .build(),
            Category.builder()
                .name("旅行用品")
                .description("轻便实用的旅行必备品")
                .imageUrl("https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=300&h=200&fit=crop")
                .build(),
            Category.builder()
                .name("厨房用品")
                .description("简约实用的厨房用具")
                .imageUrl("https://images.unsplash.com/photo-1556911220-bff31c812dba?w=300&h=200&fit=crop")
                .build()
        );

        categoryRepository.saveAll(categories);
        System.out.println("已初始化 " + categories.size() + " 个商品分类");
    }
}












