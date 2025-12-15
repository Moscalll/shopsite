package com.example.shopsite.repository;

import com.example.shopsite.model.Message;
import com.example.shopsite.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByUserOrderByCreateTimeDesc(User user);
    List<Message> findByUserAndIsReadFalseOrderByCreateTimeDesc(User user);
    Long countByUserAndIsReadFalse(User user);
}