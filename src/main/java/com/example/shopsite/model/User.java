package com.example.shopsite.model;

import java.util.List;
import jakarta.persistence.*;
import lombok.Data; 
import lombok.NoArgsConstructor; // 🚨 新增：JPA 需要无参构造函数
import lombok.AllArgsConstructor; // 🚨 新增：方便全参构造
import lombok.Builder; // 🚨 新增：方便 Service 层构建对象
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.security.core.GrantedAuthority; 
import org.springframework.security.core.authority.SimpleGrantedAuthority; 
import org.springframework.security.core.userdetails.UserDetails; 
import java.util.Collection;

@Entity
@Table(name = "user")
@Data
@NoArgsConstructor // 必须有：JPA/Hibernate 需要
@AllArgsConstructor // 方便构建
@Builder // 方便在 UserService 中构建 User 对象
public class User implements UserDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🚨 唯一约束：确保用户名不重复（注册时查重）
    @Column(unique = true, nullable = false)
    private String username;
    
    // 密码：必须是加密后的密码，且不可为空
    @Column(nullable = false)
    private String password;
    
    // 🚨 唯一约束：确保邮箱不重复（注册时查重）
    @Column(unique = true, nullable = false)
    private String email;

    // 角色：使用枚举的字符串形式存储
    @Enumerated(EnumType.STRING)
    @Column(nullable = false) // 角色不可为空
    private Role role; 
    
    // 用户拥有的订单列表
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore // 🚨 关键：阻止序列化时递归加载订单，打破无限循环
    private List<Order> orders;
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 返回用户的权限集合
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name())); 
    }

    // 实现其余必需的 UserDetails 方法（都返回 true 即可）
    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
    
    // 额外的方法：返回用户名（UserDetails 接口要求）
    @Override
    public String getUsername() { return username; } 
    
    // 额外的 getter：返回加密后的密码（UserDetails 接口要求）
    @Override
    public String getPassword() { return password; }

    // 💡 Lombok @Data 会自动生成所有 Getter/Setter (包括 getRole())，
    //    满足 CustomUserDetailsService 的需求。
}

