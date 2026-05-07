package com.example.shopsite.security;

import com.example.shopsite.model.AuthLoginLog;
import com.example.shopsite.model.User;
import com.example.shopsite.repository.UserRepository;
import com.example.shopsite.support.ClientIpExtractor;
import com.example.shopsite.service.AuthLoginLogService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final AuthLoginLogService authLoginLogService;
    private final UserRepository userRepository;

    public CustomAuthenticationFailureHandler(AuthLoginLogService authLoginLogService,
                                             UserRepository userRepository) {
        this.authLoginLogService = authLoginLogService;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String username = request.getParameter("username");
        Optional<User> userOpt = (username == null || username.isBlank())
                ? Optional.empty()
                : userRepository.findByUsername(username.trim());

        User user = userOpt.orElse(null);
        String role = user != null && user.getRole() != null ? user.getRole().name() : "UNKNOWN";

        AuthLoginLog log = AuthLoginLog.builder()
                .user(user)
                .role(role)
                .ip(ClientIpExtractor.resolve(request))
                .userAgent(truncate(request.getHeader("User-Agent"), 512))
                .sessionId(request.getSession(false) != null ? request.getSession(false).getId() : null)
                .success(false)
                .failureReason(truncate(exception.getMessage(), 512))
                .build();
        // #region agent log
        {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("usernamePresent", username != null && !username.isBlank());
            data.put("userFound", user != null);
            data.put("userId", user != null ? user.getId() : null);
            data.put("role", role);
            data.put("ip", log.getIp());
            data.put("hasXFF", request.getHeader("X-Forwarded-For") != null);
            data.put("success", log.getSuccess());
            DebugNdjsonLogger.log("pre-fix", "H1", "CustomAuthenticationFailureHandler.java:authLoginLogService.create", "Attempt create AuthLoginLog (failure)", data);
        }
        // #endregion
        try {
            authLoginLogService.create(log);
            // #region agent log
            DebugNdjsonLogger.log("pre-fix", "H1", "CustomAuthenticationFailureHandler.java:afterCreate", "AuthLoginLog created (failure)", Map.of("ok", true));
            // #endregion
        } catch (Exception ex) {
            // #region agent log
            DebugNdjsonLogger.log("pre-fix", "H3", "CustomAuthenticationFailureHandler.java:catch", "AuthLoginLog create failed; login continues", Map.of(
                    "ex", ex.getClass().getName(),
                    "msg", truncate(ex.getMessage(), 256)
            ));
            // #endregion
        }

        response.sendRedirect("/login?error");
    }

    private static String truncate(String v, int maxLen) {
        if (v == null) return null;
        String s = v.trim();
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen);
    }
}

