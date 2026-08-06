---
patch_id: SPEC-218-01
status: accepted
issue: 218
base_spec_version: 3.0.1
targets:
  - requirement: WORK-005
  - requirement: WORK-006
  - requirement: INVITE-001
  - requirement: INVITE-002
  - requirement: INVITE-003
  - requirement: INVITE-004
  - decision: DEC-INVITE-LIFECYCLE
  - decision: DEC-INVITE-LOGIN-BADGE
  - decision: DEC-INVITE-ACCEPT
  - decision: DEC-INVITE-ERROR-CATALOG
  - decision: DEC-OPEN-ERROR-CATALOG
  - rest_operation: "POST /api/work-cases/{workCaseId}/invitations"
  - rest_operation: "POST /api/work-cases/{workCaseId}/invitations/reissue"
  - rest_operation: "GET /api/invitations/{token}"
---

# SPEC-218-01: M4 초대 생명주기 계약 확정

## 추가 사항

미매칭 Work Case의 초대를 특정 WORKER에게 미리 귀속하지 않는 Bearer Link로 확정하고,
발급·동일 Link 재조회·재발급·만료·철회·조회까지의 관찰 가능한 생명주기를 하나의 최소
기능 단위로 제안한다.

정식 명세 3.0.1은 Token Hash 저장, 조건 Version 검증, Body 없는 수락과 OWNER Badge 노출
경계는 승인했지만 초대 대상, 만료 시각, 활성 초대가 있을 때의 발급 응답, 재발급 경로,
조회 DTO 전체 필드와 상태별 오류를 확정하지 않았다. 현재 스키마에는 사전 대상 WORKER
컬럼이 없고 `accepted_by_user_id`만 있으므로 Bearer 모델은 Flyway `202608061428`과
호환되며 Migration이 필요하지 않다.

이 Patch는 초대 Link 자체의 생명주기만 다룬다. 수락 멱등 Fingerprint, 전자동의 증거,
계약 문서, 에스크로·정산 Aggregate와 Work Case 목록·상세 응답 Shape는 각각 후속 Patch로
분리한다.

## 현재 명세와 문제

- `INVITE-002`와 `GET /api/invitations/{token}`은 “대상 WORKER” 검증을 요구하지만
  `work_invitations`에는 사전 대상 컬럼이 없다. 구현자가 Bearer와 지정형 중 하나를 추측해야
  한다.
- `INVITE-001`은 만료형 Link만 요구하며 TTL과 `expiresAt` 계산 기준을 정하지 않는다.
- `POST /api/work-cases/{workCaseId}/invitations`은 항상 201만 기술한다. 이미 유효한
  `PENDING` 초대가 있을 때 같은 Link를 돌려줄지, 충돌시킬지, 기존 Link를 철회할지 알 수
  없다.
- DB에 Token Hash만 저장하면 무작위 Token 원문을 다시 제공할 수 없다. 현재 계약만으로는
  클립보드 실패나 페이지 이탈 뒤 OWNER가 활성 Link를 복구할 수 없다.
- 기존 Link를 명시적으로 폐기하고 새 Link를 발급하는 Operation이 없다.
- 초대 조회 예시는 제목·시각·Badge만 포함해 사업장, 휴게, 일급, 조건 Version, 만료와
  Badge 부재 표현을 확정하지 않는다.
- `DEC-OPEN-ERROR-CATALOG` 때문에 만료·철회·수락 완료·조건 변경을 구현자가 공통
  `CONFLICT` 하나로 합치거나 임의의 도메인 Code로 나눌 수 있다.
- `WORK-006`은 초대 이력이 있는 DRAFT를 취소 상태로 전환하지만 남아 있는 `PENDING`
  초대의 철회를 명시하지 않는다.

## 전달 방식과 위험 판정

`spec_first`를 사용한다.

- 사전 지정 초대가 아닌 Bearer Link 의미, 재발급 Operation과 상태·오류 계약을 확정하는
  breaking 변경이다.
- Token Hash, 인증·역할, 만료·철회와 조건 Version은 보안·인가 경계이며 구현이 먼저
  병합되면 승인되지 않은 Link 접근 의미를 강제할 수 있다.
- Backend 발급·조회·수락과 Frontend 로그인 복귀가 공유하고 SPEC-220/221이 이 생명주기에
  의존하므로 독립 구현 PR에서 일방적으로 바꾸기 어렵다.
- Flyway `202608061428` 기준으로 이 Patch 자체의 Migration·DDL은 없지만 정식 명세에
  적용되기 전 관련 구현을 `dev`에 병합하지 않는다.

## 제안할 최종 규범 문장 또는 Before/After

### 1. 요구사항

정식 명세의 `INVITE-001`~`INVITE-003`, `WORK-005`, `WORK-006`을 다음 의미로 편집하고
`INVITE-004`를 추가한다.

#### `INVITE-001` 교체

> 해당 OWNER는 `DRAFT`이고 배정 WORKER가 없으며 근무 시작 전인 Work Case에 Bearer
> 초대 Link를 발급한다. 초대는 특정 WORKER에게 미리 귀속하지 않고, 유효한 Link를 가진
> 인증 WORKER 중 수락 Aggregate를 먼저 성공시킨 사용자가 당사자가 된다. 초대의
> `expiresAt`은 Work Case의 `startsAt`과 같으며 `now >= expiresAt`이면 사용할 수 없다.
> Token 원문과 HMAC Secret은 DB에 저장하지 않고 Token의 SHA-256 Hash만 저장한다.

#### `INVITE-002` 교체

> 비로그인 사용자가 초대 웹 경로에 접근하면 `/worker/login?redirect={encodedInvitationPath}`로 보내고
> 로그인 성공 뒤 원래 경로로 복귀한다. API는 비인증 요청에 `401 AUTH_REQUIRED`, WORKER가
> 아닌 인증 사용자에게 `403 ROLE_MISMATCH`를 반환한다. 로그인 뒤에는 특정 사전 대상이
> 아니라 인증 사용자의 WORKER 역할과 Token의 상태·만료·조건 Version을 다시 검증한다.

#### `INVITE-003` 교체

> 유효한 `PENDING` 초대를 가진 인증 WORKER에게 제목, 사업장명, 시작·종료 시각, 휴게,
> 일급, 조건 Version, 만료 시각과 초대한 OWNER의 Badge를 읽기 전용으로 반환한다. 활성
> OWNER Badge가 없으면 `ownerBadge`는 `null`이다. 인증 전에는 초대 내용과 Badge를
> 노출하지 않는다.

#### `INVITE-004` 추가

> 최초 발급 뒤 같은 조건 Version의 유효한 `PENDING` 초대가 있으면 발급 Operation은 새
> 행을 만들지 않고 같은 Link와 만료 시각을 반환한다. OWNER가 재발급 Operation을 호출하면
> 기존 `PENDING` 초대를 `REVOKED`로 바꾸고 새 `PENDING` 초대를 하나 만드는 처리를 원자적으로
> 수행한다. 조건 변경과 초대 이력이 있는 Work Case 취소도 활성 `PENDING` 초대를 같은
> 트랜잭션에서 철회한다. 별도 거절 Operation은 제공하지 않으며 승인 API는 `REJECTED`
> 상태를 만들지 않는다.

#### `WORK-005` 수용 기준 보강

> 조건 변경은 현재 조건 Version의 `PENDING` 초대를 `REVOKED`로 전이하고
> `work_cases.terms_version`을 증가시키는 처리를 같은 트랜잭션으로 수행한다.

#### `WORK-006` 수용 기준 보강

> 초대 이력이 있어 `DRAFT`를 `CANCELED`로 전이할 때 활성 `PENDING` 초대도 같은
> 트랜잭션에서 `REVOKED`로 전이한다. 초대 이력이 없는 DRAFT만 Hard Delete할 수 있다.

### 2. 승인 결정

`DEC-INVITE-LIFECYCLE`을 Approved 결정으로 추가한다.

> 초대는 사전 지정 WORKER가 없는 Bearer Link다. 만료 시각은 Work Case의 `startsAt`이며
> API와 DB의 시간 변환은 `DEC-TIME`을 따른다. 외부 설정의 HMAC-SHA-256 Secret과 고유한
> 초대 ID로 32byte Token을 파생하고 Base64url 무패딩 문자열로 노출하며 DB에는 Token의
> SHA-256 Hash만 저장한다. 활성 초대를 다시 요청하면 저장된 Hash와 일치하는 HMAC Key로
> 같은 Token을 재현해 200으로 반환한다. HMAC Key를 교체할 때는 활성 초대가 사용하는 이전
> Key를 해당 초대가 모두 종료될 때까지 검증 Key 집합에 유지한다. 명시적 재발급은 기존
> `PENDING`을 원자적으로 철회한 뒤 새 초대를 생성한다. 재발급은 멱등 Replay를 제공하지
> 않으며 잠금을 획득한 요청마다 한 번씩 Link를 교체한다. 성공 응답을 받았는지 불명확하면
> 재발급을 자동 재시도하지 않고 일반 발급 Operation으로 현재 활성 Link를 다시 조회한다.

`DEC-INVITE-LOGIN-BADGE`를 다음 의미로 보강한다.

> 비로그인 초대 웹 접근은 WORKER 로그인으로 연결하고 원래 경로로 복귀한다. 인증 뒤 Bearer
> Token을 검증한 WORKER에게만 근무 조건과 초대한 OWNER의 Badge를 노출하며 Badge가 없으면
> `ownerBadge: null`을 반환한다.

`DEC-INVITE-ACCEPT`의 당사자 문장을 다음과 같이 명확히 한다.

> 초대 수락은 `/api/invitations/{token}/accept`의 Body 없는 POST이며 서버가 유효한 Bearer
> Token과 인증 WORKER로 당사자를 결정하고 Token으로 Work Case, 조건 Version과 금액을
> 도출한다. 수락 Aggregate의 나머지 원자성·멱등·전자동의 규칙은 별도 Patch에서 확정한다.

`DEC-INVITE-ERROR-CATALOG`을 Approved 결정으로 추가한다.

> 형식이 잘못됐거나 존재하지 않는 Token은 `404 RESOURCE_NOT_FOUND`, 만료된 초대는
> `410 INVITATION_EXPIRED`, 철회된 초대는 `409 INVITATION_REVOKED`, 이미 수락된 초대는
> `409 INVITATION_ALREADY_ACCEPTED`, 현재 조건 Version과 다른 `PENDING` 초대는
> `409 INVITATION_TERMS_CHANGED`다. 역할 불일치는 `403 ROLE_MISMATCH`, 발급할 수 없는
> Work Case 상태는 `409 WORK_CASE_LOCKED`, 그 밖의 발급·재발급 상태 충돌은
> `409 CONFLICT`를 사용한다.

`DEC-OPEN-ERROR-CATALOG`의 질문과 승인 경계에서는 “초대”를 제거한다. QR, 근태, 문서와
정산의 세부 오류 Catalog는 Open으로 유지한다.

### 3. REST API

#### `POST /api/work-cases/{workCaseId}/invitations`

- 해당 Work Case의 OWNER만 호출하며 Body는 없다.
- Work Case가 `DRAFT`, `workerId=null`, `now < startsAt`일 때만 발급할 수 있다.
- Work Case와 활성 초대를 순서대로 잠가 동시에 여러 요청이 와도 활성 `PENDING` 행은
  하나만 남긴다.
- 잠근 `PENDING` 초대가 `now >= expiresAt`이면 같은 트랜잭션에서 먼저 `EXPIRED`로 전이한
  뒤 신규 발급 여부를 판단한다.
- 현재 조건 Version의 유효한 `PENDING` 초대가 없으면 새 초대를 만들고 201을 반환한다.
- 현재 조건 Version의 유효한 `PENDING` 초대가 있으면 새 행을 만들거나 만료를 늘리지 않고
  같은 응답을 200으로 반환한다.
- `inviteUrl`은 요청의 `Host`나 `Origin`에서 만들지 않는다. 배포 설정으로 주입한 허용 Web
  Origin과 `/invitations/{token}`을 결합한 Query·Fragment 없는 절대 URL이다.

```json
{
  "data": {
    "inviteUrl": "https://app.example.com/invitations/BASE64URL_TOKEN",
    "expiresAt": "2026-08-20T01:00:00Z"
  }
}
```

#### `POST /api/work-cases/{workCaseId}/invitations/reissue`

- 해당 Work Case의 OWNER만 호출하며 Body는 없다.
- 현재 조건 Version의 유효한 `PENDING` 초대가 있어야 한다. 없으면 `409 CONFLICT`다.
- Work Case와 현재 초대를 잠근 뒤 기존 초대를 `REVOKED`로 전이하고 다른 초대 ID와 Token을
  가진 새 `PENDING` 행을 만드는 처리를 한 트랜잭션으로 수행한다.
- 새 Link와 `expiresAt`을 발급 Operation과 같은 Envelope로 반환하며 성공은 201이다.
- 재발급 전에 전달된 Link는 즉시 `409 INVITATION_REVOKED`가 된다.
- 재발급에는 `Idempotency-Key` Replay를 적용하지 않는다. 동시 요청은 잠금으로 직렬화되어
  요청마다 Link를 교체하며 마지막 요청이 만든 Link만 유효하다. 성공 응답이 유실된
  클라이언트는 이 Operation을 자동 재시도하지 않고 일반 발급 Operation으로 현재 Link를
  복구한다.

#### `GET /api/invitations/{token}`

- 인증된 WORKER만 호출한다. 초대는 Bearer이므로 별도의 사전 대상 사용자 ID를 검사하지
  않는다.
- Token Hash로 초대를 찾고 상태, `now < expiresAt`, 현재 `termsVersion` 일치와 Work Case의
  수락 가능 상태를 검증한 뒤에만 내용을 반환한다.
- `now >= expiresAt`인 `PENDING` 행은 조회 시 `EXPIRED`로 전이한 뒤 같은 요청에
  `410 INVITATION_EXPIRED`를 반환한다. 별도 Scheduler는 이 Patch의 요구사항이 아니다.
- 시각은 UTC `Instant`, 금액은 KRW 원 단위 정수다.

```json
{
  "data": {
    "title": "주말 홀 서빙",
    "workplaceName": "강남점",
    "startsAt": "2026-08-20T01:00:00Z",
    "endsAt": "2026-08-20T09:00:00Z",
    "breakMinutes": 60,
    "breakPaid": false,
    "dailyWage": 120000,
    "termsVersion": 3,
    "expiresAt": "2026-08-20T01:00:00Z",
    "ownerBadge": {
      "badgeType": "TRUST_OWNER",
      "level": 2
    }
  }
}
```

활성 OWNER Badge가 없으면 다음과 같이 반환한다.

```json
{
  "data": {
    "title": "주말 홀 서빙",
    "workplaceName": "강남점",
    "startsAt": "2026-08-20T01:00:00Z",
    "endsAt": "2026-08-20T09:00:00Z",
    "breakMinutes": 60,
    "breakPaid": false,
    "dailyWage": 120000,
    "termsVersion": 3,
    "expiresAt": "2026-08-20T01:00:00Z",
    "ownerBadge": null
  }
}
```

#### 초대 오류 응답

| 상황                                      | HTTP | Code                          | Message                                           |
| ----------------------------------------- | ---: | ----------------------------- | ------------------------------------------------- |
| Token 형식 오류 또는 미존재               |  404 | `RESOURCE_NOT_FOUND`          | `초대 링크를 찾을 수 없습니다.`                   |
| 인증 없음                                 |  401 | `AUTH_REQUIRED`               | 공통 인증 메시지                                  |
| WORKER 역할이 아님                        |  403 | `ROLE_MISMATCH`               | 공통 역할 불일치 메시지                           |
| 만료                                      |  410 | `INVITATION_EXPIRED`          | `초대 링크가 만료되었습니다.`                     |
| 철회                                      |  409 | `INVITATION_REVOKED`          | `철회된 초대 링크입니다.`                         |
| 수락 완료                                 |  409 | `INVITATION_ALREADY_ACCEPTED` | `이미 수락된 초대 링크입니다.`                    |
| `PENDING`이지만 조건 Version 불일치       |  409 | `INVITATION_TERMS_CHANGED`    | `근무 조건이 변경되어 초대를 사용할 수 없습니다.` |
| 발급할 수 없는 Work Case 상태·시각        |  409 | `WORK_CASE_LOCKED`            | `초대를 발급할 수 없는 근무입니다.`               |
| 활성 초대가 없는 재발급 등 기타 상태 충돌 |  409 | `CONFLICT`                    | `초대 상태를 다시 확인해 주세요.`                 |

오류 Body는 공통 `{code,message,traceId,fieldErrors?}` Envelope를 그대로 사용한다.

### 4. 추적성

`SPEC_TRACEABILITY.md`의 연결을 다음과 같이 정렬한다.

| 요구사항   | REST Operation                                          | 도메인·데이터                                          | 연결 결정                                            |
| ---------- | ------------------------------------------------------- | ------------------------------------------------------ | ---------------------------------------------------- |
| WORK-005   | `PATCH /api/work-cases/{workCaseId}`                    | `work_cases.terms_version`, `work_invitations.status`  | `DEC-INVITE-LIFECYCLE`                               |
| WORK-006   | `DELETE /api/work-cases/{workCaseId}`                   | `work_cases.status`, `work_invitations.status`         | `DEC-INVITE-LIFECYCLE`                               |
| INVITE-001 | `POST /api/work-cases/{workCaseId}/invitations`         | `work_invitations`, Token Hash, HMAC Key, 조건 Version | `DEC-INVITE-LIFECYCLE`, `DEC-INVITE-ERROR-CATALOG`   |
| INVITE-002 | `GET /api/invitations/{token}`                          | `work_invitations`, Session Redirect, Bearer Token     | `DEC-INVITE-LOGIN-BADGE`, `DEC-INVITE-LIFECYCLE`     |
| INVITE-003 | `GET /api/invitations/{token}`                          | `work_cases`, `work_invitations`, `user_badges`        | `DEC-INVITE-LOGIN-BADGE`, `DEC-INVITE-ERROR-CATALOG` |
| INVITE-004 | `POST /api/work-cases/{workCaseId}/invitations/reissue` | `work_invitations.status`, Token Hash, 조건 Version    | `DEC-INVITE-LIFECYCLE`, `DEC-INVITE-ERROR-CATALOG`   |

## 영향 분석

### 요구사항

- `INVITE-001`~`INVITE-003`의 미결 의미를 교체하고 `INVITE-004`를 추가한다.
- `WORK-005`, `WORK-006`에 활성 초대 철회 원자성을 보강한다.
- `CONTRACT-001`, `CONTRACT-002`는 수락 성공 시 Bearer WORKER가 당사자가 된다는 연결점만
  사용하며 수락 Aggregate의 내부 규칙은 바꾸지 않는다.

### API

- 기존 발급 Operation은 최초 201 외에 활성 Link 재조회 200을 추가하므로 외부 동작이
  확장·변경된다.
- 재발급 Operation 하나와 조회 응답 필드, 상태별 오류 Code 네 개를 추가한다.
- `POST /api/invitations/{token}/accept`의 Body, 멱등 Header, Fingerprint, 성공·Replay 응답은
  이 Patch에서 바꾸지 않는다.

### 데이터 및 Migration

- 현재 `work_invitations`의 `id`, `work_case_id`, `token_hash`, `status`,
  `expected_terms_version`, `expires_at`, 수락·철회 시각과 활성 슬롯으로 구현 가능하다.
- 사전 대상 WORKER, Token 원문, HMAC Key Version 또는 추가 상태 컬럼을 저장하지 않는다.
- 새 Flyway Migration, DDL, Backfill과 기존 Migration 수정은 없다.
- HMAC Key Ring과 Web Origin은 외부 설정이며 저장소에 Secret을 커밋하지 않는다.

### 보안

- Token은 인증을 대체하지 않는 Bearer Capability다. Link를 가진 WORKER에게만 로그인 뒤
  최소 근무 조건을 노출한다.
- Token 원문, HMAC Secret과 파생값은 DB, 애플리케이션·오류·감사·SQL 바인딩·분석 로그에
  기록하지 않는다. URL 전체를 로그할 때는 초대 경로의 Token segment를 마스킹한다.
- 초대 웹 화면은 Token이 외부 Referrer로 전파되지 않도록 `Referrer-Policy: no-referrer`를
  사용하고 인증 전에는 초대 내용을 렌더링하지 않는다.
- `inviteUrl` Origin은 요청 Header가 아니라 허용된 배포 설정에서만 만든다.
- 만료·철회·수락 상태의 구체 오류는 유효 Token 보유자에게만 알려지며 알 수 없는 Token은
  항상 404로 통일한다.

### Frontend

- OWNER는 최초 발급과 활성 Link 복사를 같은 UI에서 처리하고, 재발급은 기존 Link가 즉시
  폐기된다는 경고가 있는 별도 동작으로 노출한다.
- WORKER 로그인 Redirect는 원래 `/invitations/{token}` 경로를 보존한다.
- 초대 화면은 확정된 조회 DTO만 소비하고 `ownerBadge: null`을 Badge 없음으로 표시한다.
- 상태별 Code에 맞는 사용자 메시지를 표시하되 수락 Canvas 제거와 Body 없는 수락 전환은
  수락·전자동의 Patch 및 구현 이슈 #159 범위다.

### Backend

- Work Case를 먼저 잠그고 활성 초대를 다음으로 잠그는 순서를 발급·재발급·조건 수정·취소에
  공통 적용한다.
- 고유 초대 ID 기반 HMAC Token 재현과 SHA-256 Hash 조회를 구현한다.
- 만료된 `PENDING`은 조회·발급·재발급·수락의 진입 시점에 잠금 아래 `EXPIRED`로 지연
  전이하며 별도 Scheduler를 요구하지 않는다.
- 수락 성공의 `PENDING → ACCEPTED`는 수락 Aggregate 트랜잭션에서만 수행한다.

### 테스트

- 동일 조건의 순차·동시 발급이 같은 Link와 만료를 반환하고 활성 `PENDING` 행 하나만 남는지
  검증한다.
- 재발급이 기존 Link를 즉시 철회하고 새 Link만 유효하게 만드는지 검증한다.
- 동시 재발급이 직렬로 Link를 교체해 마지막 Link만 유효한지, 응답 유실 뒤 일반 발급으로
  현재 Link를 복구할 수 있는지 검증한다.
- 조건 변경·Work Case 취소와 초대 철회가 함께 Commit 또는 Rollback되는지 검증한다.
- `now == startsAt` 경계에서 신규 발급과 조회가 거부되고 `PENDING`이 `EXPIRED`가 되는지
  검증한다.
- 서로 다른 인증 WORKER가 같은 유효 Link를 조회할 수 있고 인증 전·OWNER 역할에서는
  내용을 노출하지 않는지 검증한다.
- 조회 DTO의 전체 필드, UTC 변환, `ownerBadge` 객체와 `null`을 계약 테스트로 고정한다.
- Token·Secret이 DB 원문, 응답 외 로그, 오류와 SQL 바인딩에 남지 않는지 검증한다.
- 네 가지 초대 도메인 Code와 공통 Code의 HTTP 매핑을 검증한다.

## 완료 조건

- [ ] Patch의 기준이 Spec `3.0.1`, `origin/dev` Commit
      `1ad5d6458361a8c5ec32afb53185e22ad475a016`, Flyway `202608061428`과 일치한다.
- [ ] 초대가 사전 지정형이 아닌 Bearer Link이며 추가 WORKER FK나 Migration을 요구하지 않는다.
- [ ] 최초 발급 201, 동일 활성 Link 재조회 200, 별도 재발급 201의 응답과 상태 전이가
      구분된다.
- [ ] 재발급 동시 호출은 직렬로 Link를 교체하고, 불명확한 성공 응답은 일반 발급으로 현재
      Link를 복구한다.
- [ ] `expiresAt == startsAt`이고 `now >= expiresAt`인 초대는 사용할 수 없다.
- [ ] Token 원문과 HMAC Secret을 저장하지 않으면서 활성 Link를 동일하게 재현할 수 있다.
- [ ] 조회 DTO의 필드·형식·`ownerBadge: null` 의미가 예시와 일치한다.
- [ ] 조건 변경·취소·재발급이 활성 초대를 원자적으로 `REVOKED`로 전이한다.
- [ ] 초대 오류 네 종류와 기존 공통 오류의 HTTP Status·Code·메시지가 고정된다.
- [ ] 수락 Aggregate, 전자동의, Work Case 응답 Shape, 구현 코드와 DDL이 Patch에 섞이지 않는다.
- [ ] `docs/specs/**`와 `SPEC_LOCK.json`은 이 Patch 브랜치에서 변경되지 않는다.

## 미결 사항

없음

## 관련 Issue·PR·의존 Patch

- 상위 추적 이슈: [#153](https://github.com/Flamingo7562/KB-PJT-24-2/issues/153)
- Patch 이슈: [#218](https://github.com/Flamingo7562/KB-PJT-24-2/issues/218)
- Backend 초대 구현: [#155](https://github.com/Flamingo7562/KB-PJT-24-2/issues/155)
- Frontend OWNER 초대 연동: [#158](https://github.com/Flamingo7562/KB-PJT-24-2/issues/158)
- Frontend WORKER 조회·수락 연동: [#159](https://github.com/Flamingo7562/KB-PJT-24-2/issues/159)
- 의존 Patch: 없음. 수락·전자동의 Patch는 Bearer 당사자와 초대 오류를 이 Patch에 의존해
  확장한다.
