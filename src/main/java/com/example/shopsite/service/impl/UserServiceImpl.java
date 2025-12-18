package com.example.shopsite.service.impl;

import com.example.shopsite.dto.UserLoginDto;
import com.example.shopsite.dto.UserRegistrationRequest;
import com.example.shopsite.model.Role;
import com.example.shopsite.model.User;
import com.example.shopsite.repository.UserRepository;
import com.example.shopsite.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.shopsite.security.JwtTokenProvider;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // 注入密码加密器
    private final JwtTokenProvider tokenProvider;

    // 依赖注入 UserRepository 和 PasswordEncoder
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Override
    @Transactional // 确保整个操作在事务中执行
    public User registerUser(UserRegistrationRequest request) {
        return registerUser(request, Role.CUSTOMER);
    }

    @Override
    @Transactional // 确保整个操作在事务中执行
    public User registerUser(UserRegistrationRequest request, Role role) {
        // 不允许注册管理员角色
        if (role == Role.ADMIN) {
            throw new RuntimeException("不允许注册管理员账户");
        }

        // 1. 检查用户名是否已存在
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("用户名已被占用");
        }

        // 2. 检查邮箱是否已存在
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("邮箱已被注册");
        }

        // 3. 构建用户实体
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                // 4. 对密码进行加密处理
                .password(passwordEncoder.encode(request.getPassword()))
                // 5. 设置角色
                .role(role)
                .build();

        // 6. 保存到数据库
        return userRepository.save(user);
    }

    // 登录方法的实现骨架
    @Override
    public String authenticateUser(UserLoginDto loginRequest) {
        // 1. 根据用户名查找用户
        Optional<User> userOptional = userRepository.findByUsername(loginRequest.getUsername());

        // 2. 检查用户是否存在
        if (userOptional.isEmpty()) {
            throw new RuntimeException("用户名或密码错误");
            // 安全最佳实践：不透露是用户名还是密码错误
        }

        User user = userOptional.get();
        String rawPassword = loginRequest.getPassword();
        String encodedPassword = user.getPassword();

        // 3. 使用 PasswordEncoder 验证密码
        // 检查用户输入的密码 (rawPassword) 是否匹配数据库中存储的加密密码 (encodedPassword)
        if (passwordEncoder.matches(rawPassword, encodedPassword)) {

            String jwtToken = tokenProvider.generateToken(user.getUsername());
            return jwtToken;

        } else {
            // 4. 密码不匹配
            throw new RuntimeException("用户名或密码错误");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAllMerchants() {
        return userRepository.findByRole(Role.MERCHANT);
    }

    @Override
    @Transactional(readOnly = true)
    public User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    @Override
    @Transactional
    public User updateUserRole(Long userId, Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setRole(newRole);
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findMerchantsByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAllMerchants();
        }
        return userRepository.findMerchantsByKeyword(Role.MERCHANT, keyword.trim());
    }
}