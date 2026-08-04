# 기능 개발 라우팅 테이블 (2026-07-31 아카이브)

> [!WARNING]
> 이 문서는 현재 개발 기준이 아닌 보관 문서입니다.
>
> - 보관일: 2026-07-31
> - 보관 사유: 구현 현황과 코드 경로를 중앙 표로 복제해 충돌과 노후화를 유발함
> - 현재 문서: [`../../agent/IMPLEMENTATION_GUIDE.md`](../../agent/IMPLEMENTATION_GUIDE.md)

마지막 코드·팀 결정 대조: 2026-07-31

이 문서는 기능 하나를 개발할 때 요구사항에서 화면·API·백엔드·DB·검증 근거로 이동하는
공용 지도입니다. 상세 내용을 복제하지 않고 각 단일 원본의 진입점만 연결합니다.

## 작업별 진입 경로

| 하려는 작업              | 먼저 읽을 문서                                                           | 실행 기준                                                                                                   | 함께 확인·갱신할 문서                                |
| ------------------------ | ------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------- | ---------------------------------------------------- |
| 기능 범위·수용 기준 변경 | [`REQUIREMENTS.md`](../../specs/REQUIREMENTS.md)                         | 승인된 요구사항과 현재 코드                                                                                 | 이 파일의 기능 상태·근거                             |
| HTTP API 설계·구현       | [`API_SPEC.md`](../../specs/API_SPEC.md), 해당 요구사항                  | Controller → Service → Mapper와 테스트                                                                      | 프론트 Service, API 명세의 연결 상태                 |
| Vue 페이지·Route 변경    | [`FRONTEND_ROUTE_OVERVIEW.md`](../FRONTEND_ROUTE_OVERVIEW-2026-07-31.md) | [`router/index.js`](../../../frontend/src/router/index.js), View                                            | 이 파일의 Route name                                 |
| 프론트 API 연결          | [`API_SPEC.md`](../../specs/API_SPEC.md)                                 | `frontend/src/services/*.js`, Store, 실제 Controller                                                        | Mock 상태, 응답 Envelope, 통합 테스트                |
| 인증·Session·권한        | API 명세의 Session 절                                                    | Backend Security 설정, `frontend/src/services/http.js`                                                      | AUTH 요구사항, CSRF·로컬 CORS·오류                   |
| DB·상태·관계 변경        | [`SCHEMA_OVERVIEW.md`](../../agent/SCHEMA_OVERVIEW.md)                   | 소유자 전용 Flyway `V*.sql`                                                                                 | 소유자에게 스키마 변경 요청 후 Mapper·관련 문서 갱신 |
| 금융·에스크로·정산       | API 명세와 WALLET·CONTRACT·SETTLE 요구사항                               | Service Transaction·Mapper·DB 테스트                                                                        | 멱등성, 잠금 순서, 원장, 재전송 응답                 |
| Swagger 개선             | API 명세의 Swagger 절                                                    | [`SwaggerConfig.java`](../../../backend/src/main/java/com/gighub/config/SwaggerConfig.java), Controller DTO | 실제 오류·보안·예시와 문서 일치                      |

현재 동작과 팀 승인 목표가 다르면 둘을 먼저 분리해 기록한 뒤 코드를 목표 계약에 맞춥니다.
계획 API를 Controller가 있는 것처럼 문서화하지 않습니다.

## 현재 전체 상태

| 영역                 | 현재 상태                                                        |
| -------------------- | ---------------------------------------------------------------- |
| Vue named page route | 33개, 모든 대상 View 존재                                        |
| 프론트 API 계약      | 48개, 업무 Service 전부 Mock                                     |
| 백엔드 상시 Endpoint | 8개                                                              |
| Local 전용 Endpoint  | 1개                                                              |
| 현재 호환 계약       | 지갑 요약·거래내역·정산 승인 3개                                 |
| Schema               | 추적 Head `202607301152`; 후보 `202607311428`은 소유자 채택 대기 |

화면 구현 상태는
[`FRONTEND_ROUTE_OVERVIEW.md`](../FRONTEND_ROUTE_OVERVIEW-2026-07-31.md), API의 Method·Path·입출력은
[`API_SPEC.md`](../../specs/API_SPEC.md), 정확한 DB 구조는 Flyway에서 확인합니다.

## 기능별 라우팅

`implemented`는 End-to-End 수용 기준을 충족할 때만 사용합니다. `Frontend 근거`가 있어도 Mock
분기라면 기능 상태는 `planned`입니다.

| 기능                    | 요구사항                                  | 상태          | Vue route name                                                                                          | Frontend 진입점                                                                                                                                                                             | Backend 진입점                                                                                                                                                                                                                                    | 현재 차이·다음 검증                                                                                   |
| ----------------------- | ----------------------------------------- | ------------- | ------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------- |
| Health                  | COMMON-001 일부                           | `implemented` | 없음                                                                                                    | 없음                                                                                                                                                                                        | [`HealthController`](../../../backend/src/main/java/com/gighub/health/controller/HealthController.java), [`HealthControllerTest`](../../../backend/src/test/java/com/gighub/health/controller/HealthControllerTest.java)                          | Liveness만 확인하며 DB 상태는 확인하지 않음                                                           |
| 역할 선택               | AUTH-001                                  | `planned`     | `onboarding`                                                                                            | [`OnboardingView`](../../../frontend/src/views/OnboardingView.vue), Auth Store                                                                                                              | 없음                                                                                                                                                                                                                                              | UI는 있으나 서버 역할 기준과 통합되지 않음                                                            |
| 회원가입·중복 확인      | AUTH-002, AUTH-003                        | `planned`     | `owner-signup`, `worker-signup`                                                                         | [`auth.js`](../../../frontend/src/services/auth.js), [`AuthSignupForm`](../../../frontend/src/components/auth/AuthSignupForm.vue)                                                           | 없음                                                                                                                                                                                                                                              | 사용자·지갑 원자 생성과 UNIQUE 오류 통합 필요                                                         |
| 로그인·Session·권한     | AUTH-004~007                              | `planned`     | `owner-login`, `worker-login`                                                                           | [`auth.js`](../../../frontend/src/services/auth.js), [`http.js`](../../../frontend/src/services/http.js), Router Guard                                                                      | [`TestLoginController`](../../../backend/src/main/java/com/gighub/support/TestLoginController.java)만 Local; 로컬 CORS·Cookie 설정 구현                                                                                                           | Spring Security·정식 Session 인증·CSRF와 공통 401·403 구현 필요                                       |
| 프로필·비밀번호·탈퇴    | AUTH-008~010                              | `planned`     | `owner-mypage`, `owner-profile`, `owner-password`, `worker-mypage`, `worker-profile`, `worker-password` | [`users.js`](../../../frontend/src/services/users.js), 양 역할 Profile View                                                                                                                 | 없음                                                                                                                                                                                                                                              | 화면은 ID·이메일·이름을 비활성화했지만 Service가 `name`도 PATCH함; 목표는 `phone`만 허용              |
| 비밀번호 재설정         | AUTH-011                                  | `planned`     | 없음                                                                                                    | 로그인 화면에 준비 중 안내만 있음                                                                                                                                                           | Controller·Service·Mapper 없음; [`password_reset_tokens` 후보 Migration](../../../backend/src/main/resources/db/migration/V202607311428__add_password_reset_tokens.sql)                                                                           | 소유자 후보 채택 후 `/requests`, `/confirmations`; Token 전달 방식과 현재 없는 페이지 구현 필요       |
| 신뢰 뱃지               | BADGE-001~003                             | `planned`     | 양 역할 `mypage`, `invite-confirm`, 근무 상세                                                           | [`users.js`](../../../frontend/src/services/users.js), Router Guard, Invite View Mock                                                                                                       | 없음                                                                                                                                                                                                                                              | 초대 로그인 복귀와 OWNER 뱃지 표시는 구현; 실제 초대 조회·상대방 권한 API와 재계산 Job 필요           |
| 사업장 등록·관리        | WORKPLACE-001~004                         | `planned`     | `owner-workplace-new`, `owner-workplaces`                                                               | [`workplaces.js`](../../../frontend/src/services/workplaces.js)                                                                                                                             | 없음                                                                                                                                                                                                                                              | 임의 반경·150m Mock 정리와 PATCH 허용 목록 필요; 좌표 보유 사업장의 주소 변경 정합성 정책은 `on_hold` |
| 수동 Mock 계좌 식별     | BANK-001                                  | `planned`     | `owner-charge`, `owner-withdraw`, `worker-withdraw`                                                     | 은행·계좌번호 입력 UI, [`wallet.js`](../../../frontend/src/services/wallet.js)                                                                                                              | 현재 목록 조회 Controller만 있음; 계좌 식별 Mapper 없음                                                                                                                                                                                           | UI는 유지하고 백엔드가 로그인 사용자·숫자 은행 코드·계좌번호로 내부 ID를 찾아야 함                    |
| 지갑 요약               | WALLET-001                                | `planned`     | `owner-home`, `worker-home`                                                                             | [`wallet.js`](../../../frontend/src/services/wallet.js)                                                                                                                                     | [`WalletController`](../../../backend/src/main/java/com/gighub/wallet/controller/WalletController.java), Controller Test                                                                                                                          | 계약은 호환되지만 Frontend가 Mock이라 End-to-End 미검증                                               |
| Mock 충전               | WALLET-002, WALLET-005, WALLET-006        | `planned`     | `owner-charge`                                                                                          | [`wallet.js`](../../../frontend/src/services/wallet.js)                                                                                                                                     | [`FundingController`](../../../backend/src/main/java/com/gighub/wallet/controller/FundingController.java), Funding Service·DB Test                                                                                                                | 백엔드 Body를 `bankCode/accountNo/amount`로 변경; 성공 후 지갑 재조회는 화면에 이미 있음              |
| Mock 출금               | WALLET-003, WALLET-005, WALLET-006        | `planned`     | `owner-withdraw`, `worker-withdraw`                                                                     | [`wallet.js`](../../../frontend/src/services/wallet.js)                                                                                                                                     | [`WithdrawalController`](../../../backend/src/main/java/com/gighub/wallet/controller/WithdrawalController.java), Withdrawal Service·DB Test                                                                                                       | 백엔드 Body를 `bankCode/accountNo/amount`로 변경; 성공 후 OWNER 지갑·WORKER 홈 재조회                 |
| 지갑 거래 내역          | WALLET-004                                | `planned`     | `owner-home`                                                                                            | [`wallet.js`](../../../frontend/src/services/wallet.js), Wallet Store, Filter·Item Component                                                                                                | [`WalletController`](../../../backend/src/main/java/com/gighub/wallet/controller/WalletController.java), Controller Test                                                                                                                          | 계약과 UTC `Instant` 변환은 정렬됨; Mock 해제 후 실제 통합 테스트 필요                                |
| 사장 근무 관리          | WORK-001~006                              | `planned`     | `owner-attendance`, `owner-work-case-new`, `owner-work-case-detail`                                     | [`workCases.js`](../../../frontend/src/services/workCases.js)                                                                                                                               | 일반 CRUD Controller 없음; [`WorkMapper`](../../../backend/src/main/java/com/gighub/work/mapper/WorkMapper.java)는 금융 Context만 조회                                                                                                            | 화면·Mock만 존재; `CHECK_OUT_MISSING` 요약은 소유자 상태 Migration 뒤 구현                            |
| 근무 초대·계약 확정     | INVITE-001~003, CONTRACT-001~003, DOC-002 | `planned`     | `invite-confirm`, `owner-work-case-detail`                                                              | [`invites.js`](../../../frontend/src/services/invites.js), [`workCases.js`](../../../frontend/src/services/workCases.js)                                                                    | [`EscrowController`](../../../backend/src/main/java/com/gighub/wallet/controller/EscrowController.java), Escrow Service·Test                                                                                                                      | 로그인 복귀·OWNER 뱃지 Mock은 구현; 수락 Body에서 서명 제거, Token 도출·계약서 자동 생성·Replay 필요  |
| 알바생 홈·근무 이력     | DASH-001~003, WORK-007                    | `planned`     | `worker-home`, `worker-work`, `worker-work-case-detail`                                                 | [`worker.js`](../../../frontend/src/services/worker.js), [`earning.js`](../../../frontend/src/utils/earning.js), [`useEarningTick.js`](../../../frontend/src/composables/useEarningTick.js) | 없음                                                                                                                                                                                                                                              | 60초·세금 계산은 구현; 목표 Instant/Break 필드 Adapter, 무급 휴게 규칙과 누락 상태 표시가 미구현      |
| QR·GPS 출퇴근           | ATT-001~004                               | `planned`     | `owner-qr`, `worker-scan`                                                                               | [`workplaces.js`](../../../frontend/src/services/workplaces.js), [`worker.js`](../../../frontend/src/services/worker.js)                                                                    | Controller·Service 없음; [`사업장 QR 후보 Migration`](../../../backend/src/main/resources/db/migration/V202607311427__move_qr_tokens_to_workplace_scope.sql)                                                                                      | 소유자 후보 채택 후 HMAC QR 조회·재발급, 100m·자동 판별, GPS와 조기퇴근 확인 UI·API 필요              |
| 노쇼·퇴근 누락 판정     | ATT-005, ATT-006                          | `on_hold`     | `owner-attendance`, 양 역할 근무 상세, `worker-work`                                                    | `CHECK_OUT_MISSING` 표시 없음                                                                                                                                                               | Scheduler·Service 없음                                                                                                                                                                                                                            | 상태 추가는 승인; 소유자 Migration과 판정 시점·실행 주체·해소·정산 정책 확정 전 구현 보류             |
| 정산 승인·지급          | SETTLE-001~004                            | `planned`     | 양 역할 근무 상세                                                                                       | [`workCases.js`](../../../frontend/src/services/workCases.js)                                                                                                                               | [`EscrowController`](../../../backend/src/main/java/com/gighub/wallet/controller/EscrowController.java), [`SettlementServiceImpl`](../../../backend/src/main/java/com/gighub/settlement/service/impl/SettlementServiceImpl.java), Service·DB Test | `completedAt: Instant` 계약은 정렬됨; 누락 상태 정산 정책, Replay Header와 자동 Scheduler 필요        |
| 노쇼 환불               | SETTLE-005                                | `on_hold`     | 근무 상세·홈 상태                                                                                       | Mock 상태 표시                                                                                                                                                                              | 환불 Service·Scheduler 없음                                                                                                                                                                                                                       | Settlement 종료 상태 결정과 Migration 필요                                                            |
| 연락·임금분쟁 접수·조회 | CONTACT-001, DISPUTE-001~002              | `planned`     | `worker-work-case-detail`, `worker-report`                                                              | [`workCases.js`](../../../frontend/src/services/workCases.js)                                                                                                                               | 없음                                                                                                                                                                                                                                              | 신고 생성·당사자 조회는 Mock; 신고는 정산과 분리                                                      |
| 관리자 분쟁 처리        | DISPUTE-003 관리자 확장                   | `on_hold`     | 현재 route 없음                                                                                         | 없음                                                                                                                                                                                        | 없음                                                                                                                                                                                                                                              | ADMIN 역할·운영 화면·처리 Workflow 확정 필요                                                          |
| 시스템 생성 계약서      | DOC-001~004, DOC-009~011                  | `planned`     | `owner-documents`, `owner-document-viewer`, `worker-documents`, `worker-document-viewer`                | [`documents.js`](../../../frontend/src/services/documents.js), 양 역할 Documents View                                                                                                       | 없음                                                                                                                                                                                                                                              | 목표는 시스템 생성·조회 전용; 현재 사장 업로드와 양 역할 삭제 UI·Mock 제거, 저장소·권한·감사 API 필요 |
| 보건증 관리·공유        | DOC-001, DOC-003, DOC-005~011             | `planned`     | `owner-documents`, `owner-document-viewer`, `worker-documents`, `worker-document-viewer`                | [`documents.js`](../../../frontend/src/services/documents.js)                                                                                                                               | 없음                                                                                                                                                                                                                                              | WORKER 업로드·수정·삭제·공유와 OWNER 읽기 전용 권한·저장소·감사 API 필요                              |
| 계약서 3년 자동 삭제    | DOC-012, COMMON-003                       | `on_hold`     | 기존 문서함·Viewer                                                                                      | 만료 후 접근 차단 표시 필요                                                                                                                                                                 | Scheduler·저장소 삭제 Service 없음                                                                                                                                                                                                                | 사용자 DELETE 없음; 기준일과 파일·Metadata·Checksum·감사 삭제 범위 확정 후 구현                       |
| 전자서명 원본           | CONTRACT-001 확장                         | `on_hold`     | 현재 route 없음                                                                                         | 현재 `signatureImage` Mock 입력은 제거 대상                                                                                                                                                 | 스키마는 있으나 저장소·API 정책 없음                                                                                                                                                                                                              | 저장소 Key·Checksum·양측 서명·증거 보존 정책 필요                                                     |
| 알림                    | ALERT-001, ALERT-002                      | `on_hold`     | 전역 Header 아이콘                                                                                      | [`notifications.js`](../../../frontend/src/services/notifications.js), Notifications Store                                                                                                  | 없음                                                                                                                                                                                                                                              | Store에서 기능 비활성; Schema·Event 전달 방식 필요                                                    |
| 공통 응답·오류·시간     | COMMON-001~004                            | `planned`     | 전 화면                                                                                                 | [`http.js`](../../../frontend/src/services/http.js)                                                                                                                                         | [`CommonExceptionHandler`](../../../backend/src/main/java/com/gighub/common/exception/CommonExceptionHandler.java), [`ApiTimes`](../../../backend/src/main/java/com/gighub/common/api/ApiTimes.java), Config·Service                              | 기존 거래·정산 시간 변환 반영; Health·직접 오류·신규 API와 기능별 Page 통합 필요                      |
| PortOne                 | EXT-001, EXT-002                          | `on_hold`     | 향후 충전 흐름                                                                                          | 없음                                                                                                                                                                                        | 없음                                                                                                                                                                                                                                              | Provider 주문·Webhook Inbox Schema와 연동 정책 필요                                                   |

## 승인 목표에 필요한 소유자 DB 작업

이 표는 구현 Agent가 Flyway나 DDL을 작성하라는 뜻이 아닙니다. DB 변경은 프로젝트 소유자에게
요청하고, 소유자가 새 Migration과 통합 DDL을 반영한 뒤에만 현재 Schema로 취급합니다.
현재 작업공간의 `202607311427`·`202607311428`과 통합 Snapshot도 정책 확정 전 생성된 미추적
후보이므로 같은 소유자 검토·채택이 필요합니다.

| 연결 기능                | 현재 Head와 차이                                                                      | 소유자 요청                                                                                       |
| ------------------------ | ------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------- |
| 퇴근 누락 상태           | `work_cases.status`와 근로자 필수 CHECK에 `CHECK_OUT_MISSING` 없음                    | 두 CHECK에 상태 추가, `(status, ends_at, id)` Scheduler Index와 기존 `IN_PROGRESS` 이관 여부 검토 |
| 사업장 등록·QR 위치 검증 | `workplaces.radius_meters`와 근무 Snapshot `allowed_radius_meters`가 다른 양수도 허용 | 애플리케이션이 두 값에 100m를 강제하는 것으로 충분한지, DB에서도 100 고정 CHECK가 필요한지 결정   |
| 시스템 생성 계약서       | `EMPLOYMENT_CONTRACT`도 `work_case_id=NULL` 가능                                      | 계약서 근무 연결을 DB에서도 강제할지 결정                                                         |
| 계약서 3년 자동 삭제     | 후보에 `DELETED`는 있으나 기준일·삭제 범위가 미정이고 전용 추적 컬럼·Index 없음       | 제품 결정을 먼저 확정한 뒤 필요한 보존·삭제 추적 컬럼과 Index를 결정                              |

자세한 목표와 미결정 사항은 [`REQUIREMENTS.md`](../../specs/REQUIREMENTS.md)의
“승인됐지만 현재 DB에 없는 변경”, 현재 구조는
[`SCHEMA_OVERVIEW.md`](../../agent/SCHEMA_OVERVIEW.md)의 “Approved target gaps”를 읽습니다.

## API 구현 순서

기능을 하나씩 구현할 때는 다음 순서로 좁게 진행합니다.

1. 이 표에서 한 행을 선택하고 연결된 요구사항과 수용 기준을 읽습니다.
2. 연결된 View가 실제로 보내는 입력과 프론트 Service의 Mock 반환값을 확인합니다.
3. 소유자가 작성한 Flyway에서 저장 대상 컬럼·상태·제약을 확인합니다. 필요한 구조가 없으면
   테이블·컬럼·제약과 전환 요구를 소유자에게 보고하고 Migration이나 DDL을 직접 수정하지
   않습니다.
4. `API_SPEC.md`의 승인된 Method·Path·입출력·오류·권한과 현재 코드를 대조합니다.
5. `Controller → Service → Mapper`와 요청·응답 DTO를 구현합니다.
6. Controller Test와 Service 또는 DB Test로 수용 기준을 검증합니다.
7. 구현과 테스트가 완료된 Endpoint만 실제 API로 전환하고 나머지 Endpoint는 Mock으로
   유지합니다.
8. 이 표의 근거와 API 연결 상태를 갱신합니다. 모든 수용 기준을 충족한 뒤에만
   `implemented`로 변경합니다.
9. 한 기능의 실제 연동이 끝나면 해당 Mock과 전환 설정을 제거하고 운영 빌드에서 Mock이
   실행되지 않는지 검증합니다.

회원가입을 먼저 구현한 뒤 로그인, 사업장, 근무 등록처럼 기능 단위 Vertical Slice로 이어 가도
됩니다. 다른 기능의 미구현 계약을 동시에 확정할 필요는 없습니다.

## 안정적인 연결 키

- 요구사항: `AUTH-002`, `WALLET-003` 같은 요구사항 ID
- Vue: URL보다 `owner-signup`, `worker-work` 같은 Route name
- API: Method + 정규화된 Path
- Backend: Controller Class와 대표 Test
- DB: Table 이름과 Flyway Version. 추적 Head는 `202607301152`; QR `202607311427`과 비밀번호
  재설정 `202607311428`은 소유자 채택 대기 후보. `CHECK_OUT_MISSING`은 아직 Version이 없는
  소유자 변경 요청

파일명이나 화면 문구가 바뀌어도 이 키를 먼저 갱신하면 Agent가 관련 자료를 다시 찾기 쉽습니다.

## 사용하지 않는 과거 기준

| 과거 기준                                  | 현재 기준                                                                |
| ------------------------------------------ | ------------------------------------------------------------------------ |
| JWT/accessToken 혼용                       | HttpSession 목표; 현재 정식 인증은 미구현                                |
| `/api/shifts/**`                           | `/api/work-cases/**` 계약 후보와 `work_cases` Schema                     |
| `work_cases.status=INVITED`                | 미수락 근무는 `DRAFT`, 초대 상태는 `work_invitations`                    |
| `/api/auth/check-login-id`, `/check-email` | 프론트 후보는 `/login-id-availability`, `/email-availability`            |
| `/api/worker/scan`                         | 프론트 후보는 `/api/attendance/scans`                                    |
| 근무·동작별 만료형 QR                      | 사업장별 고정 HMAC QR, OWNER 재발급, 서버 출퇴근 자동 판별               |
| 퇴근 미스캔을 `NO_SHOW`로 처리             | 성공 출근이 있으면 `CHECK_OUT_MISSING`; 해소·정산 흐름은 `on_hold`       |
| 사용자 지정 사업장 인증 반경               | 모든 사업장 100m 고정, 요청·수정 Payload에서 제외                        |
| 로그인 아이디·이메일·이름 프로필 수정      | 세 값은 읽기 전용, 전화번호만 수정                                       |
| 일급 전체 3.3% 공제                        | 15만원 초과분에 소득세 2.7%·지방소득세 0.27% 표시식                      |
| 계약서 직접·스캔 업로드와 사용자 삭제      | 계약 확정 시 시스템 생성, 3년 후 백엔드 자동 삭제; 상세 범위는 `on_hold` |
| 초대 수락 Body의 사용자·근무·금액·서명     | `/api/invitations/{token}/accept` Body 없음                              |
| 충전·출금 `bankAccountId` 선택             | 수동 `bankCode/accountNo` 입력 후 서버 내부 ID 도출                      |
| 알림 API 구현 완료                         | Schema Gap으로 비활성                                                    |

과거 값이 코드 주석, `.http` 예제나 Agent 생성 엑셀에 남아 있어도 이 표와 최신 실행 코드를
우선 확인합니다.
