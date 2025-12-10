package com.example.shopsite.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain; // 引入 SecurityFilterChain

@Configuration
@EnableWebSecurity // 启用 Spring Security 的 Web 安全功能
public class SecurityConfig {

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
            
            // 配置请求授权
            .authorizeHttpRequests(auth -> auth
                // 🚨 允许任何人访问注册接口（核心修改）
                .requestMatchers("/api/auth/register").permitAll() 
                
                // 允许任何人访问登录接口（如果稍后创建登录接口）
                .requestMatchers("/api/auth/login").permitAll() 
                
                // 其他所有请求都需要认证
                .anyRequest().authenticated() 
            )
            // 禁用默认的 HTTP Basic 认证（或者只配置需要使用的认证方式）
            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(formLogin -> formLogin.disable());

        return http.build();
    }
}