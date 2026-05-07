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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "admin_operation_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminOperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_user_id")
    @JsonIgnore
    private User operator;

    @Column(nullable = false, length = 32)
    private String operatorRole;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime operationTime = LocalDateTime.now();

    @Column(nullable = false, length = 100)
    private String operationType;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(length = 64)
    private String ip;

    @Column(length = 100)
    private String account;
}
