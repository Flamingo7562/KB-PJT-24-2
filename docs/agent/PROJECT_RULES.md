# Shared Repository Agent Contract

## Context loading

1. Read this contract completely for the first repository task in a new agent conversation, after a context reset or agent handoff, after switching branches, or when this file changes.
2. Reuse the unchanged contract within the same conversation instead of reopening it on every turn.
3. Read `docs/agent/ARCHITECTURE_OVERVIEW.md` once in each agent conversation before material implementation for an issue or branch. Read it again after a context reset or agent handoff, when it changes, or when the work crosses an architecture boundary.
4. Use `docs/README.md` only when the relevant task-specific document is not already known or when the task moves to a different area.
5. Load detailed domain, runbook, schema, API, and testing documents only when the current change touches that subject. Do not preload the documentation tree.
6. Treat executable code, configuration, migrations, and current verification results as authoritative when documentation may be stale.
7. General questions, status checks, and narrow read-only inspection do not require the architecture overview or unrelated task documents.

## Task startup

1. Inspect `git status` and the files relevant to the request before editing.
2. Preserve unrelated user changes.
3. Treat Vue.js, Spring Framework 5 non-Boot, MyBatis, MySQL, Java 17, and Tomcat 9 as fixed constraints.
4. Define the smallest reviewable task boundary and its verification before editing.

## Implementation rules

- Keep the monorepo split into `frontend/` and `backend/`; shared automation belongs at the repository root.
- Never add React, Spring Boot, JPA, embedded secrets, API keys, personal data, or environment-specific credentials.
- Use Spring MVC layering (`controller -> service -> mapper`) and explicit DTOs.
- Keep SQL in MyBatis mapper XML unless a tracked project document explicitly changes that convention.
- Make scoped changes and follow the existing lint and formatting rules.
- Ask for direction only when a missing decision would materially change behavior, data, security, or architecture.
- Do not alter another contributor's unrelated work.

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
