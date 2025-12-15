package com.example.shopsite.config;

import com.example.shopsite.model.*;
import com.example.shopsite.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品数据初始化器
 * 在应用启动时自动创建测试商品数据
 */
@Component
@Order(3) // 在 AdminInitializer 和 CategoryInitializer 之后执行
public class ProductInitializer implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(ProductInitializer.class);
    
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    
    public ProductInitializer(ProductRepository productRepository,
                              CategoryRepository categoryRepository,
                              UserRepository userRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }
    
    @Override
    @Transactional
    public void run(String... args) {
        // 检查是否已有商品数据
        if (productRepository.count() > 0) {
            logger.info("商品数据已存在，跳过初始化");
            return;
        }
        
        // 获取商户和分类
        List<User> merchants = userRepository.findByRole(Role.MERCHANT);
        List<Category> categories = categoryRepository.findAll();
        
        if (merchants.isEmpty()) {
            logger.warn("没有找到商户用户，无法创建商品数据");
            return;
        }
        
        if (categories.isEmpty()) {
            logger.warn("没有找到商品分类，无法创建商品数据");
            return;
        }
        
        User merchant = merchants.get(0); // 使用第一个商户
        
        logger.info("开始初始化商品数据...");
        
        // 为每个分类创建多个商品
        int productIndex = 0;
        for (Category category : categories) {
            // 根据分类创建不同类型的商品
            List<Product> categoryProducts = createProductsForCategory(category, merchant, productIndex);
            productRepository.saveAll(categoryProducts);
            productIndex += categoryProducts.size();
        }
        
        logger.info("商品数据初始化完成，共创建 {} 个商品", productRepository.count());
    }
    
    private List<Product> createProductsForCategory(Category category, User merchant, int startIndex) {
        String categoryName = category.getName();
        
        // 根据分类名称创建对应的商品
        if (categoryName.contains("服装") || categoryName.contains("服饰")) {
            return createClothingProducts(category, merchant, startIndex);
        } else if (categoryName.contains("家居") || categoryName.contains("收纳")) {
            return createHomeProducts(category, merchant, startIndex);
        } else if (categoryName.contains("文具") || categoryName.contains("办公")) {
            return createStationeryProducts(category, merchant, startIndex);
        } else if (categoryName.contains("食品") || categoryName.contains("零食")) {
            return createFoodProducts(category, merchant, startIndex);
        } else if (categoryName.contains("美妆") || categoryName.contains("护肤")) {
            return createBeautyProducts(category, merchant, startIndex);
        } else if (categoryName.contains("电子") || categoryName.contains("数码")) {
            return createElectronicsProducts(category, merchant, startIndex);
        } else {
            return createGeneralProducts(category, merchant, startIndex);
        }
    }
    
    private List<Product> createClothingProducts(Category category, User merchant, int startIndex) {
        return List.of(
            createProduct("MUJI 无印良品 基础款T恤", "简约舒适的纯棉T恤，适合日常穿着。采用优质棉质面料，透气舒适。", 
                         new BigDecimal("99.00"), 100, category, merchant, "/images/products/tshirt.jpg"),
            createProduct("MUJI 无印良品 休闲长裤", "舒适宽松的休闲长裤，适合居家和外出。", 
                         new BigDecimal("199.00"), 80, category, merchant, "/images/products/pants.jpg"),
            createProduct("MUJI 无印良品 针织开衫", "温暖舒适的针织开衫，春秋季节必备单品。", 
                         new BigDecimal("299.00"), 60, category, merchant, "/images/products/cardigan.jpg"),
            createProduct("MUJI 无印良品 纯棉袜子", "三双装纯棉袜子，柔软舒适，吸汗透气。", 
                         new BigDecimal("45.00"), 200, category, merchant, "/images/products/socks.jpg")
        );
    }
    
    private List<Product> createHomeProducts(Category category, User merchant, int startIndex) {
        return List.of(
            createProduct("MUJI 无印良品 透明收纳盒", "透明收纳盒，整理家居必备。可堆叠设计，节省空间。", 
                         new BigDecimal("45.00"), 200, category, merchant, "/images/products/storage.jpg"),
            createProduct("MUJI 无印良品 香薰机", "超声波香薰机，营造舒适氛围。静音设计，适合卧室使用。", 
                         new BigDecimal("299.00"), 50, category, merchant, "/images/products/diffuser.jpg"),
            createProduct("MUJI 无印良品 懒人沙发", "舒适懒人沙发，填充优质颗粒，贴合身体曲线。", 
                         new BigDecimal("599.00"), 30, category, merchant, "/images/products/sofa.jpg"),
            createProduct("MUJI 无印良品 床品四件套", "纯棉床品四件套，柔软舒适，简约设计。", 
                         new BigDecimal("399.00"), 40, category, merchant, "/images/products/bedding.jpg"),
            createProduct("MUJI 无印良品 台灯", "简约设计台灯，护眼LED光源，可调节亮度。", 
                         new BigDecimal("199.00"), 70, category, merchant, "/images/products/lamp.jpg")
        );
    }
    
    private List<Product> createStationeryProducts(Category category, User merchant, int startIndex) {
        return List.of(
            createProduct("MUJI 无印良品 凝胶墨水笔", "0.5mm 黑色凝胶墨水笔，书写流畅，不易断墨。", 
                         new BigDecimal("8.00"), 500, category, merchant, "/images/products/pen.jpg"),
            createProduct("MUJI 无印良品 活页笔记本", "A5尺寸活页笔记本，内页可替换，方便整理。", 
                         new BigDecimal("35.00"), 150, category, merchant, "/images/products/notebook.jpg"),
            createProduct("MUJI 无印良品 文件袋", "透明文件袋，A4尺寸，方便收纳文件。", 
                         new BigDecimal("15.00"), 300, category, merchant, "/images/products/folder.jpg"),
            createProduct("MUJI 无印良品 便签纸", "彩色便签纸，5色装，方便标记和提醒。", 
                         new BigDecimal("12.00"), 400, category, merchant, "/images/products/sticky.jpg")
        );
    }
    
    private List<Product> createFoodProducts(Category category, User merchant, int startIndex) {
        return List.of(
            createProduct("MUJI 无印良品 有机绿茶", "有机绿茶，清香淡雅，健康饮品。", 
                         new BigDecimal("58.00"), 100, category, merchant, "/images/products/tea.jpg"),
            createProduct("MUJI 无印良品 坚果混合装", "多种坚果混合装，营养丰富，适合作为零食。", 
                         new BigDecimal("68.00"), 120, category, merchant, "/images/products/nuts.jpg"),
            createProduct("MUJI 无印良品 即食燕麦片", "即食燕麦片，营养早餐，方便快捷。", 
                         new BigDecimal("45.00"), 150, category, merchant, "/images/products/oats.jpg")
        );
    }
    
    private List<Product> createBeautyProducts(Category category, User merchant, int startIndex) {
        return List.of(
            createProduct("MUJI 无印良品 敏感肌化妆水", "温和无添加，适合敏感肌肤使用。", 
                         new BigDecimal("88.00"), 80, category, merchant, "/images/products/toner.jpg"),
            createProduct("MUJI 无印良品 卸妆油", "温和卸妆油，轻松卸除彩妆，不刺激肌肤。", 
                         new BigDecimal("98.00"), 70, category, merchant, "/images/products/cleanser.jpg"),
            createProduct("MUJI 无印良品 护手霜", "滋润护手霜，有效保湿，适合秋冬季节。", 
                         new BigDecimal("45.00"), 150, category, merchant, "/images/products/handcream.jpg")
        );
    }
    
    private List<Product> createElectronicsProducts(Category category, User merchant, int startIndex) {
        return List.of(
            createProduct("MUJI 无印良品 LED台灯", "护眼LED台灯，可调节亮度和色温。", 
                         new BigDecimal("299.00"), 50, category, merchant, "/images/products/ledlamp.jpg"),
            createProduct("MUJI 无印良品 加湿器", "静音加湿器，改善室内空气湿度。", 
                         new BigDecimal("399.00"), 40, category, merchant, "/images/products/humidifier.jpg")
        );
    }
    
    private List<Product> createGeneralProducts(Category category, User merchant, int startIndex) {
        return List.of(
            createProduct("MUJI 无印良品 基础款商品 " + (startIndex + 1), 
                         "简约实用的基础款商品，品质保证。", 
                         new BigDecimal("99.00"), 100, category, merchant, "/images/products/default.jpg"),
            createProduct("MUJI 无印良品 基础款商品 " + (startIndex + 2), 
                         "简约实用的基础款商品，品质保证。", 
                         new BigDecimal("149.00"), 80, category, merchant, "/images/products/default.jpg")
        );
    }
    
    private Product createProduct(String name, String description, BigDecimal price, 
                                  Integer stock, Category category, User merchant, String imageUrl) {
        return Product.builder()
            .name(name)
            .description(description)
            .price(price)
            .stock(stock)
            .isAvailable(true)
            .imageUrl(imageUrl)
            .category(category)
            .merchant(merchant)
            .build();
    }
}






