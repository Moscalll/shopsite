package com.example.shopsite.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "auth_login_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthLoginLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @Column(nullable = false, length = 32)
    private String role;

    @Column(name = "login_time", nullable = false)
    @Builder.Default
    private LocalDateTime loginTime = LocalDateTime.now();

    @Column(length = 64)
    private String ip;

    @Column(length = 512)
    private String userAgent;

    /**
     * 库表若同时存在 {@code success}、{@code is_success} 且均为 NOT NULL，只映射其一插入会失败。
     * 逻辑字段 {@link #success}；{@link #isSuccessColumn} 与 {@link #success} 在写入前强制同步。
     */
    @Column(name = "success", nullable = false)
    private Boolean success;

    @JsonIgnore
    @Column(name = "is_success", nullable = false)
    private Boolean isSuccessColumn;

    @PrePersist
    @PreUpdate
    private void syncDualSuccessColumns() {
        Boolean v = success != null ? success : isSuccessColumn;
        if (v == null) {
            v = Boolean.FALSE;
        }
        this.success = v;
        this.isSuccessColumn = v;
    }

    @PostLoad
    private void normalizeSuccessAfterLoad() {
        if (success == null && isSuccessColumn != null) {
            this.success = isSuccessColumn;
        } else if (isSuccessColumn == null && success != null) {
            this.isSuccessColumn = success;
        }
    }

    @Column(length = 512)
    private String failureReason;

    @Column(length = 128)
    private String sessionId;
}
