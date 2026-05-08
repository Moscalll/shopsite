package com.example.shopsite.config;

import com.example.shopsite.security.CustomAuthenticationSuccessHandler;
import com.example.shopsite.security.CustomAuthenticationFailureHandler;
import com.example.shopsite.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
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
                .cors(Customizer.withDefaults())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/products",
                                "/product/**",
                                "/login",
                                "/register",
                                "/error",
                                "/help",
                                "/about",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/uploads/**",
                                "/webjars/**"
                        ).permitAll()
                        .requestMatchers("/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories", "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/recommendations", "/api/recommendations/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/mine")
                        .hasAnyAuthority("ROLE_MERCHANT", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/behavior/dwell").authenticated()
                        .requestMatchers("/cart/**", "/favorites/**", "/orders/**").authenticated()
                        .requestMatchers("/merchant/**").hasAnyAuthority("ROLE_MERCHANT", "ROLE_ADMIN")
                        .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/products")
                        .hasAnyAuthority("ROLE_MERCHANT", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**")
                        .hasAnyAuthority("ROLE_MERCHANT", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**")
                        .hasAnyAuthority("ROLE_MERCHANT", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/orders").hasAuthority("ROLE_CUSTOMER")
                        .requestMatchers("/api/orders/**").authenticated()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(authenticationSuccessHandler)
                        .failureHandler(authenticationFailureHandler)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login")
                        .permitAll())
                // SPA / JSON API 使用 JWT，不按 Session CSRF；Thymeleaf 表单登录仍受 CSRF 保护
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));

        return http.build();
    }
}
