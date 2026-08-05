# REST API 명세

| 항목        | 값              |
| ----------- | --------------- |
| 명세 릴리스 | `3.0.0`         |
| 승인일      | 2026-08-05      |
| 소유자      | PM/Admin Master |
| Base Path   | `/api`          |

이 문서는 승인된 외부 REST 계약만 정의합니다. 제품 의미는
[REQUIREMENTS.md](REQUIREMENTS.md), 미결 계약은 [DECISIONS.md](DECISIONS.md)를 따릅니다.
문서에 없는 필드, 상태, Endpoint를 구현 편의로 추정하지 않습니다.

## 공통 계약

### 성공 응답

단일 결과는 `data`로 감쌉니다.

```json
{
  "data": {
    "id": 1
  }
}
```

목록은 다음 Page Envelope를 사용합니다.

```json
{
  "data": {
    "content": [],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 0,
      "totalPages": 0
    }
  }
}
```

`204 No Content`는 본문을 반환하지 않습니다.

### 오류 응답

```json
{
  "code": "ERROR_CODE",
  "message": "사용자가 이해할 수 있는 오류 설명",
  "traceId": "UUID",
  "fieldErrors": [
    {
      "field": "fieldName",
      "reason": "거부 사유"
    }
  ]
}
```

- `fieldErrors`는 필드 오류가 있을 때만 포함합니다.
- 인증 없음과 Session 만료는 401, 역할 또는 리소스 소유권 위반은 403입니다.
- 내부 SQL, Stack Trace, Token 원문과 타인의 리소스 존재 여부를 노출하지 않습니다.
- 승인된 공통 오류 Code는 `VALIDATION_ERROR`, `AUTH_REQUIRED`, `FORBIDDEN`,
  `ROLE_MISMATCH`, `RESOURCE_NOT_FOUND`, `CONFLICT`, `IDEMPOTENCY_KEY_REUSED`,
  `WORK_CASE_LOCKED`, `CONTRACT_RETENTION_REQUIRED`, `INTERNAL_ERROR`입니다.
- 아이디 없음·비밀번호 불일치·비활성 또는 잠금 계정은 이유를 구분하지 않고
  `401 AUTH_REQUIRED`로 응답합니다.
- CSRF 검증 실패는 `403 FORBIDDEN`, 중복 가입은 `409 CONFLICT`, 역할 불일치는
  `403 ROLE_MISMATCH`, 입력 검증 실패는 `400 VALIDATION_ERROR`입니다.
- 더 구체적인 승인 오류로 변환되지 않은 예상 밖의 서버 오류는
  `500 INTERNAL_ERROR`와 `서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.` 메시지로
  응답하며 `fieldErrors`를 포함하지 않습니다.
- `500 INTERNAL_ERROR` 응답의 `traceId`는 서버 오류 로그에도 같은 값으로 기록합니다. 내부
  예외 메시지, SQL, Stack Trace와 민감 정보는 서버 로그에서만 다루고 응답에는 노출하지
  않습니다. 단, PIN 원문이나 파생값은 오류·감사·SQL 바인딩을 포함한 어떤 로그에도 기록하지
  않습니다.
- Mock 계좌·지갑 금융 오류는 "지갑과 거래" 절의 표(`DEC-BANK-ERROR-CATALOG`)를 따르며
  위 공통 오류 Code만 사용합니다. QR, 초대, 근태, 문서와 정산의 추가 도메인 오류 Code는
  `DEC-OPEN-ERROR-CATALOG`가 승인하기 전까지 새 규범 값으로 확정하지 않습니다.

### Session, CSRF와 로컬 CORS

- 인증은 `JSESSIONID` 기반 HttpSession입니다. 응답과 저장소에 JWT 또는 Access Token을
  만들지 않습니다.
- 로그인 성공 시 Session ID를 교체하고 로그아웃 시 Session을 무효화합니다.
- `JSESSIONID`는 `HttpOnly=true`, 로컬 환경에서 `Secure=false`, `SameSite=Lax`인 Host-only
  Cookie입니다.
- `GET /api/auth/csrf`는 `204 No Content`로 `XSRF-TOKEN` Cookie를 준비합니다.
- 앱 최초 실행, 로그인 성공 후, 로그아웃 성공 후 `GET /api/auth/csrf`를 다시 호출합니다.
- 로그인과 로그아웃 POST도 CSRF를 검증합니다.
- 상태 변경 요청은 Cookie 값을 `X-XSRF-TOKEN` Header로 보냅니다.
- CSRF 실패는 `403 FORBIDDEN`이며 실패한 상태 변경 요청을 자동 재실행하지 않습니다.
- 로컬 CORS 허용 Origin은 `http://localhost:5173` 하나이며 Credential을 허용합니다.
- 로컬 허용 요청 Header는 `Accept`, `Content-Type`, `X-XSRF-TOKEN`,
  `Idempotency-Key`입니다.
- 노출 응답 Header는 `Location`, `Idempotency-Replayed`입니다.

### 입력 정규화와 검증

입력은 정규화한 뒤 검증하며 가용성 조회와 실제 가입은 같은 규칙을 사용합니다.

- `loginId`: trim 후 소문자로 저장·비교하며 정규화한 값은 필수이고 최대 50자입니다.
- `email`: trim 후 소문자로 저장·비교하며 정규화한 값은 필수이고 최대 255자입니다.
- `loginId`와 `email`은 필수 여부와 최대 길이 외에 최소 길이, 허용 문자 조합 또는 추가
  이메일 형식 규칙을 강제하지 않습니다.
- `name`: trim만 적용하며 최대 100자입니다.
- `phone`: 공백과 하이픈을 제거한 뒤 `0`으로 시작하는 9~11자리 숫자인지 검증합니다.
- API는 정규화된 전화번호 숫자만 반환하고 화면 표시 형식은 클라이언트가 적용합니다.
- 비밀번호에는 trim이나 대소문자 변환을 적용하지 않습니다.
- 비밀번호는 8~64자이면서 UTF-8 기준 72byte 이하여야 합니다. 72byte를 넘는 입력은
  절단하지 않고 `400 VALIDATION_ERROR`로 거부하며 문자 종류 조합은 강제하지 않습니다.
- 전화번호 저장 대상은 서로 독립된 `users.phone`, `workplaces.phone`입니다.
  `employer_profiles.contact_phone`으로 옮기거나 이름을 변경하지 않습니다.
- 전화번호 값은 요청·응답·SQL 바인딩 로그에 남기지 않습니다.

### 시간과 날짜

- 생성, 완료, 만료, 기록, 확인 등 실제 발생 시점은 UTC ISO-8601 `Instant`의 `Z`
  문자열입니다.
- 날짜 자체가 의미인 값은 `YYYY-MM-DD` 형식의 `LocalDate`입니다.
- UI가 날짜와 시간을 분리해 보내는 근무 입력은 서버가 `Asia/Seoul` 기준으로 하나의
  시점으로 결합합니다.
- DB `DATETIME(6)`의 `Asia/Seoul` 값은 API 경계에서 UTC로 변환합니다.

```text
DB:  2026-07-31 18:00:00.000000  (Asia/Seoul)
API: 2026-07-31T09:00:00Z
```

### 페이지네이션

- `page`: 기본값 0
- `size`: 기본값 20, 허용 범위 1~100
- 날짜 범위 Query의 `from`, `to`는 `LocalDate`이며 `to` 날짜 전체를 포함합니다.

### 멱등 요청

다음 요청은 `Idempotency-Key` Header가 필수입니다.

- `POST /api/wallet/funding-orders`
- `POST /api/wallet/withdrawal-requests`
- `POST /api/invitations/{token}/accept`
- `POST /api/work-cases/{workCaseId}/settlement/approve`

Key는 공백 없는 출력 가능한 ASCII 1~100자입니다. 저장 범위는
`(인증 사용자, Operation, Idempotency-Key)` 조합이며 같은 사용자가 다른 Operation에서
같은 Key 문자열을 사용해도 충돌하지 않습니다. 같은 요청 여부는 검증을 통과한 정규화 값의
Fingerprint로 판정합니다.

- 충전 Fingerprint는 정규화한 `bankCode`, `accountNo`, `amount`만 포함하고 PIN과 PIN의
  Hash·HMAC 등 파생값은 포함하지 않습니다. 출금도 같은 세 필드를 사용합니다.
- 형식 검증 실패와 계좌 인증 실패 등 자금 이동 전 실패는 저장·재생하지 않습니다. 실패
  과정에서 만든 `PROCESSING` Claim은 별도 짧은 트랜잭션에서 삭제하므로 같은 Key로 입력을
  고쳐 다시 시도할 수 있습니다.
- 저장한 성공 결과는 24시간 보존합니다. 완료 Replay는 새 자금 이동이 아니라 저장 결과
  조회이므로 현재 계좌 상태·잔액이나 PIN을 다시 확인하지 않고 같은 `data`를 `200`과
  `Idempotency-Replayed: true`로 반환합니다.
- 같은 Key를 다른 Fingerprint에 사용하면 `409 IDEMPOTENCY_KEY_REUSED`입니다.

#### Claim 상태 모델

멱등 처리는 요청 본체보다 먼저 Claim을 선점하는 `PROCESSING → COMPLETED` 모델을
사용합니다(`DEC-IDEMPOTENCY-CLAIM-LIFECYCLE`).

1. 구조 검증과 정규화 뒤 별도의 짧은 트랜잭션에서 `PROCESSING` Claim을 삽입하고
   Commit합니다.
2. Claim 선점 요청만 자금 이동 본 트랜잭션을 실행합니다. 충전의 계좌·PIN 인증 실패를
   포함해 본 처리가 실패하면 그 요청이 선점한 Claim을 삭제합니다.
3. 본 처리가 성공하면 HTTP 상태와 Body Snapshot, 완료·만료 시각을 기록하고
   `COMPLETED`로 전이합니다.

| 기존 Claim 상태 | Fingerprint | 응답                                                   |
| --------------- | ----------- | ------------------------------------------------------ |
| `COMPLETED`     | 같음        | 저장된 Body와 `200` + `Idempotency-Replayed: true`     |
| `COMPLETED`     | 다름        | `409 IDEMPOTENCY_KEY_REUSED`                           |
| `PROCESSING`    | 같음        | `409 CONFLICT` — 처리 중이며 잠시 후 같은 Key로 재시도 |
| `PROCESSING`    | 다름        | `409 IDEMPOTENCY_KEY_REUSED`                           |

중복 요청은 짧은 Claim 선점 트랜잭션이 끝난 뒤 상태를 판정하며 자금 본 처리의 잠금 해제를
기다리지 않습니다. 프로세스 중단으로 남은 `PROCESSING` Claim과 만료된 `COMPLETED` Claim은
`expires_at` 이후 정리합니다. Claim이 만료되어도 주문·요청과 양쪽 원장의 고유 제약으로 이미
완료된 자금 이동의 중복을 막습니다. Claim 선점·비교·실패 정리·만료 정리는 애플리케이션
책임이며 DB는 범위 유일성과 상태 필드 정합성을 강제합니다.

QR 재발급의 재시도 방식은 `DEC-OPEN-QR-REISSUE-IDEMPOTENCY`를 따릅니다.

## 인증·회원

| Method | Path                                     | 인증         | 요청                            | 성공                                                          |
| ------ | ---------------------------------------- | ------------ | ------------------------------- | ------------------------------------------------------------- |
| GET    | `/api/auth/csrf`                         | 불필요       | 없음                            | `204` + `XSRF-TOKEN` Cookie 준비                              |
| GET    | `/api/auth/session`                      | Session 확인 | 없음                            | `200 {data:{authenticated,role?,name?,needsWorkplaceSetup?}}` |
| GET    | `/api/auth/login-id-availability`        | 불필요       | Query `loginId`                 | `200 {data:{available}}`                                      |
| GET    | `/api/auth/email-availability`           | 불필요       | Query `email`                   | `200 {data:{available}}`                                      |
| POST   | `/api/auth/signup`                       | 불필요       | 가입 Body                       | `201 {data:{userId}}`                                         |
| POST   | `/api/auth/login`                        | 불필요       | 로그인 Body                     | `200 {data:{role,name,needsWorkplaceSetup}}`                  |
| POST   | `/api/auth/logout`                       | 필요         | 없음                            | `204`                                                         |
| GET    | `/api/users/me`                          | 필요         | 없음                            | 내 프로필                                                     |
| PATCH  | `/api/users/me`                          | 필요         | `{phone}`                       | 변경된 내 프로필                                              |
| PATCH  | `/api/users/me/password`                 | 필요         | `{currentPassword,newPassword}` | `204`                                                         |
| POST   | `/api/users/me/withdrawal`               | 필요         | `{password}`                    | `204`                                                         |
| GET    | `/api/users/me/badge`                    | 필요         | 없음                            | 최신 뱃지                                                     |
| POST   | `/api/auth/password-reset/requests`      | 불필요       | `{email}`                       | `202 {data:{accepted:true}}`                                  |
| POST   | `/api/auth/password-reset/confirmations` | 불필요       | `{token,newPassword}`           | `204`                                                         |

### Session 조회

`GET /api/auth/session`은 공개 부트스트랩 API입니다. 비인증 상태는 다음 200 응답입니다.

```json
{
  "data": {
    "authenticated": false
  }
}
```

인증된 OWNER 응답에는 현재 DB를 기준으로 계산한 `needsWorkplaceSetup`을 포함합니다.

```json
{
  "data": {
    "authenticated": true,
    "role": "OWNER",
    "name": "김사장",
    "needsWorkplaceSetup": true
  }
}
```

`needsWorkplaceSetup`은 요청 시점에 `role == OWNER`이면서 `status=ACTIVE`인 소유 사업장이
0개일 때만 true입니다. 계산 결과를 Session에 저장하지 않고 로그인과 Session 조회마다
현재 DB 상태로 계산합니다. 다른 보호 API는 인증이 없거나 만료되면
`401 AUTH_REQUIRED`를 반환합니다.

### 가입

```json
{
  "loginId": "worker01",
  "password": "secret123",
  "passwordConfirm": "secret123",
  "name": "김근로",
  "email": "worker@example.com",
  "phone": "01012345678",
  "role": "WORKER"
}
```

- `role`은 `OWNER` 또는 `WORKER`입니다.
- `phone`만 선택 필드입니다.
- 가입은 `users`의 사용자와 KRW `wallets`만 함께 생성하며 Mock 은행계좌를 생성하거나
  사용자에게 귀속·연결하지 않습니다.
- OWNER 가입도 `employer_profiles`를 만들지 않습니다.
- OWNER의 사업장 입력을 가입 Body에 포함하지 않고 사업체·사업장 기준정보는 후속
  `workplaces` 등록에서 관리합니다.

### 로그인

```json
{
  "loginId": "owner01",
  "password": "secret123",
  "expectedRole": "OWNER"
}
```

서버의 실제 역할과 `expectedRole`이 다르면 `403 ROLE_MISMATCH`입니다. 성공 응답에 Token을
포함하지 않습니다. 아이디 없음, 비밀번호 불일치, 비활성 또는 잠금 계정은 모두
`401 AUTH_REQUIRED`로 응답하고 계정 존재 여부나 상태를 구분해 노출하지 않습니다.
OWNER의 `needsWorkplaceSetup`은 Session 조회와 같은 ACTIVE 사업장 실시간 기준으로 계산합니다.

### 내 프로필

`GET /api/users/me`와 `PATCH /api/users/me`의 `data`는 다음 필드를 반환합니다.

```json
{
  "data": {
    "loginId": "owner01",
    "email": "owner@example.com",
    "name": "김사장",
    "phone": "01012345678",
    "role": "OWNER",
    "status": "ACTIVE"
  }
}
```

PATCH Body는 `phone`만 허용합니다. `loginId`, `email`, `name`, `role`, `status`를 보내면
무시하지 않고 `400 VALIDATION_ERROR`로 거부합니다. 전화번호는 공통 정규화 규칙을 적용한
숫자 문자열로 저장·반환합니다.

### 비밀번호 입력

가입, 로그인, 비밀번호 변경과 비밀번호 재설정의 비밀번호 입력은 공통 8~64자 및 UTF-8
72byte 이하 경계를 사용합니다. 비밀번호와 확인값은 변환하지 않은 원문이 같아야 하며,
새 비밀번호를 72byte에서 절단하거나 문자 종류 조합을 추가로 강제하지 않습니다.

### 최신 뱃지

`GET /api/users/me/badge`는 `badgeType`, `level`, `recentCount`,
`remainingToNextLevel`, `criterionLabel`, `criterionDesc`를 `data`에 반환합니다.

### 비밀번호 재설정

- 요청은 이메일 존재 여부와 무관하게 같은 202 응답을 반환합니다.
- Token 원문 전달 채널은 `DEC-OPEN-PASSWORD-RESET-DELIVERY`를 따릅니다.
- 확인은 유효하고 사용되지 않은 Token만 한 번 허용하며 성공 시 기존 Session 정책에 따라
  인증 상태를 갱신합니다.
- `newPassword`는 공통 비밀번호 경계를 적용합니다.

## 사업장

| Method | Path                            | 권한       | 요청             | 성공                       |
| ------ | ------------------------------- | ---------- | ---------------- | -------------------------- |
| POST   | `/api/workplaces`               | OWNER      | 사업장 등록 Body | `201 {data:{workplaceId}}` |
| GET    | `/api/workplaces`               | OWNER      | 공통 Page Query  | 사업장 목록                |
| PATCH  | `/api/workplaces/{workplaceId}` | 해당 OWNER | 허용 필드        | 변경된 사업장              |
| DELETE | `/api/workplaces/{workplaceId}` | 해당 OWNER | 없음             | `204`                      |

### 사업장 등록

```json
{
  "businessRegistrationNumber": "1234567890",
  "name": "강남점",
  "representativeName": "김사장",
  "roadAddress": "서울 강남구 테헤란로 1",
  "detailAddress": "2층",
  "phone": "0212345678",
  "latitude": 37.123,
  "longitude": 127.123
}
```

- `detailAddress`, `latitude`, `longitude`는 선택값입니다.
- 위도와 경도는 함께 보내거나 모두 생략합니다.
- `radiusMeters`, `radiusM`은 받지 않으며 서버가 100m를 적용합니다.
- `phone`은 공통 전화번호 정규화 후 숫자 문자열로 저장·반환합니다.

### OWNER 사업장 목록

`GET /api/workplaces`는 공통 Page Envelope를 사용하며, 각 `content` Item은 다음 필드만
반환합니다.

```json
{
  "workplaceId": 1,
  "businessRegistrationNumber": "1234567890",
  "name": "강남점",
  "representativeName": "김사장",
  "roadAddress": "서울 강남구 테헤란로 1",
  "detailAddress": "2층",
  "phone": "0212345678",
  "radiusMeters": 100,
  "status": "INACTIVE"
}
```

- OWNER가 소유한 `ACTIVE`, `INACTIVE` 사업장을 반환합니다.
- `DELETED` 사업장은 반환하지 않습니다.
- 목록 Item에 `latitude`, `longitude`를 포함하지 않습니다.
- 전역 작업 Context로 선택할 수 있는 사업장은 기존 계약대로 `ACTIVE`만 허용합니다.

### 사업장 수정

허용 필드는 `name`, `roadAddress`, `detailAddress`, `phone`입니다.
`businessRegistrationNumber`, `representativeName`, `latitude`, `longitude`,
`radiusMeters`를 보내면 `400 VALIDATION_ERROR`입니다. 좌표가 있는 사업장의 주소 변경
처리는 `DEC-OPEN-WORKPLACE-COORDINATES`를 따릅니다.

## WORKER 홈과 근무 이력

| Method | Path                     | 권한   | 성공                         |
| ------ | ------------------------ | ------ | ---------------------------- |
| GET    | `/api/worker/home`       | WORKER | 오늘 근무와 계산 기준        |
| GET    | `/api/worker/work-cases` | WORKER | 본인 근무 목록               |
| GET    | `/api/worker/workplaces` | WORKER | 보건증 공유 가능 사업장 목록 |

`GET /api/worker/home`은 최소한 `workCaseId`, `title`, `workplaceName`, `startsAt`,
`endsAt`, `breakMinutes`, `breakPaid`, `dailyWage`, `expectedNetAmount`, `status`를
반환합니다.

`expectedNetAmount`는 다음 계약으로 계산합니다.

```text
taxableBase = max(dailyWage - 150000, 0)
incomeTax = taxableBase × 0.027, 10원 미만 절사
localIncomeTax = taxableBase × 0.0027, 10원 미만 절사
if incomeTax < 1000:
  incomeTax = 0
  localIncomeTax = 0
expectedNetAmount = dailyWage - incomeTax - localIncomeTax
```

확보 안심금액은 클라이언트가 응답 기준값으로 진입 즉시 계산하고 근무 종료 전까지 60초마다
갱신합니다. API를 매분 재호출하는 계약은 아닙니다. 무급 휴게 반영은
`DEC-OPEN-DASHBOARD-BREAK`를 따릅니다.

근무 목록과 상세 응답은 `CHECK_OUT_MISSING`을 `NO_SHOW`와 구분하여 전달합니다.

`GET /api/worker/workplaces`는 WORKER에게 노출 가능한 별도 목록이며 `ACTIVE` 사업장만
반환합니다. 전체 Item 필드는 별도 승인 전까지 이 계약에서 추정하지 않습니다.

## 지갑과 거래

| Method | Path                              | 권한        | 요청                                 | 성공           |
| ------ | --------------------------------- | ----------- | ------------------------------------ | -------------- |
| GET    | `/api/wallet`                     | 인증 사용자 | 없음                                 | 잔액           |
| GET    | `/api/wallet/transactions`        | 인증 사용자 | 거래 Query                           | 거래 Page      |
| POST   | `/api/wallet/funding-orders`      | OWNER       | `{bankCode, accountNo, pin, amount}` | 충전 처리 결과 |
| POST   | `/api/wallet/withdrawal-requests` | 인증 사용자 | `{bankCode, accountNo, amount}`      | 출금 처리 결과 |

### Mock 은행계좌 경계

Mock 은행계좌는 서비스 회원보다 먼저 Demo/Disposable Seed로 생성한 합성 Fixture입니다
(`DEC-MOCK-ACCOUNT-FIXTURE`). Gig Hub 사용자에게 소유·귀속·연결하지 않으며
`mock_bank_accounts.user_id`를 사용하지 않습니다.

- 가입, 프로필 또는 별도 제품 API에서 계좌를 생성·등록·연결하지 않습니다.
- 사용자용 `GET /api/mock-bank-accounts`와 계좌 CRUD·연결 API를 제공하지 않습니다.
- 서버는 정규화한 `bankCode + accountNo`로 계좌를 식별하고 기존 내부 ID를
  `funding_orders.linked_account_id`, `withdrawal_requests.linked_account_id`와
  `mock_bank_transactions.account_id`에 기록합니다.
- 내부 `bankAccountId`, `mockFintechUseNum`, Mock 계좌 잔액과 PIN은 외부 요청·응답으로
  노출하지 않습니다.
- 이체 확인 화면은 클라이언트가 입력값을 마스킹해 확인하고 PIN을 받는 단계입니다. 별도
  REST Operation이나 계좌 등록·연결·조회 절차가 아닙니다.

지원하는 canonical 은행 코드는 다음 5개입니다. 별칭(`KB`, `SHINHAN` 등)은 API 값으로
허용하지 않습니다.

| `bankCode` | 은행명 |
| ---------- | ------ |
| `004`      | KB국민 |
| `088`      | 신한   |
| `020`      | 우리   |
| `081`      | 하나   |
| `011`      | NH농협 |

계좌·금액 입력 검증은 다음과 같습니다(`DEC-BANK-INPUT-VALIDATION`).

- `bankCode`: 위 코드표의 숫자 3자리 문자열
- `accountNo`: 공백과 하이픈을 제거한 뒤 10~14자리 숫자
- `amount`: 0보다 크고 100,000,000 이하인 KRW 원 단위 정수
- `pin`: 충전에만 필요하며 정규화하거나 trim하지 않은 정확히 4자리 ASCII 숫자
- 위 조건을 벗어난 입력은 `400 VALIDATION_ERROR`

Demo 계좌 PIN은 모두 `0000`입니다. 실행 시 인증 기준 값은
`mock_bank_accounts.pin`에만 두며 요청 PIN은 계좌 인증 시점에만 비교합니다. 주문·원장·
멱등 Fingerprint와 응답 Snapshot·응답·예외 메시지·오류·감사·SQL 바인딩을 포함한 모든
로그에는 PIN 원문이나 파생값을 저장하지 않습니다.

### 잔액

```json
{
  "data": {
    "currency": "KRW",
    "availableBalance": 100000,
    "lockedBalance": 50000
  }
}
```

### 거래 목록

Query:

- `workplaceId?`: 지정하면 해당 사업장 거래만 반환하며 `FUNDING`, `WITHDRAWAL`처럼
  사업장과 무관한 거래는 제외합니다.
- `from?`, `to?`
- `type?`: `FUNDING`, `ESCROW_HOLD`, `ESCROW_RELEASE`, `ESCROW_REFUND`,
  `WITHDRAWAL`, `WITHDRAWAL_REFUND`, `ADJUSTMENT`
- `minAmount?`, `maxAmount?`: `amount` 절대값 기준
- `keyword?`: `workTitle`, `workplaceName` 부분 일치
- `sort?`: 기본 `LATEST`, 또는 `OLDEST`, `AMOUNT_ASC`, `AMOUNT_DESC`이며 금액 정렬은
  절대값 기준
- `page?`, `size?`

Item은 `transactionId`, `type`, `amount`, `direction`, `availableAfter`, `lockedAfter`,
`workCaseId`, `workTitle`, `workplaceName`, `displayStatus`, `createdAt`을 반환합니다
(`DEC-TRANSACTION-DISPLAY`).

- `amount`는 항상 0 이상의 절대값이며 부호를 포함하지 않습니다.
- `direction`은 해당 거래 Row가 속한 사용자 지갑 기준 `CREDIT`(증가) 또는
  `DEBIT`(감소)입니다. 같은 `ESCROW_RELEASE` 사건도 OWNER Row는 `DEBIT`, WORKER Row는
  `CREDIT`입니다. `ADJUSTMENT`도 그 Row의 실제 지갑 증감으로 결정합니다.
- 사업장·근무와 무관한 거래의 `workCaseId`, `workTitle`, `workplaceName`은 `null`입니다.
- `displayStatus`는 `PENDING`, `COMPLETED`, `FAILED`, `REFUNDED` 중 하나입니다.
  `FUNDING`, `WITHDRAWAL`, `ESCROW_HOLD`, `ESCROW_RELEASE`, `ADJUSTMENT`는 원장 반영
  시점에만 Row를 생성하므로 `COMPLETED`, `ESCROW_REFUND`는 `REFUNDED`입니다.
  `PENDING`과 `FAILED`는 이번 릴리스에서 생성하지 않는 예약값입니다.
- `createdAt`은 UTC `Instant`입니다.

### 충전과 출금

충전 요청:

```json
{
  "bankCode": "004",
  "accountNo": "170000000001",
  "pin": "0000",
  "amount": 100000
}
```

출금 요청:

```json
{
  "bankCode": "004",
  "accountNo": "170000000001",
  "amount": 100000
}
```

충전은 OWNER가 ACTIVE Mock 계좌의 PIN을 인증해 그 계좌에서 지갑으로 자금을 옮기는
Operation입니다. 신규 실행에서 계좌 Row를 잠근 뒤 상태·PIN·잔액을 확인하고 계좌 차감,
은행 원장, 충전 주문 완료, 지갑 증가와 지갑 원장을 하나의 자금 트랜잭션으로 처리합니다.

출금은 인증 사용자의 지갑에서 ACTIVE Mock 계좌로 자금을 입금하는 Operation입니다. 입금
대상의 존재와 ACTIVE 상태만 확인하며 계좌 소유권이나 PIN을 검사하지 않습니다. 출금 요청
완료, 지갑 감소, 계좌 입금과 양쪽 원장을 하나의 자금 트랜잭션으로 처리합니다.

두 Operation 모두 서버가 `bankCode + accountNo`로 찾은 내부 ID를 기존
`linked_account_id`에 기록합니다. 클라이언트는 `bankAccountId`를 보내지 않고 성공 응답도
이를 반환하지 않습니다.

계좌·금액 오류는 다음 경계를 사용합니다(`DEC-BANK-ERROR-CATALOG`).

| 상황                                                   | Code               | HTTP  | `message`                                          |
| ------------------------------------------------------ | ------------------ | ----- | -------------------------------------------------- |
| 은행 코드·계좌번호·PIN·금액 형식 오류                  | `VALIDATION_ERROR` | `400` | `입력값을 확인해 주세요.`                          |
| 충전 계좌 미존재·비활성·PIN 불일치                     | `FORBIDDEN`        | `403` | `계좌를 사용할 수 없습니다.`                       |
| 출금 입금계좌 미존재·비활성                            | `FORBIDDEN`        | `403` | `계좌를 사용할 수 없습니다.`                       |
| 충전할 Mock 계좌 잔액 부족                             | `CONFLICT`         | `409` | `계좌 잔액이 부족합니다.`                          |
| 출금할 지갑 가용 잔액 부족                             | `CONFLICT`         | `409` | `출금 가능한 잔액이 부족합니다.`                   |
| 계좌·지갑 동시 갱신 충돌 또는 처리 중인 같은 멱등 요청 | `CONFLICT`         | `409` | `요청이 충돌했습니다. 잠시 후 다시 시도해 주세요.` |

미존재·비활성·PIN 불일치는 동일한 Code·HTTP 상태·메시지이며 `fieldErrors`를 포함하지
않습니다. 응답만으로 계좌 존재, 상태 또는 PIN 일치 여부를 구분할 수 없습니다.

최초 충전 성공:

```json
{
  "data": {
    "fundingOrderId": 10,
    "status": "COMPLETED",
    "bankTransactionId": 20
  }
}
```

최초 출금 성공:

```json
{
  "data": {
    "withdrawalRequestId": 11,
    "status": "COMPLETED",
    "bankTransactionId": 21
  }
}
```

최초 성공은 201, 멱등 재전송은 같은 `data`와 200을 반환합니다. 성공 응답에는 최신 잔액을
넣지 않으며 클라이언트가 잔액 API를 다시 조회합니다.

## OWNER 근무 관리

| Method | Path                                               | 권한        | 계약                       |
| ------ | -------------------------------------------------- | ----------- | -------------------------- |
| GET    | `/api/workplaces/{workplaceId}/work-cases/summary` | 해당 OWNER  | 상태별 건수                |
| GET    | `/api/workplaces/{workplaceId}/work-cases`         | 해당 OWNER  | 근무 Page                  |
| POST   | `/api/workplaces/{workplaceId}/work-cases`         | 해당 OWNER  | `DRAFT` 생성               |
| GET    | `/api/work-cases/{workCaseId}`                     | 당사자      | 근무 상세                  |
| PATCH  | `/api/work-cases/{workCaseId}`                     | 해당 OWNER  | 계약 전 조건 수정          |
| DELETE | `/api/work-cases/{workCaseId}`                     | 해당 OWNER  | 삭제 또는 취소             |
| POST   | `/api/work-cases/{workCaseId}/invitations`         | 해당 OWNER  | 초대 Link 생성             |
| GET    | `/api/work-cases/{workCaseId}/workplace-contact`   | 해당 WORKER | `{ownerName,phone}`        |
| GET    | `/api/work-cases/{workCaseId}/disputes`            | 당사자      | 신고 Page                  |
| POST   | `/api/work-cases/{workCaseId}/disputes`            | 당사자      | `{content}` → `{reportId}` |

목록 Query는 `keyword?`, `status?`, `from?`, `to?`, `page?`, `size?`를 사용합니다.

등록·수정 조건은 `title`, `workDate`, `startTime`, `endTime`, `breakMinutes`,
`breakPaid`, `dailyWage`입니다. 서버는 날짜와 시간을 `Asia/Seoul`로 결합합니다. 생성
성공은 `201 {data:{workCaseId}}`입니다. 계약 확정 뒤 조건 변경은
`409 WORK_CASE_LOCKED`입니다.

초대 생성은 Body가 없고 `201 {data:{inviteUrl,expiresAt}}`을 반환합니다.
근무 상세와 상태별 요약의 전체 필드 집합은 `DEC-OPEN-WORK-CASE-RESPONSE-SHAPES`를 따릅니다.

## 초대 조회와 수락

### `GET /api/invitations/{token}`

- 인증된 WORKER만 조회합니다.
- 비로그인 웹 접근은 `/worker/login?redirect=<초대 경로>`로 보낸 뒤 로그인 성공 시 원래
  경로로 돌아옵니다.
- 서버는 Token Hash, 만료·철회·수락 상태, 근무 조건 Version과 대상 WORKER를 검증합니다.
- 인증 전에는 초대 내용과 OWNER 뱃지를 노출하지 않습니다.

```json
{
  "data": {
    "title": "주말 홀 서빙",
    "startsAt": "2026-07-31T01:00:00Z",
    "endsAt": "2026-07-31T09:00:00Z",
    "ownerBadge": {
      "badgeType": "TRUST_OWNER",
      "level": 2
    }
  }
}
```

### `POST /api/invitations/{token}/accept`

- 인증된 초대 대상 WORKER만 호출합니다.
- `Idempotency-Key`가 필수이며 Body는 없습니다.
- 사용자, OWNER, 근무, 금액 ID 또는 전자서명 이미지를 받지 않습니다.
- 서버가 Token으로 당사자, 근무, 조건 Version과 금액을 도출합니다.
- 매칭, 계약 Snapshot, 에스크로 HOLD와 정산 예약을 한 트랜잭션으로 처리합니다.

```json
{
  "data": {
    "workCaseId": 123,
    "escrowStatus": "HELD"
  }
}
```

성공과 멱등 재전송 모두 200이며 재전송에는 `Idempotency-Replayed: true`를 설정합니다.

## 정산 즉시 승인

### `POST /api/work-cases/{workCaseId}/settlement/approve`

- 해당 근무 OWNER만 호출합니다.
- `Idempotency-Key`가 필수이며 Body는 없습니다.

```json
{
  "data": {
    "settlementId": 1,
    "status": "COMPLETED",
    "completedAt": "2026-07-31T09:00:00Z"
  }
}
```

`completedAt`은 UTC `Instant`입니다. 성공과 멱등 재전송 모두 200이며 재전송에는
`Idempotency-Replayed: true`를 설정합니다.

## 사업장 고정 QR과 근태

QR은 사업장별로 활성 Token이 최대 하나인 비만료 고정 QR입니다. 공개 nonce와 외부 HMAC
Key로 `workplaceId`를 포함한 Token을 검증하며 HMAC Key와 완성된 Token 원문은 DB에
저장하지 않습니다.

### `GET /api/workplaces/{workplaceId}/qr`

- 해당 사업장 OWNER만 호출합니다.
- 조회는 QR을 교체하지 않습니다.

```json
{
  "data": {
    "workplaceId": 1,
    "qrToken": "signed-token",
    "createdAt": "2026-07-31T00:00:00Z"
  }
}
```

### `POST /api/workplaces/{workplaceId}/qr/reissue`

- 해당 사업장 OWNER만 호출합니다.
- 기존 활성 QR을 즉시 `REVOKED` 처리하고 새 nonce를 발급합니다.

```json
{
  "data": {
    "workplaceId": 1,
    "qrToken": "new-signed-token",
    "reissuedAt": "2026-07-31T00:10:00Z"
  }
}
```

재시도와 멱등 Header 계약은 `DEC-OPEN-QR-REISSUE-IDEMPOTENCY`를 따릅니다.

### `POST /api/attendance/scans`

인증된 WORKER가 호출합니다.

```json
{
  "qrToken": "signed-token",
  "latitude": 37.123,
  "longitude": 127.123,
  "confirmEarlyCheckout": false
}
```

처리 순서는 다음과 같습니다.

1. HMAC과 nonce 상태로 QR과 사업장을 확인합니다.
2. 인증 WORKER에게 해당 사업장에서 처리할 근무가 최대 하나인지 확인합니다.
3. 사업장 좌표와 고정 100m 반경으로 위치를 확인합니다.
4. 성공 출근이 없으면 `CHECK_IN`, 출근만 있으면 `CHECK_OUT`으로 판별합니다.
5. 출퇴근이 모두 완료됐으면 중복 스캔을 거부합니다.

일반 성공:

```json
{
  "data": {
    "result": "RECORDED",
    "workCaseId": 123,
    "scanType": "CHECK_IN",
    "recordedAt": "2026-07-31T00:00:00Z",
    "earlyCheckoutConfirmedAt": null
  }
}
```

예정 종료 전 두 번째 스캔은 기록하지 않고 200으로 확인 필요 결과를 반환합니다.

```json
{
  "data": {
    "result": "CONFIRMATION_REQUIRED",
    "scanType": "CHECK_OUT",
    "scheduledEndAt": "2026-07-31T09:00:00Z"
  }
}
```

사용자가 확인하면 같은 QR과 위치에 `confirmEarlyCheckout:true`로 다시 요청합니다. 성공 시
`CHECK_OUT`과 `earlyCheckoutConfirmedAt`을 기록합니다.

성공 `CHECK_IN` 뒤 성공 `CHECK_OUT`이 없는 근무는 `CHECK_OUT_MISSING` 후보이며
`NO_SHOW`로 바꾸지 않습니다. 판정과 해소 API 또는 자동 처리 계약은
`DEC-OPEN-CHECK-OUT-MISSING-FLOW`를 따릅니다.

## 문서

| Method | Path                                               | 권한          | 계약                                                |
| ------ | -------------------------------------------------- | ------------- | --------------------------------------------------- |
| GET    | `/api/documents`                                   | 문서 접근자   | 문서 Page                                           |
| POST   | `/api/documents`                                   | WORKER        | 보건증 Multipart 업로드                             |
| PATCH  | `/api/documents/{documentId}`                      | 보건증 소유자 | `{issuedDate}`                                      |
| DELETE | `/api/documents/{documentId}`                      | 보건증 소유자 | `204`                                               |
| GET    | `/api/documents/{documentId}/file`                 | 문서 접근자   | Query `mode=view` 또는 `mode=download`, 파일 Stream |
| GET    | `/api/documents/{documentId}/shares`               | 보건증 소유자 | 공유 목록                                           |
| POST   | `/api/documents/{documentId}/shares`               | 보건증 소유자 | `{workplaceId}` → `{shareId}`                       |
| DELETE | `/api/documents/{documentId}/shares/{workplaceId}` | 보건증 소유자 | `204`                                               |

`GET /api/documents`는 `workplaceId?`, `docType?`, `page?`, `size?` Query를 사용합니다.

`POST /api/documents`의 Multipart Part는 다음과 같습니다.

- `docType=HEALTH_CERTIFICATE`
- `file`: JPG, PNG 또는 PDF
- `issuedDate`: `LocalDate`

사용자 업로드에서 `EMPLOYMENT_CONTRACT`를 받지 않습니다. 근로계약서는 초대 수락과 계약
확정 과정에서 시스템이 자동 생성하므로 별도 업로드 Endpoint가 없습니다.

PATCH와 DELETE는 보건증에만 적용합니다. 근로계약서를 직접 삭제하려는 요청은
`409 CONTRACT_RETENTION_REQUIRED`입니다. 근로일 이후 3년 보존과 자동 삭제는 HTTP
요청이 아니며, 기준일과 삭제 범위는 `DEC-OPEN-DOCUMENT-RETENTION-SCOPE`를 따릅니다.

문서 목록, 파일, 공유 응답의 전체 필드 집합은
`DEC-OPEN-DOCUMENT-RESPONSE-SHAPES`를 따릅니다.

## 알림과 외부 결제

알림 Endpoint의 요청·응답과 전달 방식은 `DEC-OPEN-NOTIFICATION-CONTRACT`, 외부 결제
Endpoint는 `DEC-OPEN-PAYMENT-PROVIDER`가 승인된 뒤 이 문서에 추가합니다. 결정 전에는
경로 또는 Payload를 규범 계약으로 추정하지 않습니다.
