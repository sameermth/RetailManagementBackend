package com.retailmanagement.modules.auth.repository;

import com.retailmanagement.modules.auth.model.AuthRefreshSession;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthRefreshSessionRepository extends JpaRepository<AuthRefreshSession, Long> {
    Optional<AuthRefreshSession> findByRefreshTokenHash(String refreshTokenHash);
    List<AuthRefreshSession> findByAccountIdAndRevokedAtIsNull(Long accountId);
    List<AuthRefreshSession> findByAccountIdAndRevokedAtIsNullAndExpiresAtAfter(Long accountId, LocalDateTime now);
}
