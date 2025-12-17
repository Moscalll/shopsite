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
import java.util.Map;
import java.util.stream.Collectors;

// @Order(2) 保持不变，在 CategoryInitializer 之后执行
@Component
@Profile("init-data")
@Order(2)
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
        log.info("--- 正在清空数据库并重新初始化数据 (Profile: init-data) ---");
        
        // 步骤 0: 清空数据库
        productRepository.deleteAllInBatch(); // 批量删除商品
        userRepository.deleteAllInBatch();    // 批量删除用户
        log.info("已清空 Product 和 User 表，准备重新创建基础数据。");


        // 1. 创建用户和商户
        // 商户 testmerchant
        User merchant = createUser(
                "testmerchant",
                "merchant@shopsite.com",
                "testmerchantPASSWORD",
                Role.MERCHANT
        );

        // 普通用户 clientuser
        createUser(
                "clientuser",
                "client@shopsite.com",
                "ClientSecurePassword789",
                Role.CUSTOMER
        );
        
        // 管理员 platformadmin
        createUser(
                "platformadmin",
                "admin@shopsite.com",
                "AdminSecurePassword123",
                Role.ADMIN
        );
        
        // 2. 为 testmerchant 创建商品
        createSpecificProductsForMerchant(merchant);
    }

    // 通用用户创建方法
    private User createUser(String username, String email, String rawPassword, Role role) {
        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .build();

        user = userRepository.save(user);
        log.info("已创建测试账户: {} (角色: {})", username, role.name());
        return user;
    }


    private void createSpecificProductsForMerchant(User merchant) {
        // 1. 获取所有分类，并映射为 Map<ID, Category>
        List<Category> categories = categoryRepository.findAll();
        if (categories.isEmpty()) {
            log.error("没有找到任何商品分类 (Category)，无法创建商品！请先运行 CategoryInitializer。");
            return;
        }

        // 使用 Category ID (假设它对应 SQL 结果中的 category_id) 进行查找
        Map<Long, Category> categoryMap = categories.stream()
                .collect(Collectors.toMap(Category::getId, category -> category));

        // 2. 定义精确的商品数据
        // 格式: { name, description, price, stock, isAvailable (默认为 true), imageUrl, categoryId }
        List<ProductData> dataList = List.of(
            // 分类 1: 服饰
            new ProductData("基础款T恤", "简约舒适的纯棉T恤，适合日常穿着。采用优质棉质面料，透气舒适。", "99.00", 100, true, "/images/products/tshirt.webp", 1L),
            new ProductData("休闲长裤", "舒适宽松的休闲长裤，适合居家和外出。", "199.00", 80, true, "/images/products/pants.webp", 1L),
            new ProductData("针织开衫", "温暖舒适的针织开衫，春秋季节必备单品。", "299.00", 60, true, "/images/products/cardigan.webp", 1L),
            new ProductData("纯棉袜子", "三双装纯棉袜子，柔软舒适，吸汗透气。", "45.00", 200, true, "/images/products/socks.webp", 1L),
            new ProductData("经典男士牛仔裤", "舒适透气的男士牛仔裤，经典版型，四季可穿。", "299.00", 75, true, "/images/products/denim.webp", 1L),
            new ProductData("法式长袖连衣裙", "法式简约长袖连衣裙，优雅显瘦，适合多种场合。", "389.00", 55, true, "/images/products/dress.webp", 1L),
            new ProductData("户外防晒衣", "超轻薄防晒衣，UPF50+有效防晒，户外运动必备。", "149.00", 150, true, "/images/products/sunscreen.webp", 1L),
            new ProductData("高腰修身直筒裤", "舒适棉麻混纺面料，版型简约，提升日常穿搭质感。", "269.00", 90, true, "/images/products/straight_pants.webp", 1L),
            new ProductData("简约款连帽卫衣", "纯色基础款，内里抓绒，柔软保暖，适合休闲或运动。", "189.00", 120, true, "/images/products/plain_hoodie.webp", 1L),
            new ProductData("轻便小白鞋", "帆布材质，透气舒适，经典百搭款，适合长时间行走。", "159.00", 150, true, "/images/products/canvas_sneakers.webp", 1L),
            new ProductData("羊毛混纺格纹围巾", "柔软亲肤，经典英伦格纹设计，秋冬保暖百搭。", "159.00", 75, true, "/images/products/wool_scarf.webp", 1L),
            new ProductData("轻薄款羽绒背心", "90%白鸭绒填充，轻便保暖，可作为内搭或外穿。", "399.00", 60, true, "/images/products/down_vest.webp", 1L),

            // 分类 2: 家居
            new ProductData("透明收纳盒", "透明收纳盒，整理家居必备。可堆叠设计，节省空间。", "45.00", 200, true, "/images/products/storage.webp", 2L),
            new ProductData("香薰机", "超声波香薰机，营造舒适氛围。静音设计，适合卧室使用。", "299.00", 50, true, "/images/products/diffuser.webp", 2L),
            new ProductData("懒人沙发", "舒适懒人沙发，填充优质颗粒，贴合身体曲线。", "599.00", 30, true, "/images/products/sofa.webp", 2L),
            new ProductData("床品四件套", "纯棉床品四件套，柔软舒适，简约设计。", "399.00", 40, true, "/images/products/bedding.webp", 2L),
            new ProductData("台灯", "简约设计台灯，护眼LED光源，可调节亮度。", "199.00", 70, true, "/images/products/lamp.webp", 2L),
            new ProductData("记忆棉枕头", "高密度记忆棉枕头，有效承托颈部，改善睡眠质量。", "269.00", 65, true, "/images/products/pillow.webp", 2L),
            new ProductData("木质餐具托盘", "日式简约木质托盘，可用于餐点或桌面收纳。", "89.00", 110, true, "/images/products/tray.webp", 2L),
            new ProductData("简约棉签 (200支)", "环保纸轴设计，双头圆润，吸水性强，适合日常清洁及美妆。", "19.90", 300, true, "/images/products/qtip.webp", 2L),
            new ProductData("无痕壁挂式衣架", "简约设计，强力无痕胶，可收纳毛巾或衣物，节省空间。", "25.00", 250, true, "/images/products/wall_hooks.webp", 2L),
            new ProductData("硅藻泥地垫", "快速吸水，脚感舒适，防滑耐用，适合卫生间门口。", "79.00", 110, true, "/images/products/diatomite_mat.webp", 2L),
            new ProductData("可拆卸分格储物箱", "帆布材质，带盖设计，防尘防潮，可用于衣柜或床下。", "59.00", 130, true, "/images/products/divided_storage_box.webp", 2L),
            new ProductData("复古陶瓷花瓶 (中号)", "简约哑光釉面，手工制作，适合搭配干花或鲜花。", "119.00", 85, true, "/images/products/ceramic_vase.webp", 2L),
            new ProductData("香薰无火精油套装", "天然植物精油，清新白茶香型，营造舒适居家氛围。", "89.00", 105, true, "/images/products/reed_diffuser.webp", 2L),

            // 分类 3: 文具
            new ProductData("凝胶墨水笔", "0.5mm 黑色凝胶墨水笔，书写流畅，不易断墨。", "8.00", 500, true, "/images/products/pen.webp", 3L),
            new ProductData("活页笔记本", "A5尺寸活页笔记本，内页可替换，方便整理。", "35.00", 150, true, "/images/products/notebook.webp", 3L),
            new ProductData("文件袋", "透明文件袋，A4尺寸，方便收纳文件。", "15.00", 300, true, "/images/products/folder.webp", 3L),
            new ProductData("便签纸", "彩色便签纸，5色装，方便标记和提醒。", "12.00", 400, true, "/images/products/sticky.webp", 3L),
            new ProductData("牛皮纸笔记本", "复古牛皮纸封面笔记本，内页空白，适合涂鸦创作。", "25.00", 180, true, "/images/products/craftnotebook.webp", 3L),
            new ProductData("四合一多色笔", "按动式多色圆珠笔，一笔多色，方便标记重点。", "18.00", 350, true, "/images/products/multi-pen.webp", 3L),
            new ProductData("迷你订书机", "便携式迷你订书机套装，小巧实用，办公学生必备。", "19.90", 280, true, "/images/products/stapler.webp", 3L),
            new ProductData("原色木质尺子 (15cm)", "榉木材质，刻度清晰，边缘圆润，手感舒适。", "15.00", 300, true, "/images/products/wood_ruler.webp", 3L),
            new ProductData("环保再生纸信封套装", "简约牛皮纸信封，内含同色信纸，适合手写书信。", "29.00", 220, true, "/images/products/kraft_envelope_set.webp", 3L),
            new ProductData("透明亚克力笔筒", "简约圆柱形设计，透明材质，方便查找文具。", "22.00", 190, true, "/images/products/acrylic_pen_holder.webp", 3L),
            new ProductData("点阵方格笔记本 (A5)", "100g道林纸，平铺设计，适用于手帐、子弹笔记或绘图。", "39.00", 280, true, "/images/products/dot_grid_notebook.webp", 3L),
            new ProductData("复古黄铜书签", "叶片造型，精致轻巧，带有流苏装饰。", "25.00", 210, true, "/images/products/brass_bookmark.webp", 3L),
            
            // 分类 4: 零食/食品
            new ProductData("有机绿茶", "有机绿茶，清香淡雅，健康饮品。", "58.00", 100, true, "/images/products/tea.webp", 4L),
            new ProductData("坚果混合装", "多种坚果混合装，营养丰富，适合作为零食。", "68.00", 120, true, "/images/products/nuts.webp", 4L),
            new ProductData("即食燕麦片", "即食燕麦片，营养早餐，方便快捷。", "45.00", 150, true, "/images/products/oats.webp", 4L),
            new ProductData("日式咖喱块", "正宗日式咖喱浓缩速食块，质感浓稠，口味地道。", "149.00", 80, true, "/images/products/gali.webp", 4L),
            new ProductData("阿拉比卡咖啡豆", "新鲜阿拉比卡咖啡豆，中度烘焙，口感醇厚。", "79.00", 90, true, "/images/products/coffeebean.webp", 4L),
            new ProductData("冻干水果片", "混合口味冻干水果片，保留水果原味，健康无添加。", "49.00", 140, true, "/images/products/fruit.webp", 4L),
            new ProductData("全麦面包", "高蛋白低脂全麦面包，代餐首选，健康轻食。", "32.00", 210, true, "/images/products/bread.webp", 4L),
            new ProductData("原味海苔脆片", "非油炸，低卡路里，酥脆可口，健康零食。", "35.00", 160, true, "/images/products/seaweed_snack.webp", 4L),
            new ProductData("日式酱油仙贝", "传统工艺制作，米香浓郁，酱油味咸香酥脆。", "42.00", 100, true, "/images/products/senbei.webp", 4L),
            new ProductData("纯黑芝麻糊 (无糖)", "传统石磨工艺，无添加糖，温和滋补，营养早餐。", "55.00", 130, true, "/images/products/black_sesame_paste.webp", 4L),
            new ProductData("冻干草莓酸奶块", "低温冻干技术，保留水果营养，酸甜酥脆。", "58.00", 145, true, "/images/products/freeze_dried_yogurt.webp", 4L),
            new ProductData("低温烘焙综合坚果", "每日坚果包，无油盐添加，健康营养。", "99.00", 110, true, "/images/products/mixed_nuts.webp", 4L),

            // 分类 5: 美妆个护
            new ProductData("敏感肌化妆水", "温和无添加，适合敏感肌肤使用。", "88.00", 80, true, "/images/products/toner.webp", 5L),
            new ProductData("卸妆油", "温和卸妆油，轻松卸除彩妆，不刺激肌肤。", "98.00", 70, true, "/images/products/cleanser.webp", 5L),
            new ProductData("护手霜", "滋润护手霜，有效保湿，适合秋冬季节。", "45.00", 150, true, "/images/products/handcream.webp", 5L),
            new ProductData("玻尿酸补水面膜", "深层补水保湿面膜，富含玻尿酸精华，密集滋养。", "119.00", 120, true, "/images/products/mask.webp", 5L),
            new ProductData("无硅油洗发水", "天然精油无硅油洗发水，温和清洁头皮，柔顺发丝。", "65.00", 160, true, "/images/products/shampoo.webp", 5L),
            new ProductData("日常防晒乳", "SPF30PA+++清爽防晒乳，轻薄不油腻，日常通勤适用。", "95.00", 130, true, "/images/products/suncream.webp", 5L),
            new ProductData("天然竹炭牙刷套装", "细软竹炭刷毛，环保竹制手柄，简约包装。", "28.00", 200, true, "/images/products/bamboo_toothbrush.webp", 5L),
            new ProductData("无香型身体乳", "适用于全身，质地清爽不油腻，持久保湿。", "75.00", 90, true, "/images/products/unscented_lotion.webp", 5L),
            new ProductData("茶树精油洗面奶", "蕴含茶树精油，温和清洁毛孔，适合油性/混合性肌肤。", "69.00", 110, true, "/images/products/tea_tree_cleanser.webp", 5L),
            new ProductData("便携式迷你卷发棒", "USB充电设计，陶瓷涂层，旅行必备。", "199.00", 65, true, "/images/products/mini_curler.webp", 5L),
            new ProductData("玻尿酸补水面膜套装", "蕴含多种保湿精华，急救干燥肌肤，舒缓修护。", "129.00", 100, true, "/images/products/hyaluronic_mask.webp", 5L),
            
            // 分类 6: 整理收纳
            new ProductData("桌面文具收纳盒", "多功能桌面收纳盒，分格设计，轻松整理文具杂物。", "55.00", 95, true, "/images/products/desktidy.webp", 6L),
            new ProductData("内衣收纳盒", "抽屉式内衣收纳格，防潮防尘，保持衣物整洁。", "39.00", 180, true, "/images/products/drawerbox.webp", 6L),
            new ProductData("可折叠脏衣篮", "可折叠脏衣篮，大容量设计，不使用时可收起。", "75.00", 85, true, "/images/products/laundrybasket.webp", 6L),
            new ProductData("桌面多功能数据线收纳盒", "隐藏式设计，可收纳电源线、数据线及充电器。", "65.00", 125, true, "/images/products/cable_organizer_box.webp", 6L),
            new ProductData("化妆品透明抽屉收纳架", "亚克力材质，多层抽屉，防尘防水，适用于梳妆台。", "135.00", 80, true, "/images/products/cosmetic_drawer.webp", 6L),
            new ProductData("网格沥水收纳篮", "塑料材质，底部网格设计，适用于厨房或浴室的挂式收纳。", "32.00", 140, true, "/images/products/drain_basket.webp", 6L),
            new ProductData("门后多层挂袋", "帆布材质，多层口袋设计，用于鞋子或杂物的门后收纳。", "88.00", 70, true, "/images/products/over_door_organizer.webp", 6L),
            new ProductData("真空压缩袋套装", "适用于衣物和被褥，节省80%空间，旅行居家必备。", "49.00", 150, true, "/images/products/vacuum_compress_bag.webp", 6L),

            // 分类 7: 户外旅行
            new ProductData("旅行收纳袋", "网格旅行收纳袋套装，轻便透气，让行李井井有条。", "45.00", 200, true, "/images/products/travelbag.webp", 7L),
            new ProductData("便携式颈枕", "舒适U型颈枕，慢回弹材质，缓解旅途疲劳。", "299.00", 50, true, "/images/products/neckpillow.webp", 7L),
            new ProductData("旅行分装瓶", "透明旅行分装瓶套装，小巧轻便，符合航空标准。", "599.00", 30, true, "/images/products/travelbottles.webp", 7L),
            new ProductData("折叠旅行包", "轻量可折叠旅行包/箱，大容量设计，方便携带。", "399.00", 40, true, "/images/products/luggage.webp", 7L),
            new ProductData("电子配件收纳包", "电子产品收纳包，多层设计，可放置数据线、充电宝等。", "69.00", 105, true, "/images/products/gadgetbag.webp", 7L),
            new ProductData("多功能旅行魔术头巾", "轻薄透气多功能头巾，可作围巾、发带、面罩等，旅行必备。", "39.00", 150, true, "/images/products/multifunctional_scarf.webp", 7L),
            new ProductData("衣物压缩袋", "可压缩羽绒服收纳袋，节省行李箱空间。", "39.00", 170, true, "/images/products/compressbag.webp", 7L),
            new ProductData("防水手机袋", "透明防水材质，可触屏操作，适合水上活动或雨天旅行。", "35.00", 180, true, "/images/products/waterproof_phone_pouch.webp", 7L),
            new ProductData("便携折叠雨伞", "五折设计，超轻碳纤维骨架，防晒防雨。", "129.00", 95, true, "/images/products/foldable_umbrella.webp", 7L),
            new ProductData("旅行证件收纳包", "多层插袋，可放护照、卡片和登机牌，尼龙耐磨。", "55.00", 115, true, "/images/products/document_pouch.webp", 7L),
            new ProductData("户外轻量化双肩背包 (20L)", "尼龙防水面料，多功能分层设计，适合日常通勤或短途旅行。", "289.00", 55, true, "/images/products/lightweight_backpack.webp", 7L),
            new ProductData("高品质旅行挂锁 (TSA认证)", "锌合金材质，三位密码锁，适用于行李箱和背包，安全可靠。", "45.00", 180, true, "/images/products/tsa_lock.webp", 7L),

            // 分类 8: 厨房餐具
            new ProductData("硅胶刮刀", "优质硅胶刮刀，耐高温，易清洗，烘焙烹饪必备。", "199.00", 70, true, "/images/products/spatula.webp", 8L),
            new ProductData("可堆叠碗碟", "简约白色陶瓷餐具，可堆叠设计，节省收纳空间。", "99.00", 100, true, "/images/products/dishes.webp", 8L),
            new ProductData("木质砧板", "优质榉木砧板，坚固耐用，设计简约，适合日常备菜。", "149.00", 80, true, "/images/products/cuttingboard.webp", 8L),
            new ProductData("滴滤式咖啡壶", "简约滴滤式咖啡壶，玻璃材质，享受手冲咖啡乐趣。", "99.00", 100, true, "/images/products/coffeecarafe.webp", 8L),
            new ProductData("多功能削皮刀", "不锈钢多功能削皮刀，锋利耐用，轻松处理食材。", "22.00", 190, true, "/images/products/peeler.webp", 8L),
            new ProductData("硅胶隔热手套", "硅胶耐高温隔热手套，保护双手，烘焙必备。", "45.00", 125, true, "/images/products/glove.webp", 8L),
            new ProductData("保鲜膜切割器", "食物保鲜膜切割器，内含刀片，方便快捷。", "35.00", 165, true, "/images/products/filmcutter.webp", 8L),
            new ProductData("食品级硅胶保鲜盖", "多尺寸一套，可重复使用，替代保鲜膜。", "49.00", 135, true, "/images/products/silicone_lids.webp", 8L),
            new ProductData("不锈钢量勺套装", "五件套，刻度清晰，适用于烘焙和烹饪。", "28.00", 160, true, "/images/products/measuring_spoons.webp", 8L),
            new ProductData("日式竹制筷子筒", "镂空设计，透气沥水，简约自然的餐具收纳。", "65.00", 90, true, "/images/products/bamboo_chopstick_holder.webp", 8L),
            new ProductData("日式陶瓷手柄马克杯", "大容量设计，哑光釉面，适合饮用咖啡或热饮。", "55.00", 115, true, "/images/products/ceramic_mug.webp", 8L),
            new ProductData("硅胶隔热防烫手套", "加厚设计，耐高温，带有挂孔，方便收纳。", "38.00", 130, true, "/images/products/silicone_oven_mitt.webp", 8L)
        );

        // 3. 批量创建商品
        List<Product> productsToSave = new ArrayList<>();
        int missingCategoryCount = 0;

        for (ProductData data : dataList) {
            Category category = categoryMap.get(data.categoryId);
            if (category == null) {
                log.error("创建商品失败：未找到 Category ID {}", data.categoryId);
                missingCategoryCount++;
                continue;
            }
            
            // 假设 SQL 中 0x01 表示 true (上架)
            Product product = Product.builder()
                    .name(data.name)
                    .description(data.description)
                    .price(new BigDecimal(data.price))
                    .stock(data.stock)
                    .isAvailable(data.isAvailable) 
                    .imageUrl(data.imageUrl)
                    .category(category)
                    .merchant(merchant)
                    .build();
            productsToSave.add(product);
        }

        if (!productsToSave.isEmpty()) {
            productRepository.saveAll(productsToSave);
            log.info("已为商户 {} 成功创建 {} 个测试商品。", merchant.getUsername(), productsToSave.size());
        }
        if (missingCategoryCount > 0) {
            log.error("由于缺少分类，未能创建 {} 个商品。请检查 CategoryInitializer。", missingCategoryCount);
        }
    }

    // 辅助类用于封装商品数据
    private static class ProductData {
        String name;
        String description;
        String price;
        int stock;
        boolean isAvailable;
        String imageUrl;
        Long categoryId;

        public ProductData(String name, String description, String price, int stock, boolean isAvailable, String imageUrl, Long categoryId) {
            this.name = name;
            this.description = description;
            this.price = price;
            this.stock = stock;
            this.isAvailable = isAvailable;
            this.imageUrl = imageUrl;
            this.categoryId = categoryId;
        }
    }
}