This directory contains a consolidated bootstrap path for fresh databases:

- `001_bootstrap_schema.sql`
- `002_bootstrap_master_data.sql`
- `003_bootstrap_demo_data.sql`
- `../db.changelog-bootstrap.yml`

Liquibase context usage:

- `seed` for `002_bootstrap_master_data.sql`
- `demo` for `003_bootstrap_demo_data.sql`

Important notes:

- This bootstrap path is the active default via `db.changelog-master.yml`.
- It is safe for fresh environments and guarded with preconditions for already-initialized environments.
- The files are generated from the current DB state via Liquibase and then sanitized for bootstrap safety.
- `001_bootstrap_schema.sql` is schema-only and identity starts are normalized.
- `002_bootstrap_master_data.sql` contains reference/master data only.
- `003_bootstrap_demo_data.sql` contains demo/sample data only and must not be enabled in upper environments.
- Legacy incremental files are archived under `../legacy/`.
