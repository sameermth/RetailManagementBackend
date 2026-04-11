ALTER TABLE serial_number DROP CONSTRAINT IF EXISTS chk_serial_status;

ALTER TABLE serial_number
  ADD CONSTRAINT chk_serial_status CHECK (
    status IN (
      'IN_STOCK',
      'RESERVED',
      'ALLOCATED',
      'SOLD',
      'RETURNED',
      'REPLACED',
      'SCRAPPED',
      'SERVICE_IN',
      'SERVICE_OUT'
    )
  );
