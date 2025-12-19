package com.example.shopsite.controller;

import com.example.shopsite.model.Product;
import com.example.shopsite.model.User;
import com.example.shopsite.model.Category;
import com.example.shopsite.repository.ProductRepository;
import com.example.shopsite.repository.UserRepository; 
import com.example.shopsite.repository.OrderItemRepository;
import com.example.shopsite.repository.CartItemRepository;
import com.example.shopsite.repository.FavoriteRepository;
import com.example.shopsite.service.ProductService;
import com.example.shopsite.service.CategoryService;
import com.example.shopsite.service.FileUploadService;
import com.example.shopsite.model.OrderItem;
import com.example.shopsite.model.CartItem;
import com.example.shopsite.model.Favorite;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder; 
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.shopsite.model.Role;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/merchant")
// 限制：只有拥有 ROLE_MERCHANT 或 ROLE_ADMIN 角色的用户才能访问此 Controller 内的方法
@PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
public class MerchantController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final ProductRepository productRepository;
    private final FileUploadService fileUploadService;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final FavoriteRepository favoriteRepository;

    @Autowired
    public MerchantController(ProductService productService, CategoryService categoryService,
            ProductRepository productRepository, FileUploadService fileUploadService,
            UserRepository userRepository, OrderItemRepository orderItemRepository,
            CartItemRepository cartItemRepository, FavoriteRepository favoriteRepository) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.productRepository = productRepository;
        this.fileUploadService = fileUploadService;
        this.userRepository = userRepository; 
        this.orderItemRepository = orderItemRepository;
        this.cartItemRepository = cartItemRepository;
        this.favoriteRepository = favoriteRepository;
    }

    /**
     * GET /merchant/dashboard
     * 商户后台主页/商品列表
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // 从 SecurityContext 获取当前登录用户的用户名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            model.addAttribute("error", "用户未找到");
            return "merchant/dashboard";
        }
        User merchant = userOpt.get();

        // 1. 获取商品列表：管理员查看所有商品，商户查看自己的商品
        List<Product> products;
        if (merchant.getRole() == Role.ADMIN) {
            products = productService.findAllProducts();
        } else {
            products = productService.findProductsByMerchant(merchant);
        }

        model.addAttribute("pageTitle", "商户商品管理");
        model.addAttribute("products", products);

        // 假设模板路径为 templates/merchant/dashboard.html
        return "merchant/dashboard";
    }

    /**
     * GET /merchant/product/new 或 /merchant/product/edit/{id}
     * 显示创建或编辑商品的表单
     */
    @GetMapping({ "/product/new", "/product/edit/{id}" })
    public String showProductForm(
            @PathVariable(required = false) Long id,
            Model model) {

        // 从 SecurityContext 获取当前登录用户的用户名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            model.addAttribute("error", "用户未找到");
            return "merchant/dashboard";
        }
        User merchant = userOpt.get();

        List<Category> categories = categoryService.findAllCategories();
        model.addAttribute("categories", categories);
        model.addAttribute("product", new Product());

        if (id != null) {
            // 编辑现有商品逻辑
            Optional<Product> productOpt = productRepository.findById(id);
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                // 检查权限：管理员可以编辑所有商品，商户只能编辑自己的商品
                boolean isAdmin = merchant.getRole() == Role.ADMIN;
                if (!isAdmin && !product.getMerchant().getId().equals(merchant.getId())) {
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

        // 模板路径为 templates/merchant/product_form.html
        return "merchant/product_form";
    }


    /**
     * 根据用户角色返回正确的重定向路径
     */
    private String getRedirectPath(User user) {
        if (user.getRole() == Role.ADMIN) {
            return "redirect:/admin/products";
        } else {
            return "redirect:/merchant/dashboard";
        }
    }


    /**
     * POST /merchant/save
     * 处理商品创建和编辑的表单提交（支持图片上传）
     */
    @PostMapping("/save")
    public String saveProduct(
            @ModelAttribute Product product,
            @RequestParam Long categoryId,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes redirectAttributes) {

        // 从 SecurityContext 获取当前登录用户的用户名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        // 从数据库加载 User 实体
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "用户未找到");
            return "redirect:/merchant/dashboard";
        }

        User merchant = userOpt.get();
        boolean isAdmin = merchant.getRole() == Role.ADMIN;

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
                // 管理员创建商品时，需要指定商户（从商品对象中获取）
                // 如果是管理员且商品没有指定商户，使用当前登录用户作为商户
                if (isAdmin && product.getMerchant() == null) {
                    product.setMerchant(merchant);
                    redirectAttributes.addFlashAttribute("success", "商品创建成功");
                } else if (!isAdmin) {
                    // 商户创建商品时，确保商户是自己
                    product.setMerchant(merchant);
                }
                productService.createProduct(product, categoryId, merchant);
                redirectAttributes.addFlashAttribute("success", "商品创建成功");
            } else {
                // 更新现有商品
                Optional<Product> existingProductOpt = productRepository.findById(product.getId());
                if (existingProductOpt.isPresent()) {
                    Product existingProduct = existingProductOpt.get();

                    // 检查权限：管理员可以编辑所有商品，商户只能编辑自己的商品
                    if (!isAdmin && !existingProduct.getMerchant().getId().equals(merchant.getId())) {
                        redirectAttributes.addFlashAttribute("error", "无权编辑该商品");
                        return "redirect:/merchant/dashboard";
                    }

                    // 如果上传了新图片，删除旧图片
                    if (imageFile != null && !imageFile.isEmpty() &&
                            existingProduct.getImageUrl() != null &&
                            existingProduct.getImageUrl().startsWith("/uploads/")) {
                        try {
                            fileUploadService.deleteImage(existingProduct.getImageUrl());
                        } catch (Exception e) {
                            System.err.println("删除旧图片失败: " + e.getMessage());
                        }
                    } else if (product.getImageUrl() == null || product.getImageUrl().isEmpty()) {
                        product.setImageUrl(existingProduct.getImageUrl());
                    }

                    // 管理员编辑时，保持原商户不变
                    if (isAdmin) {
                        product.setMerchant(existingProduct.getMerchant());
                    } else {
                        product.setMerchant(merchant);
                    }
                }
                productService.updateProduct(product.getId(), product, merchant);
                redirectAttributes.addFlashAttribute("success", "商品更新成功");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return getRedirectPath(merchant);
        }

        return getRedirectPath(merchant);
    }

    /**
     * POST /merchant/product/toggle/{id}
     * 切换商品上架/下架状态（商户和管理员都可以操作）
     */
    @PostMapping("/product/toggle/{id}")
    public String toggleProductStatus(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        User currentUser = null;
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
            // 从 SecurityContext 获取当前登录用户的用户名
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "用户未找到");
                return "redirect:/merchant/dashboard";
            }
            currentUser = userOpt.get();

            // 检查权限：管理员可以操作所有商品，商户只能操作自己的商品
            boolean isAdmin = currentUser.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

            if (!isAdmin && !product.getMerchant().getId().equals(currentUser.getId())) {
                redirectAttributes.addFlashAttribute("error", "无权操作该商品");
                return getRedirectPath(currentUser);
            }

            product.setIsAvailable(!product.getIsAvailable());
            productRepository.save(product);
            redirectAttributes.addFlashAttribute("success", product.getIsAvailable() ? "商品已上架" : "商品已下架");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
             // 在 catch 块中重新获取用户，确保可以正确重定向
             try {
                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                Optional<User> userOpt = userRepository.findByUsername(username);
                if (userOpt.isPresent()) {
                    currentUser = userOpt.get();
                }
            } catch (Exception ex) {
                // 如果获取用户失败，使用默认重定向
                return "redirect:/merchant/dashboard";
            }
        }
         // 如果 currentUser 仍为 null，使用默认重定向
         if (currentUser == null) {
            return "redirect:/merchant/dashboard";
        }
        
        return getRedirectPath(currentUser);
    }

    /**
     * POST /merchant/product/delete/{id}
     * 删除商品（商户和管理员都可以操作）
     */
    @PostMapping("/product/delete/{id}")
    @Transactional
    public String deleteProduct(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        User currentUser = null; 
        try {
            Optional<Product> productOpt = productRepository.findById(id);
            if (productOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "商品不存在");
                return "redirect:/merchant/dashboard";
            }

            Product product = productOpt.get();
            // 从 SecurityContext 获取当前登录用户的用户名
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "用户未找到");
                return "redirect:/merchant/dashboard";
            }
            currentUser = userOpt.get();

            // 检查权限：管理员可以删除所有商品，商户只能删除自己的商品
            boolean isAdmin = currentUser.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

            if (!isAdmin && !product.getMerchant().getId().equals(currentUser.getId())) {
                redirectAttributes.addFlashAttribute("error", "无权删除该商品");
                return "redirect:/merchant/dashboard";
            }

             // 检查商品是否被订单引用
             List<OrderItem> orderItems = orderItemRepository.findByProduct(product);
             boolean hasOrderItems = !orderItems.isEmpty();
            
             if (hasOrderItems) {
                // 如果有订单引用，不允许删除，使用软删除
                product.setIsAvailable(false);
                product.setStock(0);
                productRepository.save(product);
                redirectAttributes.addFlashAttribute("success", 
                    "商品已下架（该商品有 " + orderItems.size() + " 条订单记录，无法完全删除）");
                return "redirect:/merchant/dashboard";
            }
             
           // 删除所有相关的数据（按顺序删除以避免外键约束）
            // 1. 删除订单项（警告：这会破坏订单历史）
            if (!orderItems.isEmpty()) {
                orderItemRepository.deleteAll(orderItems);
            }
            
            // 2. 删除购物车项
            List<CartItem> cartItems = cartItemRepository.findByProduct(product);
            if (!cartItems.isEmpty()) {
                cartItemRepository.deleteAll(cartItems);
            }
            
            // 3. 删除收藏
            List<Favorite> favorites = favoriteRepository.findByProduct(product);
            if (!favorites.isEmpty()) {
                favoriteRepository.deleteAll(favorites);
            }
             
             // 删除商品本身
            productRepository.delete(product);
            redirectAttributes.addFlashAttribute("success", "商品已删除");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            // 在 catch 块中重新获取用户，确保可以正确重定向
            if (currentUser == null) {
                try {
                    String username = SecurityContextHolder.getContext().getAuthentication().getName();
                    Optional<User> userOpt = userRepository.findByUsername(username);
                    if (userOpt.isPresent()) {
                        currentUser = userOpt.get();
                    }
                } catch (Exception ex) {
                    // 如果获取用户失败，使用默认重定向
                    return "redirect:/merchant/dashboard";
                }
            }
        }
        // 如果 currentUser 仍为 null，使用默认重定向
        if (currentUser == null) {
            return "redirect:/merchant/dashboard";
        }
        return getRedirectPath(currentUser);
    }
}