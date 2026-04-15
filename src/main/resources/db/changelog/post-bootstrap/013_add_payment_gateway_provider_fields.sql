ALTER TABLE "sales_invoice_payment_request"
  ADD COLUMN IF NOT EXISTS "provider_reference" VARCHAR(120),
  ADD COLUMN IF NOT EXISTS "provider_status" VARCHAR(40),
  ADD COLUMN IF NOT EXISTS "provider_payload_json" TEXT,
  ADD COLUMN IF NOT EXISTS "provider_created_at" TIMESTAMP WITH TIME ZONE,
  ADD COLUMN IF NOT EXISTS "provider_last_synced_at" TIMESTAMP WITH TIME ZONE;

CREATE INDEX IF NOT EXISTS "idx_sales_invoice_payment_request_provider_ref"
  ON "sales_invoice_payment_request" ("provider_code", "provider_reference");
