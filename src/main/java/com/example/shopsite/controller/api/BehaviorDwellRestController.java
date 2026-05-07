package com.example.shopsite.controller.api;

import com.example.shopsite.dto.BehaviorDwellRequest;
import com.example.shopsite.model.Product;
import com.example.shopsite.model.Role;
import com.example.shopsite.model.User;
import com.example.shopsite.model.UserBehaviorLog;
import com.example.shopsite.repository.ProductRepository;
import com.example.shopsite.repository.UserRepository;
import com.example.shopsite.support.ClientIpExtractor;
import com.example.shopsite.service.UserBehaviorLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 普通用户商品详情页停留时长上报（写入 {@link UserBehaviorLog}：类别 + 停留秒数）。
 */
@RestController
@RequestMapping("/api/behavior")
public class BehaviorDwellRestController {

    public static final String EVENT_CATEGORY_DWELL = "CATEGORY_DWELL";

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final UserBehaviorLogService userBehaviorLogService;

    public BehaviorDwellRestController(UserRepository userRepository,
                                       ProductRepository productRepository,
                                       UserBehaviorLogService userBehaviorLogService) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.userBehaviorLogService = userBehaviorLogService;
    }

    @PostMapping("/dwell")
    public ResponseEntity<Void> recordDwell(@RequestBody BehaviorDwellRequest body,
                                            Authentication authentication,
                                            HttpServletRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null || user.getRole() != Role.CUSTOMER) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        if (body == null || body.getProductId() == null || body.getDurationSeconds() == null) {
            return ResponseEntity.badRequest().build();
        }
        int sec = body.getDurationSeconds();
        if (sec < 1 || sec > 86400) {
            return ResponseEntity.badRequest().build();
        }

        Product product = productRepository.findById(body.getProductId()).orElse(null);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }

        Long categoryId = product.getCategory() != null ? product.getCategory().getId() : null;

        UserBehaviorLog log = UserBehaviorLog.builder()
                .user(user)
                .sessionId(request.getSession(false) != null ? request.getSession(false).getId() : null)
                .productId(product.getId())
                .categoryId(categoryId)
                .eventType(EVENT_CATEGORY_DWELL)
                .durationSeconds(sec)
                .pageUrl(request.getRequestURI())
                .referrer(request.getHeader("Referer"))
                .ip(ClientIpExtractor.resolve(request))
                .userAgent(truncate(request.getHeader("User-Agent"), 512))
                .build();

        userBehaviorLogService.create(log);
        return ResponseEntity.noContent().build();
    }

    private static String truncate(String v, int maxLen) {
        if (v == null) {
            return null;
        }
        String s = v.trim();
        if (s.length() <= maxLen) {
            return s;
        }
        return s.substring(0, maxLen);
    }
}
