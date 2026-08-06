# Shared Repository Agent Contract

## Context loading

1. Read this contract completely for the first repository task in a new agent conversation, after a context reset or agent handoff, after switching branches, or when this file changes.
2. Reuse the unchanged contract within the same conversation instead of reopening it on every turn.
3. Read `docs/agent/ARCHITECTURE_OVERVIEW.md` once in each agent conversation before material implementation for an issue or branch. Read it again after a context reset or agent handoff, when it changes, or when the work crosses an architecture boundary.
4. Use `docs/README.md` only when the relevant task-specific document is not already known or when the task moves to a different area.
5. Load detailed domain, runbook, schema, API, and testing documents only when the current change touches that subject. Do not preload the documentation tree.
6. Read `docs/DEPENDENCY_SPECIFICATION.md` before changing a language, runtime, build tool, container image, or direct dependency. Do not load it for unrelated work.
7. For current-state facts, treat executable code, configuration, owner-controlled migrations, focused tests, and runtime Swagger as authoritative. Protected product specifications remain normative until an authorized human publishes a new administrative spec release.
8. General questions, status checks, and narrow read-only inspection do not require the architecture overview or unrelated task documents.

## Task startup

1. Inspect `git status` and the files relevant to the request before editing.
2. Preserve unrelated user changes.
3. Treat Vue.js, Spring Framework 5 non-Boot, MyBatis, MySQL, Java 17, and Tomcat 9 as fixed constraints.
4. Treat JavaScript as the default frontend language. TypeScript is conditional, not prohibited, and requires the decision process in `docs/DEPENDENCY_SPECIFICATION.md`.
5. Define the smallest reviewable task boundary and its verification before editing.

## Implementation rules

- Keep the monorepo split into `frontend/` and `backend/`; shared automation belongs at the repository root.
- Never add React, Spring Boot, JPA, embedded secrets, API keys, personal data, or environment-specific credentials.
- Use Spring MVC layering (`controller -> service -> mapper`) and explicit DTOs.
- Keep SQL in MyBatis mapper XML unless a tracked project document explicitly changes that convention.
- Update the executable manifest or build configuration, its lock or coupled configuration, and `docs/DEPENDENCY_SPECIFICATION.md` in the same pull request when adding, removing, replacing, or repurposing a direct dependency.
- Make scoped changes and follow the existing lint and formatting rules.
- Ask for direction only when a missing decision would materially change behavior, data, security, or architecture.
- Do not alter another contributor's unrelated work.

## Documentation ownership and maintenance

1. Treat every file under `docs/specs/` as a protected product contract. Ordinary implementation agents may read these files but must not create, modify, delete, rename, move, format, regenerate, stage, commit, restore, or revert them.
2. A protected-spec exception exists only when a human Product Manager or Repository Administrator explicitly assigns the current personal agent an administrative spec-release task. The request must identify the approved decision and the exact file or bounded subject area. The exception applies only to that administrative release, includes the corresponding `docs/specs/SPEC_LOCK.json` refresh, and does not carry into later implementation work.
3. Treat Flyway migrations under `backend/src/main/resources/db/migration/`, DDL artifacts under `docs/database/`, and schema-level DDL elsewhere as human Product Manager or Repository Administrator controlled. The current personal agent may change them only under an explicit, scoped administrative request that identifies the migration or DDL release. A feature request that merely implies a schema change is not such authorization.
4. Outside a scoped administrative release, agents may inspect protected specifications, migrations, and DDL; identify exact contract or schema gaps; and report the required human decision, table, column, constraint, transition, or backfill. Do not hide a gap with an application workaround or present proposed protected content as an applied repository change.
5. Treat an existing protected-file modification as human-owned unless the current task contains the scoped administrative authorization described above. Do not format, stage, amend, restore, or otherwise alter that modification.
6. Agents may run existing owner-controlled migrations in a disposable verification database or an explicitly scoped local development database when the task requires it. Never apply schema changes to a shared, staging, production, or otherwise team-managed database on an agent's own initiative.
7. Treat `docs/spec-patches/` as the non-normative proposal and audit layer described in its `README.md`. A patch never changes the product contract by itself. An implementation pull request may use its own `implementation_bundled` patch as the reviewed change record allowed below, but agents must not combine unrelated patches with `docs/specs/` to infer a newer canonical contract.
8. Create one patch per smallest independently approvable functional change. Keep related requirement, REST operation, decision, traceability, data, security, frontend, backend, and test impacts together when they arise from that same change, and identify targets with stable contract identifiers instead of line numbers.
9. A proposer may prepare `draft` content and submit it as `proposed`. Only the PM/Repository Admin acting as Controller may transition a patch to `accepted`, `rejected`, `superseded`, or `applied`. Keep `draft`, `proposed`, and `accepted` patches under `docs/spec-patches/proposed/`; move terminal `applied`, `rejected`, and `superseded` records to `docs/spec-patches/archive/` without erasing their history or revision links.
10. Use one of two explicit delivery modes. `implementation_bundled` is limited to additive or clarifying, backward-compatible changes without migration, DDL, security, privacy, shared-data, or external-consumer risk. Its patch and implementation may share one pull request, but the Controller must transition the patch to `accepted` before that pull request merges. `spec_first` is the default and is mandatory for breaking, security, privacy, data, migration, shared-contract, external-consumer, or otherwise hard-to-reverse changes; keep its patch proposal pull request isolated from application code, Flyway migrations, DDL or schema artifacts, and `docs/specs/`, and do not merge implementation until the patch is `applied` through a canonical spec release. Every implementation pull request records `Spec Patch`, `Base Spec`, delivery mode, and compatibility classification.
11. An accepted `implementation_bundled` patch may temporarily coexist on `dev` with the code it describes before canonical application. It remains non-normative, must not authorize unrelated implementation, and must be applied or the implementation corrected before an approved or production release. Rejection, conflict, or material revision requires a new patch revision and any corresponding code correction; merged code never forces Controller acceptance.
12. In a Controller-owned canonical spec release, start from current `origin/dev`; recheck the patch base version and commit, target overlap, dependencies, delivery mode, and compatibility; then update all affected canonical documents, release metadata, changelog, any required compatibility baseline, `SPEC_LOCK.json`, and the patch's `applied` archive state atomically. Stale or conflicting patches require an explicit new revision or reapproval instead of automatic application. The release guardrail must find no accepted, unapplied `implementation_bundled` patch before an approved or production release proceeds.
13. Reverse a released contract only through a separate revert patch and a new Controller-owned canonical spec release. Never restore an older protected file directly, silently rewrite an accepted revision, or infer rollback authority from an implementation request.
14. Do not maintain current feature inventory, endpoint status, mock status, or implementation progress in `docs/specs/` or in another central status document. Determine current behavior from executable code, configuration, focused tests, verification results, and runtime Swagger. Use `IMPLEMENTATION_GUIDE.md` only for stable exploration order and entrypoints.
15. Update unprotected derived documentation only when its stable architecture, operating procedure, or schema explanation changes. Do not create or treat a generated route or endpoint inventory as canonical current behavior; inspect `frontend/src/router/index.js` and the affected code.
16. Remove a mock only after the corresponding endpoint or explicitly bounded feature unit is implemented and focused verification passes. Do not remove unrelated mocks in the same service merely because one endpoint is live. Production builds must not enable mock behavior.
17. Edit `PROJECT_RULES.md`, `ARCHITECTURE_OVERVIEW.md`, `IMPLEMENTATION_GUIDE.md`, and dependency policy only when the task explicitly changes shared policy, a top-level architecture boundary, stable exploration guidance, or dependency governance. Do not add feature inventory or transient implementation status to these files.
18. Treat `docs/archive/` as historical evidence, not a current document to rewrite. Do not edit other agents' local or personal files, including root `AGENTS.md`, `docs/memory/`, `docs/reports/`, and `NOTICE.md`.
19. For shared Markdown-only changes, verify formatting, relative links, and Git tracking. Keep documents unchanged when a change does not alter their contract, stable entrypoint, architecture, ownership, operating procedure, or verified schema explanation.
20. Never bypass protected-file ownership, patch governance, or the spec-lock guardrail with `--no-verify`, environment overrides, alternate Git plumbing, generated output, another tool, or a delegated sub-agent.

## Verification

1. During incremental work, run the narrowest checks relevant to the changed area.
2. The pre-commit hook selects Frontend, Backend, both, or no application lint from staged paths. Unknown or shared automation paths fail closed to both areas.
3. Run the full root `npm run check` once before pull-request handoff when application code, dependencies, build or test configuration, shared verification automation, or multiple application areas changed.
4. For shared Markdown-only changes, verify formatting, local links, and Git tracking status without running the full application check.
5. Repeat a successful full check only when later changes can invalidate it. Record every skipped required check and the reason in the pull request.
6. Keep local-only agent state, reports, memories, plugins, permissions, models, and credentials out of shared files.

## Language contract

- Agent-only instructions are written in English.
- Human-facing reports, notices, guides, and review explanations are written in Korean.
- Source-code identifiers follow the conventions of their language and framework.

## Shared versus personal configuration

- This file contains only rules that every repository agent must follow.
- `CLAUDE.md` is the tracked Claude Code adapter that imports this contract.
- Root `AGENTS.md`, `CLAUDE.local.md`, `.claude/settings.local.json`, `.codex/`, local reports, and agent memory are user-owned and are not team requirements.
- Do not commit or standardize personal plugin and permission choices without a separate team decision.
