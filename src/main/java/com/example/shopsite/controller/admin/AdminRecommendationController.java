package com.example.shopsite.controller.admin;

import com.example.shopsite.model.RecommendationAlgorithm;
import com.example.shopsite.service.RecommendationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/admin/recommendations")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRecommendationController {

    private final RecommendationService recommendationService;

    public AdminRecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public String dashboard(@RequestParam(defaultValue = "10") int topN, Model model) {
        Map<String, Object> metrics = recommendationService.getMetrics(topN);
        model.addAttribute("pageTitle", "推荐系统");
        model.addAttribute("activeMenu", "recommendations");
        model.addAttribute("metrics", metrics);
        model.addAttribute("topN", topN);
        model.addAttribute("algorithms", RecommendationAlgorithm.values());
        return "admin/recommendation_dashboard";
    }

    @PostMapping("/recompute")
    public String recompute(@RequestParam(defaultValue = "10") int topN, RedirectAttributes ra) {
        try {
            recommendationService.recomputeAll(topN);
            ra.addFlashAttribute("success", "推荐已重新计算");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/recommendations?topN=" + topN;
    }
}

