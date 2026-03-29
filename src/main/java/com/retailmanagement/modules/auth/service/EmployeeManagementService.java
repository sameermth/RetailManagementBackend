package com.retailmanagement.modules.auth.service;

import com.retailmanagement.modules.auth.dto.request.EmployeeManagementRequests;
import com.retailmanagement.modules.auth.dto.response.EmployeeManagementResponses;
import java.util.List;

public interface EmployeeManagementService {
    List<EmployeeManagementResponses.EmployeeResponse> list(Long organizationId);
    EmployeeManagementResponses.EmployeeResponse get(Long organizationId, Long userId);
    EmployeeManagementResponses.EmployeeResponse create(EmployeeManagementRequests.CreateEmployeeRequest request);
    EmployeeManagementResponses.EmployeeResponse update(Long organizationId, Long userId, EmployeeManagementRequests.UpdateEmployeeRequest request);
    void activate(Long organizationId, Long userId);
    void deactivate(Long organizationId, Long userId);
}
