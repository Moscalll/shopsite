package com.example.shopsite.service.impl;

import com.example.shopsite.dto.OrderCreationRequest;
import com.example.shopsite.dto.OrderItemRequest;
import com.example.shopsite.model.*;
import com.example.shopsite.repository.OrderItemRepository;
import com.example.shopsite.repository.OrderRepository;
import com.example.shopsite.repository.ProductRepository;
import com.example.shopsite.repository.UserRepository;
import com.example.shopsite.service.OrderService;
import com.example.shopsite.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.example.shopsite.service.CartService;
import com.example.shopsite.model.CartItem;
import com.example.shopsite.service.MessageService;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository; 
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartService cartService; // 添加 CartService
    private final MessageService messageService;

    public OrderServiceImpl(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
            ProductRepository productRepository, UserRepository userRepository, CartService cartService,
            MessageService messageService) { // 添加
        // CartService
        // 参数
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.cartService = cartService; // 添加赋值
        this.messageService = messageService;
    }

    @Override
    @Transactional // 确保订单创建、库存扣减在同一事务中
    public Order createOrder(OrderCreationRequest request, String username) {

        // 1. 查找用户
        User customer = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在或未登录"));

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        // 2. 创建订单实体（先占位）
        Order newOrder = Order.builder()
                .user(customer)
                .orderDate(LocalDateTime.now())
                .status(OrderStatus.PENDING_PAYMENT)
                .items(new ArrayList<>()) // 初始化列表
                .build();

        // 3. 处理订单项和库存
        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new RuntimeException("商品ID: " + itemRequest.getProductId() + " 不存在"));

            if (!product.getIsAvailable() || product.getStock() < itemRequest.getQuantity()) {
                throw new BusinessException("商品 " + product.getName() + " 库存不足或已下架");
            }

            // 1. 将购买数量 (Integer) 转换为 BigDecimal
            BigDecimal quantityBd = new BigDecimal(itemRequest.getQuantity());

            // 计算单项总价
            BigDecimal itemPrice = product.getPrice().multiply(quantityBd); // 使用 multiply 方法

            // 3. 累加到订单总价: totalAmount = totalAmount.add(itemPrice)
            totalAmount = totalAmount.add(itemPrice); // 使用 add 方法

            // 扣减库存（关键的业务操作）
            product.setStock(product.getStock() - itemRequest.getQuantity());
            productRepository.save(product); // 立即更新库存

            // 创建订单项
            OrderItem orderItem = OrderItem.builder()
                    .order(newOrder) // 关联到新的订单实体
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .priceAtOrder(product.getPrice()) // 记录购买时的价格
                    .build();

            orderItems.add(orderItem);
        }

        // 4. 更新订单总价和订单项关联
        newOrder.setTotalAmount(totalAmount);

        // 5. 保存订单和订单项 (JPA的CascadeType.ALL会帮助保存)
        Order savedOrder = orderRepository.save(newOrder);

        // 确保订单项关联到已保存的订单
        for (OrderItem item : orderItems) {
            item.setOrder(savedOrder);
            orderItemRepository.save(item);
        }

        savedOrder.setItems(orderItems); // 重新设置 Items 列表以返回完整的对象
        return savedOrder;
    }

    @Override
    @Transactional
    public Order createOrderFromCart(User user) {
        // 从购物车获取商品
        List<CartItem> cartItems = cartService.getCartItems(user);

        if (cartItems.isEmpty()) {
            throw new BusinessException("购物车为空，无法创建订单");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        // 创建订单实体
        Order newOrder = Order.builder()
                .user(user)
                .orderDate(LocalDateTime.now())
                .status(OrderStatus.PENDING_PAYMENT)
                .items(new ArrayList<>())
                .build();

        // 处理购物车中的每个商品
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            Integer quantity = cartItem.getQuantity();

            if (!product.getIsAvailable() || product.getStock() < quantity) {
                throw new BusinessException("商品 " + product.getName() + " 库存不足或已下架");
            }

            // 计算单项总价
            BigDecimal quantityBd = BigDecimal.valueOf(quantity);
            BigDecimal itemPrice = product.getPrice().multiply(quantityBd);
            totalAmount = totalAmount.add(itemPrice);

            // 扣减库存
            product.setStock(product.getStock() - quantity);
            productRepository.save(product);

            // 创建订单项
            OrderItem orderItem = OrderItem.builder()
                    .order(newOrder)
                    .product(product)
                    .quantity(quantity)
                    .priceAtOrder(product.getPrice())
                    .build();

            orderItems.add(orderItem);
        }

        // 更新订单总价
        newOrder.setTotalAmount(totalAmount);

        // 保存订单
        Order savedOrder = orderRepository.save(newOrder);

        // 保存订单项
        for (OrderItem item : orderItems) {
            item.setOrder(savedOrder);
            orderItemRepository.save(item);
        }

        savedOrder.setItems(orderItems);
        return savedOrder;
    }

    @Override
    public List<Order> findMyOrders(String username) {
        User customer = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 直接使用 OrderRepository 中定义的 findByUser 方法
        return orderRepository.findByUser(customer);
    }

    @Override
    @Transactional(readOnly = true) // 只读事务，提高性能
    public Order findOrderDetails(Long orderId, String username) {
        User customer = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 1. 查找订单
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在: " + orderId));

        // 2. 权限校验：确保当前用户是订单所有者
        if (!order.getUser().getId().equals(customer.getId())) {
            throw new RuntimeException("无权访问该订单");
        }

        // 3. 强制加载订单项（如果 Order.java 仍是 Lazy 加载）
        // 如果你在 Order.java 中将 items 设为了 Eager，则不需要这行
        order.getItems().size();

        return order;
    }

    @Override
    @Transactional
    public Order processPayment(Long orderId, String username) {
        // 1. 查找用户
        User customer = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 2. 查找订单
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在: " + orderId));

        // 3. 权限校验
        if (!order.getUser().getId().equals(customer.getId())) {
            throw new RuntimeException("无权处理该订单的支付");
        }

        // 4. 状态校验：只允许 PENDING_PAYMENT 状态的订单进行支付
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException("订单状态不允许支付: " + order.getStatus());
        }

        // 5. 模拟支付成功，更新状态
        order.setStatus(OrderStatus.PROCESSING);

        // 6. 保存更新
        return orderRepository.save(order);
    }

    @Override
    public List<Order> findAllOrders() {
        // 简单地返回所有订单。权限控制在 Controller 层完成。
        return orderRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findOrdersByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAllOrders();
        }
        return orderRepository.findOrdersByKeyword(keyword.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public Order findOrderDetailsForAdmin(Long orderId) {
        // 1. 查找订单
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在: " + orderId));

        // 2. 强制加载订单项（如果 Order.java 仍是 Lazy 加载）
        order.getItems().size();

        // 权限校验在 Controller 层完成，这里只负责返回数据
        return order;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findOrdersByMerchant(User merchant) {
        return orderRepository.findOrdersByMerchant(merchant);
    }

    @Override
    @Transactional(readOnly = true)
    public Order findOrderDetailsForMerchant(Long orderId, User merchant) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在: " + orderId));

        // 验证订单是否包含该商户的商品
        boolean hasMerchantProduct = order.getItems().stream()
                .anyMatch(item -> item.getProduct().getMerchant().getId().equals(merchant.getId()));

        if (!hasMerchantProduct) {
            throw new RuntimeException("无权访问该订单");
        }

        // 强制加载订单项
        order.getItems().size();
        return order;
    }

    @Override
    @Transactional
    public Order shipOrder(Long orderId, User merchant) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在: " + orderId));

        // 验证订单是否包含该商户的商品
        boolean hasMerchantProduct = order.getItems().stream()
                .anyMatch(item -> item.getProduct().getMerchant().getId().equals(merchant.getId()));

        if (!hasMerchantProduct) {
            throw new RuntimeException("无权处理该订单");
        }

        // 只有已付款的订单才能发货
        if (order.getStatus() != OrderStatus.PROCESSING) {
            throw new BusinessException("订单状态不允许发货: " + order.getStatus());
        }

        order.setStatus(OrderStatus.SHIPPED);
        Order savedOrder = orderRepository.save(order);

        // 创建消息通知客户
        String messageContent = String.format("您的订单 #%d 已发货，请注意查收！", savedOrder.getId());
        messageService.createMessage(savedOrder.getUser(), messageContent, savedOrder.getId());

        return savedOrder;
    }

    @Override
    @Transactional
    public Order updateOrder(Long orderId, User merchant, Order updatedOrder) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在: " + orderId));

        // 验证订单是否包含该商户的商品
        boolean hasMerchantProduct = order.getItems().stream()
                .anyMatch(item -> item.getProduct().getMerchant().getId().equals(merchant.getId()));

        if (!hasMerchantProduct) {
            throw new RuntimeException("无权修改该订单");
        }

        // 更新订单状态
        if (updatedOrder.getStatus() != null) {
            order.setStatus(updatedOrder.getStatus());
        }

        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order cancelOrderByCustomer(Long orderId, String username) {
        User customer = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在: " + orderId));

        // 权限校验：确保当前用户是订单所有者
        if (!order.getUser().getId().equals(customer.getId())) {
            throw new RuntimeException("无权取消该订单");
        }

        // 客户可以取消任意状态的订单（除了已取消、已完成和已发货的）
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("订单已取消");
        }
        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new BusinessException("已完成的订单不能取消");
        }
        if (order.getStatus() == OrderStatus.SHIPPED ||
                order.getStatus() == OrderStatus.DELIVERED) {
            throw new BusinessException("已发货的订单不能取消");
        }

        // 如果订单已付款，需要恢复库存
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            // 恢复库存
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                product.setStock(product.getStock() + item.getQuantity());
                productRepository.save(product);
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }
}