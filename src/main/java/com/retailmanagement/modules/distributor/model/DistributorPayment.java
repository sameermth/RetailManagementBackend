package com.retailmanagement.modules.distributor.model;

import com.retailmanagement.modules.distributor.enums.PaymentMethod;
import com.retailmanagement.modules.distributor.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "distributor_payments")
@EntityListeners(AuditingEntityListener.class)
public class DistributorPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String paymentReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "distributor_id", nullable = false)
    private Distributor distributor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private DistributorOrder order;

    @Column(nullable = false)
    private LocalDateTime paymentDate;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String transactionId;

    private String bankName;

    private String chequeNumber;

    private LocalDateTime chequeDate;

    private String cardLastFour;

    private String cardType;

    private String upiId;

    private String notes;

    private String receivedBy;

    @CreatedDate
    private LocalDateTime createdAt;
}