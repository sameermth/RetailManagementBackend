package com.retailmanagement.modules.erp.service.service;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.erp.approval.dto.ErpApprovalDtos;
import com.retailmanagement.modules.erp.approval.service.ErpApprovalService;
import com.retailmanagement.modules.erp.audit.service.AuditEventWriter;
import com.retailmanagement.modules.erp.catalog.entity.StoreProduct;
import com.retailmanagement.modules.erp.catalog.repository.StoreProductRepository;
import com.retailmanagement.modules.erp.catalog.repository.UomRepository;
import com.retailmanagement.modules.erp.common.constants.ErpDocumentStatuses;
import com.retailmanagement.modules.erp.common.constants.ErpInventoryMovementTypes;
import com.retailmanagement.modules.erp.common.ErpSecurityUtils;
import com.retailmanagement.modules.erp.common.util.ErpJsonPayloads;
import com.retailmanagement.modules.erp.finance.service.ErpAccountingPostingService;
import com.retailmanagement.modules.erp.inventory.entity.SerialNumber;
import com.retailmanagement.modules.erp.inventory.repository.SerialNumberRepository;
import com.retailmanagement.modules.erp.inventory.service.InventoryPostingService;
import com.retailmanagement.modules.erp.party.entity.Customer;
import com.retailmanagement.modules.erp.party.repository.CustomerRepository;
import com.retailmanagement.modules.erp.party.repository.DistributorRepository;
import com.retailmanagement.modules.erp.party.repository.SupplierRepository;
import com.retailmanagement.modules.erp.returns.entity.SalesReturn;
import com.retailmanagement.modules.erp.returns.repository.SalesReturnRepository;
import com.retailmanagement.modules.erp.sales.entity.ProductOwnership;
import com.retailmanagement.modules.erp.sales.entity.SalesInvoice;
import com.retailmanagement.modules.erp.sales.entity.SalesInvoiceLine;
import com.retailmanagement.modules.erp.sales.repository.ProductOwnershipRepository;
import com.retailmanagement.modules.erp.sales.repository.SalesInvoiceRepository;
import com.retailmanagement.modules.erp.sales.repository.SalesInvoiceLineRepository;
import com.retailmanagement.modules.erp.service.dto.ErpServiceDtos;
import com.retailmanagement.modules.erp.service.entity.ServiceTicket;
import com.retailmanagement.modules.erp.service.entity.ServiceTicketItem;
import com.retailmanagement.modules.erp.service.entity.ServiceReplacement;
import com.retailmanagement.modules.erp.service.entity.ServiceAgreement;
import com.retailmanagement.modules.erp.service.entity.ServiceAgreementItem;
import com.retailmanagement.modules.erp.service.entity.ServiceVisit;
import com.retailmanagement.modules.erp.service.entity.WarrantyClaim;
import com.retailmanagement.modules.erp.service.entity.WarrantyExtension;
import com.retailmanagement.modules.erp.service.repository.ServiceAgreementItemRepository;
import com.retailmanagement.modules.erp.service.repository.ServiceAgreementRepository;
import com.retailmanagement.modules.erp.service.repository.ServiceReplacementRepository;
import com.retailmanagement.modules.erp.service.repository.ServiceTicketItemRepository;
import com.retailmanagement.modules.erp.service.repository.ServiceTicketRepository;
import com.retailmanagement.modules.erp.service.repository.ServiceVisitRepository;
import com.retailmanagement.modules.erp.service.repository.WarrantyClaimRepository;
import com.retailmanagement.modules.erp.service.repository.WarrantyExtensionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ErpServiceWarrantyService {

    private final ServiceTicketRepository serviceTicketRepository;
    private final ServiceTicketItemRepository serviceTicketItemRepository;
    private final ServiceVisitRepository serviceVisitRepository;
    private final ServiceAgreementRepository serviceAgreementRepository;
    private final ServiceAgreementItemRepository serviceAgreementItemRepository;
    private final ServiceReplacementRepository serviceReplacementRepository;
    private final WarrantyClaimRepository warrantyClaimRepository;
    private final WarrantyExtensionRepository warrantyExtensionRepository;
    private final CustomerRepository customerRepository;
    private final StoreProductRepository productRepository;
    private final UomRepository uomRepository;
    private final SalesInvoiceRepository salesInvoiceRepository;
    private final SalesInvoiceLineRepository salesInvoiceLineRepository;
    private final ProductOwnershipRepository productOwnershipRepository;
    private final SerialNumberRepository serialNumberRepository;
    private final InventoryPostingService inventoryPostingService;
    private final SupplierRepository supplierRepository;
    private final DistributorRepository distributorRepository;
    private final SalesReturnRepository salesReturnRepository;
    private final ErpAccountingPostingService accountingPostingService;
    private final ServiceReplacementPostingService serviceReplacementPostingService;
    private final ErpApprovalService erpApprovalService;
    private final AuditEventWriter auditEventWriter;

    @Transactional(readOnly = true)
    public List<ServiceTicket> listTickets(Long organizationId) {
        return serviceTicketRepository.findTop100ByOrganizationIdOrderByReportedOnDescIdDesc(organizationId);
    }

    @Transactional(readOnly = true)
    public ServiceTicketDetails getTicket(Long organizationId, Long id) {
        ServiceTicket ticket = serviceTicketRepository.findByOrganizationIdAndId(organizationId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Service ticket not found: " + id));
        List<ServiceTicketItem> items = serviceTicketItemRepository.findByServiceTicketIdOrderByIdAsc(id);
        List<ServiceVisit> visits = serviceVisitRepository.findByServiceTicketIdOrderByScheduledAtAscIdAsc(id);
        return new ServiceTicketDetails(ticket, items, visits);
    }

    public ServiceTicket createTicket(Long organizationId, Long branchId, ErpServiceDtos.CreateServiceTicketRequest request) {
        Customer customer = customerRepository.findByIdAndOrganizationId(request.customerId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + request.customerId()));

        SalesInvoice invoice = null;
        if (request.salesInvoiceId() != null) {
            invoice = salesInvoiceRepository.findByOrganizationIdAndId(organizationId, request.salesInvoiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sales invoice not found: " + request.salesInvoiceId()));
        }
        SalesReturn salesReturn = null;
        if (request.salesReturnId() != null) {
            salesReturn = salesReturnRepository.findByOrganizationIdAndId(organizationId, request.salesReturnId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sales return not found: " + request.salesReturnId()));
            if (!request.customerId().equals(salesReturn.getCustomerId())) {
                throw new BusinessException("Sales return does not belong to the selected customer");
            }
            if (invoice == null) {
                Long originalSalesInvoiceId = salesReturn.getOriginalSalesInvoiceId();
                invoice = salesInvoiceRepository.findByOrganizationIdAndId(organizationId, originalSalesInvoiceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Sales invoice not found: " + originalSalesInvoiceId));
            }
        }

        String ticketNumber = "SRV-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + System.currentTimeMillis();
        ServiceTicket ticket = new ServiceTicket();
        ticket.setOrganizationId(organizationId);
        ticket.setBranchId(branchId);
        ticket.setCustomerId(request.customerId());
        ticket.setSalesInvoiceId(invoice == null ? request.salesInvoiceId() : invoice.getId());
        ticket.setSalesReturnId(salesReturn == null ? request.salesReturnId() : salesReturn.getId());
        ticket.setTicketNumber(ticketNumber);
        ticket.setSourceType(normalizeSourceType(request.sourceType()));
        ticket.setPriority(normalizePriority(request.priority()));
        ticket.setStatus(request.assignedToUserId() == null ? "OPEN" : "ASSIGNED");
        ticket.setComplaintSummary(request.complaintSummary());
        ticket.setIssueDescription(request.issueDescription());
        ticket.setReportedOn(request.reportedOn() == null ? LocalDate.now() : request.reportedOn());
        ticket.setAssignedToUserId(request.assignedToUserId());
        ticket = serviceTicketRepository.save(ticket);

        for (ErpServiceDtos.CreateServiceTicketItemRequest itemRequest : request.items()) {
            StoreProduct product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + itemRequest.productId()));
            if (!organizationId.equals(product.getOrganizationId())) {
                throw new BusinessException("Product does not belong to organization " + organizationId);
            }

            ServiceTicketItem item = new ServiceTicketItem();
            item.setServiceTicketId(ticket.getId());
            item.setProductId(itemRequest.productId());
            item.setSymptomNotes(itemRequest.symptomNotes());
            item.setResolutionStatus("PENDING");

            if (itemRequest.serialNumberId() != null) {
                SerialNumber serial = serialNumberRepository.findById(itemRequest.serialNumberId())
                        .orElseThrow(() -> new ResourceNotFoundException("Serial number not found: " + itemRequest.serialNumberId()));
                if (!itemRequest.productId().equals(serial.getProductId())) {
                    throw new BusinessException("Serial number does not belong to requested product");
                }
                item.setSerialNumberId(serial.getId());
            }

            if (itemRequest.productOwnershipId() != null) {
                ProductOwnership ownership = productOwnershipRepository.findById(itemRequest.productOwnershipId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product ownership not found: " + itemRequest.productOwnershipId()));
                if (!organizationId.equals(ownership.getOrganizationId())) {
                    throw new BusinessException("Product ownership does not belong to organization " + organizationId);
                }
                if (!request.customerId().equals(ownership.getCustomerId())) {
                    throw new BusinessException("Product ownership does not belong to the selected customer");
                }
                if (!itemRequest.productId().equals(ownership.getProductId())) {
                    throw new BusinessException("Product ownership does not belong to the selected product");
                }
                item.setProductOwnershipId(ownership.getId());
                if (item.getSerialNumberId() == null) {
                    item.setSerialNumberId(ownership.getSerialNumberId());
                }
            }

            serviceTicketItemRepository.save(item);
        }

        auditEventWriter.write(
                organizationId,
                branchId,
                "SERVICE_TICKET_CREATED",
                "service_ticket",
                ticket.getId(),
                ticket.getTicketNumber(),
                "CREATE",
                invoice == null ? null : invoice.getWarehouseId(),
                ticket.getCustomerId(),
                null,
                "Service ticket created",
                ticketPayload(ticket)
        );

        return ticket;
    }

    public ServiceTicket assignTicket(Long organizationId, Long branchId, Long ticketId, ErpServiceDtos.AssignServiceTicketRequest request) {
        ServiceTicket ticket = serviceTicketRepository.findByOrganizationIdAndId(organizationId, ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Service ticket not found: " + ticketId));
        ticket.setAssignedToUserId(request.assignedToUserId());
        ticket.setStatus("ASSIGNED");
        ticket = serviceTicketRepository.save(ticket);

        auditEventWriter.write(
                organizationId,
                branchId,
                "SERVICE_TICKET_ASSIGNED",
                "service_ticket",
                ticket.getId(),
                ticket.getTicketNumber(),
                "ASSIGN",
                null,
                ticket.getCustomerId(),
                null,
                "Service ticket assigned",
                ErpJsonPayloads.of("assignedToUserId", request.assignedToUserId())
        );
        return ticket;
    }

    public ServiceVisit addVisit(Long organizationId, Long branchId, Long ticketId, ErpServiceDtos.CreateServiceVisitRequest request) {
        ServiceTicket ticket = serviceTicketRepository.findByOrganizationIdAndId(organizationId, ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Service ticket not found: " + ticketId));

        ServiceVisit visit = new ServiceVisit();
        visit.setOrganizationId(organizationId);
        visit.setBranchId(branchId);
        visit.setServiceTicketId(ticketId);
        visit.setTechnicianUserId(request.technicianUserId());
        visit.setScheduledAt(request.scheduledAt());
        visit.setStartedAt(request.startedAt());
        visit.setCompletedAt(request.completedAt());
        visit.setVisitStatus(normalizeVisitStatus(request.visitStatus()));
        visit.setVisitNotes(request.visitNotes());
        visit.setPartsUsedJson(request.partsUsedJson());
        visit.setCustomerFeedback(request.customerFeedback());
        visit = serviceVisitRepository.save(visit);

        if ("STARTED".equals(visit.getVisitStatus()) || "COMPLETED".equals(visit.getVisitStatus())) {
            ticket.setStatus("IN_PROGRESS");
        }
        if ("COMPLETED".equals(visit.getVisitStatus())) {
            ticket.setStatus("RESOLVED");
        }
        serviceTicketRepository.save(ticket);

        auditEventWriter.write(
                organizationId,
                branchId,
                "SERVICE_VISIT_RECORDED",
                "service_visit",
                visit.getId(),
                ticket.getTicketNumber(),
                "CREATE",
                null,
                ticket.getCustomerId(),
                null,
                "Service visit recorded",
                ErpJsonPayloads.of("ticketId", ticketId, "visitStatus", visit.getVisitStatus())
        );
        return visit;
    }

    public ServiceTicket closeTicket(Long organizationId, Long branchId, Long ticketId, ErpServiceDtos.CloseServiceTicketRequest request) {
        ServiceTicket ticket = serviceTicketRepository.findByOrganizationIdAndId(organizationId, ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Service ticket not found: " + ticketId));
        List<ServiceTicketItem> items = serviceTicketItemRepository.findByServiceTicketIdOrderByIdAsc(ticketId);
        for (ServiceTicketItem item : items) {
            item.setResolutionStatus(normalizeResolutionStatus(request.resolutionStatus()));
            if (request.diagnosisNotes() != null && !request.diagnosisNotes().isBlank()) {
                item.setDiagnosisNotes(request.diagnosisNotes());
            }
            serviceTicketItemRepository.save(item);
        }
        ticket.setStatus("CLOSED");
        ticket = serviceTicketRepository.save(ticket);

        auditEventWriter.write(
                organizationId,
                branchId,
                "SERVICE_TICKET_CLOSED",
                "service_ticket",
                ticket.getId(),
                ticket.getTicketNumber(),
                "CLOSE",
                null,
                ticket.getCustomerId(),
                null,
                "Service ticket closed",
                ErpJsonPayloads.of("resolutionStatus", normalizeResolutionStatus(request.resolutionStatus()), "remarks", request.remarks())
        );
        return ticket;
    }

    @Transactional(readOnly = true)
    public List<WarrantyClaim> listClaims(Long organizationId) {
        return warrantyClaimRepository.findTop100ByOrganizationIdOrderByClaimDateDescIdDesc(organizationId);
    }

    @Transactional(readOnly = true)
    public WarrantyClaim getClaim(Long organizationId, Long id) {
        return warrantyClaimRepository.findByOrganizationIdAndId(organizationId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Warranty claim not found: " + id));
    }

    public WarrantyClaim createClaim(Long organizationId, Long branchId, ErpServiceDtos.CreateWarrantyClaimRequest request) {
        Customer customer = customerRepository.findByIdAndOrganizationId(request.customerId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + request.customerId()));
        StoreProduct product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.productId()));
        if (!organizationId.equals(product.getOrganizationId())) {
            throw new BusinessException("Product does not belong to organization " + organizationId);
        }
        ProductOwnership ownership = null;
        if (request.productOwnershipId() != null) {
            ownership = productOwnershipRepository.findById(request.productOwnershipId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product ownership not found: " + request.productOwnershipId()));
            if (!organizationId.equals(ownership.getOrganizationId())) {
                throw new BusinessException("Product ownership does not belong to organization " + organizationId);
            }
            if (!request.customerId().equals(ownership.getCustomerId())) {
                throw new BusinessException("Product ownership does not belong to the selected customer");
            }
            if (!request.productId().equals(ownership.getProductId())) {
                throw new BusinessException("Product ownership does not belong to the selected product");
            }
        }
        if (request.serialNumberId() != null) {
            SerialNumber serial = serialNumberRepository.findById(request.serialNumberId())
                    .orElseThrow(() -> new ResourceNotFoundException("Serial number not found: " + request.serialNumberId()));
            if (!request.productId().equals(serial.getProductId())) {
                throw new BusinessException("Serial number does not belong to product " + request.productId());
            }
            if (ownership != null && ownership.getSerialNumberId() != null && !request.serialNumberId().equals(ownership.getSerialNumberId())) {
                throw new BusinessException("Serial number does not match the selected product ownership");
            }
        }
        SalesInvoice invoice = null;
        if (request.salesInvoiceId() != null) {
            invoice = salesInvoiceRepository.findByOrganizationIdAndId(organizationId, request.salesInvoiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sales invoice not found: " + request.salesInvoiceId()));
        }
        SalesReturn salesReturn = null;
        if (request.salesReturnId() != null) {
            salesReturn = salesReturnRepository.findByOrganizationIdAndId(organizationId, request.salesReturnId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sales return not found: " + request.salesReturnId()));
            if (!request.customerId().equals(salesReturn.getCustomerId())) {
                throw new BusinessException("Sales return does not belong to the selected customer");
            }
            if (invoice == null) {
                Long originalSalesInvoiceId = salesReturn.getOriginalSalesInvoiceId();
                invoice = salesInvoiceRepository.findByOrganizationIdAndId(organizationId, originalSalesInvoiceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Sales invoice not found: " + originalSalesInvoiceId));
            }
        }
        if (request.supplierId() != null) {
            supplierRepository.findByOrganizationIdAndId(organizationId, request.supplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + request.supplierId()));
        }
        if (request.distributorId() != null) {
            distributorRepository.findByOrganizationIdAndId(organizationId, request.distributorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Distributor not found: " + request.distributorId()));
        }
        if (request.serviceTicketId() != null) {
            ServiceTicket ticket = serviceTicketRepository.findByOrganizationIdAndId(organizationId, request.serviceTicketId())
                    .orElseThrow(() -> new ResourceNotFoundException("Service ticket not found: " + request.serviceTicketId()));
            if (!request.customerId().equals(ticket.getCustomerId())) {
                throw new BusinessException("Service ticket does not belong to the selected customer");
            }
            if (salesReturn == null && ticket.getSalesReturnId() != null) {
                salesReturn = salesReturnRepository.findByOrganizationIdAndId(organizationId, ticket.getSalesReturnId())
                        .orElseThrow(() -> new ResourceNotFoundException("Sales return not found: " + ticket.getSalesReturnId()));
            }
            if (invoice == null && ticket.getSalesInvoiceId() != null) {
                invoice = salesInvoiceRepository.findByOrganizationIdAndId(organizationId, ticket.getSalesInvoiceId())
                        .orElseThrow(() -> new ResourceNotFoundException("Sales invoice not found: " + ticket.getSalesInvoiceId()));
            }
        }

        String claimNumber = "CLM-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + System.currentTimeMillis();
        WarrantyClaim claim = new WarrantyClaim();
        claim.setOrganizationId(organizationId);
        claim.setBranchId(branchId);
        claim.setServiceTicketId(request.serviceTicketId());
        claim.setCustomerId(request.customerId());
        claim.setProductId(request.productId());
        claim.setSerialNumberId(request.serialNumberId() != null ? request.serialNumberId() : ownership == null ? null : ownership.getSerialNumberId());
        claim.setProductOwnershipId(ownership == null ? request.productOwnershipId() : ownership.getId());
        claim.setSalesInvoiceId(invoice == null ? request.salesInvoiceId() : invoice.getId());
        claim.setSalesReturnId(salesReturn == null ? request.salesReturnId() : salesReturn.getId());
        claim.setSupplierId(request.supplierId());
        claim.setDistributorId(request.distributorId());
        claim.setUpstreamRouteType(normalizeUpstreamRouteType(request.upstreamRouteType(), request.supplierId(), request.distributorId(), request.upstreamCompanyName()));
        claim.setUpstreamCompanyName(trimToNull(request.upstreamCompanyName()));
        claim.setUpstreamReferenceNumber(trimToNull(request.upstreamReferenceNumber()));
        claim.setUpstreamStatus(normalizeUpstreamStatus(request.upstreamStatus()));
        claim.setRoutedOn(request.routedOn());
        claim.setClaimNumber(claimNumber);
        claim.setClaimType(normalizeClaimType(request.claimType()));
        claim.setStatus("OPEN");
        claim.setClaimDate(request.claimDate() == null ? LocalDate.now() : request.claimDate());
        claim.setWarrantyStartDate(ownership == null ? null : ownership.getWarrantyStartDate());
        claim.setWarrantyEndDate(ownership == null ? null : ownership.getWarrantyEndDate());
        claim.setClaimNotes(request.claimNotes());
        claim = warrantyClaimRepository.save(claim);

        auditEventWriter.write(
                organizationId,
                branchId,
                "WARRANTY_CLAIM_CREATED",
                "warranty_claim",
                claim.getId(),
                claim.getClaimNumber(),
                "CREATE",
                null,
                claim.getCustomerId(),
                claim.getSupplierId(),
                "Warranty claim created",
                claimPayload(claim)
        );
        return claim;
    }

    public WarrantyClaim updateClaimStatus(Long organizationId, Long branchId, Long claimId, ErpServiceDtos.UpdateWarrantyClaimStatusRequest request) {
        WarrantyClaim claim = warrantyClaimRepository.findByOrganizationIdAndId(organizationId, claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Warranty claim not found: " + claimId));
        String status = normalizeClaimStatus(request.status());
        if ("APPROVED".equals(status)) {
            ErpApprovalService.ApprovalEvaluation evaluation = erpApprovalService.evaluate(
                    organizationId,
                    new ErpApprovalDtos.ApprovalEvaluationQuery("warranty_claim", claim.getId(), "WARRANTY_CLAIM_APPROVE")
            );
            claim.setUpstreamRouteType(normalizeUpstreamRouteType(request.upstreamRouteType(), claim.getSupplierId(), claim.getDistributorId(), request.upstreamCompanyName()));
            claim.setUpstreamCompanyName(trimToNull(request.upstreamCompanyName()));
            claim.setUpstreamReferenceNumber(trimToNull(request.upstreamReferenceNumber()));
            claim.setUpstreamStatus(normalizeUpstreamStatus(request.upstreamStatus()));
            claim.setRoutedOn(request.routedOn());
            if (request.claimNotes() != null && !request.claimNotes().isBlank()) {
                claim.setClaimNotes(request.claimNotes());
            }
            if (evaluation.approvalRequired()) {
                claim.setStatus("SUBMITTED");
                claim = warrantyClaimRepository.save(claim);
                if (!evaluation.pendingRequestExists()) {
                    erpApprovalService.createRequest(
                            organizationId,
                            branchId,
                            new ErpApprovalDtos.CreateApprovalRequest(
                                    "warranty_claim",
                                    claim.getId(),
                                    claim.getClaimNumber(),
                                    "WARRANTY_CLAIM_APPROVE",
                                    "Warranty claim approval matched approval rule",
                                    null,
                                    evaluation.approverRoleCode()
                            )
                    );
                }
                auditEventWriter.write(
                        organizationId,
                        branchId,
                        "WARRANTY_CLAIM_PENDING_APPROVAL",
                        "warranty_claim",
                        claim.getId(),
                        claim.getClaimNumber(),
                        "REQUEST_APPROVAL",
                        null,
                        claim.getCustomerId(),
                        claim.getSupplierId(),
                        "Warranty claim sent for approval",
                        ErpJsonPayloads.of("status", "SUBMITTED")
                );
                return claim;
            }
        }
        claim.setStatus(status);
        if (request.approvedOn() != null) {
            claim.setApprovedOn(request.approvedOn());
        } else if ("APPROVED".equals(status)) {
            claim.setApprovedOn(LocalDate.now());
        }
        claim.setUpstreamRouteType(normalizeUpstreamRouteType(request.upstreamRouteType(), claim.getSupplierId(), claim.getDistributorId(), request.upstreamCompanyName()));
        claim.setUpstreamCompanyName(trimToNull(request.upstreamCompanyName()));
        claim.setUpstreamReferenceNumber(trimToNull(request.upstreamReferenceNumber()));
        claim.setUpstreamStatus(normalizeUpstreamStatus(request.upstreamStatus()));
        claim.setRoutedOn(request.routedOn());
        if (request.claimNotes() != null && !request.claimNotes().isBlank()) {
            claim.setClaimNotes(request.claimNotes());
        }
        claim = warrantyClaimRepository.save(claim);

        auditEventWriter.write(
                organizationId,
                branchId,
                "WARRANTY_CLAIM_STATUS_UPDATED",
                "warranty_claim",
                claim.getId(),
                claim.getClaimNumber(),
                "STATUS_UPDATE",
                null,
                claim.getCustomerId(),
                claim.getSupplierId(),
                "Warranty claim status updated",
                ErpJsonPayloads.of("status", status)
        );
        return claim;
    }

    @Transactional(readOnly = true)
    public List<ServiceReplacement> listReplacements(Long organizationId) {
        return serviceReplacementRepository.findTop100ByOrganizationIdOrderByIssuedOnDescIdDesc(organizationId);
    }

    @Transactional(readOnly = true)
    public List<ServiceAgreement> listAgreements(Long organizationId) {
        return serviceAgreementRepository.findTop100ByOrganizationIdOrderByServiceStartDateDescIdDesc(organizationId);
    }

    @Transactional(readOnly = true)
    public ServiceAgreementDetails getAgreement(Long organizationId, Long id) {
        ServiceAgreement agreement = serviceAgreementRepository.findByOrganizationIdAndId(organizationId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Service agreement not found: " + id));
        List<ServiceAgreementItem> items = serviceAgreementItemRepository.findByServiceAgreementIdOrderByIdAsc(id);
        return new ServiceAgreementDetails(agreement, items);
    }

    public ServiceAgreement createAgreement(Long organizationId, Long branchId, ErpServiceDtos.CreateServiceAgreementRequest request) {
        Customer customer = customerRepository.findByIdAndOrganizationId(request.customerId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + request.customerId()));
        SalesInvoice invoice = salesInvoiceRepository.findByOrganizationIdAndId(organizationId, request.salesInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Sales invoice not found: " + request.salesInvoiceId()));
        if (!customer.getId().equals(invoice.getCustomerId())) {
            throw new BusinessException("Sales invoice does not belong to the selected customer");
        }
        if (request.serviceEndDate().isBefore(request.serviceStartDate())) {
            throw new BusinessException("Service agreement end date cannot be before start date");
        }

        ServiceAgreement agreement = new ServiceAgreement();
        agreement.setOrganizationId(organizationId);
        agreement.setBranchId(branchId);
        agreement.setCustomerId(customer.getId());
        agreement.setSalesInvoiceId(invoice.getId());
        agreement.setAgreementNumber("SAG-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + System.currentTimeMillis());
        agreement.setAgreementType(normalizeAgreementType(request.agreementType()));
        agreement.setStatus(normalizeAgreementStatus(request.status()));
        agreement.setServiceStartDate(request.serviceStartDate());
        agreement.setServiceEndDate(request.serviceEndDate());
        agreement.setLaborIncluded(Boolean.TRUE.equals(request.laborIncluded()));
        agreement.setPartsIncluded(Boolean.TRUE.equals(request.partsIncluded()));
        agreement.setPreventiveVisitsIncluded(request.preventiveVisitsIncluded());
        agreement.setVisitLimit(request.visitLimit());
        agreement.setSlaHours(request.slaHours());
        agreement.setAgreementAmount(request.agreementAmount());
        agreement.setNotes(trimToNull(request.notes()));
        agreement = serviceAgreementRepository.save(agreement);

        for (ErpServiceDtos.CreateServiceAgreementItemRequest itemRequest : request.items()) {
            StoreProduct product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + itemRequest.productId()));
            if (!organizationId.equals(product.getOrganizationId())) {
                throw new BusinessException("Product does not belong to organization " + organizationId);
            }

            ProductOwnership ownership = null;
            if (itemRequest.productOwnershipId() != null) {
                ownership = productOwnershipRepository.findByIdAndOrganizationId(itemRequest.productOwnershipId(), organizationId)
                        .orElseThrow(() -> new ResourceNotFoundException("Product ownership not found: " + itemRequest.productOwnershipId()));
                if (!customer.getId().equals(ownership.getCustomerId())) {
                    throw new BusinessException("Product ownership does not belong to the selected customer");
                }
                if (!product.getId().equals(ownership.getProductId())) {
                    throw new BusinessException("Product ownership does not belong to the selected product");
                }
                if (!invoice.getId().equals(ownership.getSalesInvoiceId())) {
                    throw new BusinessException("Product ownership does not belong to the selected sales invoice");
                }
            }

            SalesInvoiceLine invoiceLine = null;
            Long invoiceLineId = itemRequest.salesInvoiceLineId();
            if (invoiceLineId == null && ownership != null) {
                invoiceLineId = ownership.getSalesInvoiceLineId();
            }
            if (invoiceLineId != null) {
                final Long resolvedInvoiceLineId = invoiceLineId;
                invoiceLine = salesInvoiceLineRepository.findById(invoiceLineId)
                        .orElseThrow(() -> new ResourceNotFoundException("Sales invoice line not found: " + resolvedInvoiceLineId));
                if (!invoice.getId().equals(invoiceLine.getSalesInvoiceId())) {
                    throw new BusinessException("Sales invoice line does not belong to the selected invoice");
                }
                if (!product.getId().equals(invoiceLine.getProductId())) {
                    throw new BusinessException("Sales invoice line does not belong to the selected product");
                }
            }

            Long serialNumberId = itemRequest.serialNumberId();
            if (serialNumberId == null && ownership != null) {
                serialNumberId = ownership.getSerialNumberId();
            }
            if (serialNumberId != null) {
                final Long resolvedSerialNumberId = serialNumberId;
                SerialNumber serial = serialNumberRepository.findById(serialNumberId)
                        .orElseThrow(() -> new ResourceNotFoundException("Serial number not found: " + resolvedSerialNumberId));
                if (!organizationId.equals(serial.getOrganizationId()) || !product.getId().equals(serial.getProductId())) {
                    throw new BusinessException("Serial number does not belong to the selected product");
                }
            }

            ServiceAgreementItem item = new ServiceAgreementItem();
            item.setServiceAgreementId(agreement.getId());
            item.setProductId(product.getId());
            item.setProductOwnershipId(ownership == null ? itemRequest.productOwnershipId() : ownership.getId());
            item.setSalesInvoiceLineId(invoiceLine == null ? invoiceLineId : invoiceLine.getId());
            item.setSerialNumberId(serialNumberId);
            item.setCoverageScope(normalizeCoverageScope(itemRequest.coverageScope()));
            item.setIncludedServiceNotes(trimToNull(itemRequest.includedServiceNotes()));
            serviceAgreementItemRepository.save(item);
        }

        auditEventWriter.write(
                organizationId,
                branchId,
                "SERVICE_AGREEMENT_CREATED",
                "service_agreement",
                agreement.getId(),
                agreement.getAgreementNumber(),
                "CREATE",
                invoice.getWarehouseId(),
                customer.getId(),
                null,
                "Service agreement created",
                ErpJsonPayloads.of("salesInvoiceId", invoice.getId(), "agreementType", agreement.getAgreementType())
        );

        return agreement;
    }

    @Transactional(readOnly = true)
    public ServiceReplacement getReplacement(Long organizationId, Long id) {
        return serviceReplacementRepository.findByOrganizationIdAndId(organizationId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Service replacement not found: " + id));
    }

    @Transactional(readOnly = true)
    public ErpServiceDtos.OwnershipWarrantySummaryResponse getOwnershipWarrantySummary(Long organizationId, Long ownershipId) {
        ProductOwnership ownership = productOwnershipRepository.findByIdAndOrganizationId(ownershipId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Product ownership not found: " + ownershipId));
        List<WarrantyExtension> extensions = warrantyExtensionRepository.findByOrganizationIdAndProductOwnershipIdOrderByIdAsc(organizationId, ownershipId);
        return new ErpServiceDtos.OwnershipWarrantySummaryResponse(
                ownership.getId(),
                ownership.getSerialNumberId(),
                ownership.getSalesInvoiceId(),
                ownership.getSalesInvoiceLineId(),
                ownership.getWarrantyStartDate(),
                ownership.getWarrantyEndDate(),
                computeEffectiveWarrantyEndDate(ownership, extensions),
                !extensions.isEmpty(),
                extensions.stream().map(this::toWarrantyExtensionResponse).toList()
        );
    }

    @Transactional(readOnly = true)
    public List<ErpServiceDtos.WarrantyExtensionResponse> listWarrantyExtensions(Long organizationId, Long ownershipId) {
        productOwnershipRepository.findByIdAndOrganizationId(ownershipId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Product ownership not found: " + ownershipId));
        return warrantyExtensionRepository.findByOrganizationIdAndProductOwnershipIdOrderByIdAsc(organizationId, ownershipId)
                .stream()
                .map(this::toWarrantyExtensionResponse)
                .toList();
    }

    public ErpServiceDtos.WarrantyExtensionResponse createWarrantyExtension(Long organizationId, Long branchId, Long ownershipId,
                                                                            ErpServiceDtos.CreateWarrantyExtensionRequest request) {
        ProductOwnership ownership = productOwnershipRepository.findByIdAndOrganizationId(ownershipId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Product ownership not found: " + ownershipId));
        List<WarrantyExtension> existingExtensions = warrantyExtensionRepository.findByOrganizationIdAndProductOwnershipIdOrderByIdAsc(organizationId, ownershipId);
        LocalDate effectiveEnd = computeEffectiveWarrantyEndDate(ownership, existingExtensions);
        LocalDate startDate = request.startDate() != null
                ? request.startDate()
                : (effectiveEnd != null ? effectiveEnd.plusDays(1) : LocalDate.now());
        LocalDate endDate = request.endDate() != null
                ? request.endDate()
                : (effectiveEnd != null ? effectiveEnd.plusMonths(request.monthsAdded()) : startDate.plusMonths(request.monthsAdded()));
        if (endDate.isBefore(startDate)) {
            throw new BusinessException("Warranty extension end date cannot be before start date");
        }

        WarrantyExtension extension = new WarrantyExtension();
        extension.setOrganizationId(organizationId);
        extension.setProductOwnershipId(ownership.getId());
        extension.setSerialNumberId(ownership.getSerialNumberId());
        extension.setSalesInvoiceId(ownership.getSalesInvoiceId());
        extension.setSalesInvoiceLineId(ownership.getSalesInvoiceLineId());
        extension.setExtensionType(normalizeExtensionType(request.extensionType()));
        extension.setMonthsAdded(request.monthsAdded());
        extension.setStartDate(startDate);
        extension.setEndDate(endDate);
        extension.setStatus("ACTIVE");
        extension.setReason(trimToNull(request.reason()));
        extension.setReferenceNumber(trimToNull(request.referenceNumber()));
        extension.setAmount(request.amount());
        extension.setRemarks(trimToNull(request.remarks()));
        extension = warrantyExtensionRepository.save(extension);

        auditEventWriter.write(
                organizationId,
                branchId,
                "WARRANTY_EXTENSION_CREATED",
                "warranty_extension",
                extension.getId(),
                extension.getReferenceNumber(),
                "CREATE",
                null,
                ownership.getCustomerId(),
                null,
                "Warranty extension created",
                ErpJsonPayloads.of(
                        "productOwnershipId", ownershipId,
                        "extensionType", extension.getExtensionType(),
                        "monthsAdded", extension.getMonthsAdded(),
                        "endDate", extension.getEndDate()
                )
        );

        return toWarrantyExtensionResponse(extension);
    }

    @Transactional(readOnly = true)
    public ErpServiceDtos.ServiceAgreementSummaryResponse resolveServiceAgreementSummary(Long organizationId,
                                                                                        Long productOwnershipId,
                                                                                        Long salesInvoiceId,
                                                                                        Long salesInvoiceLineId) {
        ServiceAgreementItem item = null;
        if (productOwnershipId != null) {
            List<ServiceAgreementItem> items = serviceAgreementItemRepository
                    .findByOrganizationIdAndProductOwnershipIdOrderByAgreementPriority(organizationId, productOwnershipId);
            if (!items.isEmpty()) {
                item = items.getFirst();
            }
        }
        if (item == null && salesInvoiceId != null) {
            List<ServiceAgreementItem> items = serviceAgreementItemRepository
                    .findByOrganizationIdAndInvoiceScopeOrderByAgreementPriority(organizationId, salesInvoiceId, salesInvoiceLineId);
            if (!items.isEmpty()) {
                item = items.getFirst();
            }
        }
        if (item == null) {
            return null;
        }
        ServiceAgreement agreement = serviceAgreementRepository.findByOrganizationIdAndId(organizationId, item.getServiceAgreementId())
                .orElse(null);
        if (agreement == null) {
            return null;
        }
        LocalDate today = LocalDate.now();
        boolean coverageActive = "ACTIVE".equals(agreement.getStatus())
                && (agreement.getServiceStartDate() == null || !today.isBefore(agreement.getServiceStartDate()))
                && (agreement.getServiceEndDate() == null || !today.isAfter(agreement.getServiceEndDate()));
        return new ErpServiceDtos.ServiceAgreementSummaryResponse(
                agreement.getId(),
                agreement.getAgreementNumber(),
                agreement.getAgreementType(),
                agreement.getStatus(),
                agreement.getServiceStartDate(),
                agreement.getServiceEndDate(),
                coverageActive,
                agreement.getLaborIncluded(),
                agreement.getPartsIncluded(),
                agreement.getPreventiveVisitsIncluded(),
                agreement.getVisitLimit(),
                agreement.getSlaHours(),
                item.getCoverageScope()
        );
    }

    public ErpServiceDtos.WarrantyExtensionResponse cancelWarrantyExtension(Long organizationId, Long branchId, Long extensionId,
                                                                            ErpServiceDtos.CancelWarrantyExtensionRequest request) {
        WarrantyExtension extension = warrantyExtensionRepository.findByIdAndOrganizationId(extensionId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Warranty extension not found: " + extensionId));
        extension.setStatus("CANCELLED");
        if (trimToNull(request.remarks()) != null) {
            String currentRemarks = trimToNull(extension.getRemarks());
            extension.setRemarks(currentRemarks == null ? request.remarks().trim() : currentRemarks + " | Cancelled: " + request.remarks().trim());
        }
        extension = warrantyExtensionRepository.save(extension);

        auditEventWriter.write(
                organizationId,
                branchId,
                "WARRANTY_EXTENSION_CANCELLED",
                "warranty_extension",
                extension.getId(),
                extension.getReferenceNumber(),
                "CANCEL",
                null,
                null,
                null,
                "Warranty extension cancelled",
                ErpJsonPayloads.of("productOwnershipId", extension.getProductOwnershipId())
        );

        return toWarrantyExtensionResponse(extension);
    }

    public ServiceReplacement createReplacement(Long organizationId, Long branchId, ErpServiceDtos.CreateServiceReplacementRequest request) {
        if (request.serviceTicketId() == null
                && request.warrantyClaimId() == null
                && request.salesReturnId() == null
                && request.originalProductOwnershipId() == null
                && request.originalSerialNumberId() == null) {
            throw new BusinessException("Replacement must reference at least one source document, ownership, or original serial");
        }

        Customer customer = customerRepository.findByIdAndOrganizationId(request.customerId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + request.customerId()));
        StoreProduct originalProduct = productRepository.findById(request.originalProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.originalProductId()));
        StoreProduct replacementProduct = productRepository.findById(request.replacementProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.replacementProductId()));
        if (!organizationId.equals(originalProduct.getOrganizationId()) || !organizationId.equals(replacementProduct.getOrganizationId())) {
            throw new BusinessException("Replacement products must belong to organization " + organizationId);
        }
        uomRepository.findById(request.replacementUomId())
                .orElseThrow(() -> new ResourceNotFoundException("UOM not found: " + request.replacementUomId()));

        ServiceTicket ticket = null;
        if (request.serviceTicketId() != null) {
            ticket = serviceTicketRepository.findByOrganizationIdAndId(organizationId, request.serviceTicketId())
                    .orElseThrow(() -> new ResourceNotFoundException("Service ticket not found: " + request.serviceTicketId()));
            if (!request.customerId().equals(ticket.getCustomerId())) {
                throw new BusinessException("Service ticket does not belong to the selected customer");
            }
        }

        WarrantyClaim claim = null;
        if (request.warrantyClaimId() != null) {
            claim = warrantyClaimRepository.findByOrganizationIdAndId(organizationId, request.warrantyClaimId())
                    .orElseThrow(() -> new ResourceNotFoundException("Warranty claim not found: " + request.warrantyClaimId()));
            if (!request.customerId().equals(claim.getCustomerId())) {
                throw new BusinessException("Warranty claim does not belong to the selected customer");
            }
            if (!request.originalProductId().equals(claim.getProductId())) {
                throw new BusinessException("Warranty claim does not belong to the original product");
            }
        }

        SalesReturn salesReturn = null;
        if (request.salesReturnId() != null) {
            salesReturn = salesReturnRepository.findByOrganizationIdAndId(organizationId, request.salesReturnId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sales return not found: " + request.salesReturnId()));
            if (!request.customerId().equals(salesReturn.getCustomerId())) {
                throw new BusinessException("Sales return does not belong to the selected customer");
            }
        }

        ProductOwnership originalOwnership = null;
        if (request.originalProductOwnershipId() != null) {
            originalOwnership = productOwnershipRepository.findById(request.originalProductOwnershipId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product ownership not found: " + request.originalProductOwnershipId()));
            if (!organizationId.equals(originalOwnership.getOrganizationId())) {
                throw new BusinessException("Product ownership does not belong to organization " + organizationId);
            }
            if (!request.customerId().equals(originalOwnership.getCustomerId())) {
                throw new BusinessException("Product ownership does not belong to the selected customer");
            }
            if (!request.originalProductId().equals(originalOwnership.getProductId())) {
                throw new BusinessException("Product ownership does not belong to the original product");
            }
            if (!ErpDocumentStatuses.ACTIVE.equals(originalOwnership.getStatus())) {
                throw new BusinessException("Only active product ownership can be replaced");
            }
        }

        SerialNumber originalSerial = null;
        if (request.originalSerialNumberId() != null) {
            originalSerial = serialNumberRepository.findById(request.originalSerialNumberId())
                    .orElseThrow(() -> new ResourceNotFoundException("Serial number not found: " + request.originalSerialNumberId()));
            if (!organizationId.equals(originalSerial.getOrganizationId())) {
                throw new BusinessException("Original serial does not belong to organization " + organizationId);
            }
            if (!request.originalProductId().equals(originalSerial.getProductId())) {
                throw new BusinessException("Original serial does not belong to the original product");
            }
            if (originalOwnership != null && originalOwnership.getSerialNumberId() != null
                    && !request.originalSerialNumberId().equals(originalOwnership.getSerialNumberId())) {
                throw new BusinessException("Original serial does not match the selected product ownership");
            }
            if (!request.customerId().equals(originalSerial.getCurrentCustomerId())) {
                throw new BusinessException("Original serial is not currently mapped to the selected customer");
            }
        }

        SerialNumber replacementSerial = null;
        if (request.replacementSerialNumberId() != null) {
            replacementSerial = serialNumberRepository.findById(request.replacementSerialNumberId())
                    .orElseThrow(() -> new ResourceNotFoundException("Serial number not found: " + request.replacementSerialNumberId()));
            if (!organizationId.equals(replacementSerial.getOrganizationId())) {
                throw new BusinessException("Replacement serial does not belong to organization " + organizationId);
            }
            if (!request.replacementProductId().equals(replacementSerial.getProductId())) {
                throw new BusinessException("Replacement serial does not belong to the replacement product");
            }
            if (!ErpDocumentStatuses.IN_STOCK.equals(replacementSerial.getStatus())) {
                throw new BusinessException("Replacement serial is not available for issue");
            }
            if (replacementSerial.getCurrentWarehouseId() == null || !request.warehouseId().equals(replacementSerial.getCurrentWarehouseId())) {
                throw new BusinessException("Replacement serial is not available in warehouse " + request.warehouseId());
            }
            if (request.replacementBaseQuantity().compareTo(java.math.BigDecimal.ONE) != 0) {
                throw new BusinessException("Serialized replacement requires base quantity of 1");
            }
        }

        if (originalSerial != null && replacementSerial != null && originalSerial.getId().equals(replacementSerial.getId())) {
            throw new BusinessException("Replacement serial must be different from the original serial");
        }

        if (claim != null && ticket != null && claim.getServiceTicketId() != null && !ticket.getId().equals(claim.getServiceTicketId())) {
            throw new BusinessException("Warranty claim does not belong to the selected service ticket");
        }
        if (claim != null && salesReturn != null && claim.getSalesReturnId() != null && !salesReturn.getId().equals(claim.getSalesReturnId())) {
            throw new BusinessException("Warranty claim does not belong to the selected sales return");
        }
        if (ticket != null && salesReturn != null && ticket.getSalesReturnId() != null && !salesReturn.getId().equals(ticket.getSalesReturnId())) {
            throw new BusinessException("Service ticket does not belong to the selected sales return");
        }
        if (claim != null && originalOwnership != null && claim.getProductOwnershipId() != null
                && !originalOwnership.getId().equals(claim.getProductOwnershipId())) {
            throw new BusinessException("Warranty claim does not belong to the selected product ownership");
        }
        if (claim != null && originalSerial != null && claim.getSerialNumberId() != null
                && !originalSerial.getId().equals(claim.getSerialNumberId())) {
            throw new BusinessException("Warranty claim does not belong to the selected original serial");
        }

        if (claim != null && (serviceReplacementRepository.existsByWarrantyClaimIdAndStatus(claim.getId(), ErpDocumentStatuses.ISSUED)
                || serviceReplacementRepository.existsByWarrantyClaimIdAndStatus(claim.getId(), ErpDocumentStatuses.PENDING_APPROVAL))) {
            throw new BusinessException("A replacement is already issued for this warranty claim");
        }
        if (ticket != null && (serviceReplacementRepository.existsByServiceTicketIdAndStatus(ticket.getId(), ErpDocumentStatuses.ISSUED)
                || serviceReplacementRepository.existsByServiceTicketIdAndStatus(ticket.getId(), ErpDocumentStatuses.PENDING_APPROVAL))) {
            throw new BusinessException("A replacement is already issued for this service ticket");
        }
        if (originalOwnership != null && (serviceReplacementRepository.existsByOriginalProductOwnershipIdAndStatus(originalOwnership.getId(), ErpDocumentStatuses.ISSUED)
                || serviceReplacementRepository.existsByOriginalProductOwnershipIdAndStatus(originalOwnership.getId(), ErpDocumentStatuses.PENDING_APPROVAL))) {
            throw new BusinessException("A replacement is already issued for this product ownership");
        }
        if (salesReturn != null && (serviceReplacementRepository.existsBySalesReturnIdAndStatus(salesReturn.getId(), ErpDocumentStatuses.ISSUED)
                || serviceReplacementRepository.existsBySalesReturnIdAndStatus(salesReturn.getId(), ErpDocumentStatuses.PENDING_APPROVAL))) {
            throw new BusinessException("A replacement is already issued for this sales return");
        }
        String replacementType = normalizeReplacementType(request.replacementType());
        if ("SALES_RETURN_REPLACEMENT".equals(replacementType) && salesReturn == null) {
            throw new BusinessException("Sales return replacement requires a linked sales return");
        }
        LocalDate issuedOn = request.issuedOn() == null ? LocalDate.now() : request.issuedOn();
        LocalDate warrantyStartDate = request.warrantyStartDate();
        LocalDate warrantyEndDate = request.warrantyEndDate();
        if (warrantyStartDate == null && originalOwnership != null) {
            warrantyStartDate = originalOwnership.getWarrantyStartDate();
        }
        if (warrantyEndDate == null && originalOwnership != null) {
            warrantyEndDate = originalOwnership.getWarrantyEndDate();
        }
        if (warrantyStartDate == null && replacementSerial != null) {
            warrantyStartDate = issuedOn;
        }

        String replacementNumber = "REP-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + System.currentTimeMillis();
        ServiceReplacement replacement = new ServiceReplacement();
        replacement.setOrganizationId(organizationId);
        replacement.setBranchId(branchId);
        replacement.setWarehouseId(request.warehouseId());
        replacement.setServiceTicketId(ticket == null ? request.serviceTicketId() : ticket.getId());
        replacement.setWarrantyClaimId(claim == null ? request.warrantyClaimId() : claim.getId());
        replacement.setSalesReturnId(salesReturn == null ? request.salesReturnId() : salesReturn.getId());
        replacement.setCustomerId(customer.getId());
        replacement.setOriginalProductId(request.originalProductId());
        replacement.setOriginalSerialNumberId(originalSerial == null ? request.originalSerialNumberId() : originalSerial.getId());
        replacement.setOriginalProductOwnershipId(originalOwnership == null ? request.originalProductOwnershipId() : originalOwnership.getId());
        replacement.setReplacementProductId(request.replacementProductId());
        replacement.setReplacementSerialNumberId(replacementSerial == null ? request.replacementSerialNumberId() : replacementSerial.getId());
        replacement.setReplacementUomId(request.replacementUomId());
        replacement.setReplacementQuantity(request.replacementQuantity());
        replacement.setReplacementBaseQuantity(request.replacementBaseQuantity());
        replacement.setReplacementNumber(replacementNumber);
        replacement.setReplacementType(replacementType);
        replacement.setStockSourceBucket(normalizeStockSourceBucket(request.stockSourceBucket(), replacementType));
        replacement.setStatus(ErpDocumentStatuses.SUBMITTED);
        replacement.setIssuedOn(issuedOn);
        replacement.setWarrantyStartDate(warrantyStartDate);
        replacement.setWarrantyEndDate(warrantyEndDate);
        replacement.setNotes(request.notes());
        replacement = serviceReplacementRepository.save(replacement);
        ErpApprovalService.ApprovalEvaluation evaluation = erpApprovalService.evaluate(
                organizationId,
                new ErpApprovalDtos.ApprovalEvaluationQuery("service_replacement", replacement.getId(), replacementType)
        );
        if (evaluation.approvalRequired()) {
            replacement.setStatus(ErpDocumentStatuses.PENDING_APPROVAL);
            replacement = serviceReplacementRepository.save(replacement);
            if (!evaluation.pendingRequestExists()) {
                erpApprovalService.createRequest(
                        organizationId,
                        branchId,
                        new ErpApprovalDtos.CreateApprovalRequest(
                                "service_replacement",
                                replacement.getId(),
                                replacement.getReplacementNumber(),
                                replacementType,
                                "Service replacement matched approval rule",
                                null,
                                null
                        )
                );
            }
            return replacement;
        }

        return serviceReplacementPostingService.finalizeApprovedReplacement(replacement.getId());
    }

    public record ServiceTicketDetails(ServiceTicket ticket, List<ServiceTicketItem> items, List<ServiceVisit> visits) {}

    public record ServiceAgreementDetails(ServiceAgreement agreement, List<ServiceAgreementItem> items) {}

    private String normalizeSourceType(String sourceType) {
        return switch (sourceType.toUpperCase()) {
            case "INVOICE", "WALK_IN", "AMC", "REPLACEMENT", "SALES_RETURN", "OTHER" -> sourceType.toUpperCase();
            default -> throw new BusinessException("Invalid service source type: " + sourceType);
        };
    }

    private String normalizePriority(String priority) {
        return switch (priority.toUpperCase()) {
            case "LOW", "MEDIUM", "HIGH", "CRITICAL" -> priority.toUpperCase();
            default -> throw new BusinessException("Invalid service priority: " + priority);
        };
    }

    private String normalizeVisitStatus(String status) {
        return switch (status.toUpperCase()) {
            case "SCHEDULED", "STARTED", "COMPLETED", "CANCELLED", "FAILED" -> status.toUpperCase();
            default -> throw new BusinessException("Invalid service visit status: " + status);
        };
    }

    private String normalizeResolutionStatus(String status) {
        return switch (status.toUpperCase()) {
            case "PENDING", "REPAIRABLE", "REPLACED", "REJECTED", "CLOSED" -> status.toUpperCase();
            default -> throw new BusinessException("Invalid service resolution status: " + status);
        };
    }

    private String normalizeClaimType(String type) {
        return switch (type.toUpperCase()) {
            case "REPAIR", "REPLACEMENT", "CREDIT_NOTE", "REJECTED" -> type.toUpperCase();
            default -> throw new BusinessException("Invalid warranty claim type: " + type);
        };
    }

    private String normalizeClaimStatus(String status) {
        return switch (status.toUpperCase()) {
            case "OPEN", "SUBMITTED", "APPROVED", "REJECTED", "SETTLED", "CLOSED" -> status.toUpperCase();
            default -> throw new BusinessException("Invalid warranty claim status: " + status);
        };
    }

    private String normalizeExtensionType(String type) {
        return switch (type.toUpperCase()) {
            case "MANUFACTURER_PROMO", "PAID_EXTENDED", "GOODWILL", "MANUAL_CORRECTION" -> type.toUpperCase();
            default -> throw new BusinessException("Invalid warranty extension type: " + type);
        };
    }

    private String normalizeAgreementType(String type) {
        return switch (type.toUpperCase()) {
            case "AMC", "INSTALLATION_SUPPORT", "SERVICE_CONTRACT", "PREVENTIVE_MAINTENANCE" -> type.toUpperCase();
            default -> throw new BusinessException("Invalid service agreement type: " + type);
        };
    }

    private String normalizeAgreementStatus(String status) {
        if (status == null || status.isBlank()) {
            return "ACTIVE";
        }
        return switch (status.toUpperCase()) {
            case "DRAFT", "ACTIVE", "EXPIRED", "CANCELLED" -> status.toUpperCase();
            default -> throw new BusinessException("Invalid service agreement status: " + status);
        };
    }

    private String normalizeCoverageScope(String coverageScope) {
        if (coverageScope == null || coverageScope.isBlank()) {
            return "FULL";
        }
        return switch (coverageScope.toUpperCase()) {
            case "FULL", "LABOR_ONLY", "PARTS_ONLY", "VISIT_ONLY" -> coverageScope.toUpperCase();
            default -> throw new BusinessException("Invalid service agreement coverage scope: " + coverageScope);
        };
    }

    private String normalizeUpstreamRouteType(String routeType, Long supplierId, Long distributorId, String upstreamCompanyName) {
        if (routeType != null && !routeType.isBlank()) {
            return routeType.trim().toUpperCase();
        }
        if (supplierId != null) {
            return "SUPPLIER";
        }
        if (distributorId != null) {
            return "DISTRIBUTOR";
        }
        if (trimToNull(upstreamCompanyName) != null) {
            return "COMPANY";
        }
        return null;
    }

    private String normalizeUpstreamStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return status.trim().toUpperCase();
    }

    private String normalizeReplacementType(String type) {
        return switch (type.toUpperCase()) {
            case "WARRANTY_REPLACEMENT", "SALES_RETURN_REPLACEMENT", "GOODWILL_REPLACEMENT" -> type.toUpperCase();
            default -> throw new BusinessException("Invalid replacement type: " + type);
        };
    }

    private String normalizeStockSourceBucket(String stockSourceBucket, String replacementType) {
        if (stockSourceBucket != null && !stockSourceBucket.isBlank()) {
            return stockSourceBucket.trim().toUpperCase();
        }
        if ("WARRANTY_REPLACEMENT".equals(replacementType)) {
            return "WARRANTY_BUFFER";
        }
        return "SALEABLE";
    }

    private String ticketPayload(ServiceTicket ticket) {
        return ErpJsonPayloads.of("ticketNumber", ticket.getTicketNumber(), "status", ticket.getStatus(), "customerId", ticket.getCustomerId());
    }

    private String claimPayload(WarrantyClaim claim) {
        return ErpJsonPayloads.of("claimNumber", claim.getClaimNumber(), "status", claim.getStatus(), "claimType", claim.getClaimType());
    }

    private LocalDate computeEffectiveWarrantyEndDate(ProductOwnership ownership, List<WarrantyExtension> extensions) {
        LocalDate effectiveEnd = ownership.getWarrantyEndDate();
        for (WarrantyExtension extension : extensions) {
            if (!"ACTIVE".equals(extension.getStatus())) {
                continue;
            }
            if (extension.getEndDate() != null) {
                effectiveEnd = effectiveEnd == null || extension.getEndDate().isAfter(effectiveEnd)
                        ? extension.getEndDate()
                        : effectiveEnd;
            } else if (effectiveEnd != null && extension.getMonthsAdded() != null) {
                effectiveEnd = effectiveEnd.plusMonths(extension.getMonthsAdded());
            }
        }
        return effectiveEnd;
    }

    private ErpServiceDtos.WarrantyExtensionResponse toWarrantyExtensionResponse(WarrantyExtension extension) {
        return new ErpServiceDtos.WarrantyExtensionResponse(
                extension.getId(),
                extension.getOrganizationId(),
                extension.getProductOwnershipId(),
                extension.getSerialNumberId(),
                extension.getSalesInvoiceId(),
                extension.getSalesInvoiceLineId(),
                extension.getExtensionType(),
                extension.getMonthsAdded(),
                extension.getStartDate(),
                extension.getEndDate(),
                extension.getStatus(),
                extension.getReason(),
                extension.getReferenceNumber(),
                extension.getAmount(),
                extension.getRemarks(),
                extension.getCreatedAt(),
                extension.getUpdatedAt()
        );
    }

    public ErpServiceDtos.ServiceAgreementResponse toServiceAgreementResponse(ServiceAgreement agreement, List<ServiceAgreementItem> items) {
        return new ErpServiceDtos.ServiceAgreementResponse(
                agreement.getId(),
                agreement.getOrganizationId(),
                agreement.getBranchId(),
                agreement.getCustomerId(),
                agreement.getSalesInvoiceId(),
                agreement.getAgreementNumber(),
                agreement.getAgreementType(),
                agreement.getStatus(),
                agreement.getServiceStartDate(),
                agreement.getServiceEndDate(),
                agreement.getLaborIncluded(),
                agreement.getPartsIncluded(),
                agreement.getPreventiveVisitsIncluded(),
                agreement.getVisitLimit(),
                agreement.getSlaHours(),
                agreement.getAgreementAmount(),
                agreement.getNotes(),
                items.stream().map(this::toServiceAgreementItemResponse).toList(),
                agreement.getCreatedAt(),
                agreement.getUpdatedAt()
        );
    }

    private ErpServiceDtos.ServiceAgreementItemResponse toServiceAgreementItemResponse(ServiceAgreementItem item) {
        return new ErpServiceDtos.ServiceAgreementItemResponse(
                item.getId(),
                item.getServiceAgreementId(),
                item.getProductId(),
                item.getProductOwnershipId(),
                item.getSalesInvoiceLineId(),
                item.getSerialNumberId(),
                item.getCoverageScope(),
                item.getIncludedServiceNotes(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
