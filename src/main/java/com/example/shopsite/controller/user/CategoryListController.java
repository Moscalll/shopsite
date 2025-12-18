package com.example.shopsite.controller.user;

import com.example.shopsite.model.Product;
import com.example.shopsite.service.CategoryService;
import com.example.shopsite.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CategoryListController {

    private final ProductService productService;
    private final CategoryService categoryService;

    public CategoryListController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    /**
     * GET /products?categoryId={id} - 分类商品列表
     */
    @GetMapping("/products")
    public String categoryProducts(@RequestParam(required = false) Long categoryId,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "12") int size,
                                   Model model) {
        Pageable pageable = PageRequest.of(page, size);
        
        if (categoryId != null) {
            // 按分类查询
            Page<Product> productPage = productService.findProductsByCategory(categoryId, pageable);
            model.addAttribute("products", productPage.getContent());
            model.addAttribute("totalPages", productPage.getTotalPages());
            model.addAttribute("currentPage", page);
            model.addAttribute("category", categoryService.findCategoryById(categoryId).orElse(null));
        } else {
            // 所有商品
            model.addAttribute("products", productService.findAllAvailableProducts());
        }
        
        model.addAttribute("categories", categoryService.findAllCategories());
        model.addAttribute("pageTitle", "商品列表");
        return "user/products_list";
    }
}
