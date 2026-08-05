# 제품 명세 추적표

| 항목        | 값              |
| ----------- | --------------- |
| 명세 릴리스 | `3.0.0`         |
| 승인일      | 2026-08-05      |
| 소유자      | PM/Admin Master |

이 표는 요구사항을 승인 REST Operation과 도메인에 연결합니다. 개발 진행률, 임시 데이터
사용 여부, 구현 세부와 테스트 결과는 포함하지 않습니다.

## 회원·인증·뱃지

| 요구사항  | REST Operation                                                                                     | 도메인·데이터                                  | 연결 결정                                                                                          |
| --------- | -------------------------------------------------------------------------------------------------- | ---------------------------------------------- | -------------------------------------------------------------------------------------------------- |
| AUTH-001  | `POST /api/auth/signup`, `POST /api/auth/login`                                                    | 사용자 역할 선택                               | DEC-AUTH-SESSION, DEC-AUTH-ERRORS                                                                  |
| AUTH-002  | `POST /api/auth/signup`                                                                            | `users`, `wallets` — Mock 계좌 생성·연결 없음  | DEC-AUTH-INPUT, DEC-AUTH-ERRORS, DEC-PHONE-STORAGE, DEC-OWNER-ONBOARDING, DEC-MOCK-ACCOUNT-FIXTURE |
| AUTH-003  | `GET /api/auth/login-id-availability`, `GET /api/auth/email-availability`, `POST /api/auth/signup` | `users.login_id`, `users.email` 고유성         | DEC-API-ENVELOPE, DEC-AUTH-INPUT, DEC-AUTH-ERRORS                                                  |
| AUTH-004  | `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/auth/session`                           | 사용자 인증 상태, Session                      | DEC-AUTH-SESSION, DEC-AUTH-ERRORS                                                                  |
| AUTH-005  | `GET /api/auth/session`, `GET /api/workplaces`, `POST /api/workplaces`                             | `users.role`, `workplaces.status`, ACTIVE 개수 | DEC-OWNER-ONBOARDING, DEC-WORKPLACE-LIST                                                           |
| AUTH-006  | `GET /api/auth/csrf`, `GET /api/auth/session`, `POST /api/auth/login`, `POST /api/auth/logout`     | Session, CSRF Token                            | DEC-AUTH-SESSION, DEC-AUTH-CSRF, DEC-LOCAL-CORS, DEC-AUTH-ERRORS                                   |
| AUTH-007  | 모든 보호 Operation                                                                                | 역할, 리소스 소유권                            | DEC-AUTH-SESSION                                                                                   |
| AUTH-008  | `GET /api/users/me`, `PATCH /api/users/me`                                                         | `users.phone`                                  | DEC-PROFILE-IMMUTABLE, DEC-AUTH-INPUT, DEC-PHONE-STORAGE                                           |
| AUTH-009  | `PATCH /api/users/me/password`                                                                     | `users.password_hash`                          | DEC-AUTH-SESSION, DEC-AUTH-INPUT                                                                   |
| AUTH-010  | `POST /api/users/me/withdrawal`                                                                    | `users.status`, 잔액·예치·근무 제약            | DEC-AUTH-SESSION                                                                                   |
| AUTH-011  | `POST /api/auth/password-reset/requests`, `POST /api/auth/password-reset/confirmations`            | `password_reset_tokens`, `users.password_hash` | DEC-PASSWORD-RESET, DEC-AUTH-INPUT, DEC-OPEN-PASSWORD-RESET-DELIVERY                               |
| BADGE-001 | HTTP 없음 — 이력 기반 산정                                                                         | `user_badges`, 정산·근태 이력                  | DEC-OPEN-WORK-CASE-RESPONSE-SHAPES                                                                 |
| BADGE-002 | `GET /api/users/me/badge`                                                                          | `user_badges`                                  | DEC-PROFILE-IMMUTABLE                                                                              |
| BADGE-003 | `GET /api/invitations/{token}`                                                                     | `work_invitations`, `user_badges`              | DEC-INVITE-LOGIN-BADGE                                                                             |

## 사업장·은행 계좌·지갑

| 요구사항      | REST Operation                                                            | 도메인·데이터                                                                                                                   | 연결 결정                                                                                                                                                                    |
| ------------- | ------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| WORKPLACE-001 | `POST /api/workplaces`                                                    | `workplaces`                                                                                                                    | DEC-WORKPLACE-RADIUS, DEC-AUTH-INPUT, DEC-PHONE-STORAGE, DEC-OWNER-ONBOARDING                                                                                                |
| WORKPLACE-002 | `PATCH /api/workplaces/{workplaceId}`                                     | `workplaces`, 근무 조건 Snapshot                                                                                                | DEC-WORKPLACE-IMMUTABLE, DEC-AUTH-INPUT, DEC-PHONE-STORAGE, DEC-OPEN-WORKPLACE-COORDINATES                                                                                   |
| WORKPLACE-003 | `DELETE /api/workplaces/{workplaceId}`                                    | `workplaces.status`, `deleted_at`                                                                                               | DEC-WORKPLACE-IMMUTABLE, DEC-WORKPLACE-LIST                                                                                                                                  |
| WORKPLACE-004 | `GET /api/workplaces`                                                     | `workplaces.owner_user_id`, `workplaces.status`                                                                                 | DEC-AUTH-SESSION, DEC-OWNER-ONBOARDING, DEC-WORKPLACE-LIST                                                                                                                   |
| BANK-001      | `POST /api/wallet/funding-orders`, `POST /api/wallet/withdrawal-requests` | `mock_bank_accounts.bank_code`, `mock_bank_accounts.mock_account_number`, `mock_bank_accounts.pin`, `mock_bank_accounts.status` | DEC-MOCK-ACCOUNT-FIXTURE, DEC-MOCK-ACCOUNT-SCHEMA, DEC-BANK-INPUT, DEC-BANK-CODE-TABLE, DEC-BANK-INPUT-VALIDATION, DEC-BANK-ERROR-CATALOG                                    |
| WALLET-001    | `GET /api/wallet`                                                         | `wallets.available_balance`, `locked_balance`                                                                                   | DEC-BALANCE-REFETCH                                                                                                                                                          |
| WALLET-002    | `POST /api/wallet/funding-orders`, `GET /api/wallet`                      | `funding_orders`, `mock_bank_accounts`, 양쪽 원장, `wallets`                                                                    | DEC-BANK-INPUT, DEC-FUNDING-PIN, DEC-BANK-INPUT-VALIDATION, DEC-BANK-ERROR-CATALOG, DEC-BALANCE-REFETCH, DEC-IDEMPOTENCY-STORAGE, DEC-IDEMPOTENCY-CLAIM-LIFECYCLE            |
| WALLET-003    | `POST /api/wallet/withdrawal-requests`, `GET /api/wallet`                 | `withdrawal_requests`, `mock_bank_accounts`, 양쪽 원장, `wallets`                                                               | DEC-BANK-INPUT, DEC-WITHDRAWAL-DESTINATION, DEC-BANK-INPUT-VALIDATION, DEC-BANK-ERROR-CATALOG, DEC-BALANCE-REFETCH, DEC-IDEMPOTENCY-STORAGE, DEC-IDEMPOTENCY-CLAIM-LIFECYCLE |
| WALLET-004    | `GET /api/wallet/transactions`                                            | `wallet_transactions`, `work_cases`, `workplaces`                                                                               | DEC-PAGE, DEC-TIME, DEC-TRANSACTION-DISPLAY                                                                                                                                  |
| WALLET-005    | 지갑·계좌 금액 변경 Operation                                             | `wallet_transactions`, `mock_bank_transactions`                                                                                 | DEC-IDEMPOTENCY                                                                                                                                                              |
| WALLET-006    | 충전·출금·초대 수락·정산 승인 Operation                                   | `idempotency_requests`, 멱등 Key, 금융 Aggregate                                                                                | DEC-IDEMPOTENCY, DEC-IDEMPOTENCY-STORAGE, DEC-IDEMPOTENCY-CLAIM-LIFECYCLE                                                                                                    |

### `3.0.0` M3 행정 추적

아래 표는 계약 책임과 후속 검증 대상을 연결하며 구현 진행률이나 완료 상태를 뜻하지 않습니다.

| 구분             | 추적 대상                                                                                                                                                                                            | 연결 계약                                                                               |
| ---------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------- |
| 상위 제품 흐름   | [Issue #201](https://github.com/Flamingo7562/KB-PJT-24-2/issues/201)                                                                                                                                 | 비귀속 Mock 계좌 기반 충전 흐름                                                         |
| 보호 명세 릴리스 | [Issue #202](https://github.com/Flamingo7562/KB-PJT-24-2/issues/202)                                                                                                                                 | `3.0.0` 요구·API·결정·추적·Lock                                                         |
| Frontend         | [Issue #203](https://github.com/Flamingo7562/KB-PJT-24-2/issues/203), [#150](https://github.com/Flamingo7562/KB-PJT-24-2/issues/150), [#151](https://github.com/Flamingo7562/KB-PJT-24-2/issues/151) | 이체 확인·PIN 입력, canonical 은행 코드, Session·CSRF·멱등 실연동                       |
| Backend          | [Issue #204](https://github.com/Flamingo7562/KB-PJT-24-2/issues/204), [#60](https://github.com/Flamingo7562/KB-PJT-24-2/issues/60)                                                                   | 충전 PIN Resolver, PIN 없는 출금 목적지, 기존 원자성·원장·Claim 보존                    |
| Migration        | [Issue #205](https://github.com/Flamingo7562/KB-PJT-24-2/issues/205), `V202608051337__replace_mock_bank_account_user_with_pin.sql`                                                                   | `mock_bank_accounts.user_id → pin`, 기존 ID·참조 보존                                   |
| 통합 검증        | [Issue #152](https://github.com/Flamingo7562/KB-PJT-24-2/issues/152)                                                                                                                                 | Disposable MySQL·Tomcat 9·Browser E2E와 자금·원장·멱등·동시성 대사                      |
| 대체 대상        | [PR #193](https://github.com/Flamingo7562/KB-PJT-24-2/pull/193)                                                                                                                                      | 자동 계좌·목록·소유권 제안은 폐기하고 오류·거래 표시·멱등 Claim 결정만 의미 단위로 보존 |

## 홈·근무·초대·계약

| 요구사항     | REST Operation                                                                 | 도메인·데이터                                                  | 연결 결정                                                 |
| ------------ | ------------------------------------------------------------------------------ | -------------------------------------------------------------- | --------------------------------------------------------- |
| DASH-001     | `GET /api/worker/home`                                                         | `work_cases`, `attendance_records`                             | DEC-TIME                                                  |
| DASH-002     | `GET /api/worker/home`                                                         | 근무 시각, 휴게, 합의 일급                                     | DEC-DASHBOARD-REFRESH, DEC-OPEN-DASHBOARD-BREAK           |
| DASH-003     | `GET /api/worker/home`                                                         | 일용근로소득 예상 세액 계산                                    | DEC-DAILY-WORKER-TAX                                      |
| WORK-001     | `GET /api/workplaces/{workplaceId}/work-cases/summary`                         | `work_cases.status`                                            | DEC-CHECK-OUT-MISSING, DEC-OPEN-WORK-CASE-RESPONSE-SHAPES |
| WORK-002     | `GET /api/workplaces/{workplaceId}/work-cases`                                 | `work_cases`, `users`                                          | DEC-PAGE, DEC-TIME                                        |
| WORK-003     | `POST /api/workplaces/{workplaceId}/work-cases`                                | `work_cases`, 사업장 Snapshot                                  | DEC-WORKPLACE-RADIUS, DEC-TIME                            |
| WORK-004     | `GET /api/work-cases/{workCaseId}`                                             | `work_cases`, 초대·계약·근태·정산 Aggregate                    | DEC-OPEN-WORK-CASE-RESPONSE-SHAPES                        |
| WORK-005     | `PATCH /api/work-cases/{workCaseId}`                                           | `work_cases.condition_version`, `work_invitations`             | DEC-INVITE-ACCEPT                                         |
| WORK-006     | `DELETE /api/work-cases/{workCaseId}`                                          | `work_cases.status`, 참조 이력                                 | DEC-INVITE-ACCEPT                                         |
| WORK-007     | `GET /api/worker/work-cases`                                                   | `work_cases`, `escrows`, `settlements`                         | DEC-CHECK-OUT-MISSING                                     |
| INVITE-001   | `POST /api/work-cases/{workCaseId}/invitations`                                | `work_invitations`, Token Hash, 조건 Version                   | DEC-INVITE-ACCEPT                                         |
| INVITE-002   | `GET /api/invitations/{token}`                                                 | `work_invitations`, Session Redirect                           | DEC-INVITE-LOGIN-BADGE                                    |
| INVITE-003   | `GET /api/invitations/{token}`                                                 | `work_cases`, `work_invitations`, `user_badges`                | DEC-INVITE-LOGIN-BADGE                                    |
| CONTRACT-001 | `POST /api/invitations/{token}/accept`                                         | 최종 동의, 조건 Version                                        | DEC-INVITE-ACCEPT, DEC-OPEN-E-SIGN-EVIDENCE               |
| CONTRACT-002 | `POST /api/invitations/{token}/accept`                                         | `work_cases`, `work_contracts`, `escrows`, `settlements`, 원장 | DEC-INVITE-ACCEPT, DEC-IDEMPOTENCY                        |
| CONTRACT-003 | `POST /api/invitations/{token}/accept`, `GET /api/documents/{documentId}/file` | `work_contracts`, `documents`, `document_versions`             | DEC-CONTRACT-AUTO-GENERATION                              |

## 근태·정산·신고

| 요구사항    | REST Operation                                                                          | 도메인·데이터                                               | 연결 결정                                              |
| ----------- | --------------------------------------------------------------------------------------- | ----------------------------------------------------------- | ------------------------------------------------------ |
| ATT-001     | `GET /api/workplaces/{workplaceId}/qr`, `POST /api/workplaces/{workplaceId}/qr/reissue` | `qr_tokens`, HMAC Key                                       | DEC-QR-FIXED, DEC-OPEN-QR-REISSUE-IDEMPOTENCY          |
| ATT-002     | `POST /api/attendance/scans`                                                            | `qr_tokens`, `work_cases`, `attendance_records`             | DEC-QR-FIXED                                           |
| ATT-003     | `POST /api/attendance/scans`                                                            | 사업장 좌표, 100m 반경, 근무 Snapshot                       | DEC-WORKPLACE-RADIUS                                   |
| ATT-004     | `POST /api/attendance/scans`                                                            | `attendance_records`, `work_cases.status`                   | DEC-EARLY-CHECKOUT                                     |
| ATT-005     | HTTP 없음 — 노쇼 판정                                                                   | `work_cases.status`, `attendance_records`                   | DEC-CHECK-OUT-MISSING                                  |
| ATT-006     | 판정·해소 Operation은 결정 후 정의                                                      | `work_cases.status=CHECK_OUT_MISSING`, `attendance_records` | DEC-CHECK-OUT-MISSING, DEC-OPEN-CHECK-OUT-MISSING-FLOW |
| SETTLE-001  | HTTP 없음 — 정산 예약                                                                   | `settlements.scheduled_at`, `work_cases.status`             | DEC-OPEN-CHECK-OUT-MISSING-FLOW                        |
| SETTLE-002  | `POST /api/work-cases/{workCaseId}/settlement/approve`                                  | `settlements`, `escrows`, `wallets`, 원장                   | DEC-SETTLEMENT-TIME, DEC-IDEMPOTENCY                   |
| SETTLE-003  | HTTP 없음 — 예정 정산 실행                                                              | `settlements`, 실행 선점·재시도                             | DEC-SETTLEMENT-TIME                                    |
| SETTLE-004  | 정산 승인·자동 정산 내부 처리                                                           | `escrows`, `wallets`, `wallet_transactions`                 | DEC-IDEMPOTENCY                                        |
| SETTLE-005  | HTTP 없음 — 노쇼 환불                                                                   | `escrows`, `wallet_transactions`, `settlements`             | DEC-OPEN-NO-SHOW-SETTLEMENT                            |
| CONTACT-001 | `GET /api/work-cases/{workCaseId}/workplace-contact`                                    | `work_cases`, `workplaces.phone`, OWNER                     | DEC-AUTH-SESSION, DEC-PHONE-STORAGE                    |
| DISPUTE-001 | `POST /api/work-cases/{workCaseId}/disputes`                                            | `disputes`                                                  | DEC-DISPUTE-SETTLEMENT                                 |
| DISPUTE-002 | `GET /api/work-cases/{workCaseId}/disputes`                                             | `disputes`, 근무 당사자                                     | DEC-DISPUTE-SETTLEMENT                                 |
| DISPUTE-003 | 분쟁 조회·처리 Operation                                                                | `disputes.status`, 처리자·결과                              | DEC-DISPUTE-SETTLEMENT, DEC-OPEN-ADMIN-DISPUTE         |

## 문서

| 요구사항 | REST Operation                                                                 | 도메인·데이터                                              | 연결 결정                                                 |
| -------- | ------------------------------------------------------------------------------ | ---------------------------------------------------------- | --------------------------------------------------------- |
| DOC-001  | `GET /api/documents`                                                           | `documents`, `document_shares`, `workplaces`               | DEC-DOCUMENT-STORAGE, DEC-OPEN-DOCUMENT-RESPONSE-SHAPES   |
| DOC-002  | `POST /api/invitations/{token}/accept`, `GET /api/documents/{documentId}/file` | `documents`, `document_versions`, `work_contracts`         | DEC-CONTRACT-AUTO-GENERATION                              |
| DOC-003  | `GET /api/documents`, `GET /api/documents/{documentId}/file`                   | `documents`, `document_shares`, `document_access_logs`     | DEC-DOCUMENT-STORAGE                                      |
| DOC-004  | `GET /api/documents`, `GET /api/documents/{documentId}/file`                   | `work_contracts`, `documents`, `document_versions`         | DEC-CONTRACT-AUTO-GENERATION, DEC-OPEN-E-SIGN-EVIDENCE    |
| DOC-005  | `POST /api/documents`                                                          | `documents`, `document_versions`                           | DEC-DOCUMENT-STORAGE                                      |
| DOC-006  | `PATCH /api/documents/{documentId}`, `DELETE /api/documents/{documentId}`      | 보건증 `documents`, `document_versions`, `document_shares` | DEC-DOCUMENT-STORAGE                                      |
| DOC-007  | `GET /api/worker/workplaces`, `POST /api/documents/{documentId}/shares`        | `document_shares`, `work_cases`, `workplaces`              | DEC-DOCUMENT-STORAGE, DEC-WORKPLACE-LIST                  |
| DOC-008  | `DELETE /api/documents/{documentId}/shares/{workplaceId}`                      | `document_shares`                                          | DEC-DOCUMENT-STORAGE                                      |
| DOC-009  | `GET /api/documents/{documentId}/file`                                         | `document_versions`, `document_shares`, 접근 권한          | DEC-DOCUMENT-STORAGE                                      |
| DOC-010  | `POST /api/documents`, 계약 확정 내부 생성                                     | 파일 형식 검증, `document_versions`                        | DEC-CONTRACT-AUTO-GENERATION, DEC-DOCUMENT-STORAGE        |
| DOC-011  | 문서 조회·파일 Operation                                                       | `document_access_logs`                                     | DEC-DOCUMENT-STORAGE                                      |
| DOC-012  | 사용자 계약서 DELETE 없음, HTTP 없음 — 보존 만료 삭제                          | `documents`, `document_versions`, `document_access_logs`   | DEC-CONTRACT-RETENTION, DEC-OPEN-DOCUMENT-RETENTION-SCOPE |

## 알림·공통·외부 결제

| 요구사항   | REST Operation                | 도메인·데이터                     | 연결 결정                                                                 |
| ---------- | ----------------------------- | --------------------------------- | ------------------------------------------------------------------------- |
| ALERT-001  | 결정 후 정의                  | 사용자 알림, 읽음 시각            | DEC-OPEN-NOTIFICATION-CONTRACT                                            |
| ALERT-002  | 결정 후 정의                  | 도메인 이벤트, 중복 식별자        | DEC-OPEN-NOTIFICATION-CONTRACT                                            |
| COMMON-001 | 모든 Operation                | 성공·목록·오류 Envelope           | DEC-API-ENVELOPE, DEC-AUTH-ERRORS, DEC-COMMON-5XX, DEC-OPEN-ERROR-CATALOG |
| COMMON-002 | 모든 보호·상태 변경 Operation | 역할, 소유권, 당사자 불변식       | DEC-AUTH-SESSION                                                          |
| COMMON-003 | HTTP 없음 — 보존 정책         | 금융·계약·근태·문서·감사 이력     | DEC-CONTRACT-RETENTION, DEC-OPEN-DOCUMENT-RETENTION-SCOPE                 |
| COMMON-004 | 모든 금액·시간·목록 Operation | KRW, `Instant`, `LocalDate`, Page | DEC-TIME, DEC-PAGE                                                        |
| EXT-001    | 결정 후 정의                  | Provider 주문·승인·취소, 지갑     | DEC-OPEN-PAYMENT-PROVIDER                                                 |
| EXT-002    | 결정 후 정의                  | Webhook 원문·서명·처리 결과       | DEC-OPEN-PAYMENT-PROVIDER                                                 |
