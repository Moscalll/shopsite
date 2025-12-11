// package com.service.impl;

// import com.example.shopsite.dto.OrderCreationRequest;
// import com.example.shopsite.dto.OrderItemRequest;
// import com.example.shopsite.exception.BusinessException;
// import com.example.shopsite.model.*;
// import com.example.shopsite.repository.*;
// import com.example.shopsite.service.impl.OrderServiceImpl;

// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;

// import java.math.BigDecimal;
// import java.util.Collections;
// import java.util.List;
// import java.util.Optional;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class)
// public class OrderServiceImplTest {

//     // 注入 Mockito 实例
//     @Mock
//     private OrderRepository orderRepository;
//     @Mock
//     private OrderItemRepository orderItemRepository;
//     @Mock
//     private ProductRepository productRepository;
//     @Mock
//     private UserRepository userRepository;

//     // 将 Mock 实例注入到 Service 实现类中
//     @InjectMocks
//     private OrderServiceImpl orderService;

//     // 定义通用测试数据
//     private User customer;
//     private Product availableProduct;
//     private OrderCreationRequest validRequest;

//     @BeforeEach
//     void setUp() {
//         // 初始化测试用的用户
//         customer = User.builder()
//                 .id(2L).username("testuser").role(Role.CUSTOMER).build();

//         // 初始化测试用的商品
//         availableProduct = Product.builder()
//                 .id(10L)
//                 .name("测试商品")
//                 .price(new BigDecimal("100.00"))
//                 .stock(10)
//                 .isAvailable(true)
//                 .build();

//         // 初始化有效的订单请求DTO
//         OrderItemRequest itemRequest = new OrderItemRequest(10L, 3);
//         validRequest = new OrderCreationRequest(Collections.singletonList(itemRequest));
//     }

//     // ... (OrderServiceImplTest.java)

//     @Test
//     void createOrder_shouldSucceedAndDeductStock() {
//         // 1. Mock 行为：当 Service 调用 Repository 时，返回我们预设的测试数据
//         // Mock 用户查找
//         when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(customer));
//         // Mock 商品查找
//         when(productRepository.findById(10L)).thenReturn(Optional.of(availableProduct));
//         // Mock 订单保存（返回相同的订单对象，以便后续设置 OrderItem）
//         when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArguments()[0]);

//         // 2. 调用被测方法
//         Order createdOrder = orderService.createOrder(validRequest, "testuser");

//         // 3. 验证结果 (Assertions)

//         // 验证订单总价是否正确: 100.00 * 3 = 300.00
//         assertEquals(new BigDecimal("300.00"), createdOrder.getTotalAmount(), "订单总价应计算正确");
//         // 验证订单状态是否正确
//         assertEquals(OrderStatus.PENDING_PAYMENT, createdOrder.getStatus(), "订单状态应为待支付");
//         // 验证订单项数量
//         assertEquals(1, createdOrder.getItems().size(), "订单应包含一个订单项");

//         // 4. 验证 Mockito 交互 (Verification)

//         // 验证商品库存是否被正确扣减 (10 - 3 = 7) 并保存
//         assertEquals(7, availableProduct.getStock(), "商品库存应被正确扣减");
//         verify(productRepository, times(1)).save(availableProduct); // 验证库存扣减后，商品被保存
//         // 验证订单和订单项被保存
//         verify(orderRepository, times(1)).save(any(Order.class));
//         verify(orderItemRepository, times(1)).save(any(OrderItem.class)); 
//     }


//     // ... (OrderServiceImplTest.java)

//     @Test
//     void createOrder_shouldThrowExceptionIfStockInsufficient() {
//         // 1. 准备数据：将请求数量改为 11，超过库存 10
//         OrderItemRequest overstockRequest = new OrderItemRequest(10L, 11);
//         OrderCreationRequest invalidRequest = new OrderCreationRequest(Collections.singletonList(overstockRequest));
        
//         // 2. Mock 行为
//         when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(customer));
//         when(productRepository.findById(10L)).thenReturn(Optional.of(availableProduct));

//         // 3. 验证是否抛出 BusinessException
//         BusinessException exception = assertThrows(BusinessException.class, () -> {
//             orderService.createOrder(invalidRequest, "testuser");
//         });

//         // 4. 验证异常信息和 Mockito 交互
//         assertTrue(exception.getMessage().contains("库存不足"), "异常信息应包含'库存不足'");
//         // 验证库存没有被扣减或保存（因为事务应该失败）
//         verify(productRepository, never()).save(availableProduct);
//         verify(orderRepository, never()).save(any(Order.class));
//     }
// }