package com.example.shopsite.controller.user;

import com.example.shopsite.model.Favorite;
import com.example.shopsite.model.User;
import com.example.shopsite.repository.UserRepository;
import com.example.shopsite.service.FavoriteService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final UserRepository userRepository;

    public FavoriteController(FavoriteService favoriteService, UserRepository userRepository) {
        this.favoriteService = favoriteService;
        this.userRepository = userRepository;
    }

    /**
     * GET /favorites - 收藏夹页面
     */
    @GetMapping
    public String favoritesPage(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }

        User user = userOpt.get();
        List<Favorite> favorites = favoriteService.getUserFavorites(user);

        model.addAttribute("favorites", favorites);
        model.addAttribute("pageTitle", "我的收藏");

        return "user/favorites";
    }

    /**
     * POST /favorites/add/{productId} - 添加收藏
     */
    @PostMapping("/add/{productId}")
    public String addFavorite(@PathVariable Long productId,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }

        try {
            favoriteService.addFavorite(productId, userOpt.get());
            redirectAttributes.addFlashAttribute("success", "已添加到收藏夹");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/product/" + productId;
    }

    /**
     * POST /favorites/remove/{productId} - 取消收藏
     */
    @PostMapping("/remove/{productId}")
    public String removeFavorite(@PathVariable Long productId,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }

        try {
            favoriteService.removeFavorite(productId, userOpt.get());
            redirectAttributes.addFlashAttribute("success", "已取消收藏");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/favorites";
    }
}





