package com.example.shopsite.controller;

import com.example.shopsite.dto.UserLoginDto;
import com.example.shopsite.dto.UserRegistrationRequest;
import com.example.shopsite.model.User;
import com.example.shopsite.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth") // 所有认证相关接口前缀
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * POST /api/auth/register
     * 处理用户注册请求
     */
    @PostMapping("/register")
    // 使用 @Valid 触发 DTO 中的校验注解 (@NotBlank, @Size, @Email)
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegistrationRequest request) {
        try {
            User registeredUser = userService.registerUser(request);
            // 注册成功，返回 201 Created 状态码和成功信息
            return new ResponseEntity<>("用户注册成功, 用户名: " + registeredUser.getUsername(), HttpStatus.CREATED);
        } catch (RuntimeException e) {
            // 注册失败（如用户名或邮箱已存在），返回 400 Bad Request
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


    /**
     * POST /api/auth/login
     * 处理用户登录请求
     * 接收 UserLoginDto，调用 Service 层处理认证
     */
    @PostMapping("/login")
    // 💡 登录时不需要 @Valid，因为密码校验通常在 Service/Security 层处理
    public ResponseEntity<?> authenticateUser(@RequestBody UserLoginDto loginRequest) {
        try {
            // 🚨 核心：调用 UserService 中的登录方法。我们稍后实现这个方法。
            String jwtToken = userService.authenticateUser(loginRequest);
            
            // 登录成功，返回 200 OK，并在响应体中返回 JWT/Token
            // 实际项目中，你需要返回一个包含 token 的 JSON 对象
            return new ResponseEntity<>("登录成功，Token: " + jwtToken, HttpStatus.OK);

        } catch (RuntimeException e) {
            // 登录失败（用户名不存在或密码错误），返回 401 Unauthorized
            // 注意：登录失败时，为了安全，应返回 401 或 400，但信息应模糊。
            return new ResponseEntity<>(e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }
}