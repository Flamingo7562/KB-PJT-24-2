---
patch_id: SPEC-145-01
author: donnyeonglee
status: proposed
issue: 145
created_at: 2026-08-06
base_spec_version: 3.0.1
base_commit: "1ad5d6458361a8c5ec32afb53185e22ad475a016"
change_type: clarification
delivery_mode: spec_first
targets:
  - decision: DEC-AUTH-ERRORS
  - requirement: COMMON-002
  - requirement: WORKPLACE-001
  - requirement: WORKPLACE-004
  - rest_operation: POST /api/workplaces
  - rest_operation: GET /api/workplaces
depends_on: []
supersedes: null
superseded_by: null
applied_in_version: null
applied_by_pr: null
---

# SPEC-145-01: 역할 기반 접근 거절의 오류 Code 확정

## 변경 요약과 필요성

인증된 사용자를 **역할(Role)만으로** 거절할 때 반환할 오류 Code를 `403 ROLE_MISMATCH`로 확정한다. 소유권, 당사자 관계 또는 리소스 상태를 근거로 한 거절은 기존대로 `403 FORBIDDEN`을 유지한다.

정식 명세는 역할 위반의 HTTP 상태를 403으로 고정했지만 인증 Operation 밖에서 사용할 오류 Code를 지정하지 않았다. 그 결과 같은 성격의 거절을 구현마다 다른 Code로 응답할 수 있고, 클라이언트는 어떤 Code로 분기해야 하는지 계약에서 확인할 수 없다.

지금 확정해야 하는 이유는 Frontend 실연동이 오류 Code로 화면 분기를 작성하기 직전이기 때문이다. 분기를 작성한 뒤 Code가 뒤집히면 Backend, Frontend와 두 계층의 Test를 동시에 수정해야 한다.

## 범위 경계

이 Patch는 구분 기준을 확정하고 **사업장 Operation의 적합성만** 영향 범위와 수용 조건으로 다룬다. 승인 단위를 Issue #145가 실제로 확인한 공백으로 유지하기 위해서다.

지갑 충전(`POST /api/wallet/funding-orders`)의 `FundingController`도 역할만으로 거절하면서 `FORBIDDEN`을 반환한다. 이 Patch가 확정하려는 기준을 그대로 적용하면 정합화 대상이지만, 이 Patch는 해당 Operation의 계약을 바꾸지 않고 검증도 요구하지 않는다. 알려진 미정합 지점으로 기록하고 별도 Patch에서 다룬다.

## 현재 명세와 문제

현재 정식 명세 `3.0.1`의 관련 진술은 다음과 같다.

- `API_SPEC.md` 공통 오류 규칙은 "인증 없음과 Session 만료는 401, 역할 또는 리소스 소유권 위반은 403"으로 **HTTP 상태만** 고정하고 Code를 지정하지 않는다.
- 같은 절의 "역할 불일치는 `403 ROLE_MISMATCH`"는 `DEC-AUTH-ERRORS`를 옮겨 적은 문장이다.
- `DEC-AUTH-ERRORS`의 영향 범위는 `AUTH-001`, `AUTH-002`, `AUTH-004`, `AUTH-006`이며 사업장 요구사항이 없다. 인접한 `DEC-AUTH-INPUT`이 영향 범위에 `WORKPLACE-001`, `WORKPLACE-002`를 명시적으로 포함한 것과 대조된다.
- `SPEC_TRACEABILITY.md`의 `COMMON-001` 행은 `DEC-AUTH-ERRORS`를 "모든 Operation"에 연결한다.

즉 결정표는 인증 Operation 한정으로 읽히고 추적표는 전 Operation 적용으로 읽힌다. 두 문서가 서로 다른 범위를 가리키는 것이 이 Patch가 해소하려는 공백이다.

`DEC-OPEN-ERROR-CATALOG`는 QR·초대·근태·문서·정산의 세부 오류 Code를 미결로 남기지만 역할 기반 거절 Code는 그 범위에 포함되지 않는다.

## 전달 방식과 위험 판정

`spec_first`를 선택한다. 다음 필수 조건에 해당하기 때문이다.

- **인가 계약 변경**: 역할만으로 거절할 때 응답할 Code를 정하는 것은 인가 계약에 대한 제품 결정이다.
- **여러 기능이 공유하는 계약 변경**: 대상인 `DEC-AUTH-ERRORS`는 `COMMON-001`을 통해 여러 Operation에 연결되어 사업장 한 기능의 계약이 아니다.
- **기존 오류 의미의 교체**: 현재 구현은 역할 거절에 `FORBIDDEN`을 반환하므로 적용 시 관측 가능한 응답 Code가 바뀐다.

위험 판정은 다음과 같다.

| 항목 | 판정 |
| --- | --- |
| 하위 호환성 | 정식 명세에 해당 진술이 없어 계약상 깨지는 소비자는 없다. 다만 현재 구현이 `FORBIDDEN`을 반환하므로 적용 시 Backend 수정이 필요하다. |
| 보안·개인정보 | 인가 판정과 노출 정보량은 바뀌지 않는다. 거절 사유의 종류만 Code로 구분된다. |
| 데이터·Migration | 영향 없음. 저장 구조와 데이터 의미를 바꾸지 않는다. |
| 외부 소비자 | 외부 공개 소비자는 없다. 내부 Frontend는 아직 오류 Code 분기를 구현하지 않았다. |
| 되돌리기 난이도 | 배포 전이라 코드 수정으로 되돌릴 수 있으나 공유 결정 문서를 바꾸므로 Revert Patch와 새 릴리스가 필요하다. |

`change_type`은 정식 명세가 침묵하는 범위를 명문화한다는 점에서 `clarification`으로 판단했다. Controller가 현재 구현 동작을 기준선으로 본다면 `breaking`으로 재분류할 수 있으며 어느 경우에도 전달 방식은 `spec_first`로 동일하다.

## 제안할 최종 규범 문장 또는 Before/After

### `DECISIONS.md` / `DEC-AUTH-ERRORS`

Before (결정 내용)

> 아이디 없음·비밀번호 불일치·비활성 또는 잠금 계정은 `401 AUTH_REQUIRED`, CSRF 실패는 `403 FORBIDDEN`, 중복 가입은 `409 CONFLICT`, 역할 불일치는 `403 ROLE_MISMATCH`, 입력 검증 실패는 `400 VALIDATION_ERROR`로 통일한다.

After (결정 내용)

> 아이디 없음·비밀번호 불일치·비활성 또는 잠금 계정은 `401 AUTH_REQUIRED`, CSRF 실패는 `403 FORBIDDEN`, 중복 가입은 `409 CONFLICT`, 역할 불일치는 `403 ROLE_MISMATCH`, 입력 검증 실패는 `400 VALIDATION_ERROR`로 통일한다. 역할 불일치 규칙은 로그인의 `expectedRole` 대조뿐 아니라 인증된 사용자의 역할만으로 접근을 거절하는 모든 Operation에 적용한다. 소유권, 당사자 관계 또는 리소스 상태를 근거로 한 거절은 `403 FORBIDDEN`을 유지한다.

Before (영향 범위)

> AUTH-001, AUTH-002, AUTH-004, AUTH-006

After (영향 범위)

> AUTH-001, AUTH-002, AUTH-004, AUTH-006, COMMON-002, WORKPLACE-001, WORKPLACE-004

### `API_SPEC.md` 공통 오류 규칙

Before

> - 인증 없음과 Session 만료는 401, 역할 또는 리소스 소유권 위반은 403입니다.

After

> - 인증 없음과 Session 만료는 401, 역할 또는 리소스 소유권 위반은 403입니다.
> - 403 중에서 인증된 사용자의 역할만으로 거절하는 경우는 `ROLE_MISMATCH`, 소유권·당사자 관계·리소스 상태를 근거로 거절하는 경우는 `FORBIDDEN`입니다. 두 Code 모두 거절 사유의 세부 내용을 본문에 노출하지 않습니다.

### `API_SPEC.md` 사업장 절

After (추가)

> `POST /api/workplaces`와 `GET /api/workplaces`는 인증된 사용자가 OWNER가 아니면 `403 ROLE_MISMATCH`로 거절합니다. 소유하지 않은 `workplaceId` 접근은 `403 FORBIDDEN`입니다.

Mock 계좌 오류 규칙인 "미존재·비활성·PIN 불일치는 동일한 Code·HTTP 상태·메시지"는 리소스 상태 기반 거절이므로 `FORBIDDEN`을 그대로 유지하며 이 Patch로 바뀌지 않는다.

## 영향 분석

영향이 없는 영역도 근거와 함께 명시한다.

### 요구사항

- `COMMON-002`(역할·소유관계 서버 검증): 역할 근거 거절과 소유권 근거 거절의 응답 Code 구분을 수용 기준에 반영한다.
- `WORKPLACE-001`, `WORKPLACE-004`: OWNER 전용 Operation의 역할 거절 Code가 `ROLE_MISMATCH`로 고정된다.
- `AUTH-001`, `AUTH-004`: 로그인 `expectedRole` 대조는 이미 `ROLE_MISMATCH`이므로 동작 변화가 없다.
- `WALLET-001`: 이 Patch의 영향 범위에 넣지 않는다. 같은 기준의 정합화 대상이지만 별도 Patch에서 다룬다.

### API

- `POST /api/workplaces`, `GET /api/workplaces`: 비-OWNER 요청의 Code가 `FORBIDDEN`에서 `ROLE_MISMATCH`로 바뀐다. HTTP 상태 403과 Envelope 구조는 그대로다.
- `POST /api/wallet/funding-orders`: 이 Patch로 계약이 바뀌지 않는다. 현재 `FORBIDDEN`을 유지하며 정합화는 별도 Patch의 몫이다.
- `POST /api/invites/{token}/accept`: 로그인 사용자와 `workerId`를 대조하는 당사자 검증이므로 `FORBIDDEN`을 유지한다.
- Mock 계좌 관련 403: 리소스 상태 기반이므로 `FORBIDDEN`을 유지하고 응답으로 사유를 구분할 수 없다는 기존 계약도 유지한다.
- 401 경계, `fieldErrors` 규칙, Page Envelope는 영향 없음.

### 데이터 및 Migration

영향 없음. 저장 구조, 컬럼 의미, 제약과 Flyway Migration을 바꾸지 않는다. 응답 Code 문자열만 달라진다.

### 보안

인가 판정 로직과 접근 허용 범위는 바뀌지 않는다. 거절 사유를 역할 근거와 소유권·리소스 근거로 구분해 노출하지만 두 Code 모두 대상 리소스의 존재 여부나 타인의 데이터를 드러내지 않으므로 정보 노출 표면은 늘지 않는다. Mock 계좌처럼 사유를 의도적으로 구분하지 않는 기존 규칙은 예외로 유지한다.

### Frontend

- 사업장 실연동이 오류 분기를 작성할 때 `ROLE_MISMATCH`를 역할 거절로 처리한다.
- 현재 Frontend에는 사업장 API 오류 Code로 분기하는 코드가 없어 회귀 대상이 없다.
- 사용자 메시지는 서버 `message`를 사용하므로 문구 변경은 필요하지 않다.

### Backend

적용 시 다음 지점을 `RoleMismatchException`으로 교체한다. 두 예외와 Handler가 이미 존재하므로 새 타입은 필요하지 않다.

- `WorkplaceServiceImpl`의 OWNER 판정 — 역할 근거 거절

다음 지점은 변경하지 않는다.

- `EscrowController`의 `workerId` 당사자 대조 — 소유권·당사자 근거
- `MockBankTransferGateway`의 계좌 사용 불가 판정 — 리소스 상태 근거
- `FundingController`의 충전 OWNER 판정 — 역할 근거 거절이지만 이 Patch의 범위 밖이다. 별도 Patch가 확정하기 전까지 `FORBIDDEN`을 유지한다.

### 테스트

- 사업장 등록·목록의 Controller·Service·통합 Test에서 기대 Code를 `ROLE_MISMATCH`로 갱신한다.
- 당사자 검증과 Mock 계좌 거절 Test가 `FORBIDDEN`을 유지하는지 회귀 검증한다.
- 지갑 충전 Test는 이 Patch에서 갱신하지 않는다.
- 두 Code가 모두 HTTP 403이므로 상태 코드만 단언하는 Test는 회귀를 잡지 못한다. 해당 Test는 Code까지 단언하도록 보강한다.

## 검증 가능한 수용 조건

- [ ] `DEC-AUTH-ERRORS`가 역할 근거 거절과 소유권·리소스 근거 거절의 Code를 구분해 진술하고 영향 범위에 `COMMON-002`, `WORKPLACE-001`, `WORKPLACE-004`를 포함한다.
- [ ] `API_SPEC.md` 공통 오류 절이 `ROLE_MISMATCH`와 `FORBIDDEN`의 구분 기준을 명시한다.
- [ ] `API_SPEC.md` 사업장 절이 비-OWNER 요청의 Code를 명시한다.
- [ ] `SPEC_TRACEABILITY.md`의 관련 행이 `DEC-AUTH-ERRORS` 연결과 새 영향 범위를 일치시킨다.
- [ ] 적용 후 비-OWNER의 `POST /api/workplaces`, `GET /api/workplaces` 응답이 `403`과 `code=ROLE_MISMATCH`를 함께 만족한다.
- [ ] 적용 후 당사자 검증 실패와 Mock 계좌 사용 불가 응답이 `403`과 `code=FORBIDDEN`을 유지한다.
- [ ] Mock 계좌 거절이 미존재·비활성·PIN 불일치를 여전히 구분하지 않는다.

## 미결 사항

- `change_type`을 `clarification`으로 볼지 `breaking`으로 볼지에 대한 Controller의 최종 분류. 전달 방식은 어느 경우에도 `spec_first`이다.
- 지갑 충전(`POST /api/wallet/funding-orders`)의 역할 거절 Code 정합화. 같은 기준의 대상이지만 이 Patch의 승인 단위에서 제외했다. 별도 Patch가 필요하다.
- ADMIN 역할이 도입될 경우 관리자 전용 Operation의 거절 Code를 같은 규칙으로 볼지 여부. 현재 `UserRole`은 `OWNER`와 `WORKER` 두 값뿐이라 이 Patch의 적용 대상이 아니다.

## 관련 Issue·PR·의존 Patch

- Issue #145 — OWNER 첫 사업장 등록·활성 목록 및 온보딩 상태 연동. 이 Patch가 다루는 공백이 확인된 작업이다.
- Issue #146 — Frontend 실연동. 오류 Code 분기를 작성하기 전에 이 Patch의 결정이 필요하다.
- 사업장 등록·목록 구현 PR #230 — 이 Patch의 제안 내용을 앞서 반영해 사업장 Operation의 역할 거절을 `ROLE_MISMATCH`로 반환한다. `spec_first`이므로 이 Patch가 `applied`되기 전에는 병합할 수 없고, Controller가 다르게 결정하면 해당 PR을 `FORBIDDEN`으로 되돌린다.
- 지갑 충전(`FundingController`)의 역할 거절 정합화는 이 Patch의 범위 밖이다. 별도 Patch와 그 Patch가 `applied`된 뒤의 구현 PR로 진행한다.
- 의존 Patch 없음. 활성 Patch `SPEC-218-01`, `SPEC-220-01`, `SPEC-221-01`은 모두 M4 초대·근무·인수 계약을 대상으로 하며 이 Patch의 대상 계약 식별자와 겹치지 않는다.
