# Shared Repository Agent Contract

## Mandatory task startup

1. Inspect `git status` and the files relevant to the request before editing.
2. Preserve unrelated user changes.
3. Treat Vue.js, Spring Framework 5 non-Boot, MyBatis, MySQL, Java 17, and Tomcat 9 as fixed constraints.
4. Define the smallest reviewable task boundary and its verification before editing.
5. Use `docs/README.md` to locate additional task-specific documentation.

## Implementation rules

- Keep the monorepo split into `frontend/` and `backend/`; shared automation belongs at the repository root.
- Never add React, Spring Boot, JPA, embedded secrets, API keys, personal data, or environment-specific credentials.
- Use Spring MVC layering (`controller -> service -> mapper`) and explicit DTOs.
- Keep SQL in MyBatis mapper XML unless a tracked project document explicitly changes that convention.
- Make scoped changes and follow the existing lint and formatting rules.
- Ask for direction only when a missing decision would materially change behavior, data, security, or architecture.
- Do not alter another contributor's unrelated work.

## Verification

1. Run `npm run check` when practical, or the narrowest relevant checks for the changed area.
2. Record any skipped check and the reason in the pull request.
3. Verify documentation links and tracked-file status when changing the shared harness.
4. Keep local-only agent state, reports, memories, plugins, permissions, models, and credentials out of shared files.

## Language contract

- Agent-only instructions are written in English.
- Human-facing reports, notices, guides, and review explanations are written in Korean.
- Source-code identifiers follow the conventions of their language and framework.

## Shared versus personal configuration

- This file contains only rules that every repository agent must follow.
- `CLAUDE.md` is the tracked Claude Code adapter that imports this contract.
- Root `AGENTS.md`, `CLAUDE.local.md`, `.claude/settings.local.json`, `.codex/`, local reports, and agent memory are user-owned and are not team requirements.
- Do not commit or standardize personal plugin and permission choices without a separate team decision.
