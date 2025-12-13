package com.example.shopsite.controller.merchant;

import com.example.shopsite.model.Order;
import com.example.shopsite.model.User;
import com.example.shopsite.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/merchant/orders")
@PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
public class MerchantOrderController {

    private final OrderService orderService;

    public MerchantOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * GET /merchant/orders - 商户订单列表
     */
    @GetMapping
    public String orderList(@AuthenticationPrincipal User merchant, Model model) {
        List<Order> orders = orderService.findOrdersByMerchant(merchant);
        model.addAttribute("orders", orders);
        model.addAttribute("pageTitle", "订单管理");
        return "merchant/order_list";
    }

    /**
     * GET /merchant/orders/{id} - 订单详情
     */
    @GetMapping("/{id}")
    public String orderDetail(@PathVariable Long id,
                             @AuthenticationPrincipal User merchant,
                             Model model) {
        try {
            Order order = orderService.findOrderDetailsForMerchant(id, merchant);
            model.addAttribute("order", order);
            model.addAttribute("pageTitle", "订单详情");
            return "merchant/order_detail";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error/404";
        }
    }

    /**
     * POST /merchant/orders/{id}/ship - 发货
     */
    @PostMapping("/{id}/ship")
    public String shipOrder(@PathVariable Long id,
                           @AuthenticationPrincipal User merchant,
                           RedirectAttributes redirectAttributes) {
        try {
            orderService.shipOrder(id, merchant);
            redirectAttributes.addFlashAttribute("success", "订单已发货");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/merchant/orders";
    }
}



