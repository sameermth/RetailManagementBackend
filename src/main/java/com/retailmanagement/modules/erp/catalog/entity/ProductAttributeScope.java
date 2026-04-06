package com.retailmanagement.modules.erp.catalog.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "product_attribute_scope", indexes = {
        @Index(name = "idx_product_attribute_scope_lookup", columnList = "organization_id,category_id,brand_id")
})
public class ProductAttributeScope extends ErpOrgScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "attribute_definition_id", nullable = false)
    private Long attributeDefinitionId;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "brand_id")
    private Long brandId;
}
