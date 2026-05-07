package com.example.shopsite.controller.user;

import com.example.shopsite.model.Message;
import com.example.shopsite.model.Order;
import com.example.shopsite.model.OrderStatus;
import com.example.shopsite.model.User;
import com.example.shopsite.repository.MessageRepository;
import com.example.shopsite.repository.OrderRepository;
import com.example.shopsite.repository.UserRepository;
import com.example.shopsite.service.MessageService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/message")
public class MessageController {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final MessageService messageService;
    private final OrderRepository orderRepository;

    public MessageController(MessageRepository messageRepository, 
                           UserRepository userRepository,
                           MessageService messageService,
                           OrderRepository orderRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.messageService = messageService;
        this.orderRepository = orderRepository;
    }

    @GetMapping
    public String messagePage(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }

        User user = userOpt.get();
        if (user.getRole() != null && user.getRole().name().equals("MERCHANT")) {
            // 商户：消息中心展示订单待办（不使用 Message 表）
            List<Order> orders = orderRepository.findOrdersByMerchant(user);
            List<Order> pendingShip = orders.stream().filter(o -> o.getStatus() == OrderStatus.PROCESSING).toList();
            List<Order> pendingDeliver = orders.stream().filter(o -> o.getStatus() == OrderStatus.SHIPPED).toList();
            List<Order> pendingConfirm = orders.stream().filter(o -> o.getStatus() == OrderStatus.PENDING_PAYMENT).toList();

            model.addAttribute("pendingShip", pendingShip);
            model.addAttribute("pendingDeliver", pendingDeliver);
            model.addAttribute("pendingConfirm", pendingConfirm);
            model.addAttribute("pendingShipCount", pendingShip.size());
            model.addAttribute("pendingDeliverCount", pendingDeliver.size());
            model.addAttribute("pendingConfirmCount", pendingConfirm.size());
            model.addAttribute("pageTitle", "消息中心");
            model.addAttribute("mode", "merchant");
            return "user/message";
        }

        List<Message> messages = messageRepository.findByUserOrderByCreateTimeDesc(user);
        Long unreadCount = messageRepository.countByUserAndIsReadFalse(user);

        model.addAttribute("messages", messages);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("pageTitle", "消息中心");
        model.addAttribute("mode", "user");
        return "user/message";
    }

    @PostMapping("/{id}/read")
    public String markAsRead(@PathVariable Long id, 
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }

        try {
            messageService.markAsRead(id, userOpt.get());
            redirectAttributes.addFlashAttribute("success", "消息已标记为已读");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/message";
    }

    @PostMapping("/read-all")
    public String markAllAsRead(Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }

        messageService.markAllAsRead(userOpt.get());
        redirectAttributes.addFlashAttribute("success", "所有消息已标记为已读");
        return "redirect:/message";
    }
}