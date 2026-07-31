# 제품 결정 기록

| 항목        | 값              |
| ----------- | --------------- |
| 명세 릴리스 | `1.0.0`         |
| 승인일      | 2026-07-31      |
| 소유자      | PM/Admin Master |

이 문서는 제품 계약에 영향을 주는 승인 결정, 아직 답이 필요한 결정과 폐기된 방향만
기록합니다. 개발 진행률, 작업 목록과 코드 위치는 기록하지 않습니다.

## Approved

| ID                           | 승인 결정                                                                                                                        | 영향 범위                            |
| ---------------------------- | -------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------ |
| DEC-AUTH-SESSION             | 인증은 Spring Security 기반 HttpSession이며 JWT와 Access Token을 사용하지 않는다. 상태 변경 요청은 CSRF를 검증한다.              | AUTH-004, AUTH-006, AUTH-007         |
| DEC-API-ENVELOPE             | 단일 성공은 `{data}`, 목록은 `{data:{content,page}}`, 오류는 `{code,message,traceId,fieldErrors?}`로 통일한다.                   | COMMON-001, 모든 REST API            |
| DEC-LOCAL-CORS               | 로컬 직접 호출 Origin은 `http://localhost:5173` 하나이며 Credential을 허용한다.                                                  | 로컬 API 접근                        |
| DEC-TIME                     | API 시점은 UTC `Instant`, 날짜는 `LocalDate`, DB `DATETIME(6)`은 `Asia/Seoul` 지역 시각으로 사용한다.                            | COMMON-004, 모든 시간 필드           |
| DEC-PAGE                     | 목록 기본값은 `page=0`, `size=20`이며 최대 `size=100`이다.                                                                       | 모든 목록 API                        |
| DEC-PROFILE-IMMUTABLE        | 가입 후 `loginId`, `email`, `name`은 변경할 수 없고 일반 프로필에서는 `phone`만 변경한다.                                        | AUTH-008                             |
| DEC-INVITE-LOGIN-BADGE       | 비로그인 초대 접근은 WORKER 로그인으로 연결하고 원래 경로로 복귀한 뒤 초대한 OWNER 뱃지를 표시한다.                              | BADGE-003, INVITE-002, INVITE-003    |
| DEC-INVITE-ACCEPT            | 초대 수락은 `/api/invitations/{token}/accept`의 Body 없는 POST이며 서버가 Token으로 당사자, 근무와 금액을 도출한다.              | CONTRACT-001, CONTRACT-002           |
| DEC-WORKPLACE-RADIUS         | 사업장 인증 반경은 입력·수정할 수 없는 100m 고정값이다.                                                                          | WORKPLACE-001, ATT-003               |
| DEC-WORKPLACE-IMMUTABLE      | 사업장 대표자, 좌표와 인증 반경은 등록 후 사용자가 직접 수정할 수 없다.                                                          | WORKPLACE-002                        |
| DEC-DASHBOARD-REFRESH        | 확보 안심금액은 화면 진입 즉시 계산하고 근무 종료 전까지 60초마다 갱신한다.                                                      | DASH-002                             |
| DEC-DAILY-WORKER-TAX         | 예상 실수령액은 15만원 초과 과세표준에 소득세 2.7%, 지방소득세 0.27%를 적용하고 각 10원 미만 절사 및 소액부징수 규칙을 적용한다. | DASH-003                             |
| DEC-BANK-INPUT               | 사용자는 은행과 계좌번호를 직접 입력하며 서버가 인증 사용자 소유의 내부 계좌 ID를 찾는다.                                        | BANK-001, WALLET-002, WALLET-003     |
| DEC-BALANCE-REFETCH          | 충전·출금 응답에 최신 잔액을 합치지 않고 성공 후 잔액을 다시 조회한다.                                                           | WALLET-002, WALLET-003               |
| DEC-IDEMPOTENCY              | 충전, 출금, 초대 수락과 정산 승인은 `Idempotency-Key`로 같은 요청을 한 번만 반영한다.                                            | WALLET-006                           |
| DEC-SETTLEMENT-TIME          | 정산 완료 시점의 외부 필드명은 `completedAt`이며 UTC `Instant`다.                                                                | SETTLE-002                           |
| DEC-QR-FIXED                 | QR은 사업장별 비만료 고정 QR 하나다. 같은 QR의 첫 성공 스캔은 출근, 두 번째는 퇴근이며 OWNER 재발급 즉시 이전 QR을 폐기한다.     | ATT-001, ATT-002                     |
| DEC-EARLY-CHECKOUT           | 예정 종료 전 두 번째 스캔은 즉시 기록하지 않고 사용자 확인 뒤 같은 요청을 재전송해 퇴근 처리한다.                                | ATT-004                              |
| DEC-CHECK-OUT-MISSING        | 성공 출근 뒤 퇴근 기록이 없는 근무는 `CHECK_OUT_MISSING`이며 `NO_SHOW`와 구분한다.                                               | WORK-001, WORK-007, ATT-005, ATT-006 |
| DEC-CONTRACT-AUTO-GENERATION | 근로계약서는 계약 확정 과정에서 시스템이 자동 생성하며 사용자 직접 업로드와 스캔 교체 기능을 제공하지 않는다.                    | CONTRACT-003, DOC-002, DOC-010       |
| DEC-CONTRACT-RETENTION       | 근로계약서는 사용자가 직접 삭제할 수 없고 근로일 이후 3년간 보존한 뒤 서버가 자동 삭제한다.                                      | DOC-012, COMMON-003                  |
| DEC-DOCUMENT-STORAGE         | 문서 파일은 외부 비공개 저장소에 두고 식별 Key, Checksum, Version과 접근 감사를 보존한다.                                        | DOC-009, DOC-011                     |
| DEC-DISPUTE-SETTLEMENT       | MVP 임금분쟁 신고는 정산 상태와 예정 시각을 자동으로 바꾸지 않는다.                                                              | DISPUTE-001, DISPUTE-003             |
| DEC-PASSWORD-RESET           | 비밀번호 재설정은 이메일 식별, Hash 저장, 만료와 1회 사용 Token 방식으로 제공한다.                                               | AUTH-011                             |

## Open

Open 항목은 승인된 경계 밖의 선택지입니다. 결론이 나기 전에는 요구사항이나 API에 임의 값을
추가하지 않습니다.

| ID                                 | 결정이 필요한 질문                                                                                                       | 이미 승인된 경계                                                                                    |
| ---------------------------------- | ------------------------------------------------------------------------------------------------------------------------ | --------------------------------------------------------------------------------------------------- |
| DEC-OPEN-AUTH-SESSION-ANONYMOUS    | 비인증 `GET /api/auth/session`을 `200 authenticated=false`로 반환할지 401로 반환할지                                     | 인증 Session이 유일한 로그인 기준이며 인증 사용자 응답에는 역할과 사업장 설정 필요 여부가 포함된다. |
| DEC-OPEN-PASSWORD-RESET-DELIVERY   | 비밀번호 재설정 Token 원문을 이메일, 개발용 별도 채널 등 어떤 방식으로 전달할지                                          | API 응답과 일반 로그에는 Token 원문을 노출하지 않고 DB에는 Hash만 저장한다.                         |
| DEC-OPEN-WORKPLACE-COORDINATES     | 좌표가 있는 사업장의 주소를 수정할 때 서버 재지오코딩, 재인증 또는 주소 수정 제한 중 어떤 규칙을 적용할지                | 대표자, 좌표와 100m 반경은 사용자가 직접 수정하지 않는다.                                           |
| DEC-OPEN-DASHBOARD-BREAK           | 휴게 분수만 있는 근무에서 무급 휴게를 확보 안심금액의 시간 경과 계산에 어떻게 반영할지                                   | 화면 계산값은 60초마다 갱신되고 0원~합의 일급 범위를 지킨다.                                        |
| DEC-OPEN-QR-REISSUE-IDEMPOTENCY    | QR 재발급에 `Idempotency-Key`를 적용할지, 다른 재시도 식별자를 사용할지                                                  | 한 번의 승인된 재발급은 이전 QR을 즉시 폐기하고 활성 QR 하나만 남긴다.                              |
| DEC-OPEN-CHECK-OUT-MISSING-FLOW    | 퇴근 누락 판정 시점과 실행 주체, 늦은 QR 허용 시간, 수동 보정 주체·증거, 해소 상태 전이와 정산·임금 처리 규칙은 무엇인지 | 성공 출근과 배정 WORKER가 있고 성공 퇴근이 없는 근무만 `CHECK_OUT_MISSING`이며 노쇼가 아니다.       |
| DEC-OPEN-NO-SHOW-SETTLEMENT        | 노쇼 환불 후 Settlement가 사용할 재처리 불가 종료 상태는 무엇인지                                                        | WORKER 지급 없이 OWNER에게 한 번만 환불하고 `ESCROW_REFUND` 원장을 남긴다.                          |
| DEC-OPEN-E-SIGN-EVIDENCE           | 최종 동의 증거, 양측 서명 표현, 원본 보존 위치와 Checksum 정책은 무엇인지                                                | 초대 수락 Body에는 서명 이미지나 원본 파일을 받지 않는다.                                           |
| DEC-OPEN-DOCUMENT-RETENTION-SCOPE  | 야간 근무의 보존 기준일을 시작일과 종료일 중 무엇으로 볼지, 3년 뒤 파일·Metadata·Checksum·감사 기록 중 무엇을 삭제할지   | 계약서는 근로일 이후 3년간 보존하고 사용자가 직접 삭제할 수 없으며 만료 뒤 서버가 자동 삭제한다.    |
| DEC-OPEN-NOTIFICATION-CONTRACT     | 알림 유형, 이벤트 식별자, 목록·읽음 처리 Payload와 전달 방식을 어떻게 정의할지                                           | 수신자만 자신의 알림을 조회하고 읽음 처리하며 동일 이벤트 중복을 막는다.                            |
| DEC-OPEN-ADMIN-DISPUTE             | 분쟁을 처리할 관리자 역할, 권한과 상태 변경 절차는 무엇인지                                                              | 당사자는 신고를 조회하며 신고 자체는 정산을 보류하지 않는다.                                        |
| DEC-OPEN-PAYMENT-PROVIDER          | 외부 결제 Provider, 주문·거래·Webhook 계약과 취소 범위를 어떻게 정할지                                                   | 서버 검증 전에는 지갑을 증가시키지 않고 중복 Webhook을 한 번만 반영한다.                            |
| DEC-OPEN-ERROR-CATALOG             | QR, 초대, 근태, 문서, 정산별 세부 오류 Code와 HTTP Status를 어떤 목록으로 고정할지                                       | 공통 Envelope, 401/403 구분과 이미 승인된 오류 Code는 유지한다.                                     |
| DEC-OPEN-WORK-CASE-RESPONSE-SHAPES | 근무 요약과 상세 응답의 전체 필드, Capability와 중첩 구조를 어떻게 고정할지                                              | 승인된 경로, 핵심 상태와 근무 조건 필드는 유지한다.                                                 |
| DEC-OPEN-DOCUMENT-RESPONSE-SHAPES  | 문서 목록, 파일 Metadata와 공유 응답의 전체 필드 구조를 어떻게 고정할지                                                  | 계약서 자동 생성·삭제 금지, 보건증 업로드·공유 권한과 승인된 경로는 유지한다.                       |

## Retired

| 폐기 결정                                                          | 대체 결정                                                         |
| ------------------------------------------------------------------ | ----------------------------------------------------------------- |
| JWT 또는 `accessToken`을 로그인 성공 응답으로 반환                 | `DEC-AUTH-SESSION`                                                |
| `/api/auth/check-login-id`, `/api/auth/check-email`                | `/api/auth/login-id-availability`, `/api/auth/email-availability` |
| `/api/invites/{token}/accept`                                      | `/api/invitations/{token}/accept`                                 |
| `/api/shifts/**`, `/api/worker/scan`                               | `/api/work-cases/**`, `/api/attendance/scans`                     |
| 근무별·동작별 만료 QR                                              | `DEC-QR-FIXED`                                                    |
| 퇴근 누락을 노쇼로 처리                                            | `DEC-CHECK-OUT-MISSING`                                           |
| 조기 퇴근 두 번째 스캔을 즉시 기록                                 | `DEC-EARLY-CHECKOUT`                                              |
| 가입 후 로그인 아이디·이메일·이름 수정                             | `DEC-PROFILE-IMMUTABLE`                                           |
| 사용자가 사업장 인증 반경을 입력·수정                              | `DEC-WORKPLACE-RADIUS`                                            |
| 대표자와 좌표를 일반 수정 API로 변경                               | `DEC-WORKPLACE-IMMUTABLE`                                         |
| 일급 전체에 단순 3.3% 공제                                         | `DEC-DAILY-WORKER-TAX`                                            |
| 사용자가 내부 `bankAccountId`를 선택해 충전·출금                   | `DEC-BANK-INPUT`                                                  |
| 초대 수락 Body에 사용자·OWNER·근무·금액 ID 또는 서명 이미지를 포함 | `DEC-INVITE-ACCEPT`                                               |
| 근로계약서 직접 업로드, 스캔 교체 또는 사용자 삭제                 | `DEC-CONTRACT-AUTO-GENERATION`, `DEC-CONTRACT-RETENTION`          |
| 정산 완료 시점 필드 `settledAt`                                    | `completedAt`                                                     |
