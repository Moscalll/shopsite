package com.example.shopsite.config;

import com.example.shopsite.model.Role;
import com.example.shopsite.model.User;
import com.example.shopsite.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // 检查是否已存在管理员（通过用户名或邮箱检查）
        boolean adminExists = userRepository.findByUsername("admin").isPresent() ||
                             userRepository.findByEmail("admin@shopsite.com").isPresent();
        
        if (!adminExists) {
            try {
                User admin = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin123")) // 默认密码：admin123
                        .email("admin@shopsite.com")
                        .role(Role.ADMIN)
                        .build();
                userRepository.save(admin);
                System.out.println("默认管理员账户已创建: 用户名=admin, 密码=admin123");
            } catch (Exception e) {
                // 如果保存失败（可能是并发问题），只记录日志，不抛出异常
                System.out.println("管理员账户可能已存在，跳过创建: " + e.getMessage());
            }
        } else {
            System.out.println("管理员账户已存在，跳过创建");
        }
    }
}

