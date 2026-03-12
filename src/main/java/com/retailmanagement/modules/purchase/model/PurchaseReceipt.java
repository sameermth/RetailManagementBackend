package com.retailmanagement.modules.purchase.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "purchase_receipts")
@EntityListeners(AuditingEntityListener.class)
public class PurchaseReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String receiptNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_id", nullable = false)
    private Purchase purchase;

    @Column(nullable = false)
    private LocalDateTime receiptDate;

    private String receivedBy;

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL)
    private List<PurchaseReceiptItem> items = new ArrayList<>();

    private String notes;

    @CreatedDate
    private LocalDateTime createdAt;
}