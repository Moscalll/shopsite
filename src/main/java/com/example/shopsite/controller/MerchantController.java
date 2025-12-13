package com.example.shopsite.controller;

import com.example.shopsite.model.Product;
import com.example.shopsite.model.User;
import com.example.shopsite.model.Category;
import com.example.shopsite.service.ProductService;
import com.example.shopsite.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize; // 权限注解
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/merchant")
// 🚨 限制：只有拥有 ROLE_MERCHANT 或 ROLE_ADMIN 角色的用户才能访问此 Controller 内的方法
@PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')") 
public class MerchantController {

    private final ProductService productService;
    private final CategoryService categoryService;

    @Autowired
    public MerchantController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    /**
     * GET /merchant/dashboard
     * 商户后台主页/商品列表
     */
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal User merchant, Model model) {
        // 1. 获取当前商户的所有商品
        List<Product> products = productService.findProductsByMerchant(merchant);

        model.addAttribute("pageTitle", "商户商品管理");
        model.addAttribute("products", products);
        
        // 假设模板路径为 templates/merchant/dashboard.html
        return "merchant/dashboard"; 
    }

    /**
     * GET /merchant/product/new 或 /merchant/product/edit/{id}
     * 显示创建或编辑商品的表单
     */
    @GetMapping({"/product/new", "/product/edit/{id}"})
    public String showProductForm(
            @PathVariable(required = false) Long id,
            @AuthenticationPrincipal User merchant, 
            Model model) {
                
        List<Category> categories = categoryService.findAllCategories();
        model.addAttribute("categories", categories);
        model.addAttribute("product", new Product()); // 默认新建商品对象

        if (id != null) {
            // 编辑现有商品逻辑
            Optional<Product> productOpt = productService.findAvailableProductById(id);
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                // 🚨 权限校验：确保商户只能编辑自己的商品
                if (!product.getMerchant().getId().equals(merchant.getId())) {
                    model.addAttribute("error", "权限不足，无法编辑该商品。");
                    return "merchant/dashboard"; // 重定向回列表页
                }
                model.addAttribute("product", product);
                model.addAttribute("pageTitle", "编辑商品: " + product.getName());
            } else {
                model.addAttribute("error", "商品未找到。");
                return "merchant/dashboard";
            }
        } else {
            // 新建商品逻辑
            model.addAttribute("pageTitle", "创建新商品");
        }

        // 假设模板路径为 templates/merchant/product_form.html
        return "merchant/product_form"; 
    }
    
    /**
     * POST /merchant/save
     * 处理商品创建和编辑的表单提交
     * * 🚨 注意：实际应用中，文件上传(如 imageUrl)应通过单独的 REST API 处理
     */
    @PostMapping("/save")
    public String saveProduct(
            @ModelAttribute Product product, 
            @RequestParam Long categoryId, // 从表单中获取 categoryId
            @AuthenticationPrincipal User merchant) {

        if (product.getId() == null) {
            // 创建新商品
            productService.createProduct(product, categoryId, merchant);
        } else {
            // 更新现有商品
            productService.updateProduct(product.getId(), product, merchant);
        }

        // 完成后重定向到商户后台列表
        return "redirect:/merchant/dashboard";
    }
}