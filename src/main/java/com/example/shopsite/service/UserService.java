package com.example.shopsite.service;

import com.example.shopsite.dto.UserLoginDto;
import com.example.shopsite.dto.UserRegistrationRequest;
import com.example.shopsite.model.User;

public interface UserService {
    /**
     * 用户注册逻辑
     * @param request 注册请求DTO
     * @return 注册成功的用户实体
     * @throws RuntimeException 如果用户名或邮箱已存在
     */
    User registerUser(UserRegistrationRequest request);

    String authenticateUser(UserLoginDto loginRequest);
}