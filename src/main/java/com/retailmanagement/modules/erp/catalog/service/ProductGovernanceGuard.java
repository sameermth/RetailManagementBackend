package com.retailmanagement.modules.erp.catalog.service;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.erp.catalog.entity.Product;
import com.retailmanagement.modules.erp.catalog.entity.StoreProduct;
import com.retailmanagement.modules.erp.catalog.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductGovernanceGuard {

    private final ProductRepository productRepository;

    public Product requireTransactionAllowed(StoreProduct storeProduct, String operation) {
        Product product = requireProductMaster(storeProduct);
        assertTransactionAllowed(product, operation);
        return product;
    }

    public Product requireProductMaster(StoreProduct storeProduct) {
        return productRepository.findById(storeProduct.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product master not found: " + storeProduct.getProductId()));
    }

    public void assertTransactionAllowed(Product product, String operation) {
        String governanceStatus = trimToNull(product.getGovernanceStatus());
        boolean hardBlocked = Boolean.TRUE.equals(product.getBlockTransactions())
                || "BLOCKED".equalsIgnoreCase(governanceStatus)
                || "RECALLED".equalsIgnoreCase(governanceStatus);
        if (!hardBlocked) {
            return;
        }
        String suffix = product.getGovernanceReason() == null ? "" : ": " + product.getGovernanceReason();
        throw new BusinessException("Catalog product is blocked for " + operation + suffix);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
