package com.retailmanagement.modules.inventory.repository;

import com.retailmanagement.modules.dashboard.dto.LowStockAlertDTO;
import com.retailmanagement.modules.inventory.model.Inventory;
import com.retailmanagement.modules.inventory.model.Warehouse;
import com.retailmanagement.modules.product.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductAndWarehouse(Product product, Warehouse warehouse);

    List<Inventory> findByProduct(Product product);

    List<Inventory> findByWarehouse(Warehouse warehouse);

    @Query("SELECT i FROM Inventory i WHERE i.quantity <= i.minimumStock")
    List<Inventory> findLowStockInventory();

    @Query("SELECT i FROM Inventory i WHERE i.quantity = 0")
    List<Inventory> findOutOfStockInventory();

    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.quantity <= i.minimumStock")
    long countLowStock();

    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.quantity = 0")
    long countOutOfStock();

    @Query("SELECT i FROM Inventory i WHERE i.product.id = :productId")
    List<Inventory> findByProductId(@Param("productId") Long productId);

    @Query("SELECT SUM(i.quantity) FROM Inventory i WHERE i.product.id = :productId")
    Integer getTotalStockByProduct(@Param("productId") Long productId);

    @Query("SELECT i from Inventory i WHERE i.quantity <= i.minimumStock")
    List<LowStockAlertDTO> getLowStockAlerts();
}