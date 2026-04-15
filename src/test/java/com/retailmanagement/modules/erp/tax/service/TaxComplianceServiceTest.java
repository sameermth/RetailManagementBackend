package com.retailmanagement.modules.erp.tax.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.retailmanagement.modules.erp.common.security.ErpAccessGuard;
import com.retailmanagement.modules.erp.foundation.entity.Branch;
import com.retailmanagement.modules.erp.foundation.entity.Organization;
import com.retailmanagement.modules.erp.foundation.repository.BranchRepository;
import com.retailmanagement.modules.erp.foundation.repository.OrganizationRepository;
import com.retailmanagement.modules.erp.party.entity.Customer;
import com.retailmanagement.modules.erp.party.repository.CustomerRepository;
import com.retailmanagement.modules.erp.sales.entity.SalesInvoice;
import com.retailmanagement.modules.erp.sales.entity.SalesInvoiceLine;
import com.retailmanagement.modules.erp.sales.repository.SalesInvoiceLineRepository;
import com.retailmanagement.modules.erp.sales.repository.SalesInvoiceRepository;
import com.retailmanagement.modules.erp.tax.entity.TaxComplianceDocument;
import com.retailmanagement.modules.erp.tax.repository.TaxComplianceDocumentRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaxComplianceServiceTest {

    @Mock private ErpAccessGuard accessGuard;
    @Mock private SalesInvoiceRepository salesInvoiceRepository;
    @Mock private SalesInvoiceLineRepository salesInvoiceLineRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private BranchRepository branchRepository;
    @Mock private TaxComplianceDocumentRepository taxComplianceDocumentRepository;

    @Test
    void createEinvoiceDraft_createsEligibleDraftWhenInvoiceDataIsComplete() {
        TaxComplianceService service = new TaxComplianceService(
                accessGuard,
                salesInvoiceRepository,
                salesInvoiceLineRepository,
                customerRepository,
                organizationRepository,
                branchRepository,
                taxComplianceDocumentRepository,
                List.of(new DisabledGstComplianceProvider(), new SimulatedGstComplianceProvider()),
                new ObjectMapper().registerModule(new JavaTimeModule())
        );
        injectComplianceProviderCode(service, "SIMULATED");

        mockInvoiceContext(true);
        when(taxComplianceDocumentRepository.save(any(TaxComplianceDocument.class))).thenAnswer(invocation -> {
            TaxComplianceDocument document = invocation.getArgument(0);
            document.setId(11L);
            return document;
        });

        var response = service.createEinvoiceDraft(54L, null);

        assertEquals(11L, response.id());
        assertEquals("E_INVOICE", response.documentType());
        assertEquals("DRAFT", response.status());
        assertTrue(response.eligibleForSubmission());
        assertEquals("INV-54", ((java.util.Map<?, ?>) response.payload().get("document")).get("invoiceNumber"));
        assertEquals("SIMULATED", response.providerCode());
    }

    @Test
    void createEinvoiceDraft_marksDraftBlockedWhenSellerGstinMissing() {
        TaxComplianceService service = new TaxComplianceService(
                accessGuard,
                salesInvoiceRepository,
                salesInvoiceLineRepository,
                customerRepository,
                organizationRepository,
                branchRepository,
                taxComplianceDocumentRepository,
                List.of(new DisabledGstComplianceProvider(), new SimulatedGstComplianceProvider()),
                new ObjectMapper().registerModule(new JavaTimeModule())
        );
        injectComplianceProviderCode(service, "SIMULATED");

        mockInvoiceContext(false);
        when(taxComplianceDocumentRepository.save(any(TaxComplianceDocument.class))).thenAnswer(invocation -> {
            TaxComplianceDocument document = invocation.getArgument(0);
            document.setId(12L);
            return document;
        });

        var response = service.createEinvoiceDraft(54L, null);

        assertEquals("BLOCKED", response.status());
        assertFalse(response.eligibleForSubmission());
        assertTrue(response.warnings().stream().anyMatch(message -> message.contains("Seller GST registration is missing")));
    }

    @Test
    void submitAndSyncDocument_updatesSubmissionLifecycle() {
        TaxComplianceService service = new TaxComplianceService(
                accessGuard,
                salesInvoiceRepository,
                salesInvoiceLineRepository,
                customerRepository,
                organizationRepository,
                branchRepository,
                taxComplianceDocumentRepository,
                List.of(new DisabledGstComplianceProvider(), new SimulatedGstComplianceProvider()),
                new ObjectMapper().registerModule(new JavaTimeModule())
        );
        injectComplianceProviderCode(service, "SIMULATED");

        TaxComplianceDocument draft = new TaxComplianceDocument();
        draft.setId(21L);
        draft.setOrganizationId(33L);
        draft.setBranchId(71L);
        draft.setSourceType("SALES_INVOICE");
        draft.setSourceId(54L);
        draft.setDocumentType("E_INVOICE");
        draft.setProviderCode("SIMULATED");
        draft.setStatus("DRAFT");
        draft.setEligibleForSubmission(true);
        draft.setPayloadJson("{\"document\":{\"invoiceNumber\":\"INV-54\"}}");
        draft.setWarningJson("[]");
        draft.setGeneratedAt(LocalDateTime.now());

        when(taxComplianceDocumentRepository.findById(21L)).thenReturn(Optional.of(draft));
        when(accessGuard.assertOrganizationAccess(33L)).thenReturn(33L);
        when(taxComplianceDocumentRepository.save(any(TaxComplianceDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var submitted = service.submitDocument(21L);
        assertEquals("SUBMITTED", submitted.status());
        assertEquals("SIMULATED", submitted.providerCode());
        assertTrue(submitted.providerResponse().containsKey("providerStatus"));
        assertEquals("ACCEPTED", submitted.providerResponse().get("providerStatus"));

        var synced = service.syncDocumentStatus(21L);
        assertEquals("GENERATED", synced.status());
        assertEquals("GENERATED", synced.providerResponse().get("providerStatus"));
    }

    private void mockInvoiceContext(boolean withSellerGstin) {
        SalesInvoice invoice = new SalesInvoice();
        invoice.setId(54L);
        invoice.setOrganizationId(33L);
        invoice.setBranchId(71L);
        invoice.setCustomerId(57L);
        invoice.setInvoiceNumber("INV-54");
        invoice.setInvoiceDate(LocalDate.of(2026, 4, 14));
        invoice.setDueDate(LocalDate.of(2026, 4, 21));
        invoice.setStatus("POSTED");
        invoice.setSellerGstin(withSellerGstin ? "24ABCDE1234F1Z5" : null);
        invoice.setCustomerGstin("24AACCU9603R1ZM");
        invoice.setPlaceOfSupplyStateCode("24");
        invoice.setSubtotal(new BigDecimal("1000.00"));
        invoice.setDiscountAmount(new BigDecimal("50.00"));
        invoice.setTaxAmount(new BigDecimal("171.00"));
        invoice.setTotalAmount(new BigDecimal("1121.00"));

        SalesInvoiceLine line = new SalesInvoiceLine();
        line.setId(90L);
        line.setProductId(44L);
        line.setQuantity(new BigDecimal("1.000000"));
        line.setBaseQuantity(new BigDecimal("1.000000"));
        line.setUnitPrice(new BigDecimal("1000.00"));
        line.setDiscountAmount(new BigDecimal("50.00"));
        line.setTaxableAmount(new BigDecimal("950.00"));
        line.setCgstRate(new BigDecimal("9.00"));
        line.setCgstAmount(new BigDecimal("85.50"));
        line.setSgstRate(new BigDecimal("9.00"));
        line.setSgstAmount(new BigDecimal("85.50"));
        line.setIgstRate(BigDecimal.ZERO);
        line.setIgstAmount(BigDecimal.ZERO);
        line.setCessRate(BigDecimal.ZERO);
        line.setCessAmount(BigDecimal.ZERO);
        line.setLineAmount(new BigDecimal("1121.00"));
        line.setHsnSnapshot("85071000");

        Customer customer = new Customer();
        customer.setId(57L);
        customer.setOrganizationId(33L);
        customer.setCustomerCode("CUST-57");
        customer.setFullName("Retail Buyer");
        customer.setLegalName("Retail Buyer LLP");
        customer.setTradeName("Retail Buyer");
        customer.setGstin("24AACCU9603R1ZM");
        customer.setState("Gujarat");
        customer.setStateCode("24");
        customer.setBillingAddress("SG Highway");
        customer.setShippingAddress("SG Highway");

        Organization organization = new Organization();
        organization.setId(33L);
        organization.setCode("SPC");
        organization.setName("Spare Center");
        organization.setLegalName("Spare Center Private Limited");
        organization.setPhone("9999999999");
        organization.setEmail("owner@spc.test");

        Branch branch = new Branch();
        branch.setId(71L);
        branch.setOrganizationId(33L);
        branch.setCode("MAIN");
        branch.setName("Main Branch");
        branch.setAddressLine1("SG Highway");
        branch.setCity("Ahmedabad");
        branch.setState("Gujarat");
        branch.setPostalCode("380015");
        branch.setCountry("India");

        when(salesInvoiceRepository.findById(54L)).thenReturn(Optional.of(invoice));
        when(salesInvoiceLineRepository.findBySalesInvoiceIdOrderByIdAsc(54L)).thenReturn(List.of(line));
        when(customerRepository.findByIdAndOrganizationId(57L, 33L)).thenReturn(Optional.of(customer));
        when(organizationRepository.findById(33L)).thenReturn(Optional.of(organization));
        when(branchRepository.findByIdAndOrganizationId(71L, 33L)).thenReturn(Optional.of(branch));
        when(accessGuard.assertOrganizationAccess(33L)).thenReturn(33L);
    }

    private static void injectComplianceProviderCode(TaxComplianceService service, String value) {
        try {
            var field = TaxComplianceService.class.getDeclaredField("configuredComplianceProviderCode");
            field.setAccessible(true);
            field.set(service, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
