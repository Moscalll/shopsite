package com;

import com.example.shopsite.dto.OrderCreationRequest;
import com.example.shopsite.dto.OrderItemRequest;
import com.example.shopsite.dto.UserRegistrationRequest;
import com.example.shopsite.model.*;
import com.example.shopsite.repository.*;
import com.example.shopsite.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;


import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 交互式命令行测试工具
 * 使用方法：启动应用时添加 --spring.profiles.active=test-cli
 */
@Component
@Profile("test-cli")
public class InteractiveTestRunner implements CommandLineRunner {

    @Autowired
    private UserService userService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private CategoryService categoryService;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private CartService cartService;
    
    @Autowired
    private FavoriteService favoriteService;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private SalesLogService salesLogService;
    
    @Autowired
    private SalesLogRepository salesLogRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private OrderRepository orderRepository;
    
    private Scanner scanner = new Scanner(System.in);
    
    // 测试数据存储
    private User testMerchant;
    private User testUser;
    private User admin;
    private List<Product> merchantProducts = new ArrayList<>();
    
    @Override
    public void run(String... args) {
        System.out.println("\n==========================================");
        System.out.println("   商店系统交互式测试工具");
        System.out.println("==========================================\n");
        
        // 初始化测试数据
        initializeTestData();
        
        // 显示主菜单
        showMainMenu();
    }
    
    private void initializeTestData() {
        System.out.println("正在初始化测试数据...");
        
        // 查找或创建admin用户
        admin = userRepository.findByUsername("admin").orElse(null);
        if (admin == null) {
            admin = userRepository.findByUsername("platformadmin").orElse(null);
        }
        
        // 查找或创建test_merchant
        testMerchant = userRepository.findByUsername("test_merchant").orElse(null);
        
        // 查找或创建test_user
        testUser = userRepository.findByUsername("test_user").orElse(null);
        
        System.out.println("测试数据初始化完成！\n");
    }
    
    private void showMainMenu() {
        while (true) {
            System.out.println("\n========== 主菜单 ==========");
            System.out.println("1. 商户测试场景 (test_merchant)");
            System.out.println("2. 用户测试场景 (test_user)");
            System.out.println("3. 管理员测试场景 (admin)");
            System.out.println("4. 查看所有测试数据");
            System.out.println("0. 退出");
            System.out.print("请选择 (0-4): ");
            
            int choice = readInt();
            
            switch (choice) {
                case 1:
                    merchantTestMenu();
                    break;
                case 2:
                    userTestMenu();
                    break;
                case 3:
                    adminTestMenu();
                    break;
                case 4:
                    showAllTestData();
                    break;
                case 0:
                    System.out.println("退出测试工具");
                    return;
                default:
                    System.out.println("无效选择，请重试");
            }
        }
    }
    
    // ========== 商户测试场景 ==========
    private void merchantTestMenu() {
        while (true) {
            System.out.println("\n========== 商户测试场景 ==========");
            System.out.println("1. 创建商户 test_merchant");
            System.out.println("2. 上传5种商品");
            System.out.println("3. 下架1种商品");
            System.out.println("4. 处理订单 - 发货");
            System.out.println("5. 处理订单 - 完成");
            System.out.println("6. 输出用户日志");
            System.out.println("7. 执行完整商户测试流程");
            System.out.println("0. 返回主菜单");
            System.out.print("请选择 (0-7): ");
            
            int choice = readInt();
            
            switch (choice) {
                case 1:
                    createTestMerchant();
                    break;
                case 2:
                    uploadProducts();
                    break;
                case 3:
                    takeDownProduct();
                    break;
                case 4:
                    shipOrder();
                    break;
                case 5:
                    completeOrder();
                    break;
                case 6:
                    showUserLogs();
                    break;
                case 7:
                    runFullMerchantTest();
                    break;
                case 0:
                    return;
                default:
                    System.out.println("无效选择，请重试");
            }
        }
    }
    
    private void createTestMerchant() {
        System.out.println("\n--- 创建商户 test_merchant ---");
        
        if (testMerchant != null) {
            System.out.println("商户 test_merchant 已存在，ID: " + testMerchant.getId());
            return;
        }
        
        try {
            UserRegistrationRequest request = new UserRegistrationRequest();
            request.setUsername("test_merchant");
            request.setEmail("test_merchant@test.com");
            request.setPassword("test123456");
            
            testMerchant = userService.registerUser(request, Role.MERCHANT);
            System.out.println("✓ 商户创建成功！");
            System.out.println("  用户名: " + testMerchant.getUsername());
            System.out.println("  邮箱: " + testMerchant.getEmail());
            System.out.println("  角色: " + testMerchant.getRole());
            System.out.println("  ID: " + testMerchant.getId());
        } catch (Exception e) {
            System.out.println("✗ 创建失败: " + e.getMessage());
        }
    }
    
    private void uploadProducts() {
        System.out.println("\n--- 上传5种商品 ---");
        
        if (testMerchant == null) {
            System.out.println("✗ 请先创建商户 test_merchant");
            return;
        }
        
        // 获取分类
        List<Category> categories = categoryService.findAllCategories();
        if (categories.isEmpty()) {
            System.out.println("✗ 没有可用的商品分类，请先初始化分类数据");
            return;
        }
        
        // 模拟登录商户
        loginAsUser(testMerchant);
        
        Category firstCategory = categories.get(0);
        merchantProducts.clear();
        
        String[] productNames = {
            "测试商品A - 简约T恤",
            "测试商品B - 舒适长裤",
            "测试商品C - 收纳盒",
            "测试商品D - 笔记本",
            "测试商品E - 绿茶"
        };
        
        String[] descriptions = {
            "简约舒适的纯棉T恤",
            "舒适宽松的休闲长裤",
            "透明收纳盒，整理家居必备",
            "A5尺寸活页笔记本",
            "有机绿茶，清香淡雅"
        };
        
        BigDecimal[] prices = {
            new BigDecimal("99.00"),
            new BigDecimal("199.00"),
            new BigDecimal("45.00"),
            new BigDecimal("35.00"),
            new BigDecimal("58.00")
        };
        
        for (int i = 0; i < 5; i++) {
            try {
                Product product = Product.builder()
                    .name(productNames[i])
                    .description(descriptions[i])
                    .price(prices[i])
                    .stock(100)
                    .isAvailable(true)
                    .imageUrl("/images/products/test" + (i+1) + ".jpg")
                    .category(firstCategory)
                    .merchant(testMerchant)
                    .build();
                
                Product saved = productService.createProduct(product, firstCategory.getId(), testMerchant);
                merchantProducts.add(saved);
                System.out.println("✓ 商品 " + (i+1) + " 创建成功: " + saved.getName() + " (ID: " + saved.getId() + ")");
            } catch (Exception e) {
                System.out.println("✗ 商品 " + (i+1) + " 创建失败: " + e.getMessage());
            }
        }
        
        System.out.println("\n总共创建了 " + merchantProducts.size() + " 个商品");
    }
    
    private void takeDownProduct() {
        System.out.println("\n--- 下架1种商品 ---");
        
        if (merchantProducts.isEmpty()) {
            System.out.println("✗ 没有可下架的商品，请先上传商品");
            return;
        }
        
        loginAsUser(testMerchant);
        
        // 下架第一个商品
        Product product = merchantProducts.get(0);
        try {
            product.setIsAvailable(false);
            productRepository.save(product);
            System.out.println("✓ 商品已下架: " + product.getName() + " (ID: " + product.getId() + ")");
        } catch (Exception e) {
            System.out.println("✗ 下架失败: " + e.getMessage());
        }
    }
    
    private void shipOrder() {
        System.out.println("\n--- 处理订单 - 发货 ---");
        
        if (testMerchant == null) {
            System.out.println("✗ 请先创建商户");
            return;
        }
        
        loginAsUser(testMerchant);
        
        List<Order> orders = orderService.findOrdersByMerchant(testMerchant);
        if (orders.isEmpty()) {
            System.out.println("✗ 该商户没有待处理的订单");
            return;
        }
        
        // 查找可以发货的订单（状态为PROCESSING）
        Optional<Order> orderToShip = orders.stream()
            .filter(o -> o.getStatus() == OrderStatus.PROCESSING)
            .findFirst();
        
        if (orderToShip.isEmpty()) {
            System.out.println("✗ 没有可以发货的订单（需要状态为PROCESSING）");
            System.out.println("当前订单列表：");
            orders.forEach(o -> System.out.println("  订单ID: " + o.getId() + ", 状态: " + o.getStatus()));
            return;
        }
        
        Order order = orderToShip.get();
        try {
            Order shipped = orderService.shipOrder(order.getId(), testMerchant);
            System.out.println("✓ 订单已发货！");
            System.out.println("  订单ID: " + shipped.getId());
            System.out.println("  状态: " + shipped.getStatus());
            System.out.println("  总金额: " + shipped.getTotalAmount());
        } catch (Exception e) {
            System.out.println("✗ 发货失败: " + e.getMessage());
        }
    }
    
    private void completeOrder() {
        System.out.println("\n--- 处理订单 - 完成 ---");
        
        if (testMerchant == null) {
            System.out.println("✗ 请先创建商户");
            return;
        }
        
        loginAsUser(testMerchant);
        
        List<Order> orders = orderService.findOrdersByMerchant(testMerchant);
        if (orders.isEmpty()) {
            System.out.println("✗ 该商户没有订单");
            return;
        }
        
        // 查找可以完成的订单（状态为SHIPPED或DELIVERED）
        Optional<Order> orderToComplete = orders.stream()
            .filter(o -> o.getStatus() == OrderStatus.SHIPPED || o.getStatus() == OrderStatus.DELIVERED)
            .findFirst();
        
        if (orderToComplete.isEmpty()) {
            System.out.println("✗ 没有可以完成的订单（需要状态为SHIPPED或DELIVERED）");
            System.out.println("当前订单列表：");
            orders.forEach(o -> System.out.println("  订单ID: " + o.getId() + ", 状态: " + o.getStatus()));
            return;
        }
        
        Order order = orderToComplete.get();
        try {
            Order updated = new Order();
            updated.setStatus(OrderStatus.COMPLETED);
            Order completed = orderService.updateOrder(order.getId(), testMerchant, updated);
            System.out.println("✓ 订单已完成！");
            System.out.println("  订单ID: " + completed.getId());
            System.out.println("  状态: " + completed.getStatus());
        } catch (Exception e) {
            System.out.println("✗ 完成订单失败: " + e.getMessage());
        }
    }
    
    private void showUserLogs() {
        System.out.println("\n--- 输出用户日志 ---");
        
        if (testMerchant == null) {
            System.out.println("✗ 请先创建商户");
            return;
        }
        
        List<SalesLog> logs = salesLogRepository.findByUser(testMerchant);
        if (logs.isEmpty()) {
            System.out.println("该用户暂无日志记录");
            return;
        }
        
        System.out.println("用户 " + testMerchant.getUsername() + " 的日志记录：");
        System.out.println("----------------------------------------");
        logs.forEach(log -> {
            System.out.println("时间: " + log.getLogTime());
            System.out.println("操作: " + log.getActionType());
            if (log.getProductId() != null) {
                Optional<Product> productOpt = productRepository.findById(log.getProductId());
                if (productOpt.isPresent()) {
                    System.out.println("商品: " + productOpt.get().getName() + " (ID: " + log.getProductId() + ")");
                } else {
                    System.out.println("商品ID: " + log.getProductId() + " (商品不存在)");
                }
            }
            System.out.println("----------------------------------------");
        });
    }
    
    private void runFullMerchantTest() {
        System.out.println("\n========== 执行完整商户测试流程 ==========");
        
        createTestMerchant();
        uploadProducts();
        takeDownProduct();
        
        System.out.println("\n提示：要测试订单处理，请先让test_user创建订单");
        System.out.println("然后可以执行：");
        System.out.println("  4. 处理订单 - 发货");
        System.out.println("  5. 处理订单 - 完成");
    }
    
    // ========== 用户测试场景 ==========
    private void userTestMenu() {
        while (true) {
            System.out.println("\n========== 用户测试场景 ==========");
            System.out.println("1. 创建用户 test_user");
            System.out.println("2. 收藏3种商品");
            System.out.println("3. 取消1种收藏");
            System.out.println("4. 加入5种商品到购物车");
            System.out.println("5. 选中3种商品，增加数量后提交订单");
            System.out.println("6. 首页挑选单件商品直接提交订单");
            System.out.println("7. 修改邮箱");
            System.out.println("8. 取消单件商品的订单");
            System.out.println("9. 执行完整用户测试流程");
            System.out.println("0. 返回主菜单");
            System.out.print("请选择 (0-9): ");
            
            int choice = readInt();
            
            switch (choice) {
                case 1:
                    createTestUser();
                    break;
                case 2:
                    addFavorites();
                    break;
                case 3:
                    removeFavorite();
                    break;
                case 4:
                    addToCart();
                    break;
                case 5:
                    checkoutSelectedItems();
                    break;
                case 6:
                    directPurchase();
                    break;
                case 7:
                    updateEmail();
                    break;
                case 8:
                    cancelOrder();
                    break;
                case 9:
                    runFullUserTest();
                    break;
                case 0:
                    return;
                default:
                    System.out.println("无效选择，请重试");
            }
        }
    }
    
    private void createTestUser() {
        System.out.println("\n--- 创建用户 test_user ---");
        
        if (testUser != null) {
            System.out.println("用户 test_user 已存在，ID: " + testUser.getId());
            return;
        }
        
        try {
            UserRegistrationRequest request = new UserRegistrationRequest();
            request.setUsername("test_user");
            request.setEmail("test_user@test.com");
            request.setPassword("test123456");
            
            testUser = userService.registerUser(request, Role.CUSTOMER);
            System.out.println("✓ 用户创建成功！");
            System.out.println("  用户名: " + testUser.getUsername());
            System.out.println("  邮箱: " + testUser.getEmail());
            System.out.println("  角色: " + testUser.getRole());
            System.out.println("  ID: " + testUser.getId());
        } catch (Exception e) {
            System.out.println("✗ 创建失败: " + e.getMessage());
        }
    }
    
    private void addFavorites() {
        System.out.println("\n--- 收藏3种商品 ---");
        
        if (testUser == null) {
            System.out.println("✗ 请先创建用户 test_user");
            return;
        }
        
        if (merchantProducts.isEmpty()) {
            System.out.println("✗ 没有可收藏的商品，请先让商户上传商品");
            return;
        }
        
        loginAsUser(testUser);
        
        // 收藏前3个商品
        int count = 0;
        for (int i = 0; i < Math.min(3, merchantProducts.size()); i++) {
            Product product = merchantProducts.get(i);
            if (!product.getIsAvailable()) {
                continue; // 跳过已下架的商品
            }
            
            try {
                favoriteService.addFavorite(product.getId(), testUser);
                count++;
                System.out.println("✓ 已收藏: " + product.getName() + " (ID: " + product.getId() + ")");
            } catch (Exception e) {
                System.out.println("✗ 收藏失败: " + product.getName() + " - " + e.getMessage());
            }
        }
        
        System.out.println("\n总共收藏了 " + count + " 个商品");
    }
    
    private void removeFavorite() {
        System.out.println("\n--- 取消1种收藏 ---");
        
        if (testUser == null) {
            System.out.println("✗ 请先创建用户");
            return;
        }
        
        loginAsUser(testUser);
        
        List<Favorite> favorites = favoriteService.getUserFavorites(testUser);
        if (favorites.isEmpty()) {
            System.out.println("✗ 没有收藏的商品");
            return;
        }
        
        // 取消第一个收藏
        Favorite favorite = favorites.get(0);
        try {
            favoriteService.removeFavorite(favorite.getProduct().getId(), testUser);
            System.out.println("✓ 已取消收藏: " + favorite.getProduct().getName());
        } catch (Exception e) {
            System.out.println("✗ 取消收藏失败: " + e.getMessage());
        }
    }
    
    private void addToCart() {
        System.out.println("\n--- 加入5种商品到购物车 ---");
        
        if (testUser == null) {
            System.out.println("✗ 请先创建用户 test_user");
            return;
        }
        
        if (merchantProducts.isEmpty()) {
            System.out.println("✗ 没有可加入购物车的商品");
            return;
        }
        
        loginAsUser(testUser);
        
        int count = 0;
        for (int i = 0; i < Math.min(5, merchantProducts.size()); i++) {
            Product product = merchantProducts.get(i);
            if (!product.getIsAvailable()) {
                continue;
            }
            
            try {
                cartService.addToCart(product.getId(), 1, testUser);
                count++;
                System.out.println("✓ 已加入购物车: " + product.getName() + " (ID: " + product.getId() + ")");
            } catch (Exception e) {
                System.out.println("✗ 加入购物车失败: " + product.getName() + " - " + e.getMessage());
            }
        }
        
        System.out.println("\n总共加入了 " + count + " 个商品到购物车");
    }
    
    private void checkoutSelectedItems() {
        System.out.println("\n--- 选中3种商品，增加数量后提交订单 ---");
        
        if (testUser == null) {
            System.out.println("✗ 请先创建用户");
            return;
        }
        
        loginAsUser(testUser);
        
        List<CartItem> cartItems = cartService.getCartItems(testUser);
        if (cartItems.isEmpty()) {
            System.out.println("✗ 购物车为空，请先添加商品");
            return;
        }
        
        // 选择前3个购物车项
        List<CartItem> selectedItems = cartItems.stream()
            .limit(3)
            .collect(Collectors.toList());
        
        // 增加数量
        for (CartItem item : selectedItems) {
            try {
                cartService.updateCartItemQuantity(item.getId(), item.getQuantity() + 1, testUser);
                System.out.println("✓ 更新数量: " + item.getProduct().getName() + " -> " + (item.getQuantity() + 1));
            } catch (Exception e) {
                System.out.println("✗ 更新数量失败: " + e.getMessage());
            }
        }
        
        // 创建订单
        try {
            OrderCreationRequest request = new OrderCreationRequest();
            List<OrderItemRequest> items = selectedItems.stream()
                .map(item -> {
                    return new OrderItemRequest(
                        item.getProduct().getId(),
                        item.getQuantity() + 1
                    );
                })
                .collect(Collectors.toList());
            request.setItems(items);
            
            Order order = orderService.createOrder(request, testUser.getUsername());
            System.out.println("\n✓ 订单创建成功！");
            System.out.println("  订单ID: " + order.getId());
            System.out.println("  总金额: " + order.getTotalAmount());
            System.out.println("  状态: " + order.getStatus());
            
            // 支付订单
            Order paid = orderService.processPayment(order.getId(), testUser.getUsername());
            System.out.println("✓ 订单已支付，状态: " + paid.getStatus());
            
            // 从购物车移除已下单的商品
            for (CartItem item : selectedItems) {
                cartService.removeFromCart(item.getId(), testUser);
            }
        } catch (Exception e) {
            System.out.println("✗ 创建订单失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void directPurchase() {
        System.out.println("\n--- 首页挑选单件商品直接提交订单 ---");
        
        if (testUser == null) {
            System.out.println("✗ 请先创建用户");
            return;
        }
        
        if (merchantProducts.isEmpty()) {
            System.out.println("✗ 没有可购买的商品");
            return;
        }
        
        loginAsUser(testUser);
        
        // 找一个可用的商品
        Optional<Product> availableProduct = merchantProducts.stream()
            .filter(Product::getIsAvailable)
            .findFirst();
        
        if (availableProduct.isEmpty()) {
            System.out.println("✗ 没有可用的商品");
            return;
        }
        
        Product product = availableProduct.get();
        
        try {
            OrderCreationRequest request = new OrderCreationRequest();
            OrderItemRequest item = new OrderItemRequest(product.getId(), 1);
            item.setProductId(product.getId());
            item.setQuantity(1);
            request.setItems(List.of(item));
            
            Order order = orderService.createOrder(request, testUser.getUsername());
            System.out.println("✓ 订单创建成功！");
            System.out.println("  订单ID: " + order.getId());
            System.out.println("  商品: " + product.getName());
            System.out.println("  总金额: " + order.getTotalAmount());
            System.out.println("  状态: " + order.getStatus());
            
            // 支付订单
            Order paid = orderService.processPayment(order.getId(), testUser.getUsername());
            System.out.println("✓ 订单已支付，状态: " + paid.getStatus());
        } catch (Exception e) {
            System.out.println("✗ 创建订单失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void updateEmail() {
        System.out.println("\n--- 修改邮箱 ---");
        
        if (testUser == null) {
            System.out.println("✗ 请先创建用户");
            return;
        }
        
        loginAsUser(testUser);
        
        String oldEmail = testUser.getEmail();
        String newEmail = "test_user_new@test.com";
        
        try {
            testUser.setEmail(newEmail);
            userRepository.save(testUser);
            System.out.println("✓ 邮箱修改成功！");
            System.out.println("  旧邮箱: " + oldEmail);
            System.out.println("  新邮箱: " + newEmail);
        } catch (Exception e) {
            System.out.println("✗ 修改失败: " + e.getMessage());
        }
    }
    
    private void cancelOrder() {
        System.out.println("\n--- 取消单件商品的订单 ---");
        
        if (testUser == null) {
            System.out.println("✗ 请先创建用户");
            return;
        }
        
        loginAsUser(testUser);
        
        List<Order> orders = orderService.findMyOrders(testUser.getUsername());
        if (orders.isEmpty()) {
            System.out.println("✗ 没有可取消的订单");
            return;
        }
        
        // 找一个可以取消的订单（PENDING_PAYMENT或PROCESSING）
        Optional<Order> orderToCancel = orders.stream()
            .filter(o -> o.getStatus() == OrderStatus.PENDING_PAYMENT || 
                        o.getStatus() == OrderStatus.PROCESSING)
            .findFirst();
        
        if (orderToCancel.isEmpty()) {
            System.out.println("✗ 没有可以取消的订单（需要状态为PENDING_PAYMENT或PROCESSING）");
            return;
        }
        
        Order order = orderToCancel.get();
        try {
            Order cancelled = orderService.cancelOrderByCustomer(order.getId(), testUser.getUsername());
            System.out.println("✓ 订单已取消！");
            System.out.println("  订单ID: " + cancelled.getId());
            System.out.println("  状态: " + cancelled.getStatus());
        } catch (Exception e) {
            System.out.println("✗ 取消订单失败: " + e.getMessage());
        }
    }
    
    private void runFullUserTest() {
        System.out.println("\n========== 执行完整用户测试流程 ==========");
        
        createTestUser();
        
        if (merchantProducts.isEmpty()) {
            System.out.println("提示：请先让商户上传商品（主菜单 -> 1 -> 2）");
            return;
        }
        
        addFavorites();
        removeFavorite();
        addToCart();
        checkoutSelectedItems();
        directPurchase();
        updateEmail();
        
        System.out.println("\n提示：要测试取消订单，请先创建订单后再执行取消操作");
    }
    
    // ========== 管理员测试场景 ==========
    private void adminTestMenu() {
        while (true) {
            System.out.println("\n========== 管理员测试场景 ==========");
            System.out.println("1. 禁用商户 test_merchant");
            System.out.println("2. 启用商户 test_merchant");
            System.out.println("3. 修改其他商户的商品状态");
            System.out.println("4. 输出订单统计");
            System.out.println("5. 执行完整管理员测试流程");
            System.out.println("0. 返回主菜单");
            System.out.print("请选择 (0-5): ");
            
            int choice = readInt();
            
            switch (choice) {
                case 1:
                    disableMerchant();
                    break;
                case 2:
                    enableMerchant();
                    break;
                case 3:
                    modifyOtherMerchantProduct();
                    break;
                case 4:
                    showOrderStatistics();
                    break;
                case 5:
                    runFullAdminTest();
                    break;
                case 0:
                    return;
                default:
                    System.out.println("无效选择，请重试");
            }
        }
    }
    
    private void disableMerchant() {
        System.out.println("\n--- 禁用商户 test_merchant ---");
        
        if (admin == null) {
            System.out.println("✗ 管理员用户不存在，请先创建admin用户");
            return;
        }
        
        if (testMerchant == null) {
            System.out.println("✗ 商户 test_merchant 不存在");
            return;
        }
        
        loginAsUser(admin);
        
        try {
            userService.updateUserRole(testMerchant.getId(), Role.CUSTOMER);
            testMerchant = userRepository.findById(testMerchant.getId()).orElse(null);
            System.out.println("✓ 商户已禁用！");
            System.out.println("  用户名: " + testMerchant.getUsername());
            System.out.println("  当前角色: " + testMerchant.getRole());
        } catch (Exception e) {
            System.out.println("✗ 禁用失败: " + e.getMessage());
        }
    }
    
    private void enableMerchant() {
        System.out.println("\n--- 启用商户 test_merchant ---");
        
        if (admin == null) {
            System.out.println("✗ 管理员用户不存在");
            return;
        }
        
        if (testMerchant == null) {
            System.out.println("✗ 商户 test_merchant 不存在");
            return;
        }
        
        loginAsUser(admin);
        
        try {
            userService.updateUserRole(testMerchant.getId(), Role.MERCHANT);
            testMerchant = userRepository.findById(testMerchant.getId()).orElse(null);
            System.out.println("✓ 商户已启用！");
            System.out.println("  用户名: " + testMerchant.getUsername());
            System.out.println("  当前角色: " + testMerchant.getRole());
        } catch (Exception e) {
            System.out.println("✗ 启用失败: " + e.getMessage());
        }
    }
    
    private void modifyOtherMerchantProduct() {
        System.out.println("\n--- 修改其他商户的商品状态 ---");
        
        if (admin == null) {
            System.out.println("✗ 管理员用户不存在");
            return;
        }
        
        loginAsUser(admin);
        
        // 查找所有商品
        List<Product> allProducts = productService.findAllProducts();
        if (allProducts.isEmpty()) {
            System.out.println("✗ 没有商品");
            return;
        }
        
        // 找一个不是test_merchant的商品
        Optional<Product> otherProduct = allProducts.stream()
            .filter(p -> !p.getMerchant().getId().equals(testMerchant != null ? testMerchant.getId() : -1L))
            .findFirst();
        
        if (otherProduct.isEmpty()) {
            System.out.println("✗ 没有其他商户的商品");
            return;
        }
        
        Product product = otherProduct.get();
        try {
            boolean oldStatus = product.getIsAvailable();
            product.setIsAvailable(!oldStatus);
            productRepository.save(product);
            System.out.println("✓ 商品状态已修改！");
            System.out.println("  商品: " + product.getName());
            System.out.println("  商户: " + product.getMerchant().getUsername());
            System.out.println("  原状态: " + (oldStatus ? "上架" : "下架"));
            System.out.println("  新状态: " + (product.getIsAvailable() ? "上架" : "下架"));
        } catch (Exception e) {
            System.out.println("✗ 修改失败: " + e.getMessage());
        }
    }
    
    private void showOrderStatistics() {
        System.out.println("\n--- 订单统计 ---");
        
        if (admin == null) {
            System.out.println("✗ 管理员用户不存在");
            return;
        }
        
        loginAsUser(admin);
        
        List<Order> allOrders = orderService.findAllOrders();
        
        System.out.println("\n========== 订单统计 ==========");
        System.out.println("总订单数: " + allOrders.size());
        
        long pendingPayment = allOrders.stream()
            .filter(o -> o.getStatus() == OrderStatus.PENDING_PAYMENT)
            .count();
        long processing = allOrders.stream()
            .filter(o -> o.getStatus() == OrderStatus.PROCESSING)
            .count();
        long shipped = allOrders.stream()
            .filter(o -> o.getStatus() == OrderStatus.SHIPPED)
            .count();
        long delivered = allOrders.stream()
            .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
            .count();
        long completed = allOrders.stream()
            .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
            .count();
        long cancelled = allOrders.stream()
            .filter(o -> o.getStatus() == OrderStatus.CANCELLED)
            .count();
        
        System.out.println("待付款: " + pendingPayment);
        System.out.println("处理中: " + processing);
        System.out.println("已发货: " + shipped);
        System.out.println("已送达: " + delivered);
        System.out.println("已完成: " + completed);
        System.out.println("已取消: " + cancelled);
        
        BigDecimal totalAmount = allOrders.stream()
            .map(Order::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        System.out.println("总金额: " + totalAmount);
        
        System.out.println("\n订单详情：");
        allOrders.forEach(order -> {
            System.out.println("  订单ID: " + order.getId() + 
                             ", 用户: " + order.getUser().getUsername() + 
                             ", 状态: " + order.getStatus() + 
                             ", 金额: " + order.getTotalAmount());
        });
    }
    
    private void runFullAdminTest() {
        System.out.println("\n========== 执行完整管理员测试流程 ==========");
        
        if (admin == null) {
            System.out.println("✗ 管理员用户不存在，请先创建admin用户");
            return;
        }
        
        if (testMerchant == null) {
            System.out.println("提示：请先创建商户 test_merchant（主菜单 -> 1 -> 1）");
            return;
        }
        
        disableMerchant();
        enableMerchant();
        modifyOtherMerchantProduct();
        showOrderStatistics();
    }
    
    // ========== 辅助方法 ==========
    private void showAllTestData() {
        System.out.println("\n========== 所有测试数据 ==========");
        
        System.out.println("\n--- 用户信息 ---");
        if (testMerchant != null) {
            System.out.println("商户: " + testMerchant.getUsername() + " (ID: " + testMerchant.getId() + ", 角色: " + testMerchant.getRole() + ")");
        } else {
            System.out.println("商户: 未创建");
        }
        
        if (testUser != null) {
            System.out.println("用户: " + testUser.getUsername() + " (ID: " + testUser.getId() + ", 角色: " + testUser.getRole() + ")");
        } else {
            System.out.println("用户: 未创建");
        }
        
        if (admin != null) {
            System.out.println("管理员: " + admin.getUsername() + " (ID: " + admin.getId() + ", 角色: " + admin.getRole() + ")");
        } else {
            System.out.println("管理员: 未找到");
        }
        
        System.out.println("\n--- 商品信息 ---");
        if (!merchantProducts.isEmpty()) {
            merchantProducts.forEach(p -> {
                System.out.println("  " + p.getName() + " (ID: " + p.getId() + 
                                 ", 状态: " + (p.getIsAvailable() ? "上架" : "下架") + 
                                 ", 库存: " + p.getStock() + ")");
            });
        } else {
            System.out.println("  无商品");
        }
        
        if (testUser != null) {
            System.out.println("\n--- 购物车信息 ---");
            List<CartItem> cartItems = cartService.getCartItems(testUser);
            System.out.println("购物车商品数: " + cartItems.size());
            
            System.out.println("\n--- 收藏信息 ---");
            List<Favorite> favorites = favoriteService.getUserFavorites(testUser);
            System.out.println("收藏商品数: " + favorites.size());
            
            System.out.println("\n--- 订单信息 ---");
            List<Order> orders = orderService.findMyOrders(testUser.getUsername());
            System.out.println("订单数: " + orders.size());
            orders.forEach(o -> {
                System.out.println("  订单ID: " + o.getId() + ", 状态: " + o.getStatus() + ", 金额: " + o.getTotalAmount());
            });
        }
    }
    
    private void loginAsUser(User user) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
            user.getUsername(),
            user.getPassword(),
            user.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
    
    private int readInt() {
        try {
            String input = scanner.nextLine().trim();
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}