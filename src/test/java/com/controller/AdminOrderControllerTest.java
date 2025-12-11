// package com.controller;

// import com.example.shopsite.model.Order;
// import com.example.shopsite.service.OrderService;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.boot.test.mock.mockito.MockBean;
// import org.springframework.security.test.context.support.WithMockUser;
// import org.springframework.test.web.servlet.MockMvc;

// import java.util.Collections;

// import static org.mockito.Mockito.when;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// @SpringBootTest // 加载完整的 Spring Context
// @AutoConfigureMockMvc // 配置 MockMvc 实例
// public class AdminOrderControllerTest {

//     @Autowired
//     private MockMvc mockMvc;

//     // 使用 @MockBean 模拟 OrderService，隔离数据库和业务逻辑
//     @MockBean
//     private OrderService orderService;

//     @Test
//     @WithMockUser(authorities = {"ROLE_ADMIN"}) // 模拟 ADMIN 角色
//     void getAllOrders_shouldAllowAdmin() throws Exception {
//         // Mock service 行为
//         when(orderService.findAllOrders()).thenReturn(Collections.emptyList());

//         mockMvc.perform(get("/api/admin/orders"))
//                 .andExpect(status().isOk()); // 期望 200 OK
//     }

//     @Test
//     @WithMockUser(authorities = {"ROLE_MERCHANT"}) // 模拟 MERCHANT 角色
//     void getAllOrders_shouldAllowMerchant() throws Exception {
//         // Mock service 行为
//         when(orderService.findAllOrders()).thenReturn(Collections.emptyList());

//         mockMvc.perform(get("/api/admin/orders"))
//                 .andExpect(status().isOk()); // 期望 200 OK
//     }

//     @Test
//     @WithMockUser(authorities = {"ROLE_CUSTOMER"}) // 模拟 CUSTOMER 角色
//     void getAllOrders_shouldDenyCustomer() throws Exception {
//         mockMvc.perform(get("/api/admin/orders"))
//                 .andExpect(status().isForbidden()); // 期望 403 Forbidden
//     }
    
//     @Test
//     void getAllOrders_shouldDenyUnauthenticated() throws Exception {
//         // 不使用 @WithMockUser，模拟未认证用户
//         mockMvc.perform(get("/api/admin/orders"))
//                 .andExpect(status().isUnauthorized()); // 期望 401 Unauthorized
//     }
// }