package com.retailmanagement.modules.product.service;

import com.retailmanagement.modules.product.dto.request.ProductRequest;
import com.retailmanagement.modules.product.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {

    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(Long id, ProductRequest request);

    ProductResponse getProductById(Long id);

    ProductResponse getProductBySku(String sku);

    Page<ProductResponse> getAllProducts(Pageable pageable);

    Page<ProductResponse> searchProducts(String searchTerm, Pageable pageable);

    void deleteProduct(Long id);

    void activateProduct(Long id);

    void deactivateProduct(Long id);

    List<ProductResponse> getProductsByCategory(Long categoryId);

    List<ProductResponse> getProductsByBrand(Long brandId);

    List<ProductResponse> getProductsNeedingReorder();

    boolean isSkuUnique(String sku);

    boolean isBarcodeUnique(String barcode);
}