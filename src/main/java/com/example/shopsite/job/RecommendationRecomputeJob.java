package com.example.shopsite.job;

import com.example.shopsite.service.RecommendationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RecommendationRecomputeJob {

    private final RecommendationService recommendationService;

    public RecommendationRecomputeJob(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    /**
     * 自动刷新推荐结果：
     * - 默认每 15 分钟重算一次（可后续抽到配置）
     */
    @Scheduled(fixedDelay = 15 * 60 * 1000L, initialDelay = 30 * 1000L)
    public void recompute() {
        recommendationService.recomputeAll(10);
    }
}

