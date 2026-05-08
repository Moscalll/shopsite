package com.example.shopsite.controller;

import com.example.shopsite.dto.ApiMessageResponse;
import com.example.shopsite.dto.LoginResponse;
import com.example.shopsite.dto.UserLoginDto;
import com.example.shopsite.dto.UserRegistrationRequest;
import com.example.shopsite.security.JwtTokenProvider;
import com.example.shopsite.model.AuthLoginLog;
import com.example.shopsite.model.User;
import com.example.shopsite.repository.UserRepository;
import com.example.shopsite.support.ClientIpExtractor;
import com.example.shopsite.service.AuthLoginLogService;
import com.example.shopsite.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth") // 所有认证相关接口前缀
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final AuthLoginLogService authLoginLogService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(UserService userService,
                          AuthLoginLogService authLoginLogService,
                          UserRepository userRepository,
                          JwtTokenProvider jwtTokenProvider) {
        this.userService = userService;
        this.authLoginLogService = authLoginLogService;
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
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
    // 登录时不需要 @Valid，因为密码校验通常在 Service/Security 层处理
    public ResponseEntity<?> authenticateUser(@RequestBody UserLoginDto loginRequest,
                                              HttpServletRequest request) {
        try {
            String jwtToken = userService.authenticateUser(loginRequest);
            // 记录成功登录（JWT 登录不会走 formLogin 的 successHandler）
            try {
                String username = loginRequest != null ? loginRequest.getUsername() : null;
                User user = (username == null || username.isBlank())
                        ? null
                        : userRepository.findByUsername(username.trim()).orElse(null);
                String role = user != null && user.getRole() != null ? user.getRole().name() : "UNKNOWN";
                AuthLoginLog log = AuthLoginLog.builder()
                        .user(user)
                        .role(role)
                        .ip(ClientIpExtractor.resolve(request))
                        .userAgent(truncate(request != null ? request.getHeader("User-Agent") : null, 512))
                        .sessionId(request != null && request.getSession(false) != null ? request.getSession(false).getId() : null)
                        .success(true)
                        .build();
                authLoginLogService.create(log);
            } catch (Exception ex) {
                log.warn("JWT 登录成功但写入 AuthLoginLog 失败: {}", ex.getMessage(), ex);
            }

            return ResponseEntity.ok(LoginResponse.bearer(jwtToken, jwtTokenProvider.getExpiresInSeconds()));

        } catch (RuntimeException e) {
            // 记录失败登录
            try {
                String username = loginRequest != null ? loginRequest.getUsername() : null;
                User user = (username == null || username.isBlank())
                        ? null
                        : userRepository.findByUsername(username.trim()).orElse(null);
                String role = user != null && user.getRole() != null ? user.getRole().name() : "UNKNOWN";
                AuthLoginLog log = AuthLoginLog.builder()
                        .user(user)
                        .role(role)
                        .ip(ClientIpExtractor.resolve(request))
                        .userAgent(truncate(request != null ? request.getHeader("User-Agent") : null, 512))
                        .sessionId(request != null && request.getSession(false) != null ? request.getSession(false).getId() : null)
                        .success(false)
                        .failureReason(truncate(e.getMessage(), 512))
                        .build();
                authLoginLogService.create(log);
            } catch (Exception ex) {
                log.warn("JWT 登录失败且写入 AuthLoginLog 失败: {}", ex.getMessage(), ex);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiMessageResponse(e.getMessage() != null ? e.getMessage() : "登录失败"));
        }
    }

    private static String truncate(String v, int maxLen) {
        if (v == null) return null;
        String s = v.trim();
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen);
    }
}