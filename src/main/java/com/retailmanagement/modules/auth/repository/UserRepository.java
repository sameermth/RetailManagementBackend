package com.retailmanagement.modules.auth.repository;

import com.retailmanagement.modules.auth.model.User;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Query("""
            SELECT u
            FROM User u
            JOIN u.account a
            LEFT JOIN u.person p
            WHERE lower(a.loginIdentifier) = lower(:login)
               OR lower(coalesce(p.primaryEmail, '')) = lower(:login)
            """)
    Optional<User> findByLogin(@Param("login") String login);

    default Optional<User> findByUsername(String username) {
        return findByLogin(username);
    }

    @Query("""
            SELECT u
            FROM User u
            JOIN u.person p
            WHERE lower(coalesce(p.primaryEmail, '')) = lower(:email)
            """)
    Optional<User> findByEmail(@Param("email") String email);

    @Query("""
            SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END
            FROM User u
            JOIN u.account a
            WHERE lower(a.loginIdentifier) = lower(:login)
            """)
    Boolean existsByUsername(@Param("login") String username);

    @Query("""
            SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END
            FROM User u
            JOIN u.person p
            WHERE lower(coalesce(p.primaryEmail, '')) = lower(:email)
            """)
    Boolean existsByEmail(@Param("email") String email);

    @Query("""
            SELECT u
            FROM User u
            JOIN u.role r
            WHERE u.organizationId = :organizationId
              AND u.active = true
              AND r.code IN :roleCodes
            ORDER BY
              CASE
                WHEN r.code = 'OWNER' THEN 0
                WHEN r.code = 'ADMIN' THEN 1
                ELSE 2
              END,
              u.id ASC
            """)
    List<User> findActiveByOrganizationIdAndRoleCodeIn(@Param("organizationId") Long organizationId,
                                                       @Param("roleCodes") Collection<String> roleCodes);
}
