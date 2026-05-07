package com.example.shopsite.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.shopsite.model.AdminOperationLog;
import com.example.shopsite.model.User;

public interface AdminOperationLogRepository extends JpaRepository<AdminOperationLog, Long> {

    List<AdminOperationLog> findByOperator(User operator);

    List<AdminOperationLog> findByOperatorAndOperationType(User operator, String operationType);

    List<AdminOperationLog> findByOperatorRole(String operatorRole);

    List<AdminOperationLog> findByOperator_IdOrderByOperationTimeDesc(Long operatorId);
}
