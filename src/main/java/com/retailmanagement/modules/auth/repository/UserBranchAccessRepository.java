package com.retailmanagement.modules.auth.repository;

import com.retailmanagement.modules.auth.model.UserBranchAccess;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBranchAccessRepository extends JpaRepository<UserBranchAccess, Long> {
    List<UserBranchAccess> findByUserId(Long userId);
}
