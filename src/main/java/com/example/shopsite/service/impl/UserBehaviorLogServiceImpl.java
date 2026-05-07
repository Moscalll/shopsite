package com.example.shopsite.service.impl;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopsite.model.UserBehaviorLog;
import com.example.shopsite.repository.UserBehaviorLogRepository;
import com.example.shopsite.service.UserBehaviorLogService;

@Service
public class UserBehaviorLogServiceImpl implements UserBehaviorLogService {

    private final UserBehaviorLogRepository userBehaviorLogRepository;

    public UserBehaviorLogServiceImpl(UserBehaviorLogRepository userBehaviorLogRepository) {
        this.userBehaviorLogRepository = userBehaviorLogRepository;
    }

    @Override
    @Transactional
    public UserBehaviorLog create(UserBehaviorLog log) {
        return userBehaviorLogRepository.save(log);
    }

    @Override
    @Transactional
    public Optional<UserBehaviorLog> update(Long id, UserBehaviorLog update) {
        return userBehaviorLogRepository.findById(id).map(existing -> {
            existing.setUser(update.getUser());
            existing.setSessionId(update.getSessionId());
            existing.setProductId(update.getProductId());
            existing.setCategoryId(update.getCategoryId());
            existing.setEventType(update.getEventType());
            existing.setDurationSeconds(update.getDurationSeconds());
            existing.setPageUrl(update.getPageUrl());
            existing.setReferrer(update.getReferrer());
            existing.setEventTime(update.getEventTime());
            existing.setIp(update.getIp());
            existing.setUserAgent(update.getUserAgent());
            return userBehaviorLogRepository.save(existing);
        });
    }

    @Override
    @Transactional
    public boolean delete(Long id) {
        if (!userBehaviorLogRepository.existsById(id)) {
            return false;
        }
        userBehaviorLogRepository.deleteById(id);
        return true;
    }

    @Override
    public Optional<UserBehaviorLog> findById(Long id) {
        return userBehaviorLogRepository.findById(id);
    }

    @Override
    public java.util.List<UserBehaviorLog> findAll() {
        return userBehaviorLogRepository.findAll();
    }
}
