package com.example.shopsite.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain; // 引入 SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.shopsite.security.JwtAuthenticationFilter;
import org.springframework.security.config.http.SessionCreationPolicy;

@Configuration
@EnableWebSecurity // 启用 Spring Security 的 Web 安全功能
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    // 1. PasswordEncoder Bean (之前已添加)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 2. 🚨 核心修改点：配置安全过滤器链
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF 保护 (适用于 API 项目，如果使用 cookie/session 需谨慎)
            .csrf(csrf -> csrf.disable()) 

            // 1. 🚨 配置无状态会话管理 (JWT 关键)
            // 告诉 Spring Security 不要创建或使用 Session
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // 配置请求授权
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll() // 注册和登录接口
               // 🚨 核心修改：允许所有人访问商品查询接口 (GET)
                .requestMatchers(HttpMethod.GET, "/api/products").permitAll() // 允许 GET /api/products
                // 允许所有人访问单个商品详情查询接口 (GET /api/products/123)
                .requestMatchers(HttpMethod.GET, "/api/products/{id}").permitAll()
                .anyRequest().authenticated() // 其他所有请求都需要认证
            )
            // 禁用默认的 HTTP Basic 认证（或者只配置需要使用的认证方式）
            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(formLogin -> formLogin.disable());
        
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}