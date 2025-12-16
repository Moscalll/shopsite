package com.example.shopsite.service.impl;

import com.example.shopsite.dto.ProductCreationRequest;
import com.example.shopsite.model.Product;
import com.example.shopsite.model.User;
import com.example.shopsite.repository.ProductRepository;
import com.example.shopsite.repository.UserRepository;
import com.example.shopsite.repository.OrderItemRepository;
import com.example.shopsite.service.ProductService;
import com.example.shopsite.service.CategoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.shopsite.model.Category;
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CategoryService categoryService;
    private final OrderItemRepository orderItemRepository;

    public ProductServiceImpl(ProductRepository productRepository, UserRepository userRepository,
            CategoryService categoryService, OrderItemRepository orderItemRepository) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.categoryService = categoryService;
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    @Transactional
    public Product createProduct(ProductCreationRequest request, String username) {
        // 1. 根据当前登录用户名查找商家
        User merchant = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("商家用户不存在"));

        Category category = categoryService.findCategoryById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found."));

        // 2. 构建商品实体
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .isAvailable(true) // 默认上架
                .merchant(merchant)
                .category(category)
                .imageUrl(request.getImageUrl())
                .build();

        // 3. 保存并返回
        return productRepository.save(product);
    }

    // 接口方法实现 2: 使用 Product 实体和 @AuthenticationPrincipal (新逻辑)
    @Override
    @Transactional
    public Product createProduct(Product product, Long categoryId, User merchant) {
        Category category = categoryService.findCategoryById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found."));

        product.setCategory(category);
        product.setMerchant(merchant);
        product.setIsAvailable(true);

        return productRepository.save(product);
    }

    // 3. 查询所有可售商品 (用于前台列表)
    @Override
    public List<Product> findAllAvailableProducts() {
        return productRepository.findByIsAvailableTrueAndStockGreaterThan(0);
    }

    // 接口方法实现 4: 更新商品 (您需要将上一步的 updateProduct 逻辑放在这里)
    @Override
    @Transactional
    public Product updateProduct(Long productId, Product updatedDetails, User merchant) {
        // ... (省略了上一步提供的更新逻辑)
        return productRepository.findById(productId)
                .map(product -> {
                    if (!product.getMerchant().getId().equals(merchant.getId())) {
                        throw new SecurityException("Access denied: You can only update your own products.");
                    }
                    if (updatedDetails.getCategory() != null && updatedDetails.getCategory().getId() != null) {
                        Category newCategory = categoryService.findCategoryById(updatedDetails.getCategory().getId())
                                .orElseThrow(() -> new IllegalArgumentException("New Category not found."));
                        product.setCategory(newCategory);
                    }
                    product.setName(updatedDetails.getName());
                    product.setDescription(updatedDetails.getDescription());
                    product.setPrice(updatedDetails.getPrice());
                    product.setStock(updatedDetails.getStock());
                    product.setIsAvailable(updatedDetails.getIsAvailable());
                    product.setImageUrl(updatedDetails.getImageUrl());
                    return productRepository.save(product);
                }).orElseThrow(() -> new IllegalArgumentException("Product not found."));
    }

    // 接口方法实现 5: 查找商户自己的所有商品
    @Override
    @Transactional(readOnly = true)
    public List<Product> findProductsByMerchant(User merchant) {
        return productRepository.findByMerchant(merchant);
    }

    // 6. 按ID查找商品 (前台详情页)
    @Override
    @Transactional(readOnly = true)
    public Optional<Product> findAvailableProductById(Long id) {
        return productRepository.findByIdAndIsAvailableTrueAndStockGreaterThan(id, 0);
    }

    // 7. 查询新品（首页展示）
    @Override
    @Transactional(readOnly = true)
    public List<Product> findNewArrivals(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        // 按ID降序查询（假设ID越大越新），只查询已上架且有库存的商品
        return productRepository.findByIsAvailableTrueAndStockGreaterThanOrderByIdDesc(0, pageable);
    }

    // 8. 查询排行榜（按销量排序）
    @Override
    @Transactional(readOnly = true)
    public List<Product> findTopSellingProducts(int limit) {
        // 从OrderItem统计销量
        List<Object[]> topSelling = orderItemRepository.findTopSellingProducts();

        // 转换为Product列表，只保留已上架且有库存的商品（双重过滤确保安全）
        List<Product> products = topSelling.stream()
                .map(result -> (Product) result[0])
                .filter(p -> p != null && p.getIsAvailable() != null && p.getIsAvailable() && p.getStock() != null
                        && p.getStock() > 0)
                .limit(limit)
                .collect(Collectors.toList());

        // 如果销量数据不足，补充其他可售商品
        if (products.size() < limit) {
            List<Product> availableProducts = productRepository.findByIsAvailableTrueAndStockGreaterThan(0);
            for (Product p : availableProducts) {
                if (products.size() >= limit)
                    break;
                if (!products.contains(p) && p.getIsAvailable() && p.getStock() > 0) {
                    products.add(p);
                }
            }
        }

        return products;
    }

    // 9. 按分类查询商品（分页）
    @Override
    @Transactional(readOnly = true)
    public Page<Product> findProductsByCategory(Long categoryId, Pageable pageable) {
        return productRepository.findByCategory_IdAndIsAvailableTrueAndStockGreaterThan(categoryId, 0, pageable);
    }

    // 10. 按分类查询商品（不分页，用于首页分类展示）
    @Override
    @Transactional(readOnly = true)
    public List<Product> findProductsByCategory(Long categoryId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        Page<Product> page = productRepository.findByCategory_IdAndIsAvailableTrueAndStockGreaterThan(categoryId, 0,
                pageable);
        return page.getContent();
    }

    // 11. 搜索商品（按关键词搜索名称和描述）
    @Override
    @Transactional(readOnly = true)
    public List<Product> searchProducts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAllAvailableProducts();
        }
        return productRepository.searchAvailableProducts(keyword.trim());
    }

    // 12. 查询所有商品（管理员使用，包括已上架和未上架）
    @Override
    @Transactional(readOnly = true)
    public List<Product> findAllProducts() {
        return productRepository.findAll();
    }

}