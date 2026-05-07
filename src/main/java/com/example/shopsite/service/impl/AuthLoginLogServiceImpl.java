package com.example.shopsite.service.impl;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopsite.model.AuthLoginLog;
import com.example.shopsite.repository.AuthLoginLogRepository;
import com.example.shopsite.service.AuthLoginLogService;

@Service
public class AuthLoginLogServiceImpl implements AuthLoginLogService {

    private final AuthLoginLogRepository authLoginLogRepository;

    public AuthLoginLogServiceImpl(AuthLoginLogRepository authLoginLogRepository) {
        this.authLoginLogRepository = authLoginLogRepository;
    }

    @Override
    @Transactional
    public AuthLoginLog create(AuthLoginLog log) {
        return authLoginLogRepository.save(log);
    }

    @Override
    @Transactional
    public Optional<AuthLoginLog> update(Long id, AuthLoginLog update) {
        return authLoginLogRepository.findById(id).map(existing -> {
            existing.setUser(update.getUser());
            existing.setRole(update.getRole());
            existing.setLoginTime(update.getLoginTime());
            existing.setIp(update.getIp());
            existing.setUserAgent(update.getUserAgent());
            existing.setSuccess(update.getSuccess());
            existing.setFailureReason(update.getFailureReason());
            existing.setSessionId(update.getSessionId());
            return authLoginLogRepository.save(existing);
        });
    }

    @Override
    @Transactional
    public boolean delete(Long id) {
        if (!authLoginLogRepository.existsById(id)) {
            return false;
        }
        authLoginLogRepository.deleteById(id);
        return true;
    }

    @Override
    public Optional<AuthLoginLog> findById(Long id) {
        return authLoginLogRepository.findById(id);
    }

    @Override
    public java.util.List<AuthLoginLog> findAll() {
        return authLoginLogRepository.findAll();
    }
}
