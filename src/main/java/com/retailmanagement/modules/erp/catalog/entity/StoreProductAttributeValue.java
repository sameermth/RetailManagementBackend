package com.retailmanagement.modules.erp.catalog.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "store_product_attribute_value", indexes = {
        @Index(name = "idx_store_product_attribute_value_lookup", columnList = "organization_id,store_product_id,attribute_definition_id")
})
public class StoreProductAttributeValue extends ErpOrgScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_product_id", nullable = false)
    private Long storeProductId;

    @Column(name = "attribute_definition_id", nullable = false)
    private Long attributeDefinitionId;

    @Column(name = "value_text")
    private String valueText;

    @Column(name = "value_number", precision = 18, scale = 6)
    private BigDecimal valueNumber;

    @Column(name = "value_boolean")
    private Boolean valueBoolean;

    @Column(name = "value_date")
    private LocalDate valueDate;

    @Column(name = "value_option_id")
    private Long valueOptionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "value_json", columnDefinition = "jsonb")
    private String valueJson;
}
