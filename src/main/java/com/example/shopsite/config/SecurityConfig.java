package com.example.shopsite.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain; // 引入 SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import com.example.shopsite.security.JwtAuthenticationFilter;
import org.springframework.security.config.http.SessionCreationPolicy;

@Configuration
@EnableWebSecurity // 启用 Spring Security 的 Web 安全功能
@EnableMethodSecurity // 🚨 推荐：启用方法级别的安全注解，比如 @PreAuthorize
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
               // 1. 公开路由
                .requestMatchers("/api/auth/**").permitAll() 
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll() // 允许所有人查询商品

                // 2. 商家/管理员路由
                // 只有 MERCHANT 或 ADMIN 才能创建/修改/删除商品
                .requestMatchers(HttpMethod.POST, "/api/products").hasAnyAuthority("ROLE_MERCHANT", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/products/**").hasAnyAuthority("ROLE_MERCHANT", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasAnyAuthority("ROLE_MERCHANT", "ROLE_ADMIN")
                
                // 3. 客户路由
                // 只有 CUSTOMER 才能创建订单
                .requestMatchers(HttpMethod.POST, "/api/orders").hasAuthority("ROLE_CUSTOMER")

                // 4. 其他所有 /api/orders/my 和 /api/orders/{id} 接口只需要认证即可
                // 因为订单详情和列表的权限控制（只能看自己的）已经在 Service 层完成了。
                .requestMatchers("/api/orders/**").authenticated() 
                
                // 5. 任何其他未明确指定的请求都需要认证
                .anyRequest().authenticated()
            )
            // 禁用默认的 HTTP Basic 认证（或者只配置需要使用的认证方式）
            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(formLogin -> formLogin.disable());
        
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}