-- liquibase formatted sql

-- changeset sameerkhan:add_missing_notification_columns
ALTER TABLE "notification"
ADD COLUMN IF NOT EXISTS "notification_id" VARCHAR(255),
ADD COLUMN IF NOT EXISTS "distributor_id" BIGINT,
ADD COLUMN IF NOT EXISTS "type" VARCHAR(50),
ADD COLUMN IF NOT EXISTS "priority" VARCHAR(50),
ADD COLUMN IF NOT EXISTS "title" VARCHAR(255),
ADD COLUMN IF NOT EXISTS "content" VARCHAR(2000),
ADD COLUMN IF NOT EXISTS "recipient" VARCHAR(255),
ADD COLUMN IF NOT EXISTS "sender" VARCHAR(255),
ADD COLUMN IF NOT EXISTS "retry_count" INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS "error_message" TEXT;

-- If notification_id should be unique and non-null as per your entity:
UPDATE "notification" SET "notification_id" = id::text WHERE "notification_id" IS NULL;
ALTER TABLE "notification" ALTER COLUMN "notification_id" SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_notification_id') THEN
        ALTER TABLE "notification" ADD CONSTRAINT "uq_notification_id" UNIQUE ("notification_id");
    END IF;
END $$;
