package com.example.shopsite.controller.admin;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.shopsite.dto.AdminOperationLogRequest;
import com.example.shopsite.dto.AuthLoginLogRequest;
import com.example.shopsite.dto.UserBehaviorLogRequest;
import com.example.shopsite.model.AdminOperationLog;
import com.example.shopsite.model.AuthLoginLog;
import com.example.shopsite.model.User;
import com.example.shopsite.model.UserBehaviorLog;
import com.example.shopsite.repository.UserRepository;
import com.example.shopsite.service.AdminOperationLogService;
import com.example.shopsite.service.AuthLoginLogService;
import com.example.shopsite.service.UserBehaviorLogService;

@RestController
@RequestMapping("/api/admin/logs")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminLogRestController {

    private final AuthLoginLogService authLoginLogService;
    private final UserBehaviorLogService userBehaviorLogService;
    private final AdminOperationLogService adminOperationLogService;
    private final UserRepository userRepository;

    public AdminLogRestController(AuthLoginLogService authLoginLogService,
                                  UserBehaviorLogService userBehaviorLogService,
                                  AdminOperationLogService adminOperationLogService,
                                  UserRepository userRepository) {
        this.authLoginLogService = authLoginLogService;
        this.userBehaviorLogService = userBehaviorLogService;
        this.adminOperationLogService = adminOperationLogService;
        this.userRepository = userRepository;
    }

    @GetMapping("/login")
    public ResponseEntity<List<AuthLoginLog>> getLoginLogs() {
        return ResponseEntity.ok(authLoginLogService.findAll());
    }

    @GetMapping("/login/{id}")
    public ResponseEntity<AuthLoginLog> getLoginLog(@PathVariable Long id) {
        return authLoginLogService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthLoginLog> createLoginLog(@RequestBody AuthLoginLogRequest request) {
        AuthLoginLog saved = authLoginLogService.create(toAuthLoginLog(request));
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/login/{id}")
    public ResponseEntity<AuthLoginLog> updateLoginLog(@PathVariable Long id,
                                                        @RequestBody AuthLoginLogRequest request) {
        return authLoginLogService.update(id, toAuthLoginLog(request))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/login/{id}")
    public ResponseEntity<Void> deleteLoginLog(@PathVariable Long id) {
        return authLoginLogService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/behavior")
    public ResponseEntity<List<UserBehaviorLog>> getBehaviorLogs() {
        return ResponseEntity.ok(userBehaviorLogService.findAll());
    }

    @GetMapping("/behavior/{id}")
    public ResponseEntity<UserBehaviorLog> getBehaviorLog(@PathVariable Long id) {
        return userBehaviorLogService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/behavior")
    public ResponseEntity<UserBehaviorLog> createBehaviorLog(@RequestBody UserBehaviorLogRequest request) {
        UserBehaviorLog saved = userBehaviorLogService.create(toUserBehaviorLog(request));
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/behavior/{id}")
    public ResponseEntity<UserBehaviorLog> updateBehaviorLog(@PathVariable Long id,
                                                              @RequestBody UserBehaviorLogRequest request) {
        return userBehaviorLogService.update(id, toUserBehaviorLog(request))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/behavior/{id}")
    public ResponseEntity<Void> deleteBehaviorLog(@PathVariable Long id) {
        return userBehaviorLogService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/operation")
    public ResponseEntity<List<AdminOperationLog>> getOperationLogs() {
        return ResponseEntity.ok(adminOperationLogService.findAll());
    }

    @GetMapping("/operation/{id}")
    public ResponseEntity<AdminOperationLog> getOperationLog(@PathVariable Long id) {
        return adminOperationLogService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/operation")
    public ResponseEntity<AdminOperationLog> createOperationLog(@RequestBody AdminOperationLogRequest request) {
        AdminOperationLog saved = adminOperationLogService.create(toAdminOperationLog(request));
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/operation/{id}")
    public ResponseEntity<AdminOperationLog> updateOperationLog(@PathVariable Long id,
                                                                 @RequestBody AdminOperationLogRequest request) {
        return adminOperationLogService.update(id, toAdminOperationLog(request))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/operation/{id}")
    public ResponseEntity<Void> deleteOperationLog(@PathVariable Long id) {
        return adminOperationLogService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    private AuthLoginLog toAuthLoginLog(AuthLoginLogRequest request) {
        return AuthLoginLog.builder()
                .user(findUser(request.getUserId()))
                .role(request.getRole())
                .loginTime(request.getLoginTime() != null ? request.getLoginTime() : LocalDateTime.now())
                .ip(request.getIp())
                .userAgent(request.getUserAgent())
                .success(Boolean.TRUE.equals(request.getSuccess()))
                .failureReason(request.getFailureReason())
                .sessionId(request.getSessionId())
                .build();
    }

    private UserBehaviorLog toUserBehaviorLog(UserBehaviorLogRequest request) {
        return UserBehaviorLog.builder()
                .user(findUser(request.getUserId()))
                .sessionId(request.getSessionId())
                .productId(request.getProductId())
                .categoryId(request.getCategoryId())
                .eventType(request.getEventType())
                .durationSeconds(request.getDurationSeconds())
                .pageUrl(request.getPageUrl())
                .referrer(request.getReferrer())
                .eventTime(request.getEventTime() != null ? request.getEventTime() : LocalDateTime.now())
                .ip(request.getIp())
                .userAgent(request.getUserAgent())
                .build();
    }

    private AdminOperationLog toAdminOperationLog(AdminOperationLogRequest request) {
        return AdminOperationLog.builder()
                .operator(findUser(request.getOperatorUserId()))
                .operatorRole(request.getOperatorRole())
                .operationTime(request.getOperationTime() != null ? request.getOperationTime() : LocalDateTime.now())
                .operationType(request.getOperationType())
                .content(request.getContent())
                .ip(request.getIp())
                .account(request.getAccount())
                .build();
    }

    private User findUser(Long userId) {
        if (userId == null) {
            return null;
        }
        Optional<User> userOpt = userRepository.findById(userId);
        return userOpt.orElse(null);
    }
}
