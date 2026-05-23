package com.example.shopsite.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import com.example.shopsite.security.CustomAuthenticationSuccessHandler;
import com.example.shopsite.security.CustomAuthenticationFailureHandler;
import com.example.shopsite.security.JwtAuthenticationFilter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // 启用方法级别的安全注解
public class SecurityConfig {

    private final CustomAuthenticationSuccessHandler authenticationSuccessHandler;
    private final CustomAuthenticationFailureHandler authenticationFailureHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(CustomAuthenticationSuccessHandler authenticationSuccessHandler,
                          CustomAuthenticationFailureHandler authenticationFailureHandler,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.authenticationSuccessHandler = authenticationSuccessHandler;
        this.authenticationFailureHandler = authenticationFailureHandler;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http

                // 配置请求授权
                .authorizeHttpRequests(auth -> auth

                        // 1. 允许匿名访问的页面路由和静态资源（前端）
                        .requestMatchers(
                                "/", // 首页
                                "/products", // 商品列表
                                "/product/**", // 商品详情
                                "/login", // 登录页面
                                "/register", // 注册页面
                                "/error",
                                "/help", // 帮助页面（新增）
                                "/about",
                                "/css/**", // 静态资源
                                "/js/**", // 静态资源
                                "/images/**", // 静态资源
                                "/uploads/**",
                                "/webjars/**" // 静态资源
                        ).permitAll()

                        // 2. 允许匿名访问的后端 API（认证和查询商品）
                        .requestMatchers("/api/auth/register").permitAll() // 允许注册 API 访问
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll() // 允许登录 API 访问（如果你使用自定义认证接口）
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll() // 允许所有人查询商品 API
                        .requestMatchers(HttpMethod.GET, "/api/categories").permitAll() // 允许所有人查询分类 API
                        .requestMatchers(HttpMethod.POST, "/api/behavior/dwell").authenticated() // 登录用户上报停留（主要用于 CUSTOMER）

                        // 3. 用户端路由（需要认证）
                        .requestMatchers("/cart/**", "/favorites/**", "/orders/**").authenticated()

                        // 4. 商户端路由（需要MERCHANT或ADMIN角色）
                        .requestMatchers("/merchant/**").hasAnyAuthority("ROLE_MERCHANT", "ROLE_ADMIN")

                        // 5. 管理员端路由（需要ADMIN角色）
                        .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")

                        // 6. 分离商家/管理员路由
                        // 只有 MERCHANT 或 ADMIN 才能创建商品 (POST)
                        .requestMatchers(HttpMethod.POST, "/api/products")
                        .hasAnyAuthority("ROLE_MERCHANT", "ROLE_ADMIN")

                        // 只有 MERCHANT 或 ADMIN 才能更新商品 (PUT)
                        .requestMatchers(HttpMethod.PUT, "/api/products/**")
                        .hasAnyAuthority("ROLE_MERCHANT", "ROLE_ADMIN")

                        // 只有 MERCHANT 或 ADMIN 才能删除商品 (DELETE)
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**")
                        .hasAnyAuthority("ROLE_MERCHANT", "ROLE_ADMIN")

                        // 7. 保护客户订单 API 路由
                        .requestMatchers(HttpMethod.POST, "/api/orders").hasAuthority("ROLE_CUSTOMER")
                        .requestMatchers("/api/orders/**").authenticated() // 其他订单相关 API 需要认证

                        // 8. 其他所有未明确指定的请求（包括未在上面的 /api/** 中列出的）
                        .anyRequest().authenticated())

                // 启用并配置基于 Session 的表单登录
                .formLogin(form -> form
                        .loginPage("/login") // 指定自定义登录页面 GET 请求
                        .loginProcessingUrl("/login") // 指定处理登录表单的 POST 请求路径
                        .successHandler(authenticationSuccessHandler) // 使用自定义成功处理器，根据角色跳转
                        .failureHandler(authenticationFailureHandler) // 失败时写登录日志并跳转
                        .permitAll() // 允许所有人访问登录路径
                )

                // 启用并配置登出
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login") // 登出后跳转到登录页
                        .permitAll())

                // 行为停留上报可能通过 sendBeacon / fetch 调用，放宽 CSRF（仍要求登录）
                .csrf(csrf -> csrf.ignoringRequestMatchers(
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/behavior/dwell"));

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}