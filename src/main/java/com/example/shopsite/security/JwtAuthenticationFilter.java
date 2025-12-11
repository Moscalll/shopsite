package com.example.shopsite.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService; // 🚨 需要注入 UserDetailsService
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService; // 用于根据用户名加载用户信息

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, UserDetailsService userDetailsService) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. 从请求头中获取 JWT
        String jwt = getJwtFromRequest(request);

        // 2. 验证和加载用户身份
        if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
            try {
                // 从 Token 中获取用户名
                String username = tokenProvider.getUsernameFromToken(jwt);

                // 从数据库加载用户详细信息 (UserDetails)
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // 创建认证对象
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 将认证对象设置到 Spring Security 上下文中
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (Exception ex) {
                // 如果 Token 有效但加载用户信息失败，则清空认证信息
                SecurityContextHolder.clearContext();
            }
        }

        // 3. 继续执行过滤器链
        filterChain.doFilter(request, response);
    }

    // 辅助方法：从请求头中提取 JWT
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        // 检查 Authorization 头部是否以 "Bearer " 开头
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            // 返回 Token 字符串
            return bearerToken.substring(7);
        }
        return null;
    }
}