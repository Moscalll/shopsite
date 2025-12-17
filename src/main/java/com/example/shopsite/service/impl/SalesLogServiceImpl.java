package com.example.shopsite.service.impl;

import com.example.shopsite.model.SalesLog;
import com.example.shopsite.model.User;
import com.example.shopsite.repository.SalesLogRepository;
import com.example.shopsite.service.SalesLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalesLogServiceImpl implements SalesLogService {

    private final SalesLogRepository salesLogRepository;

    public SalesLogServiceImpl(SalesLogRepository salesLogRepository) {
        this.salesLogRepository = salesLogRepository;
    }

    @Override
    @Transactional
    public void logView(Long productId, User user) {
        SalesLog log = SalesLog.builder()
                .productId(productId)
                .user(user)
                .actionType("VIEW")
                .build();
        salesLogRepository.save(log);
    }

    @Override
    @Transactional
    public void logPurchase(Long productId, User user) {
        SalesLog log = SalesLog.builder()
                .productId(productId)
                .user(user)
                .actionType("PURCHASE")
                .build();
        salesLogRepository.save(log);
    }

    @Override
    @Transactional
    public void logAddToCart(Long productId, User user) {
        SalesLog log = SalesLog.builder()
                .productId(productId)
                .user(user)
                .actionType("ADD_TO_CART")
                .build();
        salesLogRepository.save(log);
    }
}






















