@docs/agent/PROJECT_RULES.md

# Claude Code Adapter

- Treat `docs/agent/PROJECT_RULES.md` as the shared project contract. The import above satisfies its initial load; do not open it again until one of the contract's reload conditions applies.
- Follow the contract's context-loading cadence and do not preload unrelated project documentation.
- Keep plugins, permissions, models, automatic memory, and local preferences user-owned.
- Store personal Claude Code choices in `CLAUDE.local.md` or `.claude/settings.local.json`; do not commit them.
- Never create, read, update, or delete Codex-only files under `docs/reports/` or `docs/memory/`.
- Notion synchronization is not a team requirement. Perform it only when the current user explicitly requests it and the required integration is available.
