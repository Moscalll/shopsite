package com.example.shopsite.controller.user;

import com.example.shopsite.model.SalesLog;
import com.example.shopsite.model.User;
import com.example.shopsite.repository.SalesLogRepository;
import com.example.shopsite.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserRepository userRepository;
    private final SalesLogRepository salesLogRepository;

    public ProfileController(UserRepository userRepository, SalesLogRepository salesLogRepository) {
        this.userRepository = userRepository;
        this.salesLogRepository = salesLogRepository;
    }

    /**
     * GET /profile - 个人中心（根据角色显示不同内容）
     */
    @GetMapping
    public String profile(@RequestParam(required = false) String actionType,
                         Authentication authentication, 
                         Model model) {
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

        // 如果是客户，查询其浏览/购买日志
        if (user.getRole().name().equals("CUSTOMER")) {
            List<SalesLog> logs;
            if (actionType != null && !actionType.isEmpty()) {
                logs = salesLogRepository.findByUserAndActionType(user, actionType);
            } else {
                logs = salesLogRepository.findByUser(user);
            }
            
            // 按时间倒序排列
            logs.sort((a, b) -> b.getLogTime().compareTo(a.getLogTime()));
            
            model.addAttribute("logs", logs);
            model.addAttribute("actionType", actionType);
        }

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
