package com.example.shopsite.config;

import com.example.shopsite.model.Category;
import com.example.shopsite.model.Product;
import com.example.shopsite.model.Role;
import com.example.shopsite.model.User;
import com.example.shopsite.repository.CategoryRepository;
import com.example.shopsite.repository.ProductRepository;
import com.example.shopsite.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;



@Component
@Profile("init-data")
@Order(2) // 在 CategoryInitializer 之后、ProductInitializer 之前执行
public class TestDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TestDataInitializer.class);

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    public TestDataInitializer(UserRepository userRepository,
                               CategoryRepository categoryRepository,
                               ProductRepository productRepository,
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // 1. 创建商户 test_final_merchant
        User merchant = createMerchantIfNotExists(
                "test_final_merchant",
                "test_final_merchant@shopsite.com",
                "TEST123" // 
        );

        // 2. 创建普通用户 test_user
        createCustomerIfNotExists(
                "test_user",
                "test_user@shopsite.com",
                "USER123"
        );

        // 3. 为 test_final_merchant 创建 8 个品类 * 6 个商品
        createProductsForMerchant(merchant);
    }

    private User createMerchantIfNotExists(String username, String email, String rawPassword) {
        Optional<User> optional = userRepository.findByUsername(username);
        if (optional.isPresent()) {
            log.info("商户 {} 已存在，跳过创建", username);
            return optional.get();
        }

        User merchant = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(Role.MERCHANT)
                .build();

        merchant = userRepository.save(merchant);
        log.info("已创建测试商户：{}", username);
        return merchant;
    }

    private void createCustomerIfNotExists(String username, String email, String rawPassword) {
        if (userRepository.findByUsername(username).isPresent()) {
            log.info("用户 {} 已存在，跳过创建", username);
            return;
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(Role.CUSTOMER)
                .build();

        userRepository.save(user);
        log.info("已创建测试用户：{}", username);
    }

    private void createProductsForMerchant(User merchant) {
        // 检查该商户是否已有商品，而不是检查全局商品数量
        long merchantProductCount = productRepository.countByMerchant(merchant);
        if (merchantProductCount > 0) {
            log.info("商户 {} 已有 {} 个商品，跳过创建", merchant.getUsername(), merchantProductCount);
            return;
        }

        List<Category> categories = categoryRepository.findAll();
        if (categories.isEmpty()) {
            log.warn("没有找到商品分类，无法创建商品");
            return;
        }
        
        if (categories.size() != 8) {
            log.warn("当前分类数量为 {}，不是 8 个，仍然按现有分类创建商品", categories.size());
        }

        String placeholderImageUrl = "/images/placeholder.jpg"; 
        // 对应文件路径：D:\GitHub\shopsite\src\main\resources\static\images\placeholder.jpg

        List<Product> allProducts = new ArrayList<>();

        for (Category category : categories) {
            // 每个分类 6 个商品，其中 3 个上架，3 个未上架
            for (int i = 1; i <= 6; i++) {
                boolean isAvailable = (i <= 3); // 1~3 上架，4~6 未上架

                Product product = Product.builder()
                        .name(category.getName() + " 测试商品 " + i)
                        .description("商户 " + merchant.getUsername() +
                                " 在分类「" + category.getName() + "」下的测试商品 " + i)
                        .price(new BigDecimal("10." + i)) // 简单区分一下价格
                        .stock(100)
                        .isAvailable(isAvailable)
                        .imageUrl(placeholderImageUrl)
                        .category(category)
                        .merchant(merchant)
                        .build();

                allProducts.add(product);
            }
        }

        productRepository.saveAll(allProducts);
        log.info("已为商户 {} 创建 {} 个测试商品", merchant.getUsername(), allProducts.size());
    }
}