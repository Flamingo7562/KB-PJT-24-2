# 개발 문서 색인

이 문서는 사람과 Codex·Claude Code가 저장소 문서를 찾는 유일한 라우터입니다. 루트 `README.md`와 개별 가이드에 전체 문서 목록을 복제하지 않습니다.

문서와 코드·설정이 다르면 실행 가능한 코드, 빌드 설정, Migration과 검증 결과를 우선 확인합니다. 불일치를 발견한 문서는 그대로 기준으로 사용하지 않고 별도 이슈에서 갱신하거나 아카이브합니다.

## 문서 상태 기준

| 상태        | 의미                                                                               |
| ----------- | ---------------------------------------------------------------------------------- |
| 현재 기준   | 현재 코드와 팀 운영에 적용하며 변경 시 함께 갱신해야 하는 문서                     |
| 작업별 참고 | 특정 기능이나 작업을 할 때 선택적으로 읽는 문서                                    |
| 재검토 필요 | 현재 구현과 차이가 있을 수 있어 코드·설정 확인 없이 기준으로 사용하면 안 되는 문서 |
| 아카이브    | 현재 기준이 아니며 과거 결정이나 절차를 확인할 때만 읽는 문서                      |

## 에이전트 진입점

| 대상          | 진입 파일                                                          | 역할                                                                |
| ------------- | ------------------------------------------------------------------ | ------------------------------------------------------------------- |
| 모든 에이전트 | [`agent/PROJECT_RULES.md`](agent/PROJECT_RULES.md)                 | 컨텍스트 로딩, 기술 제약, 구현 경계와 공통 검증 기준의 단일 원본    |
| 구현 작업     | [`agent/ARCHITECTURE_OVERVIEW.md`](agent/ARCHITECTURE_OVERVIEW.md) | 구현 전에 한 번 읽는 짧은 전체 구조와 책임 경계                     |
| 의존성·빌드   | [`DEPENDENCY_SPECIFICATION.md`](DEPENDENCY_SPECIFICATION.md)       | 기술의 허용 상태, 직접 의존성 역할과 변경 절차                      |
| DB 관련 작업  | [`agent/SCHEMA_OVERVIEW.md`](agent/SCHEMA_OVERVIEW.md)             | Migration, 관계, 불변식과 애플리케이션 책임 경계의 간결한 현재 요약 |
| Claude Code   | [`../CLAUDE.md`](../CLAUDE.md)                                     | 공통 규칙을 가져오는 추적 대상 어댑터                               |
| Codex 사용자  | 로컬 `AGENTS.md`                                                   | 공통 규칙을 읽고 개인 운영 절차를 추가하는 비공유 파일              |

공유 계약은 새 대화·에이전트 인계·브랜치 전환 또는 파일 변경 시 읽고, 같은 대화에서 변경되지 않았다면 반복해서 읽지 않습니다. 구현 작업은 짧은 Architecture Overview를 각 에이전트 대화에서 한 번 읽고, 컨텍스트 초기화·인계·문서 변경이나 아키텍처 경계 이동 후에는 다시 읽습니다. 상세 문서는 현재 변경 영역과 관련될 때만 선택합니다.

`AGENTS.md`, `docs/memory/`, `docs/reports/`, `NOTICE.md`는 현재 Codex 사용자의 로컬 운영 파일입니다. 다른 에이전트는 `docs/memory/`와 `docs/reports/`를 생성·조회·수정·삭제하지 않습니다. Notion 동기화는 팀 공통 요구사항이 아니며, 현재 사용자가 명시적으로 요청하고 연결 기능을 사용할 수 있을 때만 선택적으로 수행합니다.

플러그인, 권한, 모델과 개인 설정은 각 사용자가 선택합니다. Claude Code의 `CLAUDE.local.md`와 `.claude/settings.local.json`, Codex의 `.codex/`는 Git에 포함하지 않습니다.

## 애플리케이션 진입점

| 영역     | 문서                                             | 역할                                           |
| -------- | ------------------------------------------------ | ---------------------------------------------- |
| Frontend | [`../frontend/README.md`](../frontend/README.md) | Vue 애플리케이션 설치, 실행, 검증과 구조       |
| Backend  | [`../backend/README.md`](../backend/README.md)   | Spring Legacy WAR, DB 연결, 검증과 패키지 구조 |

## 현재 개발 가이드

### 기술 스택과 의존성

| 작업                                             | 먼저 읽을 문서                                               |
| ------------------------------------------------ | ------------------------------------------------------------ |
| 기술의 기본·허용·조건부·금지 여부 확인           | [`DEPENDENCY_SPECIFICATION.md`](DEPENDENCY_SPECIFICATION.md) |
| 언어·직접 의존성·빌드 도구·Container 이미지 변경 | [`DEPENDENCY_SPECIFICATION.md`](DEPENDENCY_SPECIFICATION.md) |

### 데이터베이스

| 작업                                          | 먼저 읽을 문서                                                                                                         |
| --------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------- |
| 로컬 MySQL 시작·Migration·연결 검증·장애 대응 | [`runbooks/DATABASE_RUNBOOK.md`](runbooks/DATABASE_RUNBOOK.md)                                                         |
| Migration·Mapper·영속 도메인·트랜잭션 변경    | [`agent/SCHEMA_OVERVIEW.md`](agent/SCHEMA_OVERVIEW.md), [`runbooks/DATABASE_RUNBOOK.md`](runbooks/DATABASE_RUNBOOK.md) |

DB 스키마의 단일 원본은 `backend/src/main/resources/db/migration/V*.sql`입니다. Runbook과 Schema Overview는 코드를 대체하지 않으며 Migration 또는 DB 설정을 변경한 PR에서 함께 갱신합니다.

### 개발 환경과 저장소 구조

| 작업                    | 먼저 읽을 문서                                                                                                                   |
| ----------------------- | -------------------------------------------------------------------------------------------------------------------------------- |
| 새 clone 최초 환경 준비 | [`GETTING_STARTED.md`](GETTING_STARTED.md)                                                                                       |
| 저장소·실행 영역 확인   | [`../README.md`](../README.md), [`../frontend/README.md`](../frontend/README.md), [`../backend/README.md`](../backend/README.md) |

### 협업과 Git

| 작업                        | 먼저 읽을 문서                                                                                                                                                                         |
| --------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 이슈 생성·브랜치·PR         | [`PROJECT_MANAGEMENT_GUIDE.md`](PROJECT_MANAGEMENT_GUIDE.md)                                                                                                                           |
| 실제 Issue Form 필드·필수값 | [`feature_request.yml`](../.github/ISSUE_TEMPLATE/feature_request.yml), [`bug_report.yml`](../.github/ISSUE_TEMPLATE/bug_report.yml), [`task.yml`](../.github/ISSUE_TEMPLATE/task.yml) |
| 실제 PR 양식·체크리스트     | [`pull_request_template.md`](../.github/pull_request_template.md)                                                                                                                      |
| 커밋 작성                   | [`COMMIT_CONVENTION.md`](COMMIT_CONVENTION.md)                                                                                                                                         |
| GitHub Projects 운영        | [`GITHUB_PROJECTS_PANEL_GUIDE.md`](GITHUB_PROJECTS_PANEL_GUIDE.md)                                                                                                                     |

`GITHUB_PROJECTS_PANEL_GUIDE.md`는 현재 팀이 사용하는 보드를 관리하기 위한 사람 중심 운영 문서입니다. 모든 에이전트가 매 작업마다 읽는 문서는 아닙니다.

### 코드 품질과 Hook

| 작업                    | 먼저 읽을 문서                                         |
| ----------------------- | ------------------------------------------------------ |
| Lint·Guardrail·Git Hook | [`GIT_HOOKS_HUSKY_GUIDE.md`](GIT_HOOKS_HUSKY_GUIDE.md) |

## 아카이브

- [문서 아카이브 정책과 목록](archive/README.md)

아카이브 문서는 현재 개발 기준이 아닙니다. 이동 기준과 절차는 `docs/archive/README.md`에서만 관리합니다.

## 새 문서 추가 기준

새 공유 문서는 실제 구현이나 반복 운영 절차가 있고 검증 방법을 설명할 수 있을 때 별도 이슈와 PR로 추가합니다. 아직 합의되지 않았거나 구현 근거가 없는 문서 아이디어는 이 색인에 미리 등록하지 않고 관련 이슈에서 관리합니다.
