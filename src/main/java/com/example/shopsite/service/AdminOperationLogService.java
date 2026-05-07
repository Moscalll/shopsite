package com.example.shopsite.service;

import java.util.Optional;

import com.example.shopsite.model.AdminOperationLog;

public interface AdminOperationLogService {

    AdminOperationLog create(AdminOperationLog log);

    Optional<AdminOperationLog> update(Long id, AdminOperationLog update);

    boolean delete(Long id);

    Optional<AdminOperationLog> findById(Long id);

    java.util.List<AdminOperationLog> findAll();
}
