package com.retailmanagement.modules.expense.model;

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
@Table(name = "expense_attachments")
@EntityListeners(AuditingEntityListener.class)
public class ExpenseAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    private String fileName;

    private String fileType;

    private Long fileSize;

    private String fileUrl;

    private Boolean isReceipt = false;

    @CreatedDate
    private LocalDateTime uploadedAt;

    private String uploadedBy;
}