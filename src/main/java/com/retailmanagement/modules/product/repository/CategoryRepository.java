package com.retailmanagement.modules.product.repository;

import com.retailmanagement.modules.product.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);

    List<Category> findByParentCategoryIsNull();

    List<Category> findByParentCategoryId(Long parentId);

    @Query("SELECT c FROM Category c WHERE c.isActive = true ORDER BY c.displayOrder")
    List<Category> findAllActiveOrdered();

    boolean existsByName(String name);
}