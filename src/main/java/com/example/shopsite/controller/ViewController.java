package com.example.shopsite.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model; // 🚨 导入 Model

@Controller
public class ViewController {

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("pageTitle", "用户登录");
        return "auth/login"; // 对应 templates/auth/login.html
    }
    
    // 假设你有一个注册页面
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("pageTitle", "用户注册");
        return "auth/register"; // 对应 templates/auth/register.html
    }

    @GetMapping("/")
    public String indexPage(Model model) {
        // ... (首页逻辑，返回 "index" 或 "product/list")
        return "layout/main"; 
    }
}