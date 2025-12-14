package com.example.shopsite.repository;

import com.example.shopsite.model.SalesLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SalesLogRepository extends JpaRepository<SalesLog, Long> {
    
    // 根据商品ID列表查询日志
    List<SalesLog> findByProductIdIn(List<Long> productIds);
    
    // 根据商品ID列表和操作类型查询日志
    List<SalesLog> findByProductIdInAndActionType(List<Long> productIds, String actionType);
}





