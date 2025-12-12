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
        model.addAttribute("pageTitle", "商城首页");
        return "layout/main";
    }

    // 帮助页面
    @GetMapping("/help")
    public String helpPage(Model model) {
        model.addAttribute("pageTitle", "帮助中心");
        // 假设帮助模板路径是 utility/help.html
        ///model.addAttribute("content", "utility/help :: body"); 
        return "layout/main"; 
    }

// 关于页面
    @GetMapping("/about")
    public String aboutPage(Model model) {
        model.addAttribute("pageTitle", "关于我们");
        // 假设关于模板路径是 utility/about.html
        //model.addAttribute("content", "utility/about :: body"); 
        return "layout/main"; 
    }

    // 消息中心（需要登录，未登录 Spring Security 会跳转到 /login）
    @GetMapping("/message")
    public String messagePage(Model model) {
        model.addAttribute("pageTitle", "消息中心");
       // model.addAttribute("content", "user/message :: body"); 
        return "layout/main"; 
    }

    // 收藏夹（需要登录）
    @GetMapping("/favorite")
    public String favoritePage(Model model) {
        model.addAttribute("pageTitle", "我的收藏");
        //model.addAttribute("content", "user/favorite :: body"); 
        return "layout/main"; 
    }
}