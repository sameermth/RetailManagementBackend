package com.retailmanagement.modules.product.service.impl;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.product.dto.request.ProductRequest;
import com.retailmanagement.modules.product.dto.response.ProductResponse;
import com.retailmanagement.modules.product.mapper.ProductMapper;
import com.retailmanagement.modules.product.model.Brand;
import com.retailmanagement.modules.product.model.Category;
import com.retailmanagement.modules.product.model.Product;
import com.retailmanagement.modules.product.repository.BrandRepository;
import com.retailmanagement.modules.product.repository.CategoryRepository;
import com.retailmanagement.modules.product.repository.ProductRepository;
import com.retailmanagement.modules.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductMapper productMapper;

    @Override
    public ProductResponse createProduct(ProductRequest request) {
        log.info("Creating new product with SKU: {}", request.getSku());

        // Check if SKU already exists
        if (productRepository.existsBySku(request.getSku())) {
            throw new BusinessException("Product with SKU " + request.getSku() + " already exists");
        }

        // Check if barcode already exists (if provided)
        if (request.getBarcode() != null && productRepository.existsByBarcode(request.getBarcode())) {
            throw new BusinessException("Product with barcode " + request.getBarcode() + " already exists");
        }

        Product product = productMapper.toEntity(request);

        // Set category if provided
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));
            product.setCategory(category);
        }

        // Set brand if provided
        if (request.getBrandId() != null) {
            Brand brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + request.getBrandId()));
            product.setBrand(brand);
        }

        // Set audit info
        product.setCreatedBy("SYSTEM"); // In real app, get from SecurityContext
        product.setUpdatedBy("SYSTEM");

        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully with ID: {}", savedProduct.getId());

        return productMapper.toResponse(savedProduct);
    }

    @Override
    @CacheEvict(value = "products", key = "#id")
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        log.info("Updating product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        // Check SKU uniqueness if changed
        if (!product.getSku().equals(request.getSku()) &&
                productRepository.existsBySku(request.getSku())) {
            throw new BusinessException("Product with SKU " + request.getSku() + " already exists");
        }

        // Check barcode uniqueness if changed and provided
        if (request.getBarcode() != null &&
                !request.getBarcode().equals(product.getBarcode()) &&
                productRepository.existsByBarcode(request.getBarcode())) {
            throw new BusinessException("Product with barcode " + request.getBarcode() + " already exists");
        }

        // Update fields
        product.setSku(request.getSku());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setUnitPrice(request.getUnitPrice());
        product.setCostPrice(request.getCostPrice());
        product.setGstRate(request.getGstRate());
        product.setHsnCode(request.getHsnCode());
        product.setUnitOfMeasure(request.getUnitOfMeasure());
        product.setReorderLevel(request.getReorderLevel());
        product.setReorderQuantity(request.getReorderQuantity());
        product.setSpecifications(request.getSpecifications());
        product.setBarcode(request.getBarcode());
        product.setManufacturer(request.getManufacturer());
        product.setCountryOfOrigin(request.getCountryOfOrigin());
        product.setIsPerishable(request.getIsPerishable());
        product.setShelfLifeDays(request.getShelfLifeDays());

        // Update category if changed
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));
            product.setCategory(category);
        } else {
            product.setCategory(null);
        }

        // Update brand if changed
        if (request.getBrandId() != null) {
            Brand brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + request.getBrandId()));
            product.setBrand(brand);
        } else {
            product.setBrand(null);
        }

        product.setUpdatedBy("SYSTEM");

        Product updatedProduct = productRepository.save(product);
        log.info("Product updated successfully with ID: {}", updatedProduct.getId());

        return productMapper.toResponse(updatedProduct);
    }

    @Override
    @Cacheable(value = "products", key = "#id")
    public ProductResponse getProductById(Long id) {
        log.debug("Fetching product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        return productMapper.toResponse(product);
    }

    @Override
    public ProductResponse getProductBySku(String sku) {
        log.debug("Fetching product with SKU: {}", sku);

        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with SKU: " + sku));

        return productMapper.toResponse(product);
    }

    @Override
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        log.debug("Fetching all products with pagination");

        return productRepository.findAll(pageable)
                .map(productMapper::toResponse);
    }

    @Override
    public Page<ProductResponse> searchProducts(String searchTerm, Pageable pageable) {
        log.debug("Searching products with term: {}", searchTerm);

        return productRepository.searchProducts(searchTerm, pageable)
                .map(productMapper::toResponse);
    }

    @Override
    @CacheEvict(value = "products", key = "#id")
    public void deleteProduct(Long id) {
        log.info("Deleting product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        // Soft delete
        product.setIsActive(false);
        product.setUpdatedBy("SYSTEM");
        productRepository.save(product);

        log.info("Product deleted successfully with ID: {}", id);
    }

    @Override
    @CacheEvict(value = "products", key = "#id")
    public void activateProduct(Long id) {
        log.info("Activating product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        product.setIsActive(true);
        product.setUpdatedBy("SYSTEM");
        productRepository.save(product);
    }

    @Override
    @CacheEvict(value = "products", key = "#id")
    public void deactivateProduct(Long id) {
        log.info("Deactivating product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        product.setIsActive(false);
        product.setUpdatedBy("SYSTEM");
        productRepository.save(product);
    }

    @Override
    public List<ProductResponse> getProductsByCategory(Long categoryId) {
        log.debug("Fetching products by category ID: {}", categoryId);

        return productRepository.findByCategoryId(categoryId).stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> getProductsByBrand(Long brandId) {
        log.debug("Fetching products by brand ID: {}", brandId);

        return productRepository.findByBrandId(brandId).stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> getProductsNeedingReorder() {
        log.debug("Fetching products needing reorder");

        return productRepository.findProductsNeedingReorder().stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isSkuUnique(String sku) {
        return !productRepository.existsBySku(sku);
    }

    @Override
    public boolean isBarcodeUnique(String barcode) {
        return !productRepository.existsByBarcode(barcode);
    }
}