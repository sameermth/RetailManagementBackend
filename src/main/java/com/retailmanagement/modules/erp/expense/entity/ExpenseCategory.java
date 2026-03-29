package com.retailmanagement.modules.erp.expense.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgScopedEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(name = "ErpExpenseCategory")
@Table(name = "expense_category", indexes = {
        @Index(name = "idx_erp_expense_category_org_code", columnList = "organization_id,code")
})
public class ExpenseCategory extends ErpOrgScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "expense_account_id")
    private Long expenseAccountId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
