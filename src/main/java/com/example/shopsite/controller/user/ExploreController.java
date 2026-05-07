package com.example.shopsite.controller.user;

import com.example.shopsite.model.Product;
import com.example.shopsite.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Controller
public class ExploreController {

    private final ProductService productService;

    public ExploreController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/explore")
    public String explore(@RequestParam(required = false) Long seed, Model model) {
        long s = seed != null ? seed : System.currentTimeMillis();

        List<Product> all = productService.findAllProducts().stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsAvailable()) && p.getStock() != null && p.getStock() > 0)
                .toList();

        List<Product> shuffled = new ArrayList<>(all);
        Collections.shuffle(shuffled, new Random(s));

        // 20 行 × 6 列
        int total = 20 * 6;
        List<Object> feed = new ArrayList<>(total);
        feed.addAll(shuffled.stream().limit(total).toList());
        while (feed.size() < total) {
            feed.add(null); // 占位卡
        }

        model.addAttribute("pageTitle", "逛一逛");
        model.addAttribute("seed", s);
        model.addAttribute("nextSeed", System.currentTimeMillis());
        model.addAttribute("feed", feed);
        return "user/explore";
    }
}

