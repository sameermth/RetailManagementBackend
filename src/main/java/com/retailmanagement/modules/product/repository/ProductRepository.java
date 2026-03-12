package com.retailmanagement.modules.product.repository;

import com.retailmanagement.modules.product.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    Optional<Product> findByBarcode(String barcode);

    Page<Product> findByIsActiveTrue(Pageable pageable);

    List<Product> findByCategoryId(Long categoryId);

    List<Product> findByBrandId(Long brandId);

    @Query("SELECT p FROM Product p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.sku) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.barcode) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Product> searchProducts(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.reorderLevel IS NOT NULL AND " +
            "p.id IN (SELECT i.product.id FROM Inventory i WHERE i.quantity <= p.reorderLevel)")
    List<Product> findProductsNeedingReorder();

    boolean existsBySku(String sku);

    boolean existsByBarcode(String barcode);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.isActive = true")
    long countActiveProducts();
}