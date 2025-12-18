package com.example.shopsite.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    /**
     * GET /api/users/me
     * 访问此接口需要有效的JWT。
     * 假设这是一个需要基础用户权限的接口。
     */
    @GetMapping("/me")
    // Controller 级别不限制权限，让 SecurityConfig 统一处理
    public ResponseEntity<String> getCurrentUser() {
        // 成功访问，意味着JWT过滤器和授权检查通过。
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        
        // 打印用户的权限，用于调试角色问题
        String roles = SecurityContextHolder.getContext().getAuthentication().getAuthorities().toString();

        return ResponseEntity.ok("访问成功! 当前登录用户: " + username + ", 权限: " + roles);
    }
}