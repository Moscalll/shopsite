package com.example.shopsite.controller.user;

import com.example.shopsite.model.Message;
import com.example.shopsite.model.User;
import com.example.shopsite.repository.MessageRepository;
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

    public MessageController(MessageRepository messageRepository, 
                           UserRepository userRepository,
                           MessageService messageService) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.messageService = messageService;
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
        List<Message> messages = messageRepository.findByUserOrderByCreateTimeDesc(user);
        Long unreadCount = messageRepository.countByUserAndIsReadFalse(user);

        model.addAttribute("messages", messages);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("pageTitle", "消息中心");
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