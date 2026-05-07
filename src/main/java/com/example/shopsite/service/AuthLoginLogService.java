package com.example.shopsite.service;

import java.util.Optional;

import com.example.shopsite.model.AuthLoginLog;

public interface AuthLoginLogService {

    AuthLoginLog create(AuthLoginLog log);

    Optional<AuthLoginLog> update(Long id, AuthLoginLog update);

    boolean delete(Long id);

    Optional<AuthLoginLog> findById(Long id);

    java.util.List<AuthLoginLog> findAll();
}
