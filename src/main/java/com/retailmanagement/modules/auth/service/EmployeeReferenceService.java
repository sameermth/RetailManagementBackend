package com.retailmanagement.modules.auth.service;

import com.retailmanagement.modules.auth.dto.response.EmployeeManagementResponses;
import java.util.List;

public interface EmployeeReferenceService {
    List<EmployeeManagementResponses.RoleReferenceResponse> listRoles(String query);
}
