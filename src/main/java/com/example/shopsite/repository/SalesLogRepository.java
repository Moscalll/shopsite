
package com.example.shopsite.repository;

import com.example.shopsite.model.SalesLog;
import com.example.shopsite.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SalesLogRepository extends JpaRepository<SalesLog, Long> {
    
    // 根据商品ID列表查询日志
    List<SalesLog> findByProductIdIn(List<Long> productIds);
    
    // 根据商品ID列表和操作类型查询日志
    List<SalesLog> findByProductIdInAndActionType(List<Long> productIds, String actionType);
    
    // 根据用户和商品ID列表查询日志（用于客户详情页）
    List<SalesLog> findByUserAndProductIdIn(User user, List<Long> productIds);
    
    // 根据用户、商品ID列表和操作类型查询日志
    List<SalesLog> findByUserAndProductIdInAndActionType(User user, List<Long> productIds, String actionType);
    
    // 根据用户查询日志（用于个人中心）
    List<SalesLog> findByUser(User user);
    
    // 根据用户和操作类型查询日志
    List<SalesLog> findByUserAndActionType(User user, String actionType);
}






