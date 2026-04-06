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
@Table(name = "product_attribute_definition", indexes = {
        @Index(name = "idx_product_attribute_definition_org_active", columnList = "organization_id,is_active,sort_order")
})
public class ProductAttributeDefinition extends ErpOrgScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String label;

    @Column
    private String description;

    @Column(name = "data_type", nullable = false)
    private String dataType;

    @Column(name = "input_type", nullable = false)
    private String inputType;

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "unit_label")
    private String unitLabel;

    @Column
    private String placeholder;

    @Column(name = "help_text")
    private String helpText;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 1;
}
