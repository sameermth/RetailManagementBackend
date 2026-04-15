ALTER TABLE "tax_compliance_document"
    ADD COLUMN IF NOT EXISTS "submitted_at" TIMESTAMP WITH TIME ZONE;

ALTER TABLE "tax_compliance_document"
    ADD COLUMN IF NOT EXISTS "last_synced_at" TIMESTAMP WITH TIME ZONE;
