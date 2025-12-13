package com.example.shopsite.controller.merchant;

import com.example.shopsite.model.Product;
import com.example.shopsite.model.SalesLog;
import com.example.shopsite.model.User;
import com.example.shopsite.repository.ProductRepository;
import com.example.shopsite.repository.SalesLogRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/merchant/logs")
@PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
public class MerchantLogController {

    private final SalesLogRepository salesLogRepository;
    private final ProductRepository productRepository;

    public MerchantLogController(SalesLogRepository salesLogRepository, ProductRepository productRepository) {
        this.salesLogRepository = salesLogRepository;
        this.productRepository = productRepository;
    }

    /**
     * GET /merchant/logs - 浏览/购买日志
     */
    @GetMapping
    public String logs(@AuthenticationPrincipal User merchant,
                      @RequestParam(required = false) String actionType,
                      Model model) {
        // 获取商户的所有商品ID
        List<Long> productIds = productRepository.findByMerchant(merchant).stream()
                .map(Product::getId)
                .collect(Collectors.toList());
        
        // 查询相关日志
        List<SalesLog> logs;
        if (actionType != null && !actionType.isEmpty()) {
            logs = salesLogRepository.findByProductIdInAndActionType(productIds, actionType);
        } else {
            logs = salesLogRepository.findByProductIdIn(productIds);
        }
        
        // 按时间倒序排列
        logs.sort((a, b) -> b.getLogTime().compareTo(a.getLogTime()));
        
        model.addAttribute("logs", logs);
        model.addAttribute("actionType", actionType);
        model.addAttribute("pageTitle", "浏览/购买日志");
        
        return "merchant/logs";
    }
}



