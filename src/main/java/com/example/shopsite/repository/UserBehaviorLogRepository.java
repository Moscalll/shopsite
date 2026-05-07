package com.example.shopsite.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.shopsite.model.User;
import com.example.shopsite.model.UserBehaviorLog;

public interface UserBehaviorLogRepository extends JpaRepository<UserBehaviorLog, Long> {

    List<UserBehaviorLog> findByUser(User user);

    List<UserBehaviorLog> findByUserAndEventType(User user, String eventType);

    List<UserBehaviorLog> findByProductId(Long productId);

    List<UserBehaviorLog> findByCategoryId(Long categoryId);

    List<UserBehaviorLog> findByUser_IdOrderByEventTimeDesc(Long userId);

    List<UserBehaviorLog> findByUser_IdAndProductIdInOrderByEventTimeDesc(Long userId, Collection<Long> productIds);
}
