package com.example.shopsite.interceptor;

import com.example.shopsite.model.User;
import com.example.shopsite.model.Order;
import com.example.shopsite.model.OrderStatus;
import com.example.shopsite.repository.MessageRepository;
import com.example.shopsite.repository.OrderRepository;
import com.example.shopsite.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.Optional;

@Component
public class MessageInterceptor implements HandlerInterceptor {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public MessageInterceptor(MessageRepository messageRepository, UserRepository userRepository, OrderRepository orderRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, 
                         Object handler, ModelAndView modelAndView) throws Exception {
        if (modelAndView != null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && 
                !authentication.getName().equals("anonymousUser")) {
                try {
                    Optional<User> userOpt = userRepository.findByUsername(authentication.getName());
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        Long unreadCount;
                        if (user.getRole() != null && user.getRole().name().equals("MERCHANT")) {
                            // 商户：顶栏红点显示“待发货/待送达/待确认”订单总数
                            java.util.List<Order> orders = orderRepository.findOrdersByMerchant(user);
                            long pendingShip = orders.stream().filter(o -> o.getStatus() == OrderStatus.PROCESSING).count();
                            long pendingDeliver = orders.stream().filter(o -> o.getStatus() == OrderStatus.SHIPPED).count();
                            long pendingConfirm = orders.stream().filter(o -> o.getStatus() == OrderStatus.PENDING_PAYMENT).count();
                            unreadCount = pendingShip + pendingDeliver + pendingConfirm;
                        } else {
                            unreadCount = messageRepository.countByUserAndIsReadFalse(user);
                        }
                        modelAndView.addObject("unreadMessageCount", unreadCount);
                    }
                } catch (Exception e) {
                    // 忽略错误，不影响页面渲染
                }
            } else {
                modelAndView.addObject("unreadMessageCount", 0L);
            }
        }
    }
}