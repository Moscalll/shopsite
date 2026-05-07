package com.example.shopsite.service;

import com.example.shopsite.model.RecommendationAlgorithm;
import com.example.shopsite.model.User;

import java.util.List;
import java.util.Map;

public interface RecommendationService {
    /**
     * 离线计算：为所有普通用户生成推荐结果并入库（覆盖写入最新一版）。
     */
    void recomputeAll(int topN);

    /**
     * 在线读取：返回某用户的推荐商品ID列表（按顺序）。
     */
    List<Long> getRecommendations(User user, RecommendationAlgorithm algorithm, int topN);

    /**
     * 指标：覆盖率/命中率/稳定性（简化口径）。
     */
    Map<String, Object> getMetrics(int topN);
}

