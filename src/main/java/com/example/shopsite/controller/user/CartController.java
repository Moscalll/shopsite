package com.example.shopsite.controller.user;

import com.example.shopsite.model.CartItem;
import com.example.shopsite.model.User;
import com.example.shopsite.repository.UserRepository;
import com.example.shopsite.service.CartService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;

    public CartController(CartService cartService, UserRepository userRepository) {
        this.cartService = cartService;
        this.userRepository = userRepository;
    }

    /**
     * GET /cart - 购物车页面
     */
    @GetMapping
    public String cartPage(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }

        User user = userOpt.get();
        List<CartItem> cartItems = cartService.getCartItems(user);
        Integer totalCount = cartService.getCartItemCount(user);

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("pageTitle", "购物车");

        return "user/cart";
    }

    /**
     * POST /cart/add - 添加到购物车
     */
    @PostMapping("/add")
    public String addToCart(@RequestParam Long productId, 
                           @RequestParam(defaultValue = "1") Integer quantity,
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
            cartService.addToCart(productId, quantity, userOpt.get());
            redirectAttributes.addFlashAttribute("success", "商品已添加到购物车");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/cart";
    }

    /**
     * POST /cart/update/{id} - 更新购物车项数量
     */
    @PostMapping("/update/{id}")
    public String updateCartItem(@PathVariable Long id,
                                @RequestParam Integer quantity,
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
            cartService.updateCartItemQuantity(id, quantity, userOpt.get());
            redirectAttributes.addFlashAttribute("success", "购物车已更新");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/cart";
    }

    /**
     * POST /cart/remove/{id} - 从购物车移除
     */
    @PostMapping("/remove/{id}")
    public String removeFromCart(@PathVariable Long id,
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
            cartService.removeFromCart(id, userOpt.get());
            redirectAttributes.addFlashAttribute("success", "商品已从购物车移除");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/cart";
    }

    /**
     * POST /cart/clear - 清空购物车
     */
    @PostMapping("/clear")
    public String clearCart(Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }

        cartService.clearCart(userOpt.get());
        redirectAttributes.addFlashAttribute("success", "购物车已清空");

        return "redirect:/cart";
    }
}





