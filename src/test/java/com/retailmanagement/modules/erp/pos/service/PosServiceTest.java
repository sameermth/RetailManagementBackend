package com.retailmanagement.modules.erp.pos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.retailmanagement.modules.erp.catalog.entity.StoreProduct;
import com.retailmanagement.modules.erp.catalog.repository.StoreProductRepository;
import com.retailmanagement.modules.erp.catalog.service.ProductService;
import com.retailmanagement.modules.erp.catalog.service.StoreProductPricingService;
import com.retailmanagement.modules.erp.common.security.ErpAccessGuard;
import com.retailmanagement.modules.erp.foundation.repository.OrganizationRepository;
import com.retailmanagement.modules.erp.foundation.repository.WarehouseRepository;
import com.retailmanagement.modules.erp.inventory.entity.InventoryBalance;
import com.retailmanagement.modules.erp.inventory.repository.InventoryBalanceRepository;
import com.retailmanagement.modules.erp.inventory.repository.InventoryBatchRepository;
import com.retailmanagement.modules.erp.inventory.repository.SerialNumberRepository;
import com.retailmanagement.modules.erp.party.repository.CustomerRepository;
import com.retailmanagement.modules.erp.pos.dto.PosDtos;
import com.retailmanagement.modules.erp.pos.entity.PosSession;
import com.retailmanagement.modules.erp.pos.repository.PosSessionRepository;
import com.retailmanagement.modules.erp.sales.repository.CustomerReceiptRepository;
import com.retailmanagement.modules.erp.sales.repository.SalesInvoiceRepository;
import com.retailmanagement.modules.erp.sales.service.ErpSalesService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PosServiceTest {

    @Mock private PosSessionRepository posSessionRepository;
    @Mock private WarehouseRepository warehouseRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private StoreProductRepository storeProductRepository;
    @Mock private ProductService productService;
    @Mock private StoreProductPricingService storeProductPricingService;
    @Mock private InventoryBalanceRepository inventoryBalanceRepository;
    @Mock private InventoryBatchRepository inventoryBatchRepository;
    @Mock private SerialNumberRepository serialNumberRepository;
    @Mock private SalesInvoiceRepository salesInvoiceRepository;
    @Mock private CustomerReceiptRepository customerReceiptRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private ErpSalesService erpSalesService;
    @Mock private ErpAccessGuard accessGuard;

    @InjectMocks
    private PosService posService;

    @Test
    void searchCatalog_prioritizesExactSkuAndFiltersUnavailableStockItems() {
        PosSession session = session();
        StoreProduct exactSku = stockProduct(44L, "WIRE-6SQ", "Copper Wire 6 Sqmm");
        StoreProduct partialSku = stockProduct(41L, "WIRE-6SQ-ALT", "Copper Wire 6 Sqmm Premium");
        StoreProduct unavailable = stockProduct(45L, "WIRE-6SQ-OLD", "Copper Wire 6 Sqmm Old Stock");

        when(posSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(accessGuard.assertBranchAccess(33L, 73L)).thenReturn(73L);
        when(storeProductRepository.searchActiveForPos(33L, "WIRE-6SQ")).thenReturn(List.of(partialSku, unavailable, exactSku));
        when(inventoryBalanceRepository.findByOrganizationIdAndProductIdAndWarehouseId(33L, 44L, 141L))
                .thenReturn(List.of(balance(12)));
        when(inventoryBalanceRepository.findByOrganizationIdAndProductIdAndWarehouseId(33L, 41L, 141L))
                .thenReturn(List.of(balance(4)));
        when(inventoryBalanceRepository.findByOrganizationIdAndProductIdAndWarehouseId(33L, 45L, 141L))
                .thenReturn(List.of(balance(0)));
        when(storeProductPricingService.resolveUnitPrice(33L, 44L, null, BigDecimal.ONE, java.time.LocalDate.now()))
                .thenReturn(new BigDecimal("120.00"));
        when(storeProductPricingService.resolveUnitPrice(33L, 41L, null, BigDecimal.ONE, java.time.LocalDate.now()))
                .thenReturn(new BigDecimal("95.00"));

        List<PosDtos.PosCatalogSearchItemResponse> results = posService.searchCatalog(1L, "WIRE-6SQ", null, 20);

        assertEquals(2, results.size());
        assertEquals(44L, results.getFirst().storeProductId());
        assertTrue(results.getFirst().exactSkuMatch());
        assertEquals(41L, results.get(1).storeProductId());
    }

    @Test
    void searchCatalog_keepsServiceItemEvenWithoutWarehouseStock() {
        PosSession session = session();
        StoreProduct serviceItem = new StoreProduct();
        serviceItem.setId(99L);
        serviceItem.setOrganizationId(33L);
        serviceItem.setProductId(199L);
        serviceItem.setSku("SRV-INSTALL");
        serviceItem.setName("Battery Installation");
        serviceItem.setBaseUomId(5L);
        serviceItem.setInventoryTrackingMode("NONE");
        serviceItem.setIsActive(true);
        serviceItem.setIsServiceItem(true);
        serviceItem.setDefaultMrp(new BigDecimal("250.00"));

        when(posSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(accessGuard.assertBranchAccess(33L, 73L)).thenReturn(73L);
        when(storeProductRepository.searchActiveForPos(33L, "install")).thenReturn(List.of(serviceItem));
        when(storeProductPricingService.resolveUnitPrice(33L, 99L, null, BigDecimal.ONE, java.time.LocalDate.now()))
                .thenReturn(new BigDecimal("200.00"));

        List<PosDtos.PosCatalogSearchItemResponse> results = posService.searchCatalog(1L, "install", null, 20);

        assertEquals(1, results.size());
        assertEquals(99L, results.getFirst().storeProductId());
        assertTrue(results.getFirst().serviceItem());
        assertEquals(new BigDecimal("0.00"), results.getFirst().availableBaseQuantity());
    }

    private PosSession session() {
        PosSession session = new PosSession();
        session.setId(1L);
        session.setOrganizationId(33L);
        session.setBranchId(73L);
        session.setWarehouseId(141L);
        session.setStatus("OPEN");
        session.setSessionNumber("POS-TEST");
        session.setOpenedAt(LocalDateTime.now());
        session.setOpenedByUserId(168L);
        session.setOpenedByUsername("SPC-OWN-01");
        session.setOpeningCashAmount(BigDecimal.ZERO);
        return session;
    }

    private StoreProduct stockProduct(Long id, String sku, String name) {
        StoreProduct product = new StoreProduct();
        product.setId(id);
        product.setOrganizationId(33L);
        product.setProductId(id + 1000);
        product.setSku(sku);
        product.setName(name);
        product.setBaseUomId(5L);
        product.setInventoryTrackingMode("STANDARD");
        product.setIsActive(true);
        product.setIsServiceItem(false);
        product.setDefaultMrp(new BigDecimal("150.00"));
        return product;
    }

    private InventoryBalance balance(int available) {
        InventoryBalance balance = new InventoryBalance();
        balance.setAvailableBaseQuantity(BigDecimal.valueOf(available));
        return balance;
    }
}
