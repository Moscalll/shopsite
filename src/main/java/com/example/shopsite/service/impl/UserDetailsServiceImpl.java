package com.example.shopsite.service.impl;

import com.example.shopsite.model.User;
import com.example.shopsite.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service // 必须标记为 @Service 或 @Component，让 Spring 扫描到
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    /**
     * 根据用户名加载用户，这是 Spring Security 认证的核心方法。
     * @param username 用户输入的用户名
     * @return 包含用户权限和密码的 UserDetails 对象
     * @throws UsernameNotFoundException 如果找不到用户
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. 从数据库查找用户
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户未找到: " + username));

        // 2. 将用户角色转换为 Spring Security 要求的 GrantedAuthority 集合
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword()) // 数据库中应是 BCrypt 加密后的密码
                .roles(user.getRole().name()) 
                .build();
    }
}