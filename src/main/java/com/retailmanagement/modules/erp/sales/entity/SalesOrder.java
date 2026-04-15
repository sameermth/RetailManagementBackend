package com.retailmanagement.modules.erp.sales.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgBranchScopedEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sales_order")
public class SalesOrder extends ErpOrgBranchScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "source_quote_id")
    private Long sourceQuoteId;

    @Column(name = "order_number", nullable = false)
    private String orderNumber;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "expected_fulfillment_by")
    private LocalDate expectedFulfillmentBy;

    @Column(name = "seller_tax_registration_id")
    private Long sellerTaxRegistrationId;

    @Column(name = "seller_gstin")
    private String sellerGstin;

    @Column(name = "customer_gstin")
    private String customerGstin;

    @Column(name = "place_of_supply_state_code")
    private String placeOfSupplyStateCode;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "converted_sales_invoice_id")
    private Long convertedSalesInvoiceId;

    @Column
    private String remarks;
}
