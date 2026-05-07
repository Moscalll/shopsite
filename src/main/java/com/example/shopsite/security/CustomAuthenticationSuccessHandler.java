package com.example.shopsite.security;

import com.example.shopsite.model.AuthLoginLog;
import com.example.shopsite.model.User;
import com.example.shopsite.repository.UserRepository;
import com.example.shopsite.support.ClientIpExtractor;
import com.example.shopsite.service.AuthLoginLogService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthLoginLogService authLoginLogService;
    private final UserRepository userRepository;

    public CustomAuthenticationSuccessHandler(AuthLoginLogService authLoginLogService,
                                              UserRepository userRepository) {
        this.authLoginLogService = authLoginLogService;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                       HttpServletResponse response,
                                       Authentication authentication) throws IOException, ServletException {
        User user = null;
        if (authentication != null) {
            // Spring Security 默认 principal 是 org.springframework.security.core.userdetails.User
            // 这里用 authentication.getName() 再查一次 DB，确保能拿到我们的 User 实体（含 id/role）
            user = userRepository.findByUsername(authentication.getName()).orElse(null);
        }
        String role = user != null && user.getRole() != null ? user.getRole().name() : "UNKNOWN";

        AuthLoginLog log = AuthLoginLog.builder()
                .user(user)
                .role(role)
                .ip(ClientIpExtractor.resolve(request))
                .userAgent(truncate(request.getHeader("User-Agent"), 512))
                .sessionId(request.getSession(false) != null ? request.getSession(false).getId() : null)
                .success(true)
                .build();
        // #region agent log
        {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("principalClass", authentication != null && authentication.getPrincipal() != null
                    ? authentication.getPrincipal().getClass().getName()
                    : null);
            data.put("userId", user != null ? user.getId() : null);
            data.put("role", role);
            data.put("ip", log.getIp());
            data.put("hasXFF", request.getHeader("X-Forwarded-For") != null);
            data.put("success", log.getSuccess());
            DebugNdjsonLogger.log("pre-fix", "H1", "CustomAuthenticationSuccessHandler.java:authLoginLogService.create", "Attempt create AuthLoginLog (success)", data);
        }
        // #endregion
        try {
            authLoginLogService.create(log);
            // #region agent log
            DebugNdjsonLogger.log("pre-fix", "H1", "CustomAuthenticationSuccessHandler.java:afterCreate", "AuthLoginLog created (success)", Map.of("ok", true));
            // #endregion
        } catch (Exception ex) {
            // #region agent log
            DebugNdjsonLogger.log("pre-fix", "H3", "CustomAuthenticationSuccessHandler.java:catch", "AuthLoginLog create failed; login continues", Map.of(
                    "ex", ex.getClass().getName(),
                    "msg", truncate(ex.getMessage(), 256)
            ));
            // #endregion
        }

        // 所有用户登录后都跳转到首页，通过导航栏进入各自的管理界面
        response.sendRedirect("/");
    }

    private static String truncate(String v, int maxLen) {
        if (v == null) return null;
        String s = v.trim();
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen);
    }
}



