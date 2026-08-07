---
patch_id: SPEC-161-01
status: draft
issue: 161
base_spec_version: 4.2.0
targets:
  - requirement: WORKPLACE-001
  - requirement: WORKPLACE-002
  - requirement: WORKPLACE-004
  - requirement: DASH-001
  - requirement: WORK-007
  - requirement: ATT-002
  - requirement: ATT-003
  - requirement: ATT-004
  - requirement: ATT-005
  - requirement: ATT-006
  - requirement: SETTLE-001
  - operation: PUT /api/workplaces/{workplaceId}/coordinates
  - operation: GET /api/workplaces
  - operation: POST /api/attendance/scans
  - operation: GET /api/worker/home
  - operation: GET /api/worker/work-cases
  - decision: DEC-OPEN-WORKPLACE-COORDINATES
  - decision: DEC-OPEN-QR-REISSUE-IDEMPOTENCY
  - decision: DEC-OPEN-CHECK-OUT-MISSING-FLOW
  - decision: DEC-OPEN-ERROR-CATALOG
---

# SPEC-161-01: M5 근태 상태 전이·위치·재시도 계약

## 추가 사항

### 고정 QR 재발급 복구

고정 QR 재발급은 `SPEC-162-01`의 계약대로 `Idempotency-Key`를 요구하지 않는다. 응답을
확인하지 못한 클라이언트는 `POST`를 자동 반복하지 않고
`GET /api/workplaces/{workplaceId}/qr`로 현재 활성 QR을 복구한다. 사용자가 다시 확인한
재발급만 새로운 QR 회전 의도다. 이 규칙으로 `DEC-OPEN-QR-REISSUE-IDEMPOTENCY`를 닫는다.

재발급과 스캔은 모두 `workplaces` 행을 먼저 잠그고 활성 `qr_tokens`를 확인한다. 스캔은 그 뒤
`work_cases`를 잠근다. 사업장 잠금을 먼저 얻은 Transaction이 QR의 승패를 정한다. 재발급이
먼저 Commit되면 구 nonce 스캔은 `410 QR_REVOKED`, 스캔이 현재 nonce 검증과 근태 Commit을
먼저 끝내면 그 근태는 유효하고 뒤이은 재발급은 이후 스캔에만 적용한다. 이미 성공한 근태를
QR 재발급 때문에 되돌리지 않는다.

### 사업장 위치 확정

사업장 등록 화면은 현장 OWNER에게 브라우저 위치 권한을 요청하고 성공하면 기존
`latitude`·`longitude` 필드로 현재 위치를 함께 보낸다. 위치 권한 거부나 지원 불가로 좌표를
생략한 등록은 계속 허용하지만, 그 사업장은 `출퇴근 위치 미확정`으로 표시하고 READY 진입을
허용하지 않는다.

브라우저는 GPS 좌표·정확도·측정 시각의 공급자이고, OWNER는 등록 또는 별도 위치 확정 동작으로
그 좌표를 명시적으로 확인한다. 서버는 주소를 지오코딩하거나 좌표를 추정하지 않고 형식·신선도·
정확도·소유권을 검증한 뒤 사업장 기준 좌표를 확정한다.

좌표가 없는 기존·신규 사업장은 OWNER 전용
`PUT /api/workplaces/{workplaceId}/coordinates`로 한 번만 현장 위치를 확정한다.

```json
{
  "latitude": 37.1234567,
  "longitude": 127.1234567,
  "accuracyMeters": 18.25,
  "capturedAt": "2026-08-07T01:00:00Z"
}
```

- `ACTIVE`인 소유 사업장의 두 좌표가 모두 `null`일 때만 저장한다.
- 위도·경도는 각각 소수점 7자리 이하이고 허용 범위 안이어야 한다.
- `accuracyMeters`는 `0` 이상 `100` 이하이며 소수점 2자리 이하이다.
- `capturedAt`은 서버 수신 시각보다 5분 넘게 과거이거나 1분 넘게 미래면 거부한다.
- 최초 성공과 같은 정규화 좌표의 재전송은 `204`로 성공하고, 이미 저장된 좌표와 다른 값은
  `409 WORKPLACE_COORDINATES_ALREADY_SET`이다.
- 좌표가 확정된 사업장은 일반 수정 API와 이 API 모두에서 좌표를 바꿀 수 없다. 주소도 좌표와
  어긋나지 않도록 변경을 `409 WORKPLACE_LOCATION_LOCKED`로 거부하며 상호와 전화번호 수정은
  유지한다.
- OWNER 사업장 목록 Item은 원문 좌표 대신 `attendanceLocationConfirmed` Boolean을 반환한다.
  화면은 이 값이 `false`인 사업장에만 현장 위치 확정 동작을 제공한다.

출퇴근 거리는 `work_cases`의 과거 Snapshot이 아니라 QR이 가리키는 현재
`workplaces.latitude`·`longitude`를 기준으로 계산한다. 좌표는 최초 확정 뒤 불변이므로 좌표가
없던 기존 Work Case도 별도 계약 변경 없이 위치 확정 뒤 사용할 수 있다. Work Case와 계약의
좌표 Snapshot은 계약·감사용 이력으로 유지한다.

### 서버 시간창과 READY

모든 판정은 서버가 요청 또는 Job 시작 시 한 번 얻은 시각을 사용한다. DB에는
`Asia/Seoul` 벽시계로 저장하고 API에는 UTC `Instant`로 반환한다.

| 경계 | 값 | 포함 규칙 |
| --- | --- | --- |
| READY 시작 | `starts_at - 30분` | 해당 시각 포함 |
| CHECK_IN 종료·NO_SHOW 판정 | `starts_at + 1시간` | CHECK_IN은 해당 시각 미포함, NO_SHOW는 포함 |
| CHECK_OUT 종료·누락 판정 | `ends_at + 2시간` | CHECK_OUT은 해당 시각 미포함, 누락 판정은 포함 |
| 정상 정산 유예 | 성공 CHECK_OUT 시각부터 24시간 | `due_at`에 정확히 저장 |

서버 자동 판정 작업이 READY, NO_SHOW, CHECK_OUT_MISSING 후보를 처리한다. 조건을 만족한 행은
정상 운영 중 기준 시각부터 1분 안에 전이되어야 한다. 실행 기술, Cron, Thread 수와 Batch
크기는 내부 구현이다.

`ACCEPTED` Work Case는 READY 시간에 다음 조건을 모두 만족할 때만 `READY`가 된다.

- 배정 WORKER와 수락된 초대·`work_contracts` 행의 당사자와 조건 Version이 일치한다.
- 최종 SIGNED 계약 Version을 승인된 최종 또는 pending 복구 경로로 읽고 Checksum을 검증할 수 있다.
- 같은 일급의 Escrow가 `HELD`이고 Settlement가 `WAITING`, `due_at=null`이다.
- 사업장이 `ACTIVE`이고 현재 사업장 좌표가 모두 존재한다.

조건이 빠진 Work Case는 `ACCEPTED`에 남고 출근 후보가 되지 않는다. 조건이 뒤늦게 충족되면
CHECK_IN 종료 전까지 다음 자동 판정 주기에 READY가 될 수 있다. CHECK_IN 종료까지 조건이
충족되지 않은 Work Case는 시스템 준비 실패이므로 `ACCEPTED`에 남고 WORKER의 `NO_SHOW`로
전이하지 않는다.

`READY`에서 CHECK_IN 종료 시각까지 성공 CHECK_IN이 없으면 `NO_SHOW`로 전이한다. 성공
CHECK_IN이 있는 `IN_PROGRESS`에서 CHECK_OUT 종료 시각까지 성공 CHECK_OUT이 없으면
`CHECK_OUT_MISSING`으로 전이한다. 스캔과 Scheduler는 Work Case 행 잠금과 조건부 상태 변경으로
경쟁 승자를 하나로 만들며, 성공 출근과 NO_SHOW 또는 성공 퇴근과 CHECK_OUT_MISSING이 함께
남을 수 없다.

M5는 NO_SHOW와 CHECK_OUT_MISSING에서 늦은 QR이나 수동 보정 API를 제공하지 않는다.
두 상태의 Settlement는 `WAITING`, `due_at=null`로 유지하고 Wallet, Escrow 금액과 원장을
바꾸지 않는다. 해소·환불·지급은 M6 이후의 별도 계약이 담당한다.

### 출퇴근 스캔과 멱등 의도

`POST /api/attendance/scans`에는 공통 형식의 `Idempotency-Key` Header가 필수다.

```json
{
  "qrToken": "signed-token",
  "latitude": 37.1234567,
  "longitude": 127.1234567,
  "accuracyMeters": 18.25,
  "capturedAt": "2026-08-07T01:00:00Z",
  "confirmEarlyCheckout": false
}
```

Scope는 `(인증 WORKER, ATTENDANCE_SCAN, Idempotency-Key)`이고 Key는 공통 계약의 공백 없는
출력 가능한 ASCII 1~100자를 사용한다. Fingerprint는 QR Token의 SHA-256 소문자 Hex,
정규화한 위도·경도·정확도, UTC `capturedAt`, `confirmEarlyCheckout`을 이 순서로 줄바꿈해
결합한 문자열의 SHA-256이다. Token 원문과 정밀 좌표는 Claim, 일반 로그와 오류에 저장하지
않는다.

숫자는 후행 0을 제거한 지수 없는 10진수 문자열로 정규화하고 `-0`은 `0`으로 만든다.
`capturedAt`은 UTC `Instant`의 `Z` 표기, Boolean은 소문자 `true`·`false`를 사용한다. 다음 여섯
문자열을 표시된 순서대로 LF로 결합하고 UTF-8 Bytes의 SHA-256 32byte를 Claim에 저장한다.

```text
tokenHashHex
latitude
longitude
accuracyMeters
capturedAt
confirmEarlyCheckout
```

구조·인증·역할·Key 형식·Token 서명·위치 형식 검증 뒤 Claim을 선점하고, 완료 Replay는 현재
Work Case 상태를 다시 판정하기 전에 저장된 `200` Body를 반환한다. 성공 기록과
`CONFIRMATION_REQUIRED`는 모두 24시간 보존하며 Replay에는
`Idempotency-Replayed: true`를 붙인다. 본 처리 실패는 Claim을 제거해 같은 Key로 안전하게
재시도할 수 있게 한다.

- 응답을 확인하지 못한 같은 스캔 의도는 같은 Key와 같은 Body로 재시도한다.
- 실제 두 번째 스캔인 CHECK_OUT은 새 Key를 사용한다.
- 조기 퇴근 확인은 `confirmEarlyCheckout:true`와 새 Key를 사용하는 새 의도다.
- 같은 Key의 다른 Body는 `409 IDEMPOTENCY_KEY_REUSED`, 같은 Key가 처리 중이면 공통
  `409 CONFLICT`다.

Claim 선점 뒤 본 처리는 `workplaces → qr_tokens → work_cases` 순서로 잠근다. 같은 Key의
동시 요청은 Claim에서 한 요청만 본 처리하고, 서로 다른 Key가 같은 출퇴근 슬롯을 경쟁하면
Work Case 잠금을 먼저 얻어 Commit한 요청만 성공한다. 패자는 상태를 다시 확인해
`409 ATTENDANCE_STATE_CONFLICT`를 받고 성공 Replay로 바꾸지 않는다. 성공 슬롯 Unique 충돌도
같은 오류로 정규화한다. Deadlock·Lock Timeout은 서버가 같은 의도를 최대 2회 내부 재시도해
총 3회 시도하고, 계속 실패하면 Claim을 제거한 뒤
`503 ATTENDANCE_TEMPORARILY_UNAVAILABLE`를 반환한다.

서버는 유효한 현재 QR의 사업장과 WORKER를 기준으로 다음 활성 후보를 함께 조회한다.

- `READY`이고 `starts_at - 30분 <= attemptedAt < starts_at + 1시간`이면 CHECK_IN 후보
- 성공 CHECK_IN이 있는 `IN_PROGRESS`이고 `attemptedAt < ends_at + 2시간`이면 CHECK_OUT 후보

후보가 없으면 `404 ATTENDANCE_WORK_CASE_NOT_FOUND`, 둘 이상이면
`409 ATTENDANCE_WORK_CASE_AMBIGUOUS`다. 서버는 가까운 시작 시각이나 낮은 ID로 임의 선택하지
않는다. 활성 후보가 없고
`starts_at - 30분 <= attemptedAt < ends_at + 2시간`인 `COMPLETED`가 정확히 한 건이면
`409 ATTENDANCE_ALREADY_COMPLETED`를 반환한다. NO_SHOW와 CHECK_OUT_MISSING은 늦은 스캔
후보가 아니다. 같은 범위의 `COMPLETED`가 둘 이상이면
`409 ATTENDANCE_WORK_CASE_AMBIGUOUS`, 하나도 없으면
`404 ATTENDANCE_WORK_CASE_NOT_FOUND`다.

CHECK_IN의 `attemptedAt > starts_at`이면 지각이다. `lateMinutes`는 양의 차이를 분 단위로
올림하고 지각 여부와 분수는 저장 상태가 아니라 성공 기록에서 파생한다.

예정 종료 전 CHECK_OUT은 `confirmEarlyCheckout:false`일 때 성공 행과 상태 변경 없이
`CONFIRMATION_REQUIRED`를 반환한다. 확인 요청은 QR·위치·후보·시간창을 모두 다시 검증하고
성공하면 `early_checkout_confirmed_at`에 그 요청의 서버 수신 시각을 저장한다.

정상 또는 확인된 CHECK_OUT은 한 트랜잭션에서 성공 행, `IN_PROGRESS→COMPLETED`,
Settlement `WAITING→SCHEDULED`와 `due_at=recordedAt+24시간`을 반영한다. M5가 정산 예약
시각과 지급 가능 상태를 만들고, M6는 그 예약을 소비해 실제 자금 이동만 수행한다.

### 위치·시각·거리와 감사

요청 좌표·정확도·`capturedAt`은 사업장 위치 확정과 같은 형식·신선도 규칙을 사용한다.
`attempted_at`은 서버가 요청마다 정한 판정 시각, `captured_at`은 브라우저 위치 측정 시각,
`created_at`은 DB 감사 행 생성 시각이다. 성공 응답의 `recordedAt`은 `attempted_at`이다.

WORKER 스캔의 원문 위도·경도는 거리 판정 중 메모리에서만 사용하고 DB·Claim·일반 로그에
보존하지 않으므로 보존 기간은 0이다. `attendance_records`에는 계산 거리, 정확도,
`captured_at`·`attempted_at`만 남긴다. OWNER가 확정한 사업장 좌표는 근태 원문이 아니라 사업장
기준정보로서 사업장 보존 정책을 따르며 일반 로그에는 노출하지 않는다.

Haversine은 지구 반지름 `6,371,000m`와 라디안 Double 계산을 사용한다. 반올림하지 않은 계산값이
`100m` 이하일 때 성공하며 정확히 100m는 포함한다. `distance_meters`는 판정 뒤 소수점 2자리로
반올림해 저장한다.

요청 필드의 형식·범위·소수점 자릿수 오류는 공통 `400 VALIDATION_ERROR`로 거부하고 근태
행을 만들지 않는다. 정확히 하나의 Work Case와 출퇴근 유형을 정한 뒤 발생한 의미상
위치·시간·상태 거부는
`attendance_records.result=REJECTED`와 다음 `failure_reason` 중 하나로 남긴다.

- `LOCATION_INACCURATE`, `LOCATION_STALE`
- `OUTSIDE_RADIUS`, `TIME_WINDOW_CLOSED`, `STATE_CONFLICT`

`CONFIRMATION_REQUIRED`는 거부도 성공도 아니므로 근태 행을 만들지 않는다. 신뢰할 수 있는
Work Case를 정할 수 없는 변조 QR, 후보 없음·복수는 스키마상 근태 행으로 만들지 않고
Token·정밀 좌표를 제외한 구조화 보안 로그와 `traceId`만 남긴다.

### 응답과 오류

성공 응답은 다음 필드를 항상 반환한다. CHECK_IN의 `settlementDueAt`, 확인되지 않은
`earlyCheckoutConfirmedAt`은 `null`이다.

```json
{
  "data": {
    "result": "RECORDED",
    "workCaseId": 123,
    "scanType": "CHECK_IN",
    "recordedAt": "2026-08-07T01:00:05Z",
    "isLate": true,
    "lateMinutes": 1,
    "earlyCheckoutConfirmedAt": null,
    "settlementDueAt": null
  }
}
```

확인 필요 응답은 `workCaseId`, `result=CONFIRMATION_REQUIRED`, `scanType=CHECK_OUT`,
`scheduledEndAt`을 반환한다.

```json
{
  "data": {
    "result": "CONFIRMATION_REQUIRED",
    "workCaseId": 123,
    "scanType": "CHECK_OUT",
    "scheduledEndAt": "2026-08-07T09:00:00Z"
  }
}
```

| 상황 | HTTP | Code |
| --- | ---: | --- |
| QR 형식·Key ID·HMAC·사업장 식별 실패 | 422 | `QR_INVALID` |
| 서명은 유효하지만 현재 QR이 아님 | 410 | `QR_REVOKED` |
| 사업장 좌표 없음 | 409 | `WORKPLACE_LOCATION_REQUIRED` |
| Key·좌표·정확도·측정 시각 형식 또는 범위 오류 | 400 | `VALIDATION_ERROR` |
| 같은 Key가 같은 Fingerprint를 처리 중 | 409 | `CONFLICT` |
| 같은 Key를 다른 Fingerprint에 재사용 | 409 | `IDEMPOTENCY_KEY_REUSED` |
| 위치 정확도 초과 또는 측정 시각 신선도 오류 | 422 | `LOCATION_INVALID` |
| 반올림 전 거리 100m 초과 | 422 | `OUTSIDE_WORKPLACE_RADIUS` |
| 처리 대상 근무 없음 | 404 | `ATTENDANCE_WORK_CASE_NOT_FOUND` |
| 처리 대상 근무 복수 | 409 | `ATTENDANCE_WORK_CASE_AMBIGUOUS` |
| 이미 출퇴근 완료 | 409 | `ATTENDANCE_ALREADY_COMPLETED` |
| 상태·시간창·다른 Key 동시 요청 경합 | 409 | `ATTENDANCE_STATE_CONFLICT` |
| 내부 Deadlock·Lock Timeout이 제한 재시도 뒤에도 지속 | 503 | `ATTENDANCE_TEMPORARILY_UNAVAILABLE` |

### 근무 조회와 화면

OWNER 요약은 기존 계약의 8개 상태를 그대로 사용한다. WORKER API도 `BEFORE_WORK`, `LATE`,
`SETTLED` 같은 화면 별칭을 반환하지 않고 Work Case·Escrow·Settlement의 저장 상태를 그대로
반환한다.

`GET /api/worker/home`의 `todayWorkCase`는 없으면 `null`이고, 있으면 최소
`workCaseId`, `title`, `workplaceName`, `startsAt`, `endsAt`, `breakMinutes`, `breakPaid`,
`dailyWage`, `expectedNetAmount`, `status`, `attendance`, `escrowStatus`, `settlementStatus`,
`settlementDueAt`을 반환한다. `attendance`는 nullable
`checkedInAt`·`checkedOutAt`과 파생 `isLate`·`lateMinutes`를 가진다.

오늘 후보는 `Asia/Seoul` 시작일이 오늘인 배정 근무와 전날부터 남은 `IN_PROGRESS`,
`CHECK_OUT_MISSING`이다. 복수이면 `IN_PROGRESS`, `CHECK_OUT_MISSING`, `READY`, `ACCEPTED`,
`COMPLETED`, `NO_SHOW` 순서, 같은 상태에서는 `startsAt ASC, workCaseId ASC`로 한 건을 고른다.

`GET /api/worker/work-cases`의 각 Page Item은 같은 기본 필드와 근태·Escrow·Settlement 상태를
반환한다. 정밀 좌표, QR Token, OWNER 잔액과 계약 Storage 정보는 두 API 모두 반환하지 않는다.

QR Token 직접 입력 화면은 제거한다. MVP는 `BarcodeDetector`와 카메라·위치 권한을 지원하는
브라우저만 스캔 대상으로 명시하며, 미지원·권한 거부 시 Token 입력창 대신 지원 환경 안내를
표시한다. 지원 범위를 넓힐 때는 검토된 QR Decoder 의존성을 별도 승인한다.

지원 브라우저는 브랜드명이나 변동 가능한 최소 Version이 아니라 실행 시 Capability로 판정한다.
HTTPS 또는 로컬 Secure Context에서 `navigator.mediaDevices.getUserMedia`, Geolocation API,
`BarcodeDetector`가 모두 존재하고 `BarcodeDetector.getSupportedFormats()`에 `qr_code`가 있을
때만 지원한다. 하나라도 없거나 카메라·위치 권한이 거부되면 스캔을 시작하지 않는다.

### 스키마 경계

이 동작은 현재 좌표, 근태, Settlement와 범용 Claim Column으로 구현할 수 있으므로 이번 승인
릴리스에는 Migration이 필요하지 않다. 기존 `work_cases`·`attendance_records` Index로 M5의
정합성과 기능 계약을 충족하며 CHECK_OUT_MISSING 후보 조회용 `(status, ends_at, id)` Index는
이번 범위에 추가하지 않는다. 운영 규모의 실제 MySQL `EXPLAIN`이 성능 문제를 보일 때만 별도
관리자 승인 Migration으로 검토하며, 이는 `SPEC-161-01` 승인과 #161 종료의 미결 조건이 아니다.
M5는 수동 보정 감사 Column도 추가하지 않는다.

## 완료 조건

- [ ] 좌표를 포함한 사업장 등록과 좌표 없는 사업장의 일회성 위치 확정이 동작하고, 다른 좌표 재설정과 좌표 확정 뒤 주소 변경이 거부된다.
- [ ] 준비 Aggregate와 사업장 좌표가 모두 있는 근무만 시작 30분 전부터 READY가 된다.
- [ ] 시작 1시간 뒤 성공 출근이 없으면 NO_SHOW, 종료 2시간 뒤 성공 퇴근이 없으면 CHECK_OUT_MISSING이 되며 경계 시각 경쟁에서도 상충 상태가 없다.
- [ ] 같은 CHECK_IN Key의 응답 유실 Replay가 CHECK_OUT으로 바뀌지 않고 저장된 200을 반환한다.
- [ ] 실제 CHECK_OUT과 조기 퇴근 확인이 각각 새 Key를 사용하며 조기 퇴근 미확인 요청은 근태 행을 만들지 않는다.
- [ ] 정확히 100m는 성공하고 100m 초과, 부정확·오래된·미래 위치는 승인 오류와 감사 규칙대로 거부된다.
- [ ] 정상·확인된 퇴근은 COMPLETED와 `SCHEDULED/due_at=recordedAt+24시간`을 함께 만들고, NO_SHOW·CHECK_OUT_MISSING은 `WAITING/due_at=null`을 유지한다.
- [ ] M5 전후 Wallet, Escrow 금액과 원장이 바뀌지 않는다.
- [ ] WORKER 홈·이력과 OWNER 요약이 저장 상태를 일관되게 표현하고 정밀 좌표·QR Token·내부 자금·Storage 정보를 노출하지 않는다.
- [ ] QR 재발급 응답 유실은 GET으로 복구하며 자동 POST 반복과 QR Token 직접 입력 UI가 없다.
- [ ] 현재 Schema만으로 기능 계약을 충족하고 이번 승인 릴리스에는 Scheduler Index·수동 보정 Column Migration이 없다.
