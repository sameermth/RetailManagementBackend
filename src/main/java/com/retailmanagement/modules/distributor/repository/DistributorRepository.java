package com.retailmanagement.modules.distributor.repository;

import com.retailmanagement.modules.distributor.model.Distributor;
import com.retailmanagement.modules.distributor.enums.DistributorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DistributorRepository extends JpaRepository<Distributor, Long> {

    Optional<Distributor> findByDistributorCode(String distributorCode);

    Optional<Distributor> findByEmail(String email);

    Optional<Distributor> findByPhone(String phone);

    Page<Distributor> findByStatus(DistributorStatus status, Pageable pageable);

    @Query("SELECT d FROM Distributor d WHERE " +
            "LOWER(d.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(d.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "d.phone LIKE CONCAT('%', :searchTerm, '%') OR " +
            "LOWER(d.contactPerson) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(d.distributorCode) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Distributor> searchDistributors(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT d FROM Distributor d WHERE d.region = :region")
    List<Distributor> findByRegion(@Param("region") String region);

    @Query("SELECT d FROM Distributor d WHERE d.territory = :territory")
    List<Distributor> findByTerritory(@Param("territory") String territory);

    @Query("SELECT d FROM Distributor d WHERE d.outstandingAmount > 0")
    List<Distributor> findDistributorsWithOutstanding();

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByDistributorCode(String distributorCode);
}