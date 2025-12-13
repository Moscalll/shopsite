package com.example.shopsite.repository;

import com.example.shopsite.model.Role;
import com.example.shopsite.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

// 1. @Repository 注解是可选的，但推荐加上
@Repository 
// 2. 继承 JpaRepository<实体类, 主键类型>
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 根据用户名查询用户。
     * Spring Data JPA 会根据方法名自动生成 SQL 查询。
     */
    Optional<User> findByUsername(String username);
    
    /**
     * 根据邮箱查询用户。
     */
    Optional<User> findByEmail(String email);
    
    /**
     * 根据角色查询用户列表（用于管理员查询商户）
     */
    List<User> findByRole(Role role);
}