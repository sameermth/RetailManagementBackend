package com.retailmanagement.modules.customer.model;

import com.retailmanagement.modules.customer.enums.LoyaltyTransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "loyalty_transactions")
@EntityListeners(AuditingEntityListener.class)
public class LoyaltyTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    private String transactionReference;

    @Enumerated(EnumType.STRING)
    private LoyaltyTransactionType transactionType;

    private Integer points;

    private String description;

    private Long saleId;

    private LocalDateTime expiryDate;

    private Boolean isExpired = false;

    private LocalDateTime redeemedAt;

    private String redeemedFor;

    @CreatedDate
    private LocalDateTime createdAt;
}