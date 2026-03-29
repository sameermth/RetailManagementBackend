# ERP final consolidation notes

This pass is based on the cleaned backend baseline with the ERP bridge modules already applied.

## What this pass changes
- normalizes `application.properties` for the cleaned DB setup
- keeps PostgreSQL schema explicitly set to `retail_management_service`
- adds `application-local.properties` for easier local runs
- adds a smoke-test checklist for validating sample data and bridge APIs quickly

## Important runtime assumptions
- Liquibase is the source of truth for schema creation
- JPA runs with `ddl-auto=validate`
- the ERP bridge coexists with legacy modules
- the ERP APIs are the preferred path for validating the new schema

## Suggested next engineering step
After this pass, the best next work is not more schema changes.
It is a compile/test hardening pass in your local IDE/CI:
- fix any import or repository drift introduced during incremental bridge patches
- add integration tests for ERP inventory, sales, purchase, service, approval, and finance APIs
- then start migrating legacy modules one by one onto ERP entities
