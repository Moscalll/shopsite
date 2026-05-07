package com.example.shopsite.controller.admin;

import com.example.shopsite.model.AdminOperationLog;
import com.example.shopsite.model.AuthLoginLog;
import com.example.shopsite.model.Role;
import com.example.shopsite.model.User;
import com.example.shopsite.repository.AdminOperationLogRepository;
import com.example.shopsite.repository.AuthLoginLogRepository;
import com.example.shopsite.service.CategoryService;
import com.example.shopsite.service.ProductService;
import com.example.shopsite.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.RequestParam;
import com.example.shopsite.service.OrderService;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/merchants")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMerchantController {

    private final UserService userService;
    private final ProductService productService;
    private final OrderService orderService; 
    private final CategoryService categoryService;
    private final AuthLoginLogRepository authLoginLogRepository;
    private final AdminOperationLogRepository adminOperationLogRepository;

    public AdminMerchantController(UserService userService,
                                   ProductService productService,
                                   OrderService orderService,
                                   CategoryService categoryService,
                                   AuthLoginLogRepository authLoginLogRepository,
                                   AdminOperationLogRepository adminOperationLogRepository) {
        this.userService = userService;
        this.productService = productService;
        this.orderService = orderService;
        this.categoryService = categoryService;
        this.authLoginLogRepository = authLoginLogRepository;
        this.adminOperationLogRepository = adminOperationLogRepository;
    }

    /**
     * GET /admin/merchants - 商户列表
     */
    @GetMapping
    public String merchantList(@RequestParam(required = false) String keyword, Model model) {
        List<User> merchants;
        if (keyword != null && !keyword.trim().isEmpty()) {
            merchants = userService.findMerchantsByKeyword(keyword);
        } else {
            merchants = userService.findAllMerchants();
        }
        model.addAttribute("merchants", merchants);
        model.addAttribute("keyword", keyword);
        model.addAttribute("pageTitle", "商户管理");
        model.addAttribute("activeMenu", "merchants");
        return "admin/merchant_list";
    }

    /**
     * GET /admin/merchants/{id} - 商户详情（包含商品和订单）
     */
    @GetMapping("/{id}")
    public String merchantDetail(@PathVariable Long id,
                                 @RequestParam(required = false) Long productId,
                                 @RequestParam(required = false) String productName,
                                 @RequestParam(required = false) Long categoryId,
                                 Model model) {
        try {
            User merchant = userService.findUserById(id);
            if (merchant.getRole() != Role.MERCHANT) {
                model.addAttribute("error", "该用户不是商户");
                return "error/404";
            }

            // 查询该商户的商品
            var products = productService.findProductsByMerchant(merchant);
            if (productId != null) {
                products = products.stream()
                        .filter(p -> p.getId() != null && p.getId().equals(productId))
                        .collect(Collectors.toList());
            }
            if (productName != null && !productName.trim().isEmpty()) {
                String k = productName.trim().toLowerCase();
                products = products.stream()
                        .filter(p -> (p.getName() != null && p.getName().toLowerCase().contains(k)))
                        .collect(Collectors.toList());
            }
            if (categoryId != null) {
                products = products.stream()
                        .filter(p -> p.getCategory() != null && p.getCategory().getId() != null
                                && p.getCategory().getId().equals(categoryId))
                        .collect(Collectors.toList());
            }

            // 查询该商户的订单
            var orders = orderService.findOrdersByMerchant(merchant);

            List<AuthLoginLog> loginLogs = merchant.getId() == null ? List.of()
                    : authLoginLogRepository.findByUser_IdOrderByLoginTimeDesc(merchant.getId());
            List<AdminOperationLog> operationLogs = merchant.getId() == null ? List.of()
                    : adminOperationLogRepository.findByOperator_IdOrderByOperationTimeDesc(merchant.getId());

            model.addAttribute("merchant", merchant);
            model.addAttribute("loginLogs", loginLogs);
            model.addAttribute("operationLogs", operationLogs);
            model.addAttribute("products", products);
            model.addAttribute("orders", orders);
            model.addAttribute("allCategories", categoryService.findAllCategories());
            model.addAttribute("productId", productId);
            model.addAttribute("productName", productName);
            model.addAttribute("categoryId", categoryId);
            model.addAttribute("pageTitle", "商户详情: " + merchant.getUsername());
            model.addAttribute("activeMenu", "merchants");
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
