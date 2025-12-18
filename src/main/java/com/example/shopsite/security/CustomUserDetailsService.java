// package com.example.shopsite.security;

// import com.example.shopsite.model.User;
// import com.example.shopsite.repository.UserRepository;
// import org.springframework.security.core.GrantedAuthority;
// import org.springframework.security.core.authority.SimpleGrantedAuthority;
// import org.springframework.security.core.userdetails.UserDetails;
// import org.springframework.security.core.userdetails.UserDetailsService;
// import org.springframework.security.core.userdetails.UsernameNotFoundException;
// import org.springframework.stereotype.Service;

// import java.util.Collections;
// import java.util.Set;

// @Service // 1. 标记为 Spring Service Bean
// public class CustomUserDetailsService implements UserDetailsService {

//     private final UserRepository userRepository;

//     public CustomUserDetailsService(UserRepository userRepository) {
//         this.userRepository = userRepository;
//     }

//     /**
//      * Spring Security 将调用此方法来加载用户身份验证所需的详细信息。
//      */
//     @Override
//     public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        
//         // 2. 根据用户名从数据库查找用户
//         User user = userRepository.findByUsername(username)
//                 .orElseThrow(() -> 
//                         new UsernameNotFoundException("找不到用户: " + username));

//         // 检查点：确保你使用的是 user.getRole().name()
//         Set<GrantedAuthority> authorities = Collections.singleton(
//             new SimpleGrantedAuthority("ROLE_" + user.getRole().name()) 
//         );
        
//         // 使用 Spring Security 提供的 User 类实现 UserDetails
//         return new org.springframework.security.core.userdetails.User(
//                 user.getUsername(),
//                 user.getPassword(), // 必须是加密后的密码
//                 authorities
//         );
//     }
// }