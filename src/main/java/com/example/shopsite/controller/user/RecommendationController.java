package com.example.shopsite.controller.user;

import com.example.shopsite.model.RecommendationAlgorithm;
import com.example.shopsite.model.User;
import com.example.shopsite.repository.UserRepository;
import com.example.shopsite.service.RecommendationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final UserRepository userRepository;

    public RecommendationController(RecommendationService recommendationService,
                                    UserRepository userRepository) {
        this.recommendationService = recommendationService;
        this.userRepository = userRepository;
    }

    @GetMapping("/my")
    public List<Long> my(@RequestParam(defaultValue = "ITEM_CF") RecommendationAlgorithm algorithm,
                         @RequestParam(defaultValue = "10") int topN,
                         Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return List.of();
        }
        Optional<User> userOpt = userRepository.findByUsername(authentication.getName());
        if (userOpt.isEmpty()) {
            return List.of();
        }
        return recommendationService.getRecommendations(userOpt.get(), algorithm, topN);
    }
}

