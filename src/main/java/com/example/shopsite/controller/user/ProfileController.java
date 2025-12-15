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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

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

    @PostMapping("/email")
    @Transactional
    public String updateEmail(@RequestParam String email,
            Authentication authentication,
            RedirectAttributes ra) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        Optional<User> userOpt = userRepository.findByUsername(authentication.getName());
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }
        User user = userOpt.get();
        String newEmail = email == null ? "" : email.trim();
        if (newEmail.isEmpty()) {
            ra.addFlashAttribute("error", "邮箱不能为空");
            return "redirect:/profile";
        }
        Optional<User> existing = userRepository.findByEmail(newEmail);
        if (existing.isPresent() && !existing.get().getId().equals(user.getId())) {
            ra.addFlashAttribute("error", "邮箱已被使用");
            return "redirect:/profile";
        }
        user.setEmail(newEmail);
        userRepository.save(user);
        ra.addFlashAttribute("success", "邮箱已更新");
        return "redirect:/profile";
    }

    @PostMapping("/delete")
    @Transactional
    public String deleteAccount(Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes ra) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        Optional<User> userOpt = userRepository.findByUsername(authentication.getName());
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }
        User user = userOpt.get();
        userRepository.delete(user);
        // 主动登出
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        ra.addFlashAttribute("success", "账户已注销");
        return "redirect:/";
    }
}
