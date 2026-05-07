package com.example.shopsite.repository;

import com.example.shopsite.model.RecommendationAlgorithm;
import com.example.shopsite.model.RecommendationResult;
import com.example.shopsite.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RecommendationResultRepository extends JpaRepository<RecommendationResult, Long> {
    Optional<RecommendationResult> findFirstByUserAndAlgorithmOrderByComputedAtDesc(User user, RecommendationAlgorithm algorithm);
    List<RecommendationResult> findByAlgorithmAndComputedAtAfter(RecommendationAlgorithm algorithm, LocalDateTime after);
    List<RecommendationResult> findByUser(User user);

    long countByAlgorithm(RecommendationAlgorithm algorithm);

    @Query("SELECT MAX(r.computedAt) FROM RecommendationResult r")
    Optional<LocalDateTime> findMaxComputedAt();
}

