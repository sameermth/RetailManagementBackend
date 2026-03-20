-- =====================================================
-- MIGRATION SCRIPT: Add Barcode/QR Tracking to Inventory
-- =====================================================
-- This script preserves all existing data while adding
-- new tracking capabilities
-- =====================================================

BEGIN;

-- =====================================================
-- STEP 1: Add tracking fields to products
-- =====================================================
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS tracking_method VARCHAR(20) DEFAULT 'NONE'
    CHECK (tracking_method IN ('NONE', 'BATCH', 'SERIAL')),
    ADD COLUMN IF NOT EXISTS warranty_period_days INTEGER,
    ADD COLUMN IF NOT EXISTS default_bin_location VARCHAR(100),
    ADD COLUMN IF NOT EXISTS default_shelf_number VARCHAR(50);

-- Update existing products to have default tracking method
UPDATE products SET tracking_method = 'NONE' WHERE tracking_method IS NULL;

-- =====================================================
-- STEP 2: Create batches table
-- =====================================================
CREATE TABLE IF NOT EXISTS batches (
                                       id BIGSERIAL PRIMARY KEY,
                                       inventory_id BIGINT NOT NULL REFERENCES inventory(id) ON DELETE CASCADE,
    batch_number VARCHAR(100) NOT NULL,
    manufacturing_date DATE,
    expiry_date DATE,
    quantity_remaining INTEGER NOT NULL DEFAULT 0,
    unit_cost DECIMAL(10,2),
    selling_price DECIMAL(10,2),
    bin_location VARCHAR(100),
    shelf_number VARCHAR(50),
    quality_status VARCHAR(50) DEFAULT 'GOOD'
    CHECK (quality_status IN ('GOOD', 'QUARANTINE', 'REJECTED')),
    barcode VARCHAR(100) UNIQUE,
    supplier_id BIGINT REFERENCES suppliers(id),
    received_date DATE NOT NULL DEFAULT CURRENT_DATE,
    purchase_receipt_item_id BIGINT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),

    UNIQUE(inventory_id, batch_number)
    );

-- Create indexes for batches
CREATE INDEX IF NOT EXISTS idx_batches_inventory ON batches(inventory_id);
CREATE INDEX IF NOT EXISTS idx_batches_batch_number ON batches(batch_number);
CREATE INDEX IF NOT EXISTS idx_batches_expiry ON batches(expiry_date);
CREATE INDEX IF NOT EXISTS idx_batches_barcode ON batches(barcode);

-- =====================================================
-- STEP 3: Create inventory_items table
-- =====================================================
CREATE TABLE IF NOT EXISTS inventory_items (
                                               id BIGSERIAL PRIMARY KEY,
                                               inventory_id BIGINT NOT NULL REFERENCES inventory(id) ON DELETE CASCADE,
    batch_id BIGINT REFERENCES batches(id) ON DELETE SET NULL,
    serial_number VARCHAR(100) UNIQUE,
    barcode VARCHAR(100) UNIQUE,
    qr_code TEXT UNIQUE,
    status VARCHAR(50) DEFAULT 'IN_STOCK'
    CHECK (status IN ('IN_STOCK', 'RESERVED', 'SOLD', 'DAMAGED',
           'EXPIRED', 'RETURNED', 'QUARANTINE')),
    condition VARCHAR(50) DEFAULT 'NEW'
    CHECK (condition IN ('NEW', 'EXCELLENT', 'GOOD', 'FAIR', 'POOR', 'DAMAGED')),
    bin_location VARCHAR(100),
    shelf_number VARCHAR(50),
    purchase_price DECIMAL(10,2),
    selling_price DECIMAL(10,2),
    manufacturing_date DATE,
    expiry_date DATE,
    received_date DATE NOT NULL DEFAULT CURRENT_DATE,
    warranty_start_date DATE,
    warranty_end_date DATE,
    purchase_receipt_item_id BIGINT,
    sale_id BIGINT REFERENCES sales(id) ON DELETE SET NULL,
    sale_item_id BIGINT REFERENCES sale_items(id) ON DELETE SET NULL,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
    );

-- Create indexes for inventory_items
CREATE INDEX IF NOT EXISTS idx_items_inventory ON inventory_items(inventory_id);
CREATE INDEX IF NOT EXISTS idx_items_batch ON inventory_items(batch_id);
CREATE INDEX IF NOT EXISTS idx_items_serial ON inventory_items(serial_number);
CREATE INDEX IF NOT EXISTS idx_items_barcode ON inventory_items(barcode);
CREATE INDEX IF NOT EXISTS idx_items_status ON inventory_items(status);
CREATE INDEX IF NOT EXISTS idx_items_expiry ON inventory_items(expiry_date);
CREATE INDEX IF NOT EXISTS idx_items_warranty ON inventory_items(warranty_end_date);
CREATE INDEX IF NOT EXISTS idx_items_sale ON inventory_items(sale_id);

-- =====================================================
-- STEP 4: Create qr_code_data table
-- =====================================================
CREATE TABLE IF NOT EXISTS qr_code_data (
                                            id BIGSERIAL PRIMARY KEY,
                                            qr_code VARCHAR(255) UNIQUE NOT NULL,
    inventory_item_id BIGINT REFERENCES inventory_items(id) ON DELETE CASCADE,
    batch_id BIGINT REFERENCES batches(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id),
    data JSONB NOT NULL,
    data_version VARCHAR(20),
    data_schema VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    scanned_at TIMESTAMP,
    scanned_by VARCHAR(50)
    );

CREATE INDEX IF NOT EXISTS idx_qr_code ON qr_code_data(qr_code);

-- =====================================================
-- STEP 5: Create warranty_claims table
-- =====================================================
CREATE TABLE IF NOT EXISTS warranty_claims (
                                               id BIGSERIAL PRIMARY KEY,
                                               claim_number VARCHAR(50) UNIQUE NOT NULL,
    inventory_item_id BIGINT REFERENCES inventory_items(id),
    batch_id BIGINT REFERENCES batches(id),
    product_id BIGINT NOT NULL REFERENCES products(id),
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    sale_id BIGINT REFERENCES sales(id),
    claim_date DATE NOT NULL DEFAULT CURRENT_DATE,
    issue_description TEXT NOT NULL,
    resolution_description TEXT,
    status VARCHAR(50) DEFAULT 'PENDING'
    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'COMPLETED', 'IN_PROGRESS')),
    approved_by VARCHAR(50),
    approved_date DATE,
    rejection_reason TEXT,
    resolution_notes TEXT,
    resolution_date DATE,
    replacement_product_id BIGINT REFERENCES products(id),
    replacement_inventory_item_id BIGINT REFERENCES inventory_items(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
    );

CREATE INDEX IF NOT EXISTS idx_claims_number ON warranty_claims(claim_number);
CREATE INDEX IF NOT EXISTS idx_claims_status ON warranty_claims(status);
CREATE INDEX IF NOT EXISTS idx_claims_customer ON warranty_claims(customer_id);

-- =====================================================
-- STEP 6: Add tracking fields to purchase_receipt_items
-- =====================================================
ALTER TABLE purchase_receipt_items
    ADD COLUMN IF NOT EXISTS batch_number VARCHAR(100),
    ADD COLUMN IF NOT EXISTS manufacturing_date DATE,
    ADD COLUMN IF NOT EXISTS expiry_date DATE,
    ADD COLUMN IF NOT EXISTS notes TEXT,
    ADD COLUMN IF NOT EXISTS location_details VARCHAR(255);

-- =====================================================
-- STEP 7: Add tracking fields to sale_items
-- =====================================================
ALTER TABLE sale_items
    ADD COLUMN IF NOT EXISTS inventory_item_id BIGINT REFERENCES inventory_items(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS batch_id BIGINT REFERENCES batches(id) ON DELETE SET NULL,
ADD COLUMN IF NOT EXISTS serial_number VARCHAR(100),
ADD COLUMN IF NOT EXISTS warranty_start_date DATE,
ADD COLUMN IF NOT EXISTS warranty_end_date DATE;

CREATE INDEX IF NOT EXISTS idx_sale_items_inventory ON sale_items(inventory_item_id);
CREATE INDEX IF NOT EXISTS idx_sale_items_batch ON sale_items(batch_id);

-- =====================================================
-- STEP 8: Create triggers for automatic inventory updates
-- =====================================================

-- Function to update inventory quantity when inventory_items change
CREATE OR REPLACE FUNCTION update_inventory_quantity()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' AND NEW.status = 'IN_STOCK' THEN
UPDATE inventory
SET quantity = quantity + 1,
    available_quantity = available_quantity + 1,
    updated_at = CURRENT_TIMESTAMP
WHERE id = NEW.inventory_id;

ELSIF TG_OP = 'UPDATE' THEN
        IF OLD.status != 'SOLD' AND NEW.status = 'SOLD' THEN
UPDATE inventory
SET quantity = quantity - 1,
    available_quantity = available_quantity - 1,
    updated_at = CURRENT_TIMESTAMP
WHERE id = NEW.inventory_id;

-- Also update batch if applicable
IF NEW.batch_id IS NOT NULL THEN
UPDATE batches
SET quantity_remaining = quantity_remaining - 1,
    updated_at = CURRENT_TIMESTAMP
WHERE id = NEW.batch_id;
END IF;

        ELSIF OLD.status = 'SOLD' AND NEW.status != 'SOLD' THEN
UPDATE inventory
SET quantity = quantity + 1,
    available_quantity = available_quantity + 1,
    updated_at = CURRENT_TIMESTAMP
WHERE id = NEW.inventory_id;

IF NEW.batch_id IS NOT NULL THEN
UPDATE batches
SET quantity_remaining = quantity_remaining + 1,
    updated_at = CURRENT_TIMESTAMP
WHERE id = NEW.batch_id;
END IF;
END IF;
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger
DROP TRIGGER IF EXISTS trigger_update_inventory ON inventory_items;
CREATE TRIGGER trigger_update_inventory
    AFTER INSERT OR UPDATE OF status ON inventory_items
    FOR EACH ROW
    EXECUTE FUNCTION update_inventory_quantity();

-- Function to update inventory when batch is created
CREATE OR REPLACE FUNCTION update_inventory_from_batch()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
UPDATE inventory
SET quantity = quantity + NEW.quantity_remaining,
    available_quantity = available_quantity + NEW.quantity_remaining,
    updated_at = CURRENT_TIMESTAMP
WHERE id = NEW.inventory_id;
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger
DROP TRIGGER IF EXISTS trigger_batch_insert ON batches;
CREATE TRIGGER trigger_batch_insert
    AFTER INSERT ON batches
    FOR EACH ROW
    EXECUTE FUNCTION update_inventory_from_batch();

-- =====================================================
-- STEP 9: Create views for easy querying
-- =====================================================

-- View for available stock by product
CREATE OR REPLACE VIEW v_available_stock AS
SELECT
    p.id as product_id,
    p.sku,
    p.name,
    p.tracking_method,
    i.warehouse_id,
    i.quantity as total_stock,
    i.available_quantity,
    i.minimum_stock,
    i.reorder_point,

    -- Serial items available
    (SELECT COUNT(*) FROM inventory_items ii
     WHERE ii.inventory_id = i.id AND ii.status = 'IN_STOCK') as serial_items_available,

    -- Batch summary
    (SELECT json_agg(
                    json_build_object(
                            'batch_id', b.id,
                            'batch_number', b.batch_number,
                            'quantity', b.quantity_remaining,
                            'expiry', b.expiry_date
                    )
            ) FROM batches b
     WHERE b.inventory_id = i.id AND b.quantity_remaining > 0) as batches

FROM inventory i
         JOIN products p ON i.product_id = p.id;

-- View for warranty tracking
CREATE OR REPLACE VIEW v_warranty_tracking AS
SELECT
    ii.id as item_id,
    ii.serial_number,
    ii.barcode,
    p.name as product_name,
    p.sku,
    c.name as customer_name,
    s.invoice_number,
    s.sale_date,
    ii.warranty_start_date,
    ii.warranty_end_date,
    CASE
        WHEN ii.warranty_end_date < CURRENT_DATE THEN 'EXPIRED'
        WHEN ii.warranty_end_date < CURRENT_DATE + 30 THEN 'EXPIRING_SOON'
        ELSE 'ACTIVE'
        END as warranty_status,
    DATEDIFF(ii.warranty_end_date, CURRENT_DATE) as days_remaining
FROM inventory_items ii
         JOIN products p ON ii.inventory_id IN (
    SELECT id FROM inventory WHERE product_id = p.id
)
         LEFT JOIN sales s ON ii.sale_id = s.id
         LEFT JOIN customers c ON s.customer_id = c.id
WHERE ii.status = 'SOLD' AND ii.warranty_end_date IS NOT NULL;

COMMIT;

-- =====================================================
-- VERIFICATION QUERIES
-- =====================================================

-- Check that new columns were added
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_name = 'products'
  AND column_name IN ('tracking_method', 'warranty_period_days');

-- Check that new tables were created
SELECT table_name
FROM information_schema.tables
WHERE table_name IN ('batches', 'inventory_items', 'warranty_claims', 'qr_code_data');

-- Count records in new tables (should be 0 initially)
SELECT 'batches' as table_name, COUNT(*) FROM batches
UNION ALL
SELECT 'inventory_items', COUNT(*) FROM inventory_items
UNION ALL
SELECT 'warranty_claims', COUNT(*) FROM warranty_claims
UNION ALL
SELECT 'qr_code_data', COUNT(*) FROM qr_code_data;