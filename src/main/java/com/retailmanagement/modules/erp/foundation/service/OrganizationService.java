package com.retailmanagement.modules.erp.foundation.service;

import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.erp.common.security.ErpAccessGuard;
import com.retailmanagement.modules.erp.foundation.entity.Organization;
import com.retailmanagement.modules.erp.foundation.repository.OrganizationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Transactional
public class OrganizationService {
 private final OrganizationRepository repository;
 private final ErpAccessGuard accessGuard;
 @Transactional(readOnly=true)
 public List<Organization> list(){
  Long organizationId = com.retailmanagement.modules.erp.common.ErpSecurityUtils.currentOrganizationId()
          .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Authenticated organization context is missing"));
  accessGuard.assertOrganizationAccess(organizationId);
  return repository.findById(organizationId).stream().toList();
 }
 @Transactional(readOnly=true)
 public Organization get(Long id){
  accessGuard.assertOrganizationAccess(id);
  return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("ERP organization not found: "+id));
 }
 public Organization create(Organization organization){ return repository.save(organization); }
}
