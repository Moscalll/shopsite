package com.example.shopsite.controller.merchant;

import com.example.shopsite.model.Order;
import com.example.shopsite.model.OrderStatus;
import com.example.shopsite.model.User;
import com.example.shopsite.repository.UserRepository;
import com.example.shopsite.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/merchant/messages")
@PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
public class MerchantMessageCenterController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    public MerchantMessageCenterController(OrderService orderService, UserRepository userRepository) {
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String messageCenter(Model model) {
        // 商户消息中心统一使用顶栏入口 /message
        return "redirect:/message";
    }
}

