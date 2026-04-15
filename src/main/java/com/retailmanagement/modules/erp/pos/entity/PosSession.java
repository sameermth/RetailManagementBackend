package com.retailmanagement.modules.erp.pos.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgBranchScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "pos_session")
public class PosSession extends ErpOrgBranchScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "session_number", nullable = false)
    private String sessionNumber;

    @Column(name = "terminal_name")
    private String terminalName;

    @Column(name = "opened_by_user_id", nullable = false)
    private Long openedByUserId;

    @Column(name = "opened_by_username", nullable = false)
    private String openedByUsername;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "opening_cash_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal openingCashAmount;

    @Column(name = "opening_notes")
    private String openingNotes;

    @Column(nullable = false)
    private String status;

    @Column(name = "closed_by_user_id")
    private Long closedByUserId;

    @Column(name = "closed_by_username")
    private String closedByUsername;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "expected_closing_cash_amount", precision = 18, scale = 2)
    private BigDecimal expectedClosingCashAmount;

    @Column(name = "counted_closing_cash_amount", precision = 18, scale = 2)
    private BigDecimal countedClosingCashAmount;

    @Column(name = "cash_variance_amount", precision = 18, scale = 2)
    private BigDecimal cashVarianceAmount;

    @Column(name = "closing_notes")
    private String closingNotes;
}
