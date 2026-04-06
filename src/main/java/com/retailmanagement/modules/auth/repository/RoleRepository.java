package com.retailmanagement.modules.auth.repository;

import com.retailmanagement.modules.auth.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByCode(String code);
    Optional<Role> findByName(String name);
    List<Role> findTop50ByCodeContainingIgnoreCaseOrNameContainingIgnoreCaseOrderByNameAsc(String codeQuery, String nameQuery);
}
