---
patch_id: SPEC-221-01
author: flamingo7562
status: proposed
issue: 221
created_at: 2026-08-06
base_spec_version: 3.0.1
base_commit: "1ad5d6458361a8c5ec32afb53185e22ad475a016"
change_type: breaking
delivery_mode: spec_first
targets:
  - requirement: CONTRACT-001
  - requirement: CONTRACT-002
  - requirement: CONTRACT-003
  - requirement: SETTLE-001
  - requirement: DOC-002
  - requirement: DOC-004
  - requirement: DOC-009
  - requirement: DOC-010
  - requirement: DOC-011
  - decision: DEC-INVITATION-ACCEPTANCE-AGGREGATE
  - decision: DEC-IDEMPOTENCY
  - decision: DEC-IDEMPOTENCY-STORAGE
  - decision: DEC-IDEMPOTENCY-CLAIM-LIFECYCLE
  - decision: DEC-CONTRACT-AUTO-GENERATION
  - decision: DEC-E-SIGN-EVIDENCE
  - decision: DEC-CONTRACT-FILE-COMMIT
  - decision: DEC-DOCUMENT-STORAGE
  - decision: DEC-OPEN-E-SIGN-EVIDENCE
  - rest_operation: "POST /api/invitations/{token}/accept"
  - rest_operation: "GET /api/documents/{documentId}/file"
depends_on:
  - SPEC-218-01
  - SPEC-220-01
supersedes: null
superseded_by: null
applied_in_version: null
applied_by_pr: null
---

# SPEC-221-01: M4 수락·전자동의 Aggregate 계약 확정

## 변경 요약과 필요성

유효한 Bearer 초대를 인증 WORKER가 Body 없이 수락할 때 당사자 결정, 멱등 Claim, 동시 수락,
Work Case 매칭, 조건 Snapshot, 전자동의 증거, 근로계약 PDF, OWNER 임금 예치, 원장과 정산
예약까지의 성공·실패 경계를 하나의 최소 승인 단위로 제안한다.

정식 명세 3.0.1은 Body 없는 수락, Token 기반 당사자 도출, 성공 응답과 Aggregate 원자성을
요구하지만 수락 Fingerprint, 잠금·검증 순서, 동시 수락 패자, `TYPED_NAME` 증거, 문서
Version·소유·공유, 파일과 DB Commit 사이의 보상, Settlement 초기값을 닫지 않았다. 현재
Frontend는 Canvas 이미지를 Body로 보내고 `HOLD`를 소비하며, Backend의 임시 경로는
클라이언트가 보낸 사용자·근무·금액을 신뢰하고 계약·문서·정산을 만들지 않는다.

Flyway `202608061428`에는 사용자·Operation 범위의 `idempotency_requests`, Work Case당
하나의 계약·에스크로·정산, 문서 Version·서명·공유와 필요한 Checksum, 접근 감사의 문서
Version·거부 사유 컬럼이 모두 존재한다. 따라서 이 Patch는 그 승인된 Schema 기준선 위에서
추가 Migration이나 DDL 없이 적용할 수 있다. 초대의 Bearer 당사자·만료·오류는
`SPEC-218-01`, Work Case 조건·응답과 상태 경계는 `SPEC-220-01`을 먼저 적용한 의미에
의존한다.

## 현재 명세와 문제

- `DEC-IDEMPOTENCY-STORAGE`는 공통 Scope와 자금 요청 Fingerprint만 닫았다. Body 없는 초대
  수락에서 같은 Key와 다른 Token·조건 Version을 구별할 정규 입력이 없다.
- 성공 Aggregate와 Claim 완료가 다른 Commit이면 도메인은 성공했지만 Claim이
  `PROCESSING`으로 남아 같은 Key Replay가 성공 응답을 복구하지 못할 수 있다.
- `POST /api/invitations/{token}/accept`는 상태·조건·잔액 검증 순서와 동시 수락 승자·패자의
  오류를 정하지 않아 서로 다른 구현이 관찰 가능한 결과를 달리할 수 있다.
- `DEC-OPEN-E-SIGN-EVIDENCE`가 열려 있어 Body 없는 요청을 `document_signatures`의 어떤
  행으로 보존할지, OWNER와 WORKER 중 누가 서명자인지 알 수 없다.
- 계약서 `documents`의 작성자·소유자·공유 대상, ORIGINAL/SIGNED Version, PDF Key와
  Checksum이 정해지지 않았다.
- 외부 파일 저장은 DB Transaction에 참여하지 않는다. 파일만 남거나 DB만 Commit되는
  부분 실패에서 성공 응답, 재시도와 다운로드가 어떻게 동작하는지 결정되지 않았다.
- 수락 시 `settlements`를 만든다는 요구는 있으나 초기 `status`와 `due_at`이 닫히지 않았고,
  추적표는 존재하지 않는 `settlements.scheduled_at`을 가리킨다.
- `ESCROW_HOLD` 시 OWNER 지갑과 에스크로만 변해야 하는지 WORKER 지갑에도 원장을 만들지
  분명하지 않다.

## 전달 방식과 위험 판정

`spec_first`를 사용한다.

- 기존 Canvas Body와 Legacy 수락 경로를 제거하고 Body 없는 요청만 허용하는 breaking
  변경이다.
- Bearer Token, 인증·인가, 동의 증거, 문서 접근과 감사라는 보안·개인정보 경계를 닫는다.
- OWNER의 가용·잠금 잔액과 불변 원장을 바꾸는 자금 정합성 결정이다.
- 계약, 문서, 에스크로와 정산을 여러 구현 이슈가 공유하며 잘못 병합하면 되돌리기 어렵다.
- 승인된 Flyway `202608061428`을 기준으로 이 Patch PR 자체의 Migration·DDL·Backfill은
  없지만 저장 데이터의 의미와 트랜잭션 경계는 바뀐다.

Patch 전용 PR을 애플리케이션 코드, Flyway, DDL과 `docs/specs/**` 변경 없이 먼저 검토한다.
Controller가 `SPEC-218-01` → `SPEC-220-01` → `SPEC-221-01` 순서로 정식 명세에 적용하기 전
관련 구현 PR을 `dev`에 병합하지 않는다.

## 제안할 최종 규범 문장 또는 Before/After

### 1. 요구사항

#### `CONTRACT-001` 교체

> 유효한 Bearer 초대의 조건과 취소 제한을 확인한 인증 WORKER는 완전히 빈 Body의 수락
> 요청으로 최종 동의한다. 서버는 인증 사용자와 Token으로 당사자를 결정하고 수락 시점의
> 변경 불가능한 `users.name`을 `typed_name`, `TYPED_NAME`을 서명 방식으로 기록한다. 사용자,
> 근무, 금액, 이름, 전자서명 이미지나 파일을 Body로 받거나 신뢰하지 않는다. DRAWN 서명과
> 사용자 계약서 업로드는 M4 수락 경로에서 제공하지 않는다.

#### `CONTRACT-002` 교체

> 초대 수락은 멱등 Claim, 초대·Work Case 전이, 당사자 매칭, 계약 조건 Snapshot,
> 전자동의와 계약 문서 Metadata, OWNER 일급 예치, 에스크로·지갑 원장과 Settlement 예약을
> 하나의 DB Transaction으로 Commit한다. 가용 잔액 부족, 상태·조건 충돌, 임시 파일 저장 또는
> DB 처리 실패 시 도메인 변경과 성공 Claim을 남기지 않는다. 외부 파일은 결정적 임시 Key,
> DB Commit, 최종 Key 승격 순서와 Checksum Fallback으로 DB 원자성 밖의 실패를 보상한다.

#### `CONTRACT-003` 수용 기준 보강

> 서버는 `work_contracts` Snapshot으로 PDF ORIGINAL Version 1과 WORKER의 `TYPED_NAME`
> 증거가 포함된 SIGNED Version 2를 생성한다. OWNER와 WORKER는 같은 SIGNED Version 2의
> Bytes를 조회하며 사용자가 계약서 Version을 업로드·교체·삭제하지 못한다.

#### `DOC-002`, `DOC-004`, `DOC-009`~`DOC-011` 보강

> `EMPLOYMENT_CONTRACT` 문서는 수락 Aggregate가 한 건만 생성한다. 작성자와 소유자는 해당
> Work Case의 OWNER이고 WORKER는 `CONTRACT_PARTY` 공유를 받는다. 문서의 최종 상태는
> `ACTIVE`이며 파일 API는 양 당사자에게 같은 최신 SIGNED Version만 반환한다. 파일은
> 비공개 저장소에서 Checksum을 확인한 뒤 Stream하고 허용·거부 접근을 감사한다. 저장 Key,
> 임시 Key와 영구 공개 URL은 응답하지 않는다.

#### `SETTLE-001` 연결 보강

> 수락 시 정산 예약 행은 합의 일급으로 `WAITING`, `due_at=null`인 상태로 생성한다. 정상 퇴근
> 완료 뒤 M6 정산 흐름이 유예 시간을 적용해 `due_at`과 지급 가능한 상태를 결정한다. 수락
> 시점에는 지급 예정 시각을 추정하지 않는다.

### 2. 결정

#### `DEC-INVITATION-ACCEPTANCE-AGGREGATE` 추가

> `SPEC-218-01`이 보강한 `DEC-INVITE-ACCEPT`의 Bearer 당사자와 Body 없는 요청 경계를
> 유지한다. 새 수락 Claim의 본 처리는 Claim → Work Case → Invitation → OWNER Wallet
> 순서로 잠근다.
> 잠금 아래 초대 상태, 만료, 조건 Version, Work Case의 `DRAFT`·미매칭 상태, 자기 고용 금지와
> OWNER 가용 잔액을 순서대로 검증한다. 성공 시 Work Case와 Invitation을 `ACCEPTED`로
> 전이하고 계약·문서·예치·원장·Settlement와 성공 Claim을 한 Transaction으로 Commit한다.
> 서로 다른 Key나 WORKER의 동시 수락은 Work Case 잠금으로 직렬화되며 첫 Commit만
> 성공한다. 패자는 `INVITATION_ALREADY_ACCEPTED`를 받고 자금·계약·문서를 만들지 않는다.

#### `DEC-IDEMPOTENCY-STORAGE` 수락 규칙 보강

> 수락의 `operation_code`는 `INVITATION_ACCEPT`다. Fingerprint는 Token Hash의 소문자
> 64자리 Hex와 초대의 `expected_terms_version` 10진수를 사용해
> `SHA-256(UTF-8("INVITATION_ACCEPT\\n" + tokenHashHex + "\\n" + termsVersion))`로 만들고
> 32byte 결과를 저장한다. Scope는 공통 `(인증 사용자, Operation, Idempotency-Key)`를
> 그대로 사용한다. 같은 사용자·Key의 다른 유효 Token 또는 조건 Version은
> `IDEMPOTENCY_KEY_REUSED`이고, 다른 WORKER는 별도 Scope에서 경쟁한다. Token 원문과
> Fingerprint 입력은 로그에 기록하지 않는다.

#### `DEC-IDEMPOTENCY-CLAIM-LIFECYCLE` 수락 규칙 보강

> 구조·인증·역할·빈 Body·Key 형식과 Token 존재 검증 뒤 짧은 Transaction에서
> `PROCESSING` Claim을 선점한다. 같은 Fingerprint의 `COMPLETED`는 초대·조건·잔액·파일의
> 현재 상태를 다시 검증하지 않고 저장한 200 Body를 Replay한다. 같은 Fingerprint의
> `PROCESSING`은 본 처리 잠금을 기다리지 않고 `409 CONFLICT`, 다른 Fingerprint는 상태와
> 무관하게 `409 IDEMPOTENCY_KEY_REUSED`다. 새 Claim의 성공 Body와 200 상태는 Aggregate
> Transaction의 마지막 DB 변경으로 `COMPLETED`에 기록해 도메인 성공과 원자 Commit한다.
> 성공 결과는 공통 24시간 보존을 따른다. 실패한 본 처리의 Claim은 삭제해 같은 Key의
> 교정·재시도를 허용하고, 프로세스 중단 Claim은 공통 만료 정리 규칙을 따른다.

#### `DEC-E-SIGN-EVIDENCE` 추가

> Body 없는 POST 자체가 WORKER의 최종 동의 의사다. 서버가 잠금 검증 뒤 하나의 수락 시각을
> 정하고 `document_signatures` 한 행에 WORKER를 signer, 당시 `users.name`을
> `typed_name`, `TYPED_NAME`을 `signature_method`, 같은 시각을 `consented_at`과
> `signed_at`으로 기록한다. `source_version_id`는 ORIGINAL Version 1,
> `signed_version_id`는 SIGNED Version 2를 가리키고 두 Checksum은 각 Version과 정확히
> 같다. OWNER는 문서 작성자·소유자이지만 별도 서명 행을 만들지 않는다. 이 결정으로
> `DEC-OPEN-E-SIGN-EVIDENCE`를 해소하고 Open 표에서 제거한다.

#### `DEC-CONTRACT-FILE-COMMIT` 추가

> ORIGINAL과 SIGNED PDF는 DB Commit 전에 비공개 저장소의 결정적 임시 Key에 쓴다.
> `document_versions.storage_key`에는 최종 Key를 기록하고 Commit 뒤 임시 Object를 최종
> Key로 멱등 승격한다. DB가 Rollback되면 임시 Object를 최선 노력으로 삭제하고 정리 작업이
> 고아 Object를 제거한다. DB Commit 뒤 승격이 실패해도 계약은 성립하고 수락은 200이다.
> 파일 조회는 최종 Object의 SHA-256을 먼저 검증하고, 없거나 불일치하면 같은 Version의
> 결정적 임시 Object를 검증해 Fallback한다. 둘 다 DB Checksum과 일치하지 않으면 파일을
> 반환하지 않고 `500 INTERNAL_ERROR`와 같은 `traceId`의 무결성 로그를 남긴다.

#### `DEC-DOCUMENT-STORAGE` 계약서 규칙 보강

> 계약 PDF는 서버 내장 Template 한 종으로 만들고 구현이 선택한 한글 Font를 PDF에
> Embed한다. Font는 OFL-1.1 또는 Apache-2.0 호환 자산만 사용하며 구현 PR에서 정확한
> 자산·Version·License를 `DEPENDENCY_SPECIFICATION.md`에 기록한다. 최종 Key는
> `contracts/{workCaseId}/{documentId}/v{versionNo}.pdf`, 임시 Key는
> `contracts/{workCaseId}/{documentId}/.pending/v{versionNo}.pdf`다. MIME은
> `application/pdf`, Checksum은 파일 Bytes의 SHA-256 32byte 값이다. 저장소 승격은 기존
> 최종 Object를 무조건 덮어쓰지 않고 DB Checksum이 같을 때만 완료로 간주한다.

### 3. REST API

#### `POST /api/invitations/{token}/accept`

요청 계약은 다음과 같다.

- 인증된 `WORKER`만 호출한다. 인증 없음은 `401 AUTH_REQUIRED`, 다른 역할은
  `403 ROLE_MISMATCH`다.
- CSRF와 `Idempotency-Key` 공통 규칙을 적용한다.
- HTTP Body는 0byte여야 한다. JSON `{}`, `null`, 공백, 이름, ID, 금액, 서명 이미지와
  Multipart를 포함한 모든 Body는 `400 VALIDATION_ERROR`다.
- Token은 `SPEC-218-01`의 Base64url 형식으로 해석하고 Hash로 상태와 무관한 초대 행을
  조회한다. 형식 오류·미존재는 Claim을 만들지 않고 `404 RESOURCE_NOT_FOUND`다.
- Token 원문, Session·CSRF 값, Idempotency-Key, 이름, 계약 Bytes와 저장 Key를 일반
  로그·분석 이벤트·오류에 남기지 않는다.

Claim과 Replay 순서는 다음과 같다.

1. HTTP 구조·인증·역할·Body·Key와 Token 존재를 검증한다.
2. 초대의 Token Hash와 `expected_terms_version`으로 Fingerprint를 만든다.
3. `INVITATION_ACCEPT` Claim을 선점한다.
4. 같은 Fingerprint의 완료 Claim이면 아래 성공 Body를 200과
   `Idempotency-Replayed: true`로 즉시 반환한다.
5. 새 Claim만 Aggregate Transaction에 진입한다.

Aggregate Transaction은 다음 순서로 처리한다. 검증 실패는 성공 Claim을 남기지 않으며,
만료 전이처럼 보존할 상태 변경이 있으면 그 변경과 Claim 삭제만 Commit한 뒤 오류를
반환한다.

1. Claim 행, Work Case, Invitation, OWNER Wallet을 순서대로 잠근다.
2. Token Hash와 조회한 Invitation·Work Case 관계가 잠금 뒤에도 같은지 확인하고 인증
   WORKER가 Work Case OWNER와 같으면 `403 FORBIDDEN`으로 거부한다.
3. Invitation 상태를 검사한다. `ACCEPTED`, `REVOKED`, `EXPIRED`는 각각
   `INVITATION_ALREADY_ACCEPTED`, `INVITATION_REVOKED`, `INVITATION_EXPIRED`다.
4. `PENDING`이지만 `now >= expires_at`이면 `EXPIRED`로 전이하고 410을 반환한다.
5. `expected_terms_version != work_cases.terms_version`이면
   `409 INVITATION_TERMS_CHANGED`다.
6. Work Case가 `DRAFT`가 아니거나 이미 WORKER가 있거나 근무 시작 시각이 지났으면
   `409 WORK_CASE_LOCKED`다. 기존 계약·문서·에스크로·Settlement가 일부만 존재하면
   외부 상태 충돌로 숨기지 않고 무결성 오류인 `500 INTERNAL_ERROR`로 처리한다.
7. OWNER KRW Wallet의 `available_balance < agreed_wage`이면 숫자 잔액을 노출하지 않고
   `409 CONFLICT`와 `사장님의 예치 가능 잔액이 부족하여 근무를 확정할 수 없습니다.`를
   반환한다.
8. 하나의 `acceptedAt`을 정하고 아래 DB Aggregate와 임시 PDF를 모두 만든다.
9. 저장할 성공 Body와 200 상태로 Claim을 `COMPLETED` 전이하고 Commit한다.
10. Commit 뒤 PDF 승격을 시도한다. 승격 실패는 성공 응답을 실패로 바꾸지 않는다.

성공 시 DB 변경은 다음과 같다.

- `work_cases`: `worker_id=Principal.userId`, `DRAFT → ACCEPTED`.
- `work_invitations`: `PENDING → ACCEPTED`, `accepted_by_user_id`,
  `accepted_terms_version`, `accepted_at` 기록.
- `work_contracts`: Work Case당 한 행. 당사자, 제목, 시각, 휴게·유급 여부, 사업장 이름·주소·
  좌표·반경, 일급, `source_terms_version`, `accepted_at`을 그대로 Snapshot.
- `work_contracts.terms_snapshot`: 아래 닫힌 JSON Shape를 저장. 시각은 UTC `Instant`, 금액은
  KRW 원 정수, 좌표는 원 Snapshot이 없으면 `null`.

```json
{
  "schemaVersion": 1,
  "termsVersion": 3,
  "title": "주말 홀 서빙",
  "startsAt": "2026-08-20T01:00:00Z",
  "endsAt": "2026-08-20T09:00:00Z",
  "breakMinutes": 60,
  "breakPaid": false,
  "workplaceName": "강남점",
  "workplaceAddress": "서울특별시 강남구 테헤란로 1 2층",
  "workplaceLatitude": 37.498,
  "workplaceLongitude": 127.027,
  "allowedRadiusMeters": 100,
  "dailyWage": 120000,
  "owner": {
    "userId": 7,
    "name": "김사장"
  },
  "worker": {
    "userId": 42,
    "name": "이알바"
  }
}
```

- `documents`: Work Case당 `EMPLOYMENT_CONTRACT` 한 행. `created_by_user_id`와
  `owner_user_id`는 OWNER, `issued_on`은 `acceptedAt`의 `Asia/Seoul` 날짜,
  `expires_on=null`. Version 1 준비 시 `AWAITING_SIGNATURE`, Version 2·서명·공유 준비 시
  `SIGNED`, 최종적으로 `ACTIVE`로 전이하며 같은 Transaction 안의 중간 상태는 외부에
  노출하지 않는다.
- `document_versions`: `version_no=1`, `version_type=ORIGINAL`과 `version_no=2`,
  `version_type=SIGNED` 두 PDF 행. 최종 Key, MIME, byte 크기와 SHA-256을 기록.
- `document_signatures`: WORKER의 `TYPED_NAME` 한 행. Source/Signed Version과 각 Checksum,
  이름과 동일한 수락 시각을 기록.
- `document_shares`: WORKER에게 `purpose=CONTRACT_PARTY`, `status=ACTIVE`,
  `expires_at=null`인 한 행.
- OWNER Wallet: `available_balance -= agreed_wage`, `locked_balance += agreed_wage`.
- `escrows`: Work Case당 `amount=agreed_wage`, `status=HELD`, `held_at=acceptedAt` 한 행.
- `wallet_transactions`: OWNER Wallet에 `ESCROW_HOLD` 한 행. 잔액 전후 Snapshot,
  `reference_type=ESCROW`, `reference_id=escrow.id`를 기록하고 내부 `idempotency_key`는 Raw
  Header가 아니라 `EHLD:`와
  `SHA-256(UTF-8("INVITATION_ACCEPT\\n" + decimalClaimId))`의 소문자 Hex를 결합한 전역
  고유값을 사용한다.
- `settlements`: Work Case당 `amount=agreed_wage`, `status=WAITING`, `due_at=null`,
  승인·처리·완료·실패 필드는 `null`인 한 행.
- WORKER Wallet과 WORKER의 `wallet_transactions`는 수락 시 바꾸지 않는다. 실제 입금
  원장은 M6 에스크로 해제 시 생성한다.

최초 성공은 200이며 `Idempotency-Replayed` Header를 생략한다.

```json
{
  "data": {
    "workCaseId": 123,
    "escrowStatus": "HELD"
  }
}
```

보존 기간 안의 같은 Key·Fingerprint Replay는 정확히 같은 Body와 200을 반환하고
`Idempotency-Replayed: true`를 설정한다. 새 계약·문서·예치·원장·Settlement를 만들거나
현재 Token 상태, 조건, 잔액과 파일 승격 상태를 다시 검사하지 않는다.

#### 수락 오류 응답

`SPEC-218-01`의 초대 오류와 공통 오류를 다음처럼 조합하며 이 Patch는 새 오류 Code를
추가하지 않는다.

| 상황                                               | HTTP | Code                          |
| -------------------------------------------------- | ---: | ----------------------------- |
| Token 형식 오류·미존재                             |  404 | `RESOURCE_NOT_FOUND`          |
| 인증 없음                                          |  401 | `AUTH_REQUIRED`               |
| WORKER 역할이 아님                                 |  403 | `ROLE_MISMATCH`               |
| Work Case OWNER와 같은 사용자                      |  403 | `FORBIDDEN`                   |
| 같은 Key와 다른 Token·조건 Version                 |  409 | `IDEMPOTENCY_KEY_REUSED`      |
| 같은 Key·Fingerprint의 처리가 진행 중              |  409 | `CONFLICT`                    |
| 만료                                               |  410 | `INVITATION_EXPIRED`          |
| 철회                                               |  409 | `INVITATION_REVOKED`          |
| 이미 수락 또는 동시 수락 패배                      |  409 | `INVITATION_ALREADY_ACCEPTED` |
| 초대와 현재 조건 Version 불일치                    |  409 | `INVITATION_TERMS_CHANGED`    |
| 수락할 수 없는 Work Case 상태·매칭·시각            |  409 | `WORK_CASE_LOCKED`            |
| OWNER 예치 가능 잔액 부족                          |  409 | `CONFLICT`                    |
| 부분 Aggregate, 파일 무결성 또는 예상 밖 서버 오류 |  500 | `INTERNAL_ERROR`              |

동시 수락·잠금 교착이나 일시 충돌로 본 Transaction이 Commit되지 않았으면 Claim을 삭제하고
승인된 `409 CONFLICT`로 반환해 같은 Key 재시도를 허용한다. Commit 여부를 확정할 수 없을 때
클라이언트는 Key를 바꾸지 않고 Replay한다.

#### `GET /api/documents/{documentId}/file`

`EMPLOYMENT_CONTRACT`에 한해 다음 규칙을 추가한다. 일반 문서 목록·Metadata Shape는
`DEC-OPEN-DOCUMENT-RESPONSE-SHAPES`로 남긴다.

- 최초 수락과 Replay 응답은 승인된 최소 Shape를 유지한다. 클라이언트는 응답의
  `workCaseId`로 `SPEC-220-01`의 Work Case 상세를 다시 조회하고
  `contract.documentId`를 얻어 이 Endpoint를 호출한다.
- Work Case의 OWNER 또는 WORKER 당사자만 호출한다.
- `mode=view|download`를 지원하고 두 모드 모두 최신 SIGNED Version 2의 같은 Bytes를
  반환한다. ORIGINAL Version을 일반 사용자에게 반환하지 않는다.
- 최종 Object가 DB Checksum과 일치하면 사용한다. 없거나 불일치하면 결정적 `.pending`
  Object를 검사해 일치하는 Bytes를 반환하고 최종 승격을 재시도한다.
- 둘 다 일치하지 않으면 Stream하지 않고 `500 INTERNAL_ERROR`를 반환한다.
- 인증 뒤 기존 문서 행을 식별한 요청은 접근 결정마다 `document_access_logs` 한 행을 남긴다.
  `actor_user_id`는 인증 사용자, `document_id`는 경로의 문서, `action`은 mode에 따라
  `CONTRACT_FILE_VIEW` 또는 `CONTRACT_FILE_DOWNLOAD`, `result`는 `ALLOWED` 또는
  `DENIED`, 시각은 서버 `created_at`이다.
- `ALLOWED`는 반환할 SIGNED Version 2의 `document_version_id`를 필수로 기록하고
  `denial_reason`은 `null`이다. 감사 행 Commit에 실패하면 응답 Header나 파일 Bytes를
  보내지 않고 `500 INTERNAL_ERROR`와 같은 `traceId`의 보안 로그를 남긴다.
- 문서 식별 뒤 `DENIED`는 SIGNED Version을 찾았으면 그 `document_version_id`를 기록하고,
  Version을 확정할 수 없을 때만 `null`이다. `denial_reason`은
  `PARTY_ACCESS_DENIED`, `DOCUMENT_UNAVAILABLE`, `SIGNED_VERSION_UNAVAILABLE`,
  `FILE_UNAVAILABLE`, `CHECKSUM_MISMATCH` 중 하나를 반드시 기록한다.
- 인증·Query 형식 검증에서 문서를 조회하기 전에 거부했거나 문서 행 자체가 없으면 FK를
  만족하는 가짜 감사 행을 만들지 않는다. 이 경우 `document_access_logs` 대신 Token, 저장
  Key와 파일 내용을 제외한 같은 `traceId`의 보안 로그를 남긴다.

### 4. 추적성

`SPEC_TRACEABILITY.md`를 다음 연결로 정렬한다. `WORK-005`의
`work_cases.condition_version` 정정은 `SPEC-220-01`이 소유하므로 이 Patch에서 중복하지
않는다.

| 요구사항     | REST Operation                                                                 | 도메인·데이터                                                                                  | 연결 결정                                                                                                 |
| ------------ | ------------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------- |
| CONTRACT-001 | `POST /api/invitations/{token}/accept`                                         | `idempotency_requests`, `work_invitations`, `document_signatures`                              | `DEC-INVITE-ACCEPT`, `DEC-IDEMPOTENCY-STORAGE`, `DEC-E-SIGN-EVIDENCE`                                     |
| CONTRACT-002 | `POST /api/invitations/{token}/accept`                                         | `work_cases`, `work_contracts`, `wallets`, `escrows`, `wallet_transactions`, `settlements`     | `DEC-INVITATION-ACCEPTANCE-AGGREGATE`, `DEC-IDEMPOTENCY-CLAIM-LIFECYCLE`                                  |
| CONTRACT-003 | `POST /api/invitations/{token}/accept`, `GET /api/documents/{documentId}/file` | `documents`, `document_versions`, `document_signatures`, `document_shares`, 비공개 파일 저장소 | `DEC-CONTRACT-AUTO-GENERATION`, `DEC-E-SIGN-EVIDENCE`, `DEC-CONTRACT-FILE-COMMIT`, `DEC-DOCUMENT-STORAGE` |
| SETTLE-001   | HTTP 없음 — 정상 퇴근 뒤 정산 예정 시각 결정                                   | `settlements.due_at`, `work_cases.status`                                                      | `DEC-INVITATION-ACCEPTANCE-AGGREGATE`, `DEC-OPEN-CHECK-OUT-MISSING-FLOW`                                  |
| DOC-002      | `POST /api/invitations/{token}/accept`, `GET /api/documents/{documentId}/file` | `work_contracts`, `documents`, `document_versions`                                             | `DEC-CONTRACT-AUTO-GENERATION`, `DEC-CONTRACT-FILE-COMMIT`                                                |
| DOC-004      | `GET /api/documents/{documentId}/file`                                         | `work_contracts`, `document_versions`, `document_signatures`                                   | `DEC-E-SIGN-EVIDENCE`, `DEC-CONTRACT-FILE-COMMIT`                                                         |
| DOC-009      | `GET /api/documents/{documentId}/file`                                         | `document_versions`, `document_shares`, `document_access_logs`, Checksum Fallback              | `DEC-DOCUMENT-STORAGE`, `DEC-CONTRACT-FILE-COMMIT`                                                        |
| DOC-010      | `POST /api/invitations/{token}/accept`                                         | PDF 형식·Checksum 검증, `documents`, `document_versions`                                       | `DEC-CONTRACT-AUTO-GENERATION`, `DEC-DOCUMENT-STORAGE`, `DEC-CONTRACT-FILE-COMMIT`                        |
| DOC-011      | `GET /api/documents/{documentId}/file`                                         | `document_access_logs.document_version_id`, `action`, `result`, `denial_reason`, `created_at`  | `DEC-DOCUMENT-STORAGE`, `DEC-CONTRACT-FILE-COMMIT`                                                        |

## 영향 분석

### 요구사항

- `CONTRACT-001`~`CONTRACT-003`의 동의·원자성·자동 계약서 기준을 닫힌 계약으로 고정한다.
- `DOC-002`, `DOC-004`, `DOC-009`~`DOC-011`에 계약서만의 Version·접근·감사 규칙을
  보강한다.
- `SETTLE-001`은 수락 시 `WAITING/due_at=null`과 정상 퇴근 뒤 예정 시각 결정을 분리한다.
- 초대 발급·조회·재발급과 Work Case 응답 Shape는 선행 Patch 의미를 사용하고 중복 제안하지
  않는다.

### API

- Legacy `/api/invites/{token}/accept`와 클라이언트 ID·금액·서명 Body를 제거하고 승인 경로의
  빈 Body만 허용한다.
- 성공·Replay Body, Header, Claim Scope·Fingerprint와 상태별 오류를 고정한다.
- 계약 파일 Endpoint는 `EMPLOYMENT_CONTRACT`의 최신 SIGNED Version과 Fallback만
  보강하며 일반 문서 응답 Shape는 열어 둔다.

### 데이터 및 Migration

- `idempotency_requests`, `work_cases`, `work_invitations`, `work_contracts`, `wallets`,
  `wallet_transactions`, `escrows`, `settlements`, `documents`, `document_versions`,
  `document_signatures`, `document_shares`, `document_access_logs`의 Flyway `202608061428`
  컬럼·FK·UNIQUE·CHECK를 사용한다.
- Work Case당 계약·에스크로·정산·EMPLOYMENT_CONTRACT 하나와 문서 Version·서명 UNIQUE를
  최후 방어선으로 사용한다.
- 접근 감사의 `(document_id, document_version_id)` 복합 FK로 Version이 같은 문서에
  속하는지 검증하고, `denial_reason` CHECK와 애플리케이션의 닫힌 Code 목록을 함께 적용한다.
- 기존 감사 행은 Version·거부 사유를 안전하게 복원할 수 없어 두 상세 컬럼의 `null`을
  유지한다. 호환 Backend는 이 Patch 적용 뒤 생성하는 새 접근 결정부터 위 규칙을 지킨다.
- `settlements.scheduled_at`은 존재하지 않으므로 추적성을 실제 `due_at`으로 정정한다.
- 이 Patch PR에는 추가 Flyway Migration, DDL, Backfill, 기존 Migration 수정과 통합 Schema
  변경이 없다.

### 보안

- Bearer Token은 인증을 대체하지 않는다. WORKER Session, CSRF, Role과 Token을 모두
  검증한다.
- Token·Session·CSRF·Key·PDF·서명 증거·저장 Key·Checksum 입력을 일반 로그나 응답에
  노출하지 않는다.
- 이름은 사용자 입력 Body가 아니라 변경 불가능한 서버 Profile을 수락 시점 Snapshot으로
  기록한다.
- 계약 파일은 당사자에게만 Checksum 검증 뒤 제공하고 모든 허용·거부 접근을 감사한다.

### Frontend

- Canvas, `signatureImage`와 `HOLD` 별칭을 제거하고 `HELD`만 소비한다.
- 근무 조건·취소 제한 동의 확인은 화면의 명시적 Checkbox나 확인 단계로 남기되 요청 Body에
  값을 보내지 않는다. CTA는 `동의하고 근무 확정`처럼 실제 행위를 드러낸다.
- 한 번의 사용자 수락 의도와 네트워크 결과가 불확실한 Replay에는 같은 Key를 유지한다.
- 수락 성공 뒤 Work Case 상세를 다시 조회하고 `contract.documentId`로 계약 파일을 연다.
  일반 문서 목록의 미승인 필드에 의존하지 않는다.
- 상태·역할·멱등·잔액 오류를 Code로 분기하고 Token·Key·파일 내용을 Console과 Analytics에
  남기지 않는다.

### Backend

- Principal과 Token으로 Aggregate Command를 구성하고 Legacy Request DTO와 직접 에스크로
  제품 경로를 제거한다.
- 공통 Claim 저장소를 사용하고 Claim 완료를 수락 Aggregate와 같은 Transaction으로 묶는다.
- Work Case → Invitation → OWNER Wallet 잠금 순서와 조건부 상태·잔액 갱신을 적용한다.
- 계약 Snapshot, PDF Adapter, 문서 Metadata, Checksum 승격·Fallback과 고아 임시 파일 정리를
  구현한다.
- 기존 Escrow Service의 잔액 계산과 원장 검증은 재사용할 수 있지만 독립 Commit을 만들거나
  클라이언트 ID·금액을 다시 신뢰해서는 안 된다.

### 테스트

- 인증·Role·CSRF·0byte Body·Key 형식·Token 형식과 금지 Payload를 Controller 계약 테스트로
  검증한다.
- 같은 Key Replay, 같은 Key 다른 Token·Version, 다른 WORKER의 같은 Key 문자열과
  `PROCESSING` 충돌을 검증한다.
- 동일 Token의 순차·동시 수락 100건에서 한 요청만 성공하고 나머지는 승인 오류이며 Aggregate
  행과 자금 반영이 한 번인지 Disposable MySQL에서 검증한다.
- 조건 수정·취소·재발급과 수락 교차, 만료 경계 `now == expiresAt`, Work Case 잠금 순서와
  동시 잔액 갱신을 검증한다.
- 성공 Aggregate의 당사자·Version·금액·시각, OWNER Wallet 전후 합계, 에스크로·원장·정산
  초기값과 WORKER Wallet 불변을 대사한다.
- ORIGINAL/SIGNED Bytes·Checksum·서명·공유가 일치하고 OWNER와 WORKER가 같은 SIGNED
  Bytes를 받는지 검증한다.
- view·download 허용마다 정확한 action, SIGNED Version, `ALLOWED`, `denial_reason=null`의
  감사 행이 하나씩 Commit된 뒤 Stream되는지 검증한다.
- 당사자 아님·문서 상태·Version·파일·Checksum 거부가 닫힌 `denial_reason`과 `DENIED`로
  기록되고, Version 존재 여부에 따른 `document_version_id` 규칙을 검증한다.
- 인증·Query·미존재 문서처럼 문서 식별 전 실패에는 가짜 FK 감사 행이 없고 `traceId` 보안
  로그만 남는지, 기존 감사 행의 nullable 상세가 읽기 호환되는지 검증한다.
- 임시 파일 쓰기, 각 DB 변경, Commit 직전, Commit 뒤 승격에 실패를 주입해 Rollback·Claim
  삭제·200 Replay·Fallback·고아 정리를 검증한다.
- Token, Key, 이름·파일·저장 Key가 응답과 일반 로그에 남지 않는지 검증한다.

## 검증 가능한 수용 조건

- [ ] Patch 기준이 Spec `3.0.1`, `origin/dev` Commit
      `1ad5d6458361a8c5ec32afb53185e22ad475a016`, Flyway `202608061428`과 일치한다.
- [ ] `SPEC-218-01`, `SPEC-220-01` 의존성과 적용 순서를 기록하고 범위를 중복하지 않는다.
- [ ] 인증·역할·빈 Body·Token·Claim·잠금·상태·조건·잔액 검증 순서가 고정된다.
- [ ] 수락 Fingerprint와 같은 Key Replay·재사용·처리 중·보존 만료 결과를 구현자가 추정하지
      않는다.
- [ ] 같은 Token의 동시 수락에서 첫 Commit만 성공하고 패자는 계약·자금·문서를 만들지 않는다.
- [ ] 한 `acceptedAt`과 변경 불가능한 이름으로 WORKER의 `TYPED_NAME` 서명 한 행을 남긴다.
- [ ] 계약 Snapshot JSON, ORIGINAL/SIGNED Version, 문서 소유·공유·상태와 파일 Key·Checksum이
      고정된다.
- [ ] 매칭·초대·계약·문서 Metadata·OWNER 예치·에스크로·원장·정산·성공 Claim이 하나의 DB
      Commit에 포함된다.
- [ ] 파일 저장·DB Rollback·Commit 뒤 승격 실패·다운로드 Fallback과 고아 정리가 결정된다.
- [ ] Settlement는 `WAITING`, `due_at=null`이며 추적표가 실제 `settlements.due_at`을 가리킨다.
- [ ] 수락 시 OWNER의 `ESCROW_HOLD`만 기록하고 WORKER 지갑·원장은 바뀌지 않는다.
- [ ] 이 Patch PR의 추가 Migration·DDL 없이 Flyway `202608061428`의 FK·UNIQUE·CHECK로
      구현 가능하고 기존 감사 행의 nullable 상세를 Backfill하지 않는다.
- [ ] Frontend Canvas와 서명 Body 제거, `HELD` 소비와 같은 Key Replay 영향이 명시된다.
- [ ] 수락 성공 뒤 `workCaseId`로 상세를 재조회해 `contract.documentId`를 얻는 파일 발견
      경로가 고정된다.
- [ ] 계약 파일 view·download의 허용·거부 감사에 사용자, 문서, Version, action, result,
      시각, 닫힌 거부 사유와 문서 식별 전 실패의 보안 로그 경계가 고정된다.
- [ ] 애플리케이션 코드, Flyway, DDL, `docs/specs/**`와 `SPEC_LOCK.json`은 이 Patch 브랜치에서
      변경되지 않는다.

## 미결 사항

없음. 이 문서의 제품 방향 전체를 Controller가 승인하거나 거절한다. 승인 뒤 제품 의미를
바꿔야 하면 같은 파일을 덮어쓰지 않고 새 Patch 리비전으로 재승인받는다.

## 관련 Issue·PR·의존 Patch

- 상위 결정·추적 이슈: [#153](https://github.com/Flamingo7562/KB-PJT-24-2/issues/153)
- Patch 이슈: [#221](https://github.com/Flamingo7562/KB-PJT-24-2/issues/221)
- 선행 초대 생명주기 Patch: [#218](https://github.com/Flamingo7562/KB-PJT-24-2/issues/218),
  `SPEC-218-01`
- 선행 Work Case 응답 Patch: [#220](https://github.com/Flamingo7562/KB-PJT-24-2/issues/220),
  `SPEC-220-01`
- Backend 수락 Aggregate: [#156](https://github.com/Flamingo7562/KB-PJT-24-2/issues/156)
- Backend 계약 PDF·접근: [#157](https://github.com/Flamingo7562/KB-PJT-24-2/issues/157)
- Frontend WORKER 조회·수락: [#159](https://github.com/Flamingo7562/KB-PJT-24-2/issues/159)
- Patch 전용 Draft PR:
  [#227](https://github.com/Flamingo7562/KB-PJT-24-2/pull/227). 제출 직전 기준선과 활성 Patch
  중복을 재검토했고 상태를 `proposed`로 전환했다.
- 적용 순서: `SPEC-218-01` → `SPEC-220-01` → `SPEC-221-01`. 세 Patch가 Controller 정식
  명세 릴리스에 `applied`되기 전 #156, #157, #159 구현 PR을 병합하지 않는다.
- 대상 중복: 이 Patch는 `SPEC-218-01`의 `DEC-INVITE-ACCEPT`와
  `DEC-INVITE-ERROR-CATALOG`을 다시 편집하지 않고 적용된 의미를 사용한다.
