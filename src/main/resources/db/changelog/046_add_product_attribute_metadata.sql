CREATE TABLE product_attribute_definition (
  id                  BIGSERIAL PRIMARY KEY,
  organization_id     BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  code                VARCHAR(80) NOT NULL,
  label               VARCHAR(150) NOT NULL,
  description         VARCHAR(500),
  data_type           VARCHAR(30) NOT NULL,
  input_type          VARCHAR(30) NOT NULL,
  is_required         BOOLEAN NOT NULL DEFAULT FALSE,
  is_active           BOOLEAN NOT NULL DEFAULT TRUE,
  unit_label          VARCHAR(50),
  placeholder         VARCHAR(150),
  help_text           VARCHAR(300),
  sort_order          INTEGER NOT NULL DEFAULT 1,
  created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by          BIGINT,
  updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by          BIGINT,
  CONSTRAINT uq_product_attribute_definition_org_code UNIQUE (organization_id, code)
);

CREATE TABLE product_attribute_option (
  id                  BIGSERIAL PRIMARY KEY,
  attribute_definition_id BIGINT NOT NULL REFERENCES product_attribute_definition(id) ON DELETE CASCADE,
  code                VARCHAR(80) NOT NULL,
  label               VARCHAR(150) NOT NULL,
  sort_order          INTEGER NOT NULL DEFAULT 1,
  is_active           BOOLEAN NOT NULL DEFAULT TRUE,
  created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by          BIGINT,
  updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by          BIGINT,
  CONSTRAINT uq_product_attribute_option UNIQUE (attribute_definition_id, code)
);

CREATE TABLE product_attribute_scope (
  id                  BIGSERIAL PRIMARY KEY,
  organization_id     BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  attribute_definition_id BIGINT NOT NULL REFERENCES product_attribute_definition(id) ON DELETE CASCADE,
  category_id         BIGINT REFERENCES category(id) ON DELETE CASCADE,
  brand_id            BIGINT REFERENCES brand(id) ON DELETE CASCADE,
  created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by          BIGINT,
  updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by          BIGINT
);

CREATE INDEX idx_product_attribute_definition_org_active ON product_attribute_definition(organization_id, is_active, sort_order);
CREATE INDEX idx_product_attribute_option_def_sort ON product_attribute_option(attribute_definition_id, sort_order);
CREATE INDEX idx_product_attribute_scope_lookup ON product_attribute_scope(organization_id, category_id, brand_id);

WITH owner_ctx AS (
  SELECT o.id AS organization_id, u.id AS owner_user_id
  FROM organization o
  JOIN app_user u ON u.organization_id = o.id
  WHERE u.employee_code IN ('SPC-OWN-01','UHL-OWN-01')
),
defs AS (
  INSERT INTO product_attribute_definition (
    organization_id, code, label, description, data_type, input_type, is_required, is_active,
    unit_label, placeholder, help_text, sort_order, created_by, updated_by
  )
  SELECT owner_ctx.organization_id, src.code, src.label, src.description, src.data_type, src.input_type, src.is_required, TRUE,
         src.unit_label, src.placeholder, src.help_text, src.sort_order, owner_ctx.owner_user_id, owner_ctx.owner_user_id
  FROM owner_ctx
  JOIN (
    VALUES
      ('battery_type', 'Battery Type', 'Battery fitment / maintenance type', 'OPTION', 'SELECT', TRUE, NULL, 'Select battery type', 'Used to distinguish RMF / LMF / tubular types', 10),
      ('capacity_ah', 'Capacity (Ah)', 'Battery capacity in ampere-hour', 'NUMBER', 'NUMBER', FALSE, 'Ah', 'Enter capacity', 'Useful for battery sizing and search', 20),
      ('voltage', 'Voltage', 'Nominal voltage of the battery', 'NUMBER', 'NUMBER', FALSE, 'V', 'Enter voltage', 'Common battery voltage rating', 30),
      ('viscosity_grade', 'Viscosity Grade', 'Lubricant viscosity grade', 'OPTION', 'SELECT', TRUE, NULL, 'Select viscosity', 'Examples: 20W40, 10W30', 10),
      ('pack_size', 'Pack Size', 'Retail pack size', 'OPTION', 'SELECT', TRUE, NULL, 'Select pack size', 'Examples: 500ML, 1L, 4L', 20),
      ('oil_type', 'Oil Type', 'Type of lubricant', 'OPTION', 'SELECT', FALSE, NULL, 'Select oil type', 'Engine oil, gear oil, coolant, etc.', 30)
  ) AS src(code, label, description, data_type, input_type, is_required, unit_label, placeholder, help_text, sort_order) ON TRUE
  ON CONFLICT (organization_id, code) DO NOTHING
  RETURNING id, organization_id, code, created_by
),
all_defs AS (
  SELECT d.id, d.organization_id, d.code, d.created_by
  FROM defs d
  UNION
  SELECT pad.id, pad.organization_id, pad.code, pad.created_by
  FROM product_attribute_definition pad
  WHERE pad.code IN ('battery_type','capacity_ah','voltage','viscosity_grade','pack_size','oil_type')
),
scopes AS (
  INSERT INTO product_attribute_scope (
    organization_id, attribute_definition_id, category_id, brand_id, created_by, updated_by
  )
  SELECT ad.organization_id, ad.id, c.id, NULL, ad.created_by, ad.created_by
  FROM all_defs ad
  JOIN category c ON c.organization_id = ad.organization_id
  WHERE (
      ad.code IN ('battery_type','capacity_ah','voltage') AND c.code = 'BATTERY'
    ) OR (
      ad.code IN ('viscosity_grade','pack_size','oil_type') AND c.code = 'LUBE'
    )
  AND NOT EXISTS (
    SELECT 1
    FROM product_attribute_scope existing
    WHERE existing.organization_id = ad.organization_id
      AND existing.attribute_definition_id = ad.id
      AND existing.category_id = c.id
      AND existing.brand_id IS NULL
  )
  RETURNING id
)
INSERT INTO product_attribute_option (
  attribute_definition_id, code, label, sort_order, is_active, created_by, updated_by
)
SELECT ad.id, opt.code, opt.label, opt.sort_order, TRUE, ad.created_by, ad.created_by
FROM all_defs ad
JOIN (
  VALUES
    ('battery_type', 'RMF', 'RMF', 10),
    ('battery_type', 'LMF', 'LMF', 20),
    ('battery_type', 'TUBULAR', 'Tubular', 30),
    ('battery_type', 'SMF', 'SMF', 40),
    ('viscosity_grade', '10W30', '10W30', 10),
    ('viscosity_grade', '15W40', '15W40', 20),
    ('viscosity_grade', '20W40', '20W40', 30),
    ('viscosity_grade', '5W30', '5W30', 40),
    ('pack_size', '500ML', '500 ML', 10),
    ('pack_size', '900ML', '900 ML', 20),
    ('pack_size', '1L', '1 L', 30),
    ('pack_size', '4L', '4 L', 40),
    ('oil_type', 'ENGINE_OIL', 'Engine Oil', 10),
    ('oil_type', 'GEAR_OIL', 'Gear Oil', 20),
    ('oil_type', 'COOLANT', 'Coolant', 30),
    ('oil_type', 'BRAKE_FLUID', 'Brake Fluid', 40)
) AS opt(attribute_code, code, label, sort_order)
  ON opt.attribute_code = ad.code
WHERE NOT EXISTS (
  SELECT 1
  FROM product_attribute_option existing
  WHERE existing.attribute_definition_id = ad.id
    AND existing.code = opt.code
);
