package com.example.shopsite.service.impl;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopsite.model.AdminOperationLog;
import com.example.shopsite.repository.AdminOperationLogRepository;
import com.example.shopsite.service.AdminOperationLogService;

@Service
public class AdminOperationLogServiceImpl implements AdminOperationLogService {

    private final AdminOperationLogRepository adminOperationLogRepository;

    public AdminOperationLogServiceImpl(AdminOperationLogRepository adminOperationLogRepository) {
        this.adminOperationLogRepository = adminOperationLogRepository;
    }

    @Override
    @Transactional
    public AdminOperationLog create(AdminOperationLog log) {
        return adminOperationLogRepository.save(log);
    }

    @Override
    @Transactional
    public Optional<AdminOperationLog> update(Long id, AdminOperationLog update) {
        return adminOperationLogRepository.findById(id).map(existing -> {
            existing.setOperator(update.getOperator());
            existing.setOperatorRole(update.getOperatorRole());
            existing.setOperationTime(update.getOperationTime());
            existing.setOperationType(update.getOperationType());
            existing.setContent(update.getContent());
            existing.setIp(update.getIp());
            existing.setAccount(update.getAccount());
            return adminOperationLogRepository.save(existing);
        });
    }

    @Override
    @Transactional
    public boolean delete(Long id) {
        if (!adminOperationLogRepository.existsById(id)) {
            return false;
        }
        adminOperationLogRepository.deleteById(id);
        return true;
    }

    @Override
    public Optional<AdminOperationLog> findById(Long id) {
        return adminOperationLogRepository.findById(id);
    }

    @Override
    public java.util.List<AdminOperationLog> findAll() {
        return adminOperationLogRepository.findAll();
    }
}
