package com.example.shopsite.controller.admin;

import com.example.shopsite.model.Role;
import com.example.shopsite.model.User;
import com.example.shopsite.service.ProductService;
import com.example.shopsite.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/merchants")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMerchantController {

    private final UserService userService;
    private final ProductService productService;

    public AdminMerchantController(UserService userService, ProductService productService) {
        this.userService = userService;
        this.productService = productService;
    }

    /**
     * GET /admin/merchants - 商户列表
     */
    @GetMapping
    public String merchantList(Model model) {
        List<User> merchants = userService.findAllMerchants();
        model.addAttribute("merchants", merchants);
        model.addAttribute("pageTitle", "商户管理");
        return "admin/merchant_list";
    }

    /**
     * GET /admin/merchants/{id} - 商户详情（包含商品和订单）
     */
    @GetMapping("/{id}")
    public String merchantDetail(@PathVariable Long id, Model model) {
        try {
            User merchant = userService.findUserById(id);
            if (merchant.getRole() != Role.MERCHANT) {
                model.addAttribute("error", "该用户不是商户");
                return "error/404";
            }

            // 查询该商户的商品
            var products = productService.findProductsByMerchant(merchant);

            model.addAttribute("merchant", merchant);
            model.addAttribute("products", products);
            model.addAttribute("pageTitle", "商户详情: " + merchant.getUsername());
            return "admin/merchant_detail";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error/404";
        }
    }

    /**
     * POST /admin/merchants/{id}/toggle-role - 切换商户角色（启用/禁用）
     */
    @PostMapping("/{id}/toggle-role")
    public String toggleMerchantRole(@PathVariable Long id,
                                    RedirectAttributes redirectAttributes) {
        try {
            User merchant = userService.findUserById(id);
            if (merchant.getRole() == Role.MERCHANT) {
                // 禁用：将角色改为CUSTOMER
                userService.updateUserRole(id, Role.CUSTOMER);
                redirectAttributes.addFlashAttribute("success", "商户已禁用");
            } else if (merchant.getRole() == Role.CUSTOMER) {
                // 启用：将角色改为MERCHANT
                userService.updateUserRole(id, Role.MERCHANT);
                redirectAttributes.addFlashAttribute("success", "商户已启用");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/merchants";
    }
}






