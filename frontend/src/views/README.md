# 화면(view) 구현 가이드 — 팀 분담

공통 인프라(라우터·서비스·스토어·유틸·공통 컴포넌트)는 **이미 완성**되어 있습니다.
팀원은 **자기 view 파일만** 열어 채웁니다. 각 stub 파일 상단 주석에 `라우트·권한·연계 API·사용 service`가 적혀 있으니 그대로 따르면 됩니다.

## 절대 건드리지 않기 (공통 영역)

- `src/router/index.js` — 라우트·가드 (라우트 추가가 필요하면 팀에 먼저 공유)
- `src/services/*` — API 함수 **시그니처**. mock↔실제 전환은 각 파일 `USE_MOCK` 로만.
- `src/stores/*`, `src/components/common/*`, `src/components/{owner,worker}/*Nav.vue`, `src/layouts/*`
- `src/assets/base.css` — 색·간격·글자 토큰. 없는 값이 필요하면 **여기 먼저 추가**하고 팀 공유(개별 컴포넌트에 hex/px 금지).

## 작업 방법 (요약)

1. 데이터는 `@/services/*` 함수 호출. 화면 상태만 필요하면 `@/stores/*`.
2. 색·간격·글자·모서리는 **base.css 토큰(`var(--...)`)만**.
3. 아이콘 `lucide-vue-next`, 금액/날짜 `@/utils/format`, 상태 라벨 `@/utils/constants`/`StatusChip`.
4. 폼 검증 `@/utils/validators`, 안내는 `useUiStore().toast(...)`.
5. 계산값(확보 안심금액 등)은 **서버 응답 그대로** — 프론트 재계산 금지.

## 공통 컴포넌트 빠른 참조 (`@/components/common`)

| 컴포넌트                       | 용도                                                        |
| ------------------------------ | ----------------------------------------------------------- |
| `AppTopBar`, `AppBackHeader`   | 상단바(레이아웃이 자동) / 서브 화면 뒤로가기 헤더           |
| `BaseButton`                   | 버튼 (variant: primary·owner·worker·danger·secondary·ghost) |
| `AppField`                     | 라벨+입력+에러 (`v-model`, `#suffix` 슬롯=중복확인 버튼)    |
| `BaseBottomSheet`, `BaseModal` | 바텀시트(필터·공유·등록·문의) / 중앙 모달(확인·경고)        |
| `StatusChip`                   | 상태 칩 (`kind`: workCase·tx·settle·today)                  |
| `TrustBadge`                   | 신뢰 뱃지 (`role`: owner·worker, `level` 0~3)               |
| `EmptyState`                   | 빈 리스트·자리표시                                          |

> `ToastHost`·`NotificationModal` 은 App.vue 에 이미 전역 배치됨 — 화면에서 다시 넣지 않는다.

## 화면 분담 (8 패키지)

| 패키지                      | 화면(파일)                                                                                                                                                    |
| --------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **A. 인증/온보딩**          | `OnboardingView` · `auth/OwnerLoginView` · `auth/OwnerSignupView` · `auth/WorkerLoginView` · `auth/WorkerSignupView`                                          |
| **B. 사장 지갑**            | `owner/OwnerHomeView`(본문 완성됨·필터시트 TODO) · `owner/wallet/OwnerChargeView` · `owner/wallet/OwnerWithdrawView`                                          |
| **C. 사장 근태/QR**         | `owner/OwnerAttendanceView` · `owner/workCase/OwnerWorkCaseNewView` · `owner/workCase/OwnerWorkCaseDetailView` · `owner/OwnerQrView`                          |
| **D. 사장 문서/사업장**     | `owner/OwnerDocumentsView` · `owner/OwnerDocumentViewerView` · `owner/workplace/OwnerWorkplaceNewView` · `owner/workplace/OwnerWorkplaceManageView`           |
| **E. 사장 마이**            | `owner/OwnerMyPageView` · `owner/mypage/OwnerProfileEditView` · `owner/mypage/OwnerPasswordEditView`                                                          |
| **F. 알바생 홈/근로**       | `worker/WorkerHomeView` · `worker/WorkerWorkView` · `worker/workCase/WorkerWorkCaseDetailView` · `worker/workCase/WorkerReportView` · `worker/WorkerScanView` |
| **G. 알바생 문서**          | `worker/WorkerDocumentsView` · `worker/WorkerDocumentViewerView` (+ 보건증 공유·등록 모달은 BaseBottomSheet 로 조립)                                          |
| **H. 알바생 마이 + 딥링크** | `worker/WorkerMyPageView` · `worker/mypage/WorkerProfileEditView` · `worker/mypage/WorkerPasswordEditView` · `invite/InviteConfirmView`                       |

## 화면 개발 팁

- 로그인 없이 화면을 보려면: `src/stores/auth.js` 의 `DEV_SEED.role` 을 `'OWNER'`/`'WORKER'` 로 바꾼다.
- 모든 API 는 현재 `USE_MOCK=true` — 백엔드 없이 mock 데이터로 화면을 완성할 수 있다.
- 커밋 전: `npm run lint && npm run format:check && npm run test:run && npm run build` 통과 확인.
