package com.example.shopsite.controller.user;

import com.example.shopsite.model.Role;
import com.example.shopsite.model.User;
import com.example.shopsite.model.UserBehaviorLog;
import com.example.shopsite.repository.AdminOperationLogRepository;
import com.example.shopsite.repository.AuthLoginLogRepository;
import com.example.shopsite.repository.UserBehaviorLogRepository;
import com.example.shopsite.repository.UserRepository;
import com.example.shopsite.support.CategoryLabelService;
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
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserRepository userRepository;
    private final UserBehaviorLogRepository userBehaviorLogRepository;
    private final CategoryLabelService categoryLabelService;
    private final AuthLoginLogRepository authLoginLogRepository;
    private final AdminOperationLogRepository adminOperationLogRepository;

    public ProfileController(UserRepository userRepository,
                             UserBehaviorLogRepository userBehaviorLogRepository,
                             CategoryLabelService categoryLabelService,
                             AuthLoginLogRepository authLoginLogRepository,
                             AdminOperationLogRepository adminOperationLogRepository) {
        this.userRepository = userRepository;
        this.userBehaviorLogRepository = userBehaviorLogRepository;
        this.categoryLabelService = categoryLabelService;
        this.authLoginLogRepository = authLoginLogRepository;
        this.adminOperationLogRepository = adminOperationLogRepository;
    }

    /**
     * GET /profile - 个人中心（根据角色显示不同内容）
     */
    @GetMapping
    public String profile(Authentication authentication,
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

        Role role = user.getRole();
        if (role == Role.CUSTOMER && user.getId() != null) {
            List<UserBehaviorLog> behaviorLogs = userBehaviorLogRepository.findByUser_IdOrderByEventTimeDesc(user.getId());
            Map<Long, String> categoryNames = categoryLabelService.labelsForBehaviorLogs(behaviorLogs);
            model.addAttribute("behaviorLogs", behaviorLogs);
            model.addAttribute("categoryNames", categoryNames);
        }

        if ((role == Role.MERCHANT || role == Role.ADMIN) && user.getId() != null) {
            model.addAttribute("loginLogs", authLoginLogRepository.findByUser_IdOrderByLoginTimeDesc(user.getId()));
            model.addAttribute("operationLogs",
                    adminOperationLogRepository.findByOperator_IdOrderByOperationTimeDesc(user.getId()));
        }

        // 根据角色返回不同的模板
        switch (role) {
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
