
package com.example.shopsite.controller.merchant;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/merchant/logs")
@PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
public class MerchantLogController {

    /**
     * GET /merchant/logs - 重定向到客户管理（日志现在在客户详情中查看）
     */
    @GetMapping
    public String logs() {
        return "redirect:/merchant/customers";
    }
}






