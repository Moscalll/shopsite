package com.example.shopsite.controller.admin;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.shopsite.model.AdminOperationLog;
import com.example.shopsite.model.AuthLoginLog;
import com.example.shopsite.model.UserBehaviorLog;
import com.example.shopsite.service.AdminOperationLogService;
import com.example.shopsite.service.AuthLoginLogService;
import com.example.shopsite.service.UserBehaviorLogService;

@Controller
@RequestMapping("/admin/logs")
@PreAuthorize("hasRole('ADMIN')")
public class AdminLogController {

    private final AuthLoginLogService authLoginLogService;
    private final UserBehaviorLogService userBehaviorLogService;
    private final AdminOperationLogService adminOperationLogService;

    public AdminLogController(AuthLoginLogService authLoginLogService,
                              UserBehaviorLogService userBehaviorLogService,
                              AdminOperationLogService adminOperationLogService) {
        this.authLoginLogService = authLoginLogService;
        this.userBehaviorLogService = userBehaviorLogService;
        this.adminOperationLogService = adminOperationLogService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("pageTitle", "日志管理");
        return "admin/logs/index";
    }

    @GetMapping("/login")
    public String loginLogs(Model model) {
        model.addAttribute("pageTitle", "登录日志");
        model.addAttribute("logs", List.of());
        return "admin/logs/login_logs";
    }

    @GetMapping("/behavior")
    public String behaviorLogs(Model model) {
        model.addAttribute("pageTitle", "浏览日志");
        model.addAttribute("logs", List.of());
        return "admin/logs/behavior_logs";
    }

    @GetMapping("/operation")
    public String operationLogs(Model model) {
        model.addAttribute("pageTitle", "操作日志");
        model.addAttribute("logs", List.of());
        return "admin/logs/operation_logs";
    }

    @PostMapping("/login/{id}/delete")
    public String deleteLoginLog(@PathVariable Long id) {
        authLoginLogService.delete(id);
        return "redirect:/admin/logs/login";
    }

    @PostMapping("/behavior/{id}/delete")
    public String deleteBehaviorLog(@PathVariable Long id) {
        userBehaviorLogService.delete(id);
        return "redirect:/admin/logs/behavior";
    }

    @PostMapping("/operation/{id}/delete")
    public String deleteOperationLog(@PathVariable Long id) {
        adminOperationLogService.delete(id);
        return "redirect:/admin/logs/operation";
    }

    @PostMapping("/login/{id}/update")
    public String updateLoginLog(@PathVariable Long id,
                                 @RequestParam(required = false) String role) {
        authLoginLogService.update(id, AuthLoginLog.builder().role(role).build());
        return "redirect:/admin/logs/login";
    }

    @PostMapping("/behavior/{id}/update")
    public String updateBehaviorLog(@PathVariable Long id,
                                    @RequestParam(required = false) String eventType) {
        userBehaviorLogService.update(id, UserBehaviorLog.builder().eventType(eventType).build());
        return "redirect:/admin/logs/behavior";
    }

    @PostMapping("/operation/{id}/update")
    public String updateOperationLog(@PathVariable Long id,
                                     @RequestParam(required = false) String operationType) {
        adminOperationLogService.update(id, AdminOperationLog.builder().operationType(operationType).build());
        return "redirect:/admin/logs/operation";
    }
}
