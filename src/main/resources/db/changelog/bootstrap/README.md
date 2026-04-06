This directory contains a consolidated bootstrap path for fresh databases:

- `001_bootstrap_schema.sql`
- `002_bootstrap_master_data.sql`
- `003_bootstrap_demo_data.sql`
- `../db.changelog-bootstrap.yml`

Important notes:

- Use this bootstrap changelog only for a brand new database.
- The legacy numbered migration files are still the source of truth for already-migrated databases.
- Do not delete the legacy files until every environment has either been rebuilt from bootstrap or migrated to a new consolidated history strategy.
