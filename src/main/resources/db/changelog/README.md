# Database Changelog Guide

This project keeps two Liquibase paths:

- `db.changelog-master.yml`: active baseline path (bootstrap + post-bootstrap).
- `legacy/db.changelog-legacy.yml`: archived incremental migration history for reference/rollback support.
- `db.changelog-bootstrap.yml`: consolidated baseline changesets.

## Contexts

- `seed`: baseline master/reference data.
- `demo`: optional sample/demo data.

## Environment defaults

- Local profile (`application-local.properties`): `seed,demo`
- Prod profile (`application-prod.properties`): `seed`

This ensures upper environments do not run demo/sample seed data by default.

## Notes

- Legacy incremental files are archived under `db/changelog/legacy`.
- Do not delete legacy numbered SQL files or `legacy/db.changelog-legacy.yml`; they are retained for rollback/audit/reference.
- Use bootstrap as the default path for new environments.

## Generate Consolidated SQL From Live DB (Liquibase)

Use Gradle + Liquibase plugin with a dedicated flag:

```bash
./gradlew -PwithLiquibase \
  -PliquibaseUrl="jdbc:postgresql://localhost:5432/retail_management_db?currentSchema=retail_management_service" \
  -PliquibaseUsername=retail_admin \
  -PliquibasePassword=admin \
  -PliquibaseRunList=exportSchema \
  -PliquibaseChangelogFile=src/main/resources/db/changelog/bootstrap/_tmp_schema_export.sql \
  generateChangelog
```

```bash
./gradlew -PwithLiquibase \
  -PliquibaseUrl="jdbc:postgresql://localhost:5432/retail_management_db?currentSchema=retail_management_service" \
  -PliquibaseUsername=retail_admin \
  -PliquibasePassword=admin \
  -PliquibaseRunList=exportMasterData \
  -PliquibaseChangelogFile=src/main/resources/db/changelog/bootstrap/_tmp_master_data_export.sql \
  generateChangelog
```

```bash
./gradlew -PwithLiquibase \
  -PliquibaseUrl="jdbc:postgresql://localhost:5432/retail_management_db?currentSchema=retail_management_service" \
  -PliquibaseUsername=retail_admin \
  -PliquibasePassword=admin \
  -PliquibaseRunList=exportDemoData \
  -PliquibaseChangelogFile=src/main/resources/db/changelog/bootstrap/_tmp_demo_data_export.sql \
  generateChangelog
```

After generation, review files before replacing canonical bootstrap files.
