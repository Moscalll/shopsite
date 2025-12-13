package com.example.shopsite.controller;

import com.example.shopsite.model.Product;
import com.example.shopsite.model.User;
import com.example.shopsite.model.Category;
import com.example.shopsite.repository.ProductRepository;
import com.example.shopsite.service.ProductService;
import com.example.shopsite.service.CategoryService;
import com.example.shopsite.service.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize; // 权限注解
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/merchant")
// 🚨 限制：只有拥有 ROLE_MERCHANT 或 ROLE_ADMIN 角色的用户才能访问此 Controller 内的方法
@PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')") 
public class MerchantController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final ProductRepository productRepository;
    private final FileUploadService fileUploadService;

    @Autowired
    public MerchantController(ProductService productService, CategoryService categoryService, 
                            ProductRepository productRepository, FileUploadService fileUploadService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.productRepository = productRepository;
        this.fileUploadService = fileUploadService;
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
     * 处理商品创建和编辑的表单提交（支持图片上传）
     */
    @PostMapping("/save")
    public String saveProduct(
            @ModelAttribute Product product, 
            @RequestParam Long categoryId, // 从表单中获取 categoryId
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @AuthenticationPrincipal User merchant,
            RedirectAttributes redirectAttributes) {

        try {
            // 处理图片上传
            if (imageFile != null && !imageFile.isEmpty()) {
                try {
                    String imagePath = fileUploadService.uploadImage(imageFile);
                    product.setImageUrl(imagePath);
                } catch (Exception e) {
                    redirectAttributes.addFlashAttribute("error", "图片上传失败: " + e.getMessage());
                    return "redirect:/merchant/dashboard";
                }
            }
            
            if (product.getId() == null) {
                // 创建新商品
                if (product.getImageUrl() == null || product.getImageUrl().isEmpty()) {
                    product.setImageUrl("/images/placeholder.jpg"); // 默认图片
                }
                productService.createProduct(product, categoryId, merchant);
                redirectAttributes.addFlashAttribute("success", "商品创建成功");
            } else {
                // 更新现有商品
                Optional<Product> existingProductOpt = productRepository.findById(product.getId());
                if (existingProductOpt.isPresent()) {
                    Product existingProduct = existingProductOpt.get();
                    // 如果上传了新图片，删除旧图片
                    if (imageFile != null && !imageFile.isEmpty() && 
                        existingProduct.getImageUrl() != null && 
                        existingProduct.getImageUrl().startsWith("/uploads/")) {
                        try {
                            fileUploadService.deleteImage(existingProduct.getImageUrl());
                        } catch (Exception e) {
                            // 记录日志但不影响更新
                            System.err.println("删除旧图片失败: " + e.getMessage());
                        }
                    } else if (product.getImageUrl() == null || product.getImageUrl().isEmpty()) {
                        // 如果没有上传新图片且没有设置URL，保留原图片
                        product.setImageUrl(existingProduct.getImageUrl());
                    }
                }
                productService.updateProduct(product.getId(), product, merchant);
                redirectAttributes.addFlashAttribute("success", "商品更新成功");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/merchant/dashboard";
        }

        // 完成后重定向到商户后台列表
        return "redirect:/merchant/dashboard";
    }
    
    /**
     * POST /merchant/product/toggle/{id}
     * 切换商品上架/下架状态
     */
    @PostMapping("/product/toggle/{id}")
    public String toggleProductStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal User merchant,
            RedirectAttributes redirectAttributes) {
        try {
            Optional<Product> productOpt = productService.findAvailableProductById(id);
            if (productOpt.isEmpty()) {
                // 尝试查找下架的商品
                productOpt = productRepository.findById(id);
            }
            
            if (productOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "商品不存在");
                return "redirect:/merchant/dashboard";
            }
            
            Product product = productOpt.get();
            if (!product.getMerchant().getId().equals(merchant.getId())) {
                redirectAttributes.addFlashAttribute("error", "无权操作该商品");
                return "redirect:/merchant/dashboard";
            }
            
            product.setIsAvailable(!product.getIsAvailable());
            productRepository.save(product);
            redirectAttributes.addFlashAttribute("success", product.getIsAvailable() ? "商品已上架" : "商品已下架");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/merchant/dashboard";
    }
    
    /**
     * POST /merchant/product/delete/{id}
     * 删除商品
     */
    @PostMapping("/product/delete/{id}")
    public String deleteProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal User merchant,
            RedirectAttributes redirectAttributes) {
        try {
            Optional<Product> productOpt = productRepository.findById(id);
            if (productOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "商品不存在");
                return "redirect:/merchant/dashboard";
            }
            
            Product product = productOpt.get();
            if (!product.getMerchant().getId().equals(merchant.getId())) {
                redirectAttributes.addFlashAttribute("error", "无权删除该商品");
                return "redirect:/merchant/dashboard";
            }
            
            // 删除商品时，同时删除关联的图片文件
            if (product.getImageUrl() != null && product.getImageUrl().startsWith("/uploads/")) {
                try {
                    fileUploadService.deleteImage(product.getImageUrl());
                } catch (Exception e) {
                    // 记录日志但不影响删除
                    System.err.println("删除商品图片失败: " + e.getMessage());
                }
            }
            
            productRepository.delete(product);
            redirectAttributes.addFlashAttribute("success", "商品已删除");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/merchant/dashboard";
    }
}