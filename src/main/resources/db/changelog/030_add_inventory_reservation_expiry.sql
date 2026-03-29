ALTER TABLE inventory_reservation
    ADD COLUMN IF NOT EXISTS expires_at timestamp,
    ADD COLUMN IF NOT EXISTS released_at timestamp,
    ADD COLUMN IF NOT EXISTS release_reason varchar(100);

UPDATE inventory_reservation
SET expires_at = COALESCE(expires_at, created_at + interval '2 hours')
WHERE status = 'ACTIVE';

UPDATE inventory_reservation
SET released_at = COALESCE(released_at, updated_at, created_at),
    release_reason = COALESCE(release_reason, CASE
        WHEN status = 'CONSUMED' THEN 'CONSUMED'
        WHEN status = 'RELEASED' THEN 'LEGACY_RELEASE'
        ELSE release_reason
    END)
WHERE status IN ('CONSUMED', 'RELEASED');

CREATE INDEX IF NOT EXISTS idx_inventory_reservation_status_expires_at
    ON inventory_reservation(status, expires_at);
