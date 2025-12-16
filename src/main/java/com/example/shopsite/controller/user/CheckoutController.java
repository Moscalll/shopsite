package com.example.shopsite.controller.user;

import com.example.shopsite.model.CartItem;
import com.example.shopsite.model.User;
import com.example.shopsite.repository.UserRepository;
import com.example.shopsite.service.CartService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    private final CartService cartService;
    private final UserRepository userRepository;

    public CheckoutController(CartService cartService, UserRepository userRepository) {
        this.cartService = cartService;
        this.userRepository = userRepository;
    }

    /**
     * GET /checkout - 确认订单页面（从购物车）
     */
    @GetMapping
    public String checkoutPage(
            @RequestParam(value = "cartItemIds", required = false) List<Long> cartItemIds,
            Model model,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }

        User user = userOpt.get();
        List<CartItem> cartItems;
        if (cartItemIds != null && !cartItemIds.isEmpty()) {
            cartItems = cartService.getCartItems(user).stream()
                    .filter(item -> cartItemIds.contains(item.getId()))
                    .collect(java.util.stream.Collectors.toList());
        } else {
            cartItems = cartService.getCartItems(user);
        }

        if (cartItems.isEmpty()) {
            return "redirect:/cart";
        }

        BigDecimal totalAmount = cartItems.stream()
                .map(item -> item.getProduct().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("user", user);
        model.addAttribute("pageTitle", "确认订单");

        return "user/checkout";
    }

    /**
     * POST /checkout - 提交选中的购物车项到确认订单页面
     */
    @PostMapping
    public String checkoutSelected(@RequestParam(value = "cartItemIds", required = false) List<Long> cartItemIds,
            Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }

        User user = userOpt.get();

        // 如果传入了选中的购物车项ID，只显示选中的商品
        List<CartItem> cartItems;
        if (cartItemIds != null && !cartItemIds.isEmpty()) {
            cartItems = cartService.getCartItems(user).stream()
                    .filter(item -> cartItemIds.contains(item.getId()))
                    .collect(java.util.stream.Collectors.toList());
        } else {
            // 如果没有选择，显示所有商品
            cartItems = cartService.getCartItems(user);
        }

        if (cartItems.isEmpty()) {
            return "redirect:/cart";
        }

        // 计算总金额
        BigDecimal totalAmount = cartItems.stream()
                .map(item -> item.getProduct().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("user", user);
        model.addAttribute("pageTitle", "确认订单");

        return "user/checkout";
    }

    /**
     * GET /checkout/direct - 直接购买（从商品详情页）
     */
    @GetMapping("/direct")
    public String directCheckout(@RequestParam Long productId,
            @RequestParam(defaultValue = "1") Integer quantity,
            Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }

        User user = userOpt.get();

        try {
            CartItem cartItem = cartService.addToCart(productId, quantity, user);
            // 只带上当前加购/合并的购物车项 ID
            return "redirect:/checkout?cartItemIds=" + cartItem.getId();
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/product/" + productId;
        }
    }
}
