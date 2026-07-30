# Database Schema Overview

## Purpose and authority

This is the compact database context for repository agents. Read it before changing migrations, MyBatis mappers, persisted domain behavior, or transaction boundaries. It is not a column catalog and does not replace the DDL.

| Item                   | Current baseline                                 |
| ---------------------- | ------------------------------------------------ |
| Status                 | Current                                          |
| Last verified          | 2026-07-30                                       |
| Schema source of truth | `backend/src/main/resources/db/migration/V*.sql` |
| Migration head         | `202607301152`                                   |
| Versioned migrations   | 5                                                |
| Domain tables          | 23, excluding Flyway's `flyway_schema_history`   |
| Runtime                | MySQL 8.4.10, InnoDB                             |

When this summary and executable configuration disagree, inspect the migrations, `compose.yaml`, `DatabaseConfig.java`, and `backend/build.gradle`, then update this document in the same change.

## Runtime semantics

- Every domain table uses InnoDB, `utf8mb4`, and `utf8mb4_0900_ai_ci`.
- Domain primary keys are single-column `BIGINT UNSIGNED AUTO_INCREMENT` IDs.
- Declared foreign keys use `ON DELETE RESTRICT ON UPDATE RESTRICT`; deletion does not cascade.
- Money is stored as unsigned integer KRW with no fractional unit.
- Most timestamps use `DATETIME(6)`. Compose and JDBC currently interpret them in `Asia/Seoul` / `+09:00`; `DATETIME` itself has no timezone.
- Document issue and expiry values use `DATE`.
- Identity-like tokens, idempotency keys, bank identifiers, and storage keys use exact binary or `ascii_bin` comparison where declared. Hashes and checksums use `BINARY(32)`.

## Migration history

| Version        | File                                                      | Main effect                                                                                          |
| -------------- | --------------------------------------------------------- | ---------------------------------------------------------------------------------------------------- |
| `202607200001` | `V202607200001__create_gig_hub_baseline.sql`              | Baseline identity, work, wallet, banking, escrow, attendance, dispute, and document schema           |
| `202607211440` | `V202607211440__add_signup_and_workplace_schema.sql`      | Signup identity fields, user withdrawal lifecycle, workplaces, and work-case ownership linkage       |
| `202607221300` | `V202607221300__support_contract_escrow_test_flow.sql`    | Contract/escrow fixture support and final withdrawal naming/relationship adjustments                 |
| `202607301027` | `V202607301027__remove_invited_from_work_case_status.sql` | Map legacy `INVITED` work cases to `DRAFT` and remove `INVITED` from the work-case status constraint |
| `202607301152` | `V202607301152__split_workplace_address.sql`              | Preserve legacy workplace addresses as road addresses and add an optional nonblank detail address    |

Applied or shared versioned migrations are immutable. Add a newer `V*.sql` file for every schema correction.

## Domain inventory

| Domain                                   | Tables                                                                                                                                              |
| ---------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------- |
| Identity and organization                | `users`, `employer_profiles`, `workplaces`, `user_badges`                                                                                           |
| Work and contract                        | `work_cases`, `work_invitations`, `work_contracts`                                                                                                  |
| Wallet, mock banking, escrow, settlement | `wallets`, `mock_bank_accounts`, `mock_bank_transactions`, `funding_orders`, `withdrawal_requests`, `wallet_transactions`, `escrows`, `settlements` |
| Attendance and dispute                   | `qr_tokens`, `attendance_records`, `disputes`                                                                                                       |
| Documents and signatures                 | `documents`, `document_versions`, `document_signatures`, `document_shares`, `document_access_logs`                                                  |

Inspect the ordered migrations before relying on an exact column, key, index, generated expression, or allowed status value.

## Core relationships and invariants

### Identity and organization

- `users.email` and `users.login_id` are unique. Roles are `OWNER`, `WORKER`, and `ADMIN`.
- User status and `deleted_at` are coupled: `WITHDRAWN` requires a deletion timestamp; other statuses require it to be null.
- `employer_profiles.user_id` is unique, but the database does not prove that the referenced user has the `OWNER` role.
- A user can own multiple `workplaces`. Business registration numbers are unique, `workplaces.road_address` is required, `workplaces.detail_address` is null or nonblank, coordinates must be both null or both non-null and in range, radius is positive, and `DELETED` status must agree with `deleted_at`.
- The composite relationship from `work_cases` to `(workplaces.owner_user_id, workplaces.id)` proves that a selected workplace belongs to the recorded employer.

### Work and contract

- A `work_case` has one employer, an optional worker until assignment, one workplace, positive agreed wage, valid start/end times, and a lifecycle status constrained to `DRAFT`, `ACCEPTED`, `READY`, `IN_PROGRESS`, `COMPLETED`, `NO_SHOW`, or `CANCELED`.
- Invitation delivery and response states belong to `work_invitations`; an unaccepted work case remains `DRAFT`.
- `ACCEPTED`, `READY`, `IN_PROGRESS`, `COMPLETED`, and `NO_SHOW` work-case states require a worker.
- Generated active-slot uniqueness permits at most one pending `work_invitation` per work case while preserving terminal invitation history.
- `work_contracts.work_case_id` is unique. A composite foreign key proves that contract parties and agreed wage match the work case.
- The database validates allowed status values and selected state-dependent null rules, not the complete transition graph.

### Wallet, banking, escrow, and settlement

- A user has at most one KRW `wallet`; balances are unsigned.
- `mock_bank_accounts.available_amount` cannot exceed `balance`. Bank/account and fintech identifiers are unique.
- Funding orders, withdrawal requests, and wallet transactions use globally unique idempotency keys in their respective tables.
- `wallet_transactions` records before/after snapshots, but the database does not validate ledger arithmetic or the polymorphic reference target.
- `escrows.work_case_id` and `settlements.work_case_id` are each unique. Composite foreign keys require their amounts to equal the work case's agreed wage.
- There is no direct foreign key between a settlement and an escrow.
- Separate funding and withdrawal foreign keys do not align the actor, linked mock account, and mock bank transaction. Withdrawal foreign keys also do not prove that the request user and wallet have the same owner.

### Attendance and dispute

- Generated success-slot uniqueness permits one successful check-in and one successful check-out per work case while retaining rejected attempts.
- Separate attendance foreign keys do not prove that the recorded worker is the worker assigned to the work case.
- Generated open-slot uniqueness permits at most one `OPEN` or `UNDER_REVIEW` dispute per work case.

### Documents and signatures

- A work case has at most one document per document type when `work_case_id` is non-null. MySQL nullable-unique behavior permits multiple null work-case values.
- Document version numbers are unique within a document; storage keys are globally unique.
- Composite signature foreign keys prove that the source and signed versions belong to the stated document, and the two versions must differ.
- Generated active-slot uniqueness permits one equivalent active document share while retaining expired or revoked history.

## Application-enforced responsibilities

Database constraints do not replace application authorization or transaction rules. Services must still enforce:

- actor roles and ownership where no composite relationship proves them;
- complete state-transition graphs;
- ownership alignment across funding or withdrawal actors, wallets, linked accounts, and bank transactions;
- attendance worker assignment;
- wallet ledger arithmetic and balance conservation;
- idempotent replay behavior under concurrency;
- lock ordering, rollback behavior, and external mock-bank coordination.

Do not infer an application guarantee merely because related columns each have foreign keys.

## MyBatis and transaction configuration

- The application uses non-Boot Java configuration in `DatabaseConfig`.
- Mapper scanning currently covers `com.gighub.wallet.mapper` and `com.gighub.work.mapper`.
- Mapper XML files are loaded from `classpath*:mappers/**/*.xml`.
- `mapUnderscoreToCamelCase` is enabled.
- SQL belongs in MyBatis mapper XML; Java interfaces declare parameters explicitly.
- Transactions use Spring `DataSourceTransactionManager` with `@EnableTransactionManagement`.
- Spring `@PropertySource` reads the external file selected by JVM property `gighub.database.config`; `DatabaseConfig` applies those values to a `HikariDataSource`.
- The application does not run Flyway at startup. Schema evolution belongs to the Flyway container workflow in [`../runbooks/DATABASE_RUNBOOK.md`](../runbooks/DATABASE_RUNBOOK.md).
- The opt-in `databaseTest` verifies configured connectivity and the `users` table only; it is not a full-schema constraint test.

## Required update triggers

Update this file in the same PR when any of the following changes:

1. A `V*.sql` migration is added.
2. A table, column, primary key, foreign key, unique key, check constraint, generated column, or important index changes.
3. A status/type allowlist or state-dependent null rule changes.
4. Money representation, currency, time precision, collation, or timezone changes.
5. `DatabaseConfig` changes mapper packages, mapper XML locations, naming conversion, DataSource, or transaction management.
6. Wallet, escrow, settlement, or withdrawal logic changes ownership, locking, idempotency, or transaction invariants.
7. Compose changes MySQL/Flyway versions, migration mounts, collation, timezone, volume, or clean policy.

For each update:

1. Read all migrations in version order.
2. Record the new migration head and affected domain summary.
3. Verify `flyway migrate`, `validate`, and `info` using the DB Runbook.
4. Run the narrowest mapper, service, transaction, and database tests that cover the change.
5. Keep detailed SQL in migrations rather than duplicating a full schema dump here.
