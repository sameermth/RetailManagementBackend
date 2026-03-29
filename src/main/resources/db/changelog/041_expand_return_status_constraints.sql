ALTER TABLE sales_return DROP CONSTRAINT IF EXISTS chk_sales_return_status;

ALTER TABLE sales_return
  ADD CONSTRAINT chk_sales_return_status
    CHECK (status IN ('DRAFT','PENDING_INSPECTION','PENDING_APPROVAL','POSTED','REJECTED','CANCELLED'));

ALTER TABLE purchase_return DROP CONSTRAINT IF EXISTS chk_purchase_return_status;

ALTER TABLE purchase_return
  ADD CONSTRAINT chk_purchase_return_status
    CHECK (status IN ('DRAFT','SUBMITTED','PENDING_APPROVAL','POSTED','REJECTED','CANCELLED'));
