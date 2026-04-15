package com.retailmanagement.modules.erp.imports.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retailmanagement.modules.erp.catalog.entity.Uom;
import com.retailmanagement.modules.erp.catalog.repository.BrandRepository;
import com.retailmanagement.modules.erp.catalog.repository.CategoryRepository;
import com.retailmanagement.modules.erp.catalog.repository.StoreProductRepository;
import com.retailmanagement.modules.erp.catalog.repository.TaxGroupRepository;
import com.retailmanagement.modules.erp.catalog.repository.UomRepository;
import com.retailmanagement.modules.erp.catalog.dto.ProductDtos;
import com.retailmanagement.modules.erp.catalog.service.ProductService;
import com.retailmanagement.modules.erp.common.security.ErpAccessGuard;
import com.retailmanagement.modules.erp.imports.dto.ErpImportDtos;
import com.retailmanagement.modules.erp.imports.repository.ImportJobRepository;
import com.retailmanagement.modules.erp.imports.repository.ImportJobRowRepository;
import com.retailmanagement.modules.erp.party.repository.CustomerRepository;
import com.retailmanagement.modules.erp.party.repository.SupplierRepository;
import com.retailmanagement.modules.erp.party.service.CustomerManagementService;
import com.retailmanagement.modules.erp.party.service.SupplierManagementService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ErpImportServiceTest {

    @Mock private ErpImportFileParser importFileParser;
    @Mock private ErpAccessGuard accessGuard;
    @Mock private CustomerManagementService customerManagementService;
    @Mock private SupplierManagementService supplierManagementService;
    @Mock private ProductService productService;
    @Mock private CustomerRepository customerRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private StoreProductRepository storeProductRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private BrandRepository brandRepository;
    @Mock private UomRepository uomRepository;
    @Mock private TaxGroupRepository taxGroupRepository;
    @Mock private ImportJobRepository importJobRepository;
    @Mock private ImportJobRowRepository importJobRowRepository;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private ErpImportService erpImportService;

    @Test
    void previewProducts_marksMissingCategoryAndBrandAsCreateHintsButKeepsRowValid() {
        MockMultipartFile file = new MockMultipartFile("file", "products.csv", "text/csv", new byte[0]);
        Map<String, String> values = Map.of(
                "sku", "BAT-001",
                "name", "Battery",
                "categoryname", "Battery",
                "brandname", "Amaron",
                "baseuomcode", "NOS",
                "hsncode", "85071000",
                "inventorytrackingmode", "SERIAL"
        );
        Uom uom = new Uom();
        uom.setId(5L);
        uom.setCode("NOS");

        when(accessGuard.assertOrganizationAccess(33L)).thenReturn(33L);
        when(importFileParser.parse(any())).thenReturn(List.of(new ErpImportFileParser.ParsedRow(2, values)));
        when(storeProductRepository.findFirstByOrganizationIdAndSkuIgnoreCase(33L, "BAT-001")).thenReturn(Optional.empty());
        when(categoryRepository.findByOrganizationIdAndNameIgnoreCase(33L, "Battery")).thenReturn(Optional.empty());
        when(brandRepository.findByOrganizationIdAndNameIgnoreCase(33L, "Amaron")).thenReturn(Optional.empty());
        when(uomRepository.findByCodeIgnoreCase("NOS")).thenReturn(Optional.of(uom));
        when(productService.suggestTaxGroup(33L, "85071000", java.time.LocalDate.now()))
                .thenReturn(new ProductDtos.TaxGroupSuggestionResponse(
                        "85071000", 91L, "GST_18", "GST 18%", null, null, null, null, java.time.LocalDate.now(), true, "Suggested from HSN rate"
                ));

        ErpImportDtos.ImportPreviewResponse response = erpImportService.previewProducts(33L, false, file);

        assertEquals(1, response.totalRows());
        assertEquals(1, response.validRows());
        assertEquals(0, response.invalidRows());
        assertEquals(ErpImportDtos.ImportRowStatus.VALID, response.rows().getFirst().status());
        assertEquals("CREATE", response.rows().getFirst().values().get("categoryaction"));
        assertEquals("CREATE", response.rows().getFirst().values().get("brandaction"));
        assertTrue(response.rows().getFirst().messages().isEmpty());
    }
}
