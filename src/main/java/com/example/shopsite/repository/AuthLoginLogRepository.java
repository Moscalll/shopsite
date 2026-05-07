package com.example.shopsite.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.shopsite.model.AuthLoginLog;
import com.example.shopsite.model.User;

public interface AuthLoginLogRepository extends JpaRepository<AuthLoginLog, Long> {

    List<AuthLoginLog> findByUser(User user);

    List<AuthLoginLog> findByUser_Id(Long userId);

    List<AuthLoginLog> findByUser_IdOrderByLoginTimeDesc(Long userId);

    List<AuthLoginLog> findByUserAndSuccess(User user, Boolean success);

    List<AuthLoginLog> findByRole(String role);

    long countByUser_IdAndSuccess(Long userId, Boolean success);

    Optional<AuthLoginLog> findFirstByUser_IdAndSuccessOrderByLoginTimeDesc(Long userId, Boolean success);
}
