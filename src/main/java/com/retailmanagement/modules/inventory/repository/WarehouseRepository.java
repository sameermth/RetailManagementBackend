package com.retailmanagement.modules.inventory.repository;

import com.retailmanagement.modules.inventory.model.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    Optional<Warehouse> findByCode(String code);

    List<Warehouse> findByIsActiveTrue();

    Optional<Warehouse> findByIsPrimaryTrue();

    @Query("SELECT w FROM Warehouse w WHERE w.city = :city")
    List<Warehouse> findByCity(String city);

    boolean existsByCode(String code);
}