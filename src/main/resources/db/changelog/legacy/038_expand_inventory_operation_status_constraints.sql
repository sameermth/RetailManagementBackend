ALTER TABLE stock_adjustment
    DROP CONSTRAINT IF EXISTS chk_stock_adjustment_status;

ALTER TABLE stock_adjustment
    ADD CONSTRAINT chk_stock_adjustment_status
    CHECK (status IN ('DRAFT','SUBMITTED','PENDING_APPROVAL','APPROVED','POSTED','REJECTED','CANCELLED'));

ALTER TABLE stock_transfer
    DROP CONSTRAINT IF EXISTS chk_stock_transfer_status;

ALTER TABLE stock_transfer
    ADD CONSTRAINT chk_stock_transfer_status
    CHECK (status IN ('DRAFT','SUBMITTED','PENDING_APPROVAL','POSTED','REJECTED','CANCELLED'));
