package com.example.shopsite.service;

import com.example.shopsite.dto.UserLoginDto;
import com.example.shopsite.dto.UserRegistrationRequest;
import com.example.shopsite.model.Role;
import com.example.shopsite.model.User;
import java.util.List;

public interface UserService {
    /**
     * 用户注册逻辑
     * @param request 注册请求DTO
     * @return 注册成功的用户实体
     * @throws RuntimeException 如果用户名或邮箱已存在
     */
    User registerUser(UserRegistrationRequest request);
    
    /**
     * 用户注册逻辑（指定角色）
     * @param request 注册请求DTO
     * @param role 用户角色
     * @return 注册成功的用户实体
     * @throws RuntimeException 如果用户名或邮箱已存在
     */
    User registerUser(UserRegistrationRequest request, Role role);

    String authenticateUser(UserLoginDto loginRequest);
    
    // 管理员功能：查询所有商户
    List<User> findAllMerchants();
    
    // 管理员功能：根据ID查询用户
    User findUserById(Long id);
    
    // 管理员功能：更新用户角色或状态
    User updateUserRole(Long userId, Role newRole);
}