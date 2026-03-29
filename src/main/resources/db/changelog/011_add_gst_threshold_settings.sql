ALTER TABLE organization
  ADD COLUMN gst_threshold_amount NUMERIC(18,2) NOT NULL DEFAULT 4000000,
  ADD COLUMN gst_threshold_alert_enabled BOOLEAN NOT NULL DEFAULT TRUE;
