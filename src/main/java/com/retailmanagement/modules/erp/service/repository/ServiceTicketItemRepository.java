package com.retailmanagement.modules.erp.service.repository;

import com.retailmanagement.modules.erp.service.entity.ServiceTicketItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceTicketItemRepository extends JpaRepository<ServiceTicketItem, Long> {
    List<ServiceTicketItem> findByServiceTicketIdOrderByIdAsc(Long serviceTicketId);
}
