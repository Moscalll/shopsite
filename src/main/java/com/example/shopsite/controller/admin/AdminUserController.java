package com.example.shopsite.controller.admin;

import com.example.shopsite.model.Role;
import com.example.shopsite.model.User;
import com.example.shopsite.model.Order;
import com.example.shopsite.model.AuthLoginLog;
import com.example.shopsite.model.UserBehaviorLog;
import com.example.shopsite.repository.AuthLoginLogRepository;
import com.example.shopsite.repository.OrderRepository;
import com.example.shopsite.repository.UserBehaviorLogRepository;
import com.example.shopsite.repository.UserRepository;
import com.example.shopsite.service.UserService;
import com.example.shopsite.support.CategoryLabelService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.FileWriter;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final AuthLoginLogRepository authLoginLogRepository;
    private final UserBehaviorLogRepository userBehaviorLogRepository;
    private final CategoryLabelService categoryLabelService;
    private final OrderRepository orderRepository;

    public AdminUserController(UserRepository userRepository,
                               UserService userService,
                               AuthLoginLogRepository authLoginLogRepository,
                               UserBehaviorLogRepository userBehaviorLogRepository,
                               CategoryLabelService categoryLabelService,
                               OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.authLoginLogRepository = authLoginLogRepository;
        this.userBehaviorLogRepository = userBehaviorLogRepository;
        this.categoryLabelService = categoryLabelService;
        this.orderRepository = orderRepository;
    }

    @GetMapping
    public String users(@RequestParam(required = false) String keyword, Model model) {
        List<User> users = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.CUSTOMER)
                .toList();

        if (keyword != null && !keyword.trim().isEmpty()) {
            String k = keyword.trim().toLowerCase();
            users = users.stream()
                    .filter(u -> (u.getUsername() != null && u.getUsername().toLowerCase().contains(k))
                            || (u.getEmail() != null && u.getEmail().toLowerCase().contains(k)))
                    .toList();
        }

        users = users.stream()
                .sorted(Comparator.comparing((User u) -> u.getId() == null ? Long.MAX_VALUE : u.getId()))
                .toList();

        model.addAttribute("pageTitle", "用户管理");
        model.addAttribute("activeMenu", "users");
        model.addAttribute("keyword", keyword);
        model.addAttribute("users", users);
        return "admin/user_list";
    }

    @GetMapping("/{id}")
    public String userDetail(@PathVariable Long id, Model model) {
        // #region agent log
        debugLog("pre-fix", "H1", "AdminUserController.java:userDetail:entry",
                "Enter /admin/users/{id}", Map.of("id", id));
        // #endregion
        try {
            User user = userService.findUserById(id);
            if (user == null) {
                // #region agent log
                debugLog("pre-fix", "H2", "AdminUserController.java:userDetail:userNull",
                        "userService returned null", Map.of("id", id));
                // #endregion
                model.addAttribute("error", "用户未找到");
                return "error/404";
            }
            // #region agent log
            debugLog("pre-fix", "H2", "AdminUserController.java:userDetail:userLoaded",
                    "Loaded user", Map.of(
                            "id", id,
                            "role", user != null && user.getRole() != null ? user.getRole().name() : null,
                            "enabled", user != null ? user.getEnabled() : null
                    ));
            // #endregion

            if (user.getRole() != Role.CUSTOMER) {
                model.addAttribute("error", "该用户不是普通用户");
                return "error/404";
            }

            List<AuthLoginLog> loginLogs = user.getId() == null ? List.of()
                    : authLoginLogRepository.findByUser_IdOrderByLoginTimeDesc(user.getId());
            List<UserBehaviorLog> behaviorLogs = user.getId() == null ? List.of()
                    : userBehaviorLogRepository.findByUser_IdOrderByEventTimeDesc(user.getId());
            Map<Long, String> categoryNames = categoryLabelService.labelsForBehaviorLogs(behaviorLogs);
            List<Order> orders = orderRepository.findByUser(user);

            long nullOrderStatus = orders == null ? 0L : orders.stream().filter(o -> o == null || o.getStatus() == null).count();
            // #region agent log
            debugLog("pre-fix", "H3", "AdminUserController.java:userDetail:dataFetched",
                    "Fetched detail lists", Map.of(
                            "loginLogs", loginLogs == null ? null : loginLogs.size(),
                            "behaviorLogs", behaviorLogs == null ? null : behaviorLogs.size(),
                            "orders", orders == null ? null : orders.size(),
                            "ordersNullStatus", nullOrderStatus
                    ));
            // #endregion

            model.addAttribute("pageTitle", "用户详情: " + user.getUsername());
            model.addAttribute("activeMenu", "users");
            model.addAttribute("user", user);
            model.addAttribute("loginLogs", loginLogs);
            model.addAttribute("behaviorLogs", behaviorLogs);
            model.addAttribute("categoryNames", categoryNames);
            model.addAttribute("orders", orders);
            return "admin/user_detail";
        } catch (Exception ex) {
            // #region agent log
            debugLog("pre-fix", "H4", "AdminUserController.java:userDetail:exception",
                    "Exception in userDetail", Map.of(
                            "id", id,
                            "ex", ex.getClass().getName(),
                            "msg", truncate(ex.getMessage(), 300)
                    ));
            // #endregion
            model.addAttribute("pageTitle", "用户详情");
            model.addAttribute("activeMenu", "users");
            model.addAttribute("error", "用户详情加载失败：" + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
            return "admin/user_detail";
        }
    }

    private static void debugLog(String runId, String hypothesisId, String location, String message, Map<String, Object> data) {
        try (FileWriter fw = new FileWriter("D:/GitHub/shopsite/debug-fcad12.log", true)) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sessionId", "fcad12");
            payload.put("runId", runId);
            payload.put("hypothesisId", hypothesisId);
            payload.put("location", location);
            payload.put("message", message);
            payload.put("data", data);
            payload.put("timestamp", Instant.now().toEpochMilli());
            fw.write(toJson(payload));
            fw.write("\n");
        } catch (Exception ignored) {
        }
    }

    private static String toJson(Object v) {
        if (v == null) return "null";
        if (v instanceof String) return "\"" + escape((String) v) + "\"";
        if (v instanceof Number || v instanceof Boolean) return String.valueOf(v);
        if (v instanceof Map<?, ?> m) {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            boolean first = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!first) sb.append(",");
                first = false;
                sb.append(toJson(String.valueOf(e.getKey()))).append(":").append(toJson(e.getValue()));
            }
            sb.append("}");
            return sb.toString();
        }
        return "\"" + escape(String.valueOf(v)) + "\"";
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    private static String truncate(String v, int maxLen) {
        if (v == null) return null;
        if (v.length() <= maxLen) return v;
        return v.substring(0, maxLen);
    }

    @PostMapping("/{id}/toggle-enabled")
    public String toggleEnabled(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findUserById(id);
            if (user.getRole() != Role.CUSTOMER) {
                redirectAttributes.addFlashAttribute("error", "只能注销普通用户");
                return "redirect:/admin/users";
            }
            user.setEnabled(!Boolean.TRUE.equals(user.getEnabled()));
            userRepository.save(user);
            redirectAttributes.addFlashAttribute("success", Boolean.TRUE.equals(user.getEnabled()) ? "用户已恢复" : "用户已注销(禁用)");
            return "redirect:/admin/users/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/users";
        }
    }

    @PostMapping("/{id}/role")
    public String updateRole(@PathVariable Long id,
                             @RequestParam Role role,
                             RedirectAttributes redirectAttributes) {
        try {
            userService.updateUserRole(id, role);
            redirectAttributes.addFlashAttribute("success", "用户角色已更新");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }
}

