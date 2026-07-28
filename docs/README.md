# 개발 문서 색인

이 문서는 사람과 Codex·Claude Code가 현재 저장소에서 신뢰할 문서를 찾는 공통 진입점입니다. 로컬 저장소 문서를 원본으로 사용하고, 동일한 내용을 여러 파일에 복제하지 않습니다.

## 문서 상태 기준

| 상태 | 의미 |
| --- | --- |
| 현재 기준 | 현재 코드와 팀 운영에 적용하며 변경 시 함께 갱신해야 하는 문서 |
| 참고 | 특정 작업이나 배경을 이해할 때 선택적으로 읽는 문서 |
| 재검토 필요 | 현재 구현과 차이가 있을 수 있어 코드·설정 확인 없이 기준으로 사용하면 안 되는 문서 |
| 예정 | 아직 작성되지 않았으며 관련 이슈에서 추가할 문서 |

## 에이전트 진입점

| 대상 | 진입 파일 | 역할 |
| --- | --- | --- |
| 모든 에이전트 | [`agent/PROJECT_RULES.md`](agent/PROJECT_RULES.md) | 기술 제약, 구현 경계, 공통 검증 기준의 단일 원본 |
| Claude Code | [`../CLAUDE.md`](../CLAUDE.md) | 공통 규칙을 가져오는 추적 대상 어댑터 |
| Codex 사용자 | 로컬 `AGENTS.md` | 공통 규칙을 읽고 개인 보고서·메모리·Notion 절차를 추가하는 비공유 파일 |

`AGENTS.md`, `docs/memory/`, `docs/reports/`, `NOTICE.md`는 현재 Codex 사용자의 로컬 운영 파일입니다. 다른 에이전트는 `docs/memory/`와 `docs/reports/`를 생성·조회·수정·삭제하지 않습니다. Notion 동기화는 팀 공통 요구사항이 아니며, 현재 사용자가 명시적으로 요청하고 연결 기능을 사용할 수 있을 때만 선택적으로 수행합니다.

플러그인, 권한, 모델과 개인 설정은 각 사용자가 선택합니다. Claude Code의 `CLAUDE.local.md`와 `.claude/settings.local.json`, Codex의 `.codex/`는 Git에 포함하지 않습니다.

## 현재 기준 문서

| 작업 | 먼저 읽을 문서 |
| --- | --- |
| 개발 환경 시작 | [`GETTING_STARTED.md`](GETTING_STARTED.md) |
| 브랜치·이슈·PR | [`PROJECT_MANAGEMENT_GUIDE.md`](PROJECT_MANAGEMENT_GUIDE.md), [`ISSUE_WRITING_GUIDE.md`](ISSUE_WRITING_GUIDE.md) |
| 커밋 | [`COMMIT_CONVENTION.md`](COMMIT_CONVENTION.md) |
| Lint와 Git Hook | [`LINT_GUIDE.md`](LINT_GUIDE.md), [`GIT_HOOKS_HUSKY_GUIDE.md`](GIT_HOOKS_HUSKY_GUIDE.md) |
| Monorepo 구조 | [`MONOREPO_STRUCTURE_GUIDE.md`](MONOREPO_STRUCTURE_GUIDE.md) |
| 로컬 DB와 Migration | [`DOCKER_DATABASE_SCHEMA_GUIDE.md`](DOCKER_DATABASE_SCHEMA_GUIDE.md) |
| 회원가입 DB 변경 | [`SIGNUP_DATABASE_SCHEMA.md`](SIGNUP_DATABASE_SCHEMA.md) |
| 계약·에스크로 테스트 Seed | [`TEST_CONTRACT_SEED_GUIDE.md`](TEST_CONTRACT_SEED_GUIDE.md) |

`DOCKER_DATABASE_SCHEMA_GUIDE.md`를 포함한 기존 문서는 현재 코드와 다른 과거 설명이 남아 있을 수 있습니다. 관련 파일과 실행 결과를 함께 확인하고, 불일치를 발견하면 별도 이슈로 갱신합니다.

## 작성 예정 문서

다음 문서는 별도 이슈와 작은 PR로 작성합니다.

1. `docs/api/openapi.yaml`과 공통 API 규격
2. 수직 기능 개발 플레이북
3. 백엔드 개발 가이드
4. 프론트엔드 개발 가이드
5. 테스트 전략과 전체 로컬 실행 Runbook

새 문서를 만들 때는 이 색인에 상태와 진입 경로를 추가합니다.
