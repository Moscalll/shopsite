package com.example.shopsite.controller;

import com.example.shopsite.dto.UserRegistrationRequest;
import com.example.shopsite.model.Role;
import com.example.shopsite.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ViewController {

    private final UserService userService;

    public ViewController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("pageTitle", "用户登录");
        return "auth/login"; // 对应 templates/auth/login.html
    }

    // 注册页面
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("pageTitle", "用户注册");
        return "auth/register"; // 对应 templates/auth/register.html
    }

    // 处理注册表单提交
    @PostMapping("/register")
    public String handleRegister(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            @RequestParam String role,
            RedirectAttributes redirectAttributes) {

        // 验证密码
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("registrationError", "两次输入的密码不一致");
            return "redirect:/register";
        }

        if (password.length() < 6) {
            redirectAttributes.addFlashAttribute("registrationError", "密码长度至少需要6个字符");
            return "redirect:/register";
        }

        // 验证角色
        Role userRole;
        try {
            userRole = Role.valueOf(role.toUpperCase());
            if (userRole == Role.ADMIN) {
                redirectAttributes.addFlashAttribute("registrationError", "不允许注册管理员账户");
                return "redirect:/register";
            }
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("registrationError", "无效的角色");
            return "redirect:/register";
        }

        // 创建注册请求
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword(password);

        try {
            userService.registerUser(request, userRole);
            redirectAttributes.addFlashAttribute("registrationSuccess", true);
            return "redirect:/login";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("registrationError", e.getMessage());
            return "redirect:/register";
        }
    }

    // 帮助页面
    @GetMapping("/help")
    public String helpPage(Model model) {
        model.addAttribute("pageTitle", "帮助中心");
        return "layout/help";
    }

    // 关于页面
    @GetMapping("/about")
    public String aboutPage(Model model) {
        model.addAttribute("pageTitle", "关于我们");
        return "layout/about";
    }


}