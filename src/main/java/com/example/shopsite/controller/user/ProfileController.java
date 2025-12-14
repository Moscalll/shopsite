package com.example.shopsite.controller.user;

import com.example.shopsite.model.User;
import com.example.shopsite.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserRepository userRepository;

    public ProfileController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * GET /profile - 个人中心（根据角色显示不同内容）
     */
    @GetMapping
    public String profile(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }

        User user = userOpt.get();
        model.addAttribute("user", user);
        model.addAttribute("pageTitle", "个人中心");

        // 根据角色返回不同的模板
        switch (user.getRole()) {
            case ADMIN:
                return "admin/profile";
            case MERCHANT:
                return "merchant/profile";
            case CUSTOMER:
            default:
                return "user/profile";
        }
    }
}



