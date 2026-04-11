CREATE TABLE hsn_master (
  id                  BIGSERIAL PRIMARY KEY,
  hsn_code            VARCHAR(20) NOT NULL,
  description         VARCHAR(500) NOT NULL,
  chapter_code        VARCHAR(10),
  cgst_rate           NUMERIC(10,2) NOT NULL DEFAULT 0,
  sgst_rate           NUMERIC(10,2) NOT NULL DEFAULT 0,
  igst_rate           NUMERIC(10,2) NOT NULL DEFAULT 0,
  cess_rate           NUMERIC(10,2) NOT NULL DEFAULT 0,
  is_active           BOOLEAN NOT NULL DEFAULT TRUE,
  source_name         VARCHAR(120),
  effective_from      DATE,
  created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by          BIGINT,
  updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by          BIGINT,
  CONSTRAINT uq_hsn_master_code UNIQUE (hsn_code)
);

CREATE INDEX idx_hsn_master_description ON hsn_master(description);

INSERT INTO hsn_master (
  hsn_code, description, chapter_code, cgst_rate, sgst_rate, igst_rate, cess_rate, is_active, source_name, effective_from
) VALUES
  ('39173100', 'Flexible tubes, pipes and hoses, of plastics', '39', 9.00, 9.00, 18.00, 0.00, TRUE, 'Curated phase-1 reference seed', DATE '2025-01-01'),
  ('73269099', 'Other articles of iron or steel', '73', 9.00, 9.00, 18.00, 0.00, TRUE, 'Curated phase-1 reference seed', DATE '2025-01-01'),
  ('84145190', 'Fans: table, floor, wall, window, ceiling or roof fans, other', '84', 6.00, 6.00, 12.00, 0.00, TRUE, 'Curated phase-1 reference seed', DATE '2025-01-01'),
  ('85043190', 'Electrical transformers, other', '85', 9.00, 9.00, 18.00, 0.00, TRUE, 'Curated phase-1 reference seed', DATE '2025-01-01'),
  ('85362000', 'Automatic circuit breakers', '85', 9.00, 9.00, 18.00, 0.00, TRUE, 'Curated phase-1 reference seed', DATE '2025-01-01'),
  ('85365090', 'Electrical switches, other', '85', 9.00, 9.00, 18.00, 0.00, TRUE, 'Curated phase-1 reference seed', DATE '2025-01-01'),
  ('85366990', 'Plugs and sockets, other', '85', 9.00, 9.00, 18.00, 0.00, TRUE, 'Curated phase-1 reference seed', DATE '2025-01-01'),
  ('85371000', 'Boards, panels, consoles, cabinets and other bases for electric control or distribution', '85', 9.00, 9.00, 18.00, 0.00, TRUE, 'Curated phase-1 reference seed', DATE '2025-01-01'),
  ('85444299', 'Insulated electric conductors fitted with connectors, other', '85', 9.00, 9.00, 18.00, 0.00, TRUE, 'Curated phase-1 reference seed', DATE '2025-01-01'),
  ('85444999', 'Other insulated electric conductors, other', '85', 9.00, 9.00, 18.00, 0.00, TRUE, 'Curated phase-1 reference seed', DATE '2025-01-01'),
  ('94051090', 'Chandeliers and other electric ceiling or wall lighting fittings, other', '94', 6.00, 6.00, 12.00, 0.00, TRUE, 'Curated phase-1 reference seed', DATE '2025-01-01'),
  ('94054090', 'Other electric lamps and lighting fittings', '94', 6.00, 6.00, 12.00, 0.00, TRUE, 'Curated phase-1 reference seed', DATE '2025-01-01');

UPDATE product
SET hsn_code = '94054090'
WHERE lower(name) LIKE '%flood light%'
  AND (hsn_code IS NULL OR trim(hsn_code) = '');
