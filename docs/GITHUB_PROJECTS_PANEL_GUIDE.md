# GitHub Projects 패널 운영 가이드

GitHub Projects는 이슈와 PR을 테이블, 보드, 로드맵 형태로 추적하는 작업 패널입니다. 이 프로젝트에서는 칸반 보드와 스프린트 테이블을 중심으로 사용합니다.

## Project 생성 기준

- 프로젝트 이름: `KB-PJT-24-2`
- 연결 저장소: `Flamingo7562/KB-PJT-24-2`
- 기본 View: Board
- 반복 주기: 1주 또는 팀 스프린트 주기

## 권장 필드

| 필드 | 타입 | 값 |
| --- | --- | --- |
| `Status` | Single select | `Backlog`, `Ready`, `In Progress`, `In Review`, `QA`, `Done`, `Blocked` |
| `Type` | Single select | `Feature`, `Bug`, `Task`, `Refactor`, `Docs` |
| `Area` | Single select | `Frontend`, `Backend`, `Database`, `Common`, `Infra`, `Docs` |
| `Priority` | Single select | `P0`, `P1`, `P2`, `P3` |
| `Size` | Single select | `XS`, `S`, `M`, `L`, `XL` |
| `Iteration` | Iteration | 주차 또는 스프린트 |
| `Target date` | Date | 목표 완료일 |
| `Risk` | Single select | `Low`, `Medium`, `High` |

## Status 정의

| Status | 의미 | 이동 조건 |
| --- | --- | --- |
| `Backlog` | 아직 착수하지 않은 후보 작업 | 새 이슈가 생성됨 |
| `Ready` | 요구사항과 완료 조건이 정리됨 | 담당자, 우선순위, 범위가 명확함 |
| `In Progress` | 구현 또는 문서 작업 중 | 담당자가 브랜치를 만들고 작업 시작 |
| `In Review` | PR 리뷰 중 | PR 생성 및 리뷰 요청 |
| `QA` | 검증 중 | 리뷰 반영 후 동작 확인 필요 |
| `Done` | 완료 | PR 머지 및 검증 완료 |
| `Blocked` | 진행 불가 | 외부 의존성 또는 결정이 필요함 |

## 권장 View

| View | Layout | 용도 |
| --- | --- | --- |
| `Board` | Board by Status | 전체 진행 상황 확인 |
| `Sprint` | Table by Iteration | 이번 주 작업 계획과 담당자 확인 |
| `Backlog` | Table filtered by Status | 아직 시작하지 않은 작업 정리 |
| `Bugs` | Table filtered by Type | 버그만 모아 우선 처리 |
| `Review` | Table filtered by Status | 리뷰 대기 PR 확인 |
| `Roadmap` | Roadmap | 큰 기능의 목표 일정 확인 |

## 운영 규칙

- 모든 이슈는 Project에 연결합니다.
- 이슈 생성 직후 상태는 `Backlog` 또는 `Ready`로 둡니다.
- 담당자가 작업을 시작하면 `In Progress`로 이동합니다.
- PR을 열면 `In Review`로 이동합니다.
- 리뷰 반영 후 동작 확인이 필요하면 `QA`로 이동합니다.
- 머지 후 검증이 끝나면 `Done`으로 이동합니다.
- 막힌 작업은 `Blocked`로 이동하고, 댓글에 막힌 이유와 필요한 결정을 남깁니다.

## 자동화 권장 설정

가능하면 GitHub Projects의 built-in workflow를 사용해 다음 자동화를 설정합니다.

- 저장소에 새 이슈가 생성되면 Project에 자동 추가
- 새로 추가된 이슈의 `Status`를 `Backlog`로 설정
- PR이 연결되면 `Status`를 `In Review`로 설정
- 이슈가 닫히면 `Status`를 `Done`으로 설정
- 오래된 `Done` 항목은 일정 기간 후 archive

## 스프린트 운영

- 스프린트 시작 전 `Backlog`에서 이번 주 작업을 고릅니다.
- `Ready`가 아닌 이슈는 스프린트에 넣지 않습니다.
- `Size`가 `L` 이상이면 이슈를 나눌지 검토합니다.
- 매일 `Blocked`, `In Review`, `QA` 상태를 먼저 확인합니다.
- 스프린트 종료 시 `Done`과 미완료 작업을 회고합니다.

## 참고 링크

- [GitHub Docs - About Projects](https://docs.github.com/en/issues/planning-and-tracking-with-projects/learning-about-projects/about-projects)

