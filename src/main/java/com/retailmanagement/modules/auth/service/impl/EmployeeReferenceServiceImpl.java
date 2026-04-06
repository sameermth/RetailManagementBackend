package com.retailmanagement.modules.auth.service.impl;

import com.retailmanagement.modules.auth.dto.response.EmployeeManagementResponses;
import com.retailmanagement.modules.auth.repository.RoleRepository;
import com.retailmanagement.modules.auth.service.EmployeeReferenceService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeReferenceServiceImpl implements EmployeeReferenceService {

    private final RoleRepository roleRepository;

    @Override
    public List<EmployeeManagementResponses.RoleReferenceResponse> listRoles(String query) {
        String normalized = query == null ? "" : query.trim();
        return roleRepository.findTop50ByCodeContainingIgnoreCaseOrNameContainingIgnoreCaseOrderByNameAsc(normalized, normalized).stream()
                .filter(role -> !"PLATFORM_ADMIN".equalsIgnoreCase(role.getCode()))
                .map(role -> new EmployeeManagementResponses.RoleReferenceResponse(
                        role.getId(),
                        role.getCode(),
                        role.getName(),
                        Boolean.TRUE.equals(role.getSystem()),
                        true
                ))
                .toList();
    }
}
