package com.retailmanagement.modules.auth.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "auth_refresh_session")
public class AuthRefreshSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "client_type", nullable = false)
    private ClientType clientType;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "refresh_token_hash", nullable = false, unique = true)
    private String refreshTokenHash;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "last_used_at", nullable = false)
    private LocalDateTime lastUsedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoke_reason")
    private String revokeReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = now;
        issuedAt = issuedAt == null ? now : issuedAt;
        lastUsedAt = lastUsedAt == null ? now : lastUsedAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
