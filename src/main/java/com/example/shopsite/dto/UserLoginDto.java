package com.example.shopsite.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserLoginDto {
    
    private String username; 
    private String password;
    
}