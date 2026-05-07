package com.example.shopsite.security;

import com.example.shopsite.model.AdminOperationLog;
import com.example.shopsite.model.Role;
import com.example.shopsite.model.User;
import com.example.shopsite.repository.UserRepository;
import com.example.shopsite.support.ClientIpExtractor;
import com.example.shopsite.service.AdminOperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 记录商户、管理员在后台的 POST 操作（操作时间、内容摘要、IP、账号）。
 */
@Component
public class OperationLogInterceptor implements HandlerInterceptor {

    private final UserRepository userRepository;
    private final AdminOperationLogService adminOperationLogService;

    public OperationLogInterceptor(UserRepository userRepository,
                                   AdminOperationLogService adminOperationLogService) {
        this.userRepository = userRepository;
        this.adminOperationLogService = adminOperationLogService;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        if (ex != null) {
            return;
        }
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return;
        }
        if (response.getStatus() >= 400) {
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return;
        }

        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) {
            return;
        }
        Role role = user.getRole();
        if (role != Role.MERCHANT && role != Role.ADMIN) {
            return;
        }

        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isBlank() && uri.startsWith(ctx)) {
            uri = uri.substring(ctx.length());
        }

        String type = truncate(uri.replaceFirst("^/+", ""), 100);
        String content = buildContentSummary(request);

        AdminOperationLog log = AdminOperationLog.builder()
                .operator(user)
                .operatorRole(role.name())
                .operationType(type.isEmpty() ? "POST" : type)
                .content(content)
                .ip(ClientIpExtractor.resolve(request))
                .account(truncate(user.getUsername(), 100))
                .build();

        try {
            adminOperationLogService.create(log);
        } catch (Exception ignored) {
            // 不因日志失败阻断请求
        }
    }

    private static String buildContentSummary(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append(request.getMethod()).append(' ').append(request.getRequestURI());
        String q = request.getQueryString();
        if (q != null && !q.isBlank()) {
            sb.append('?').append(truncate(q, 400));
        }
        return truncate(sb.toString(), 4000);
    }

    private static String truncate(String v, int maxLen) {
        if (v == null) {
            return null;
        }
        if (v.length() <= maxLen) {
            return v;
        }
        return v.substring(0, maxLen);
    }
}
