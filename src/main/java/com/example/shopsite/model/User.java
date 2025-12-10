package com.example.shopsite.model;
import com.example.shopsite.model.Role;

import jakarta.persistence.*;
import lombok.Data; // 确保 pom.xml 中有 Lombok 依赖

@Entity
@Table(name = "user")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String password; // 存储加密后的密码
    private String email;

    @Enumerated(EnumType.STRING)
    private Role role; // Role 是一个自定义的枚举类

    // ... 其他字段
}