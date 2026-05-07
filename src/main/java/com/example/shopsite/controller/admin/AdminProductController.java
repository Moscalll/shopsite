package com.example.shopsite.controller.admin;

import com.example.shopsite.model.Category;
import com.example.shopsite.model.Product;
import com.example.shopsite.service.CategoryService;
import com.example.shopsite.service.ProductService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/products")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private final ProductService productService;
    private final CategoryService categoryService;

    public AdminProductController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @GetMapping
    public String productList(@RequestParam(required = false) String keyword,
                             @RequestParam(required = false) Long categoryId,
                             Model model) {
        List<Product> products;
        if (keyword != null && !keyword.trim().isEmpty()) {
            products = productService.searchProducts(keyword);
        } else if (categoryId != null) {
            products = productService.findProductsByCategory(categoryId, Integer.MAX_VALUE);
        } else {
            products = productService.findAllProducts();
        }
        
        List<Category> categories = categoryService.findAllCategories();

        Map<Long, List<Product>> productsByCategory = products.stream()
                .filter(p -> p.getCategory() != null && p.getCategory().getId() != null)
                .collect(Collectors.groupingBy(p -> p.getCategory().getId()));

        List<Category> categoriesToShow = new ArrayList<>();
        if (categoryId != null) {
            for (Category c : categories) {
                if (c.getId() != null && c.getId().equals(categoryId)) {
                    categoriesToShow.add(c);
                    break;
                }
            }
        } else if (keyword != null && !keyword.trim().isEmpty()) {
            for (Category c : categories) {
                if (c.getId() != null && productsByCategory.containsKey(c.getId())) {
                    categoriesToShow.add(c);
                }
            }
        } else {
            categoriesToShow = categories;
        }
        
        model.addAttribute("products", products);
        model.addAttribute("categories", categories);
        model.addAttribute("categoriesToShow", categoriesToShow);
        model.addAttribute("productsByCategory", productsByCategory);
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("pageTitle", "商品管理");
        model.addAttribute("activeMenu", "products");
        return "admin/product_list";
    }
}