package com.example.shopsite.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
// 💡 可以添加校验注解，但登录时通常由 Spring Security 处理，
//    这里为了快速实现，我们暂时省略 @NotBlank 等。

@Data
@NoArgsConstructor
public class UserLoginDto {
    
    private String username; // 或 email，取决于你的认证机制
    private String password;
    
}