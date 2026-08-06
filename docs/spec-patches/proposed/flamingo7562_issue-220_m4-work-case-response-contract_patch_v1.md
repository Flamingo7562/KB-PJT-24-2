---
patch_id: SPEC-220-01
author: flamingo7562
status: proposed
issue: 220
created_at: 2026-08-06
base_spec_version: 3.0.1
base_commit: "38c28f50f22ab809c1bbd4a060bb503879027766"
change_type: breaking
targets:
  - requirement: WORK-001
  - requirement: WORK-002
  - requirement: WORK-003
  - requirement: WORK-004
  - requirement: WORK-005
  - requirement: WORK-006
  - decision: DEC-WORK-CASE-RESPONSE-SHAPES
  - decision: DEC-OPEN-WORK-CASE-RESPONSE-SHAPES
  - rest_operation: "GET /api/workplaces/{workplaceId}/work-cases/summary"
  - rest_operation: "GET /api/workplaces/{workplaceId}/work-cases"
  - rest_operation: "POST /api/workplaces/{workplaceId}/work-cases"
  - rest_operation: "GET /api/work-cases/{workCaseId}"
  - rest_operation: "PATCH /api/work-cases/{workCaseId}"
  - rest_operation: "DELETE /api/work-cases/{workCaseId}"
depends_on:
  - SPEC-218-01
supersedes: null
superseded_by: null
applied_in_version: null
applied_by_pr: null
---

# SPEC-220-01: M4 Work Case 응답 계약 확정

## 변경 요약과 필요성

OWNER의 Work Case 상태별 요약, 목록, 상세, 수정과 삭제에서 관찰되는 전체 API 계약을
하나의 최소 승인 단위로 제안한다. 8개 상태의 요약 Key, 목록 Item과 상세 Aggregate의 닫힌
필드 집합, `null` 규칙, 검색·기간·페이지·정렬, 주소 Snapshot 표현, 성공 응답과 화면 동작
판단 기준을 확정한다.

정식 명세 3.0.1은 경로, 핵심 상태와 일부 요청 필드는 승인했지만 전체 응답 Shape는
`DEC-OPEN-WORK-CASE-RESPONSE-SHAPES`에 남겼다. 현재 Frontend Mock은 6개 요약 버킷,
`startTime`·`endTime`, 평면 `workerName`, `settleStatus`와 `canIssueInvitation`을 임시로
사용하므로 Backend 구현자가 이 Shape를 그대로 계약으로 오인하면 8상태 요구사항,
UTC `Instant` 원칙과 Aggregate 경계에 어긋날 수 있다.

이 Patch는 Flyway `202608051337`의 기존 컬럼과 관계만으로 반환할 수 있는 조회 계약을
고정한다. 초대 Token과 Link 생명주기는 `SPEC-218-01`, 수락·전자동의 원자성은 후속 Patch,
문서 본문과 다운로드 계약은 문서 명세가 담당한다. 애플리케이션 구현, Migration, DDL과
`docs/specs/**` 편집은 포함하지 않는다.

## 현재 명세와 문제

- `WORK-001`은 8개 상태를 열거하지만 응답 Key를 정하지 않아 현재 Mock처럼
  `CHECK_OUT_MISSING`과 `CANCELED`을 누락할 수 있다.
- `WORK-002`는 목록 조회를 요구하지만 Item 필드, `worker` 중첩과 `null`, `keyword` 대상,
  기간 포함 경계와 동률 정렬을 고정하지 않는다.
- `WORK-003`은 사업장 주소 Snapshot을 요구하지만 `roadAddress`와 `detailAddress`를 현재
  `work_cases.workplace_address` 한 컬럼에 조합하는 규칙과 외부 필드명을 정하지 않는다.
- `WORK-004`와 `GET /api/work-cases/{workCaseId}`는 초대·계약·근태·에스크로·정산을
  조회하도록 요구하면서 각 요약 객체의 필드와 Aggregate가 없을 때의 표현을 정하지 않는다.
- 현재 Mock의 `startTime`·`endTime`은 요청용 지역 시각 필드인데 조회 응답에도 사용되어
  `DEC-TIME`의 UTC `Instant` 원칙과 충돌한다.
- `canEdit`, `canDelete`, `canIssueInvitation` 같은 Capability를 서버가 반환할지, 상태와
  시각으로 화면이 파생할지 결정되지 않았다.
- `PATCH`와 `DELETE`의 성공 Status와 Body가 고정되지 않았고, Hard Delete와 `CANCELED`
  전환의 외부 응답이 달라지는지도 알 수 없다.
- `SPEC_TRACEABILITY.md`의 `WORK-005` 데이터 연결은 실제 컬럼에 없는
  `work_cases.condition_version`을 가리킨다.

## 제안할 최종 규범 문장 또는 Before/After

### 1. 요구사항

정식 명세의 `WORK-001`~`WORK-006`을 다음 의미로 편집한다.

#### `WORK-001` 교체

> 해당 OWNER는 선택한 사업장의 Work Case 수를 `DRAFT`, `ACCEPTED`, `READY`,
> `IN_PROGRESS`, `CHECK_OUT_MISSING`, `COMPLETED`, `NO_SHOW`, `CANCELED`의 서로 다른
> 8개 버킷으로 조회한다. 결과에 없는 상태도 0으로 반환하며 모든 값은 0 이상의 정수다.

#### `WORK-002` 교체

> 해당 OWNER는 선택한 사업장의 Work Case 목록을 제목 또는 매칭 WORKER 이름으로 검색하고
> 단일 상태와 `Asia/Seoul` 근무 날짜의 양끝 포함 구간으로 필터링한다. 서버는
> `starts_at DESC, id DESC` 순서와 공통 페이지 규칙을 적용한다. 목록 Item은 Work Case ID,
> 제목, 근무 날짜, UTC 시작·종료 시점, 일급, 상태와 매칭 WORKER를 반환하며 미매칭
> `worker`는 `null`이다.

#### `WORK-003` 수용 기준 보강

> Work Case 생성 시 `workplaces.road_address`를 trim하고, trim한
> `workplaces.detail_address`가 비어 있지 않을 때만 한 칸을 사이에 두어 결합한 값을
> `work_cases.workplace_address`에 저장한다. 상세 응답은 현재 사업장 주소가 아닌 이 불변
> Snapshot을 `workplaceAddress` 한 필드로 반환한다.

#### `WORK-004` 교체

> OWNER와 매칭 WORKER는 Work Case의 조건 Snapshot과 상태, 매칭 WORKER, 최신 초대,
> 계약, 성공 근태, 에스크로와 정산 요약을 상세 조회한다. 존재하지 않는 단일 Aggregate는
> `null`, 성공 출퇴근 기록은 항상 `attendance` 객체 안의 nullable 시점으로 반환한다.
> 문서 파일과 전자서명 증거는 상세에 포함하지 않고 별도 문서 권한과 API로 조회한다.

#### `WORK-005` 수용 기준 보강

> 해당 OWNER는 `DRAFT` Work Case에 등록과 같은 일곱 조건을 완전한 요청 Body로 보내
> 수정한다. 성공할 때마다 조건 Snapshot을 원자적으로 교체하고 `terms_version`을 정확히
> 1 증가시키며 Body 없는 204를 반환한다. 활성 초대 철회는 `SPEC-218-01`의
> `DEC-INVITE-LIFECYCLE`을 따른다.

#### `WORK-006` 수용 기준 보강

> 해당 OWNER가 삭제 가능한 `DRAFT` Work Case를 삭제하면 초대 이력이 없는 경우 Hard
> Delete하고, 초대 이력이 있으면 `CANCELED`로 전이한다. 두 경우 모두 Body 없는 204를
> 반환하며 저장 방식의 차이를 외부 응답으로 노출하지 않는다. 활성 초대 철회는
> `SPEC-218-01`의 `DEC-INVITE-LIFECYCLE`을 따른다.

### 2. 결정

`DEC-WORK-CASE-RESPONSE-SHAPES`를 Approved 결정으로 추가한다.

> Work Case 요약은 8개 lower camel case Count 필드를 모두 반환한다. 목록 Item과 상세는
> 이 결정에 명시한 닫힌 필드 집합을 사용하고 별칭이나 Capability를 추가하지 않는다.
> 조회의 `workDate`는 `startsAt`을 `Asia/Seoul`로 변환한 `LocalDate`, `startsAt`과
> `endsAt`은 UTC `Instant`, 금액은 KRW 원 단위 정수다. 목록과 상세의 `worker` Shape는
> 동일하며 미매칭이면 `null`이다. 상세의 최신 초대, 계약, 에스크로와 정산은 각 Aggregate가
> 없으면 `null`이고, `attendance`는 항상 존재하되 성공 기록이 없는 시점을 `null`로
> 반환한다. `canEdit`, `canDelete`, `canIssueInvitation`은 응답 필드가 아니며 화면은 상태,
> 매칭, 시각과 최신 초대 상태로 가시성을 파생하고 서버가 최종 권한과 상태를 검증한다.

`DEC-OPEN-WORK-CASE-RESPONSE-SHAPES`는 이 결정으로 해소하고 Open 표에서 제거한다.

### 3. REST API

#### `GET /api/workplaces/{workplaceId}/work-cases/summary`

- 해당 사업장의 OWNER만 호출한다.
- Query는 없고 취소를 포함한 전체 Work Case를 8개 상태로 한 번씩만 집계한다.
- 데이터가 없는 상태도 Key를 생략하지 않고 0을 반환한다.

```json
{
  "data": {
    "draft": 2,
    "accepted": 1,
    "ready": 3,
    "inProgress": 1,
    "checkOutMissing": 0,
    "completed": 8,
    "noShow": 1,
    "canceled": 2
  }
}
```

#### `GET /api/workplaces/{workplaceId}/work-cases`

Query 계약은 다음과 같다.

- `keyword?`: trim한 뒤 제목 또는 매칭 WORKER 이름의 대소문자 구분 없는 부분 일치다.
  trim 결과가 빈 문자열이면 미지정과 같다.
- `status?`: 8개 Work Case 상태 중 하나다. 미지정이면 모든 상태를 조회한다.
- `from?`, `to?`: `Asia/Seoul` 기준 `workDate`의 `LocalDate`이며 양끝을 포함한다. 둘 다
  있으면 `from <= to`여야 한다.
- `page?`, `size?`: `DEC-PAGE`의 `page=0`, `size=20`, 최대 100을 따른다.
- 별도 정렬 Query는 받지 않고 `starts_at DESC, id DESC`로 고정한다.

목록 Item의 전체 필드는 다음과 같다. `worker`가 있으면 `workerId`와 `name`을 둘 다
반환하고 없으면 객체 내부 필드를 nullable로 만들지 않고 객체 전체를 `null`로 반환한다.

```json
{
  "data": {
    "content": [
      {
        "workCaseId": 101,
        "title": "주말 홀 서빙",
        "workDate": "2026-08-20",
        "startsAt": "2026-08-20T01:00:00Z",
        "endsAt": "2026-08-20T09:00:00Z",
        "dailyWage": 120000,
        "status": "READY",
        "worker": {
          "workerId": 42,
          "name": "이알바"
        }
      },
      {
        "workCaseId": 100,
        "title": "평일 주방 보조",
        "workDate": "2026-08-19",
        "startsAt": "2026-08-19T00:00:00Z",
        "endsAt": "2026-08-19T06:00:00Z",
        "dailyWage": 90000,
        "status": "DRAFT",
        "worker": null
      }
    ],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 2,
      "totalPages": 1
    }
  }
}
```

#### `POST /api/workplaces/{workplaceId}/work-cases`

요청과 `201 {data:{workCaseId}}`는 정식 명세 3.0.1을 유지한다. 생성 시각 조합과 주소
Snapshot은 다음 순서로 처리한다.

1. `workDate`, `startTime`, `endTime`을 `Asia/Seoul` 지역 시각으로 결합하고
   `endsAt > startsAt`을 검증한다.
2. 사업장 이름, 결합 주소, 좌표와 100m 반경을 Work Case에 복사한다.
3. 상세 응답의 `workDate`는 저장한 `startsAt`에서 다시 파생하고, 요청용 `startTime`과
   `endTime`은 조회 응답에 반환하지 않는다.

#### `GET /api/work-cases/{workCaseId}`

- 해당 Work Case의 OWNER 또는 매칭 WORKER만 호출한다. 미매칭 `DRAFT`에는 OWNER만
  당사자다.
- 아래 JSON의 Key가 전체 상세 필드 집합이다. 문서 본문, 서명 증거, 좌표, 인증 반경,
  전화번호와 Capability를 포함하지 않는다.
- `latestInvitation`은 조건 Version과 관계없이 해당 Work Case에서 가장 나중에 생성된
  초대 한 건이며 생성 시각과 ID 내림차순으로 결정한다. 초대 이력이 없으면 `null`이다.
- `attendance.checkedInAt`과 `checkedOutAt`은 각 유형의 `SUCCESS` 기록 `captured_at`이며
  성공 기록이 없으면 `null`이다. 거절된 시도는 상세에 포함하지 않는다.

```json
{
  "data": {
    "workCaseId": 101,
    "title": "주말 홀 서빙",
    "workDate": "2026-08-20",
    "startsAt": "2026-08-20T01:00:00Z",
    "endsAt": "2026-08-20T09:00:00Z",
    "breakMinutes": 60,
    "breakPaid": false,
    "dailyWage": 120000,
    "status": "COMPLETED",
    "termsVersion": 3,
    "workplaceName": "강남점",
    "workplaceAddress": "서울특별시 강남구 테헤란로 1 2층",
    "worker": {
      "workerId": 42,
      "name": "이알바"
    },
    "latestInvitation": {
      "status": "ACCEPTED",
      "termsVersion": 3,
      "expiresAt": "2026-08-20T01:00:00Z"
    },
    "contract": {
      "contractId": 31,
      "sourceTermsVersion": 3,
      "acceptedAt": "2026-08-10T04:00:00Z"
    },
    "attendance": {
      "checkedInAt": "2026-08-20T01:00:00Z",
      "checkedOutAt": "2026-08-20T09:00:00Z"
    },
    "escrow": {
      "status": "RELEASED",
      "amount": 120000
    },
    "settlement": {
      "status": "COMPLETED",
      "amount": 120000,
      "dueAt": "2026-08-21T00:00:00Z",
      "completedAt": "2026-08-21T00:05:00Z"
    }
  }
}
```

중첩 객체의 정확한 `null` 규칙은 다음과 같다.

- `worker`: `work_cases.worker_id`가 없으면 `null`이다.
- `latestInvitation`: 초대 이력이 없으면 `null`이다. 객체가 있으면 `status`,
  `expected_terms_version`을 외부 `termsVersion`으로 바꾼 값과 `expiresAt`을 반환한다.
- `contract`: `work_contracts`가 없으면 `null`이다. 객체가 있으면 `contractId`,
  `sourceTermsVersion`, `acceptedAt`을 반환한다.
- `attendance`: 항상 객체이며 성공 출근·퇴근 기록별로 시점이 없으면 해당 필드가 `null`이다.
- `escrow`: `escrows`가 없으면 `null`이다. 객체가 있으면 `status`, `amount`를 반환한다.
- `settlement`: `settlements`가 없으면 `null`이다. 객체가 있으면 `status`, `amount`,
  nullable `dueAt`, nullable `completedAt`을 반환한다.

#### `PATCH /api/work-cases/{workCaseId}`

- 해당 Work Case의 OWNER만 호출한다.
- Body는 `title`, `workDate`, `startTime`, `endTime`, `breakMinutes`, `breakPaid`,
  `dailyWage` 일곱 필드를 모두 요구한다. 일부 필드 생략과 명시적 `null`은
  `400 VALIDATION_ERROR`다.
- 현재 Work Case가 `DRAFT`가 아니면 `409 WORK_CASE_LOCKED`다.
- 변경 전후 값이 같더라도 성공한 한 요청은 조건 갱신 한 번으로 간주하여
  `terms_version`을 정확히 1 증가시킨다.
- 주소, 좌표, 반경과 사업장명 Snapshot은 수정하지 않는다.
- 성공은 204이며 응답 Body가 없다. 클라이언트는 최신 상세가 필요하면 GET으로 다시
  조회한다.

#### `DELETE /api/work-cases/{workCaseId}`

- 해당 Work Case의 OWNER만 호출한다.
- `DRAFT`가 아니거나 계약·에스크로가 생성된 경우 `409 WORK_CASE_LOCKED`다.
- 초대 이력이 없으면 Hard Delete하고, 있으면 `CANCELED`로 전이한다. 두 성공 경로 모두
  204이며 응답 Body가 없다.
- 클라이언트는 Hard Delete와 상태 전이를 응답으로 분기하지 않고 목록을 다시 조회한다.

#### Capability와 화면 파생

응답에 `canEdit`, `canDelete`, `canIssueInvitation` 또는 같은 의미의 별칭을 추가하지 않는다.

- 수정·삭제 동작은 `status == DRAFT`일 때 표시한다.
- 일반 초대 발급·현재 Link 복사 동작은 `status == DRAFT`, `worker == null`,
  `now < startsAt`일 때 표시한다.
- 재발급 동작은 위 조건에 더해 `latestInvitation.status == PENDING`이고
  `latestInvitation.termsVersion == termsVersion`일 때 표시한다.
- 클라이언트 시각과 조회 뒤 상태가 달라질 수 있으므로 서버는 모든 변경 요청에서 권한,
  상태, 매칭, 시각과 초대 Version을 다시 검증한다.

### 4. 추적성

`SPEC_TRACEABILITY.md`의 연결을 다음과 같이 정렬한다.

| 요구사항 | REST Operation                                         | 도메인·데이터                                         | 연결 결정                                                           |
| -------- | ------------------------------------------------------ | ----------------------------------------------------- | ------------------------------------------------------------------- |
| WORK-001 | `GET /api/workplaces/{workplaceId}/work-cases/summary` | `work_cases.status`                                   | `DEC-CHECK-OUT-MISSING`, `DEC-WORK-CASE-RESPONSE-SHAPES`            |
| WORK-002 | `GET /api/workplaces/{workplaceId}/work-cases`         | `work_cases`, `users`                                 | `DEC-PAGE`, `DEC-TIME`, `DEC-WORK-CASE-RESPONSE-SHAPES`             |
| WORK-003 | `POST /api/workplaces/{workplaceId}/work-cases`        | `work_cases`, 사업장 Snapshot                         | `DEC-WORKPLACE-RADIUS`, `DEC-TIME`, `DEC-WORK-CASE-RESPONSE-SHAPES` |
| WORK-004 | `GET /api/work-cases/{workCaseId}`                     | `work_cases`, 초대·계약·근태·에스크로·정산 Aggregate  | `DEC-WORK-CASE-RESPONSE-SHAPES`                                     |
| WORK-005 | `PATCH /api/work-cases/{workCaseId}`                   | `work_cases.terms_version`, `work_invitations.status` | `DEC-INVITE-LIFECYCLE`, `DEC-WORK-CASE-RESPONSE-SHAPES`             |
| WORK-006 | `DELETE /api/work-cases/{workCaseId}`                  | `work_cases.status`, `work_invitations.status`        | `DEC-INVITE-LIFECYCLE`, `DEC-WORK-CASE-RESPONSE-SHAPES`             |

## 영향 분석

### 요구사항

- `WORK-001`~`WORK-004`의 조회·Snapshot 수용 기준을 닫힌 계약으로 고정한다.
- `WORK-005`, `WORK-006`에는 요청 완전성, 성공 204와 외부에서 관찰되는 삭제 경계를
  보강한다.
- `WORK-007`의 WORKER 목록은 이 Patch의 OWNER 목록과 다른 사용 사례이므로 변경하지 않는다.

### API

- 요약은 기존 Mock의 6개 Key에서 8개 Key로 바뀐다.
- 목록은 `startTime`·`endTime`, `workerName`, `matched`, Capability 대신 UTC 시점과 중첩
  `worker`를 사용한다.
- 상세는 조건과 5개 Aggregate 요약의 필드·`null`을 닫힌 집합으로 고정하고 기존 Mock의
  평면 `settleStatus`를 제거한다.
- PATCH 성공 응답을 데이터 Body에서 204로, DELETE의 두 저장 경로를 같은 204로 고정한다.

### 데이터 및 Migration

- 현재 `work_cases`, `work_invitations`, `work_contracts`, `attendance_records`, `escrows`,
  `settlements`, `users`만 조회한다.
- `workplaceAddress`는 이미 존재하는 단일 Snapshot 컬럼을 그대로 사용하므로
  `roadAddress`와 `detailAddress`로 역분해하지 않는다.
- 새 컬럼, Flyway Migration, DDL, Backfill과 기존 Migration 수정은 없다.

### 보안

- 사업장 좌표·반경, 전화번호, 문서 본문, 서명 증거와 초대 Token을 상세에 노출하지 않는다.
- OWNER 또는 매칭 WORKER의 당사자 검사를 먼저 수행하고, 문서 파일은 기존 별도 권한
  검사를 유지한다.
- 숫자 ID와 상태를 반환하는 것 외에 새로운 개인정보 또는 Secret을 추가하지 않는다.

### Frontend

- 요약 UI는 `checkOutMissing`과 `canceled`를 포함한 8개 Key를 처리해야 한다.
- 목록·상세는 `startsAt`·`endsAt`을 사용자 지역 시각으로 표시하고 `worker: null`을
  미매칭으로 처리해야 한다.
- 현재 Mock의 `workerName`, `matched`, `settleStatus`, `canIssueInvitation`, `startTime`,
  `endTime` 의존을 제거하고 닫힌 DTO와 화면 파생 규칙으로 교체해야 한다.
- 이 변경은 정식 명세 적용 뒤 별도 Frontend 구현 이슈와 브랜치에서 수행한다.

### Backend

- 요약 집계, 검색 JOIN, 기간 필터, 동률 정렬과 공통 Page Envelope를 구현해야 한다.
- 상세는 Aggregate별 Left Join 또는 분리 조회 후 중복 행 없이 한 DTO로 조립해야 한다.
- PATCH는 Work Case 잠금과 `terms_version` 증가, DELETE는 이력 유무 분기를 수행하며
  초대 잠금·철회 순서는 `SPEC-218-01`을 따른다.
- 이 변경은 정식 명세 적용 뒤 별도 Backend 구현 이슈와 브랜치에서 수행한다.

### 테스트

- 8개 요약 Key의 존재, 0 채움과 상태별 정확한 집계를 계약 테스트로 고정한다.
- 제목·WORKER 이름 검색, 단일 상태, 양끝 포함 날짜, 페이지 경계와 동률 정렬을 검증한다.
- 목록·상세의 전체 필드, UTC 변환, Worker와 Aggregate의 `null` 규칙을 검증한다.
- 주소 조합과 사업장 변경 뒤 Snapshot 불변성을 검증한다.
- PATCH의 완전한 Body, Version 증가, 204와 잠금 오류를 검증한다.
- DELETE의 Hard Delete·`CANCELED` 분기와 동일한 204, 참조 이력 잠금을 검증한다.
- 응답에 금지한 Capability, 별칭, 좌표와 민감 필드가 없는지 검증한다.

## 검증 가능한 수용 조건

- [ ] Patch 기준이 Spec `3.0.1`, `origin/dev` Commit
      `38c28f50f22ab809c1bbd4a060bb503879027766`, Flyway `202608051337`과 일치한다.
- [ ] 요약 응답이 8개 Key를 항상 포함하고 없는 상태를 0으로 반환한다.
- [ ] 목록 Query의 검색 대상, 상태, 포함 날짜 범위, 페이지와 고정 정렬이 명시되어 있다.
- [ ] 목록 Item과 상세의 닫힌 필드 집합, UTC·LocalDate·금액 형식이 JSON 예시와 일치한다.
- [ ] 미매칭 Worker와 미생성 Aggregate의 객체 단위 `null`, 성공 근태 시점의 필드 단위
      `null`이 구분된다.
- [ ] 결합 주소를 한 Snapshot 필드로 저장·반환하며 현재 사업장 주소와 역분해에 의존하지
      않는다.
- [ ] Capability를 응답하지 않고 화면 파생 조건과 서버의 최종 검증 책임을 구분한다.
- [ ] PATCH의 완전한 Body, Version 증가와 204, DELETE의 두 저장 경로와 동일한 204가
      고정된다.
- [ ] `SPEC_TRACEABILITY.md`의 잘못된 `condition_version`이 실제 `terms_version`으로
      교체된다.
- [ ] 초대 철회는 `SPEC-218-01`에 의존하고 수락·전자동의 Aggregate 의미를 이 Patch에서
      확장하지 않는다.
- [ ] 애플리케이션 코드, Flyway, DDL, `docs/specs/**`와 `SPEC_LOCK.json`은 이 Patch
      브랜치에서 변경되지 않는다.

## 미결 사항

없음. 이 문서의 제품 방향 전체를 Controller가 승인하거나 거절한다. 승인 뒤 제품 의미를
바꿔야 하면 같은 파일을 덮어쓰지 않고 새 Patch 리비전으로 재승인받는다.

## 관련 Issue·PR·의존 Patch

- 상위 추적 이슈: [#153](https://github.com/Flamingo7562/KB-PJT-24-2/issues/153)
- Patch 이슈: [#220](https://github.com/Flamingo7562/KB-PJT-24-2/issues/220)
- 선행 초대 생명주기 Patch: [#218](https://github.com/Flamingo7562/KB-PJT-24-2/issues/218),
  `SPEC-218-01`
- 후속 수락·전자동의 Aggregate Patch: [#221](https://github.com/Flamingo7562/KB-PJT-24-2/issues/221)
- Backend Work Case 구현: [#154](https://github.com/Flamingo7562/KB-PJT-24-2/issues/154)
- Frontend OWNER 근무 관리 연동: [#158](https://github.com/Flamingo7562/KB-PJT-24-2/issues/158)
- 적용 순서: `SPEC-218-01`에서 조건 변경·취소 시 초대 철회와 잠금 순서를 먼저 확정한 뒤
  `SPEC-220-01`을 적용한다. 응답 Shape의 나머지 부분은 별도로 검토할 수 있다.
