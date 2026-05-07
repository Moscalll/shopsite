package com.example.shopsite.service;

import java.util.Optional;

import com.example.shopsite.model.UserBehaviorLog;

public interface UserBehaviorLogService {

    UserBehaviorLog create(UserBehaviorLog log);

    Optional<UserBehaviorLog> update(Long id, UserBehaviorLog update);

    boolean delete(Long id);

    Optional<UserBehaviorLog> findById(Long id);

    java.util.List<UserBehaviorLog> findAll();
}
