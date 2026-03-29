package com.retailmanagement.modules.erp.service.entity;

import com.retailmanagement.modules.erp.common.model.ErpAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "service_ticket_item", indexes = {
        @Index(name = "idx_service_ticket_item_ticket", columnList = "service_ticket_id"),
        @Index(name = "idx_service_ticket_item_serial", columnList = "serial_number_id")
})
public class ServiceTicketItem extends ErpAuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_ticket_id", nullable = false)
    private Long serviceTicketId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "serial_number_id")
    private Long serialNumberId;

    @Column(name = "product_ownership_id")
    private Long productOwnershipId;

    @Column(name = "symptom_notes")
    private String symptomNotes;

    @Column(name = "diagnosis_notes")
    private String diagnosisNotes;

    @Column(name = "resolution_status")
    private String resolutionStatus;
}
