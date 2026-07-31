# Frontend Route Overview (Archived 2026-07-31)

> [!WARNING]
> This is an archived snapshot, not a current development source.
>
> - Archived: 2026-07-31
> - Reason: the route inventory duplicated executable router state and became stale
> - Current guide: [`../agent/IMPLEMENTATION_GUIDE.md`](../agent/IMPLEMENTATION_GUIDE.md)

## Purpose and authority

This is the compact page-routing index for repository agents and contributors.

| Item                   | Current baseline                                                        |
| ---------------------- | ----------------------------------------------------------------------- |
| Status                 | Current                                                                 |
| Last verified          | 2026-07-31                                                              |
| Source of truth        | `frontend/src/router/index.js`                                          |
| Named page routes      | 33                                                                      |
| Navigation-only routes | 2                                                                       |
| Route views            | 33 existing Vue files                                                   |
| Tab layouts            | `OwnerTabLayout.vue`, `WorkerTabLayout.vue`                             |
| Scope                  | Routes currently registered in Vue Router and their existing View files |

Requirements-only and planned pages are intentionally excluded. A page listed here is
"implemented" only in the routing sense: the route is registered and its target View exists.
It does not prove that its API integration or backend Controller is complete. Inspect the
affected `frontend/src/services/` mock path and matching backend Controller before changing an
end-to-end flow. Use
[`specs/DEVELOPMENT_ROUTING_TABLE-2026-07-31.md`](specs/DEVELOPMENT_ROUTING_TABLE-2026-07-31.md) to follow a
feature from its requirement and route name into the current frontend service, backend entrypoint,
database scope, and verification evidence; use
[`../specs/API_SPEC.md`](../specs/API_SPEC.md) for the current HTTP contract comparison.

When this overview and executable code disagree, follow `frontend/src/router/index.js` and update
this file in the same change.

## Access and layout labels

| Label         | Meaning                                                                                |
| ------------- | -------------------------------------------------------------------------------------- |
| Guest only    | `meta.guestOnly`; an authenticated user is redirected to the home for the current role |
| OWNER         | Authentication required and `role: 'OWNER'`                                            |
| WORKER        | Authentication required and `role: 'WORKER'`                                           |
| WORKER invite | WORKER-only authenticated deep link with `meta.invite`                                 |
| Public        | No authentication or role metadata                                                     |
| Tab           | Rendered inside the role-specific top bar and bottom navigation layout                 |
| Standalone    | Top-level page rendered outside a tab layout                                           |

## Entry and authentication pages

| Page            | Path             | Route name      | View                                                                         | Access / layout         |
| --------------- | ---------------- | --------------- | ---------------------------------------------------------------------------- | ----------------------- |
| 온보딩          | `/`              | `onboarding`    | [`OnboardingView.vue`](../../frontend/src/views/OnboardingView.vue)          | Guest only / Standalone |
| 사장 로그인     | `/owner/login`   | `owner-login`   | [`OwnerLoginView.vue`](../../frontend/src/views/auth/OwnerLoginView.vue)     | Guest only / Standalone |
| 사장 회원가입   | `/owner/signup`  | `owner-signup`  | [`OwnerSignupView.vue`](../../frontend/src/views/auth/OwnerSignupView.vue)   | Guest only / Standalone |
| 알바생 로그인   | `/worker/login`  | `worker-login`  | [`WorkerLoginView.vue`](../../frontend/src/views/auth/WorkerLoginView.vue)   | Guest only / Standalone |
| 알바생 회원가입 | `/worker/signup` | `worker-signup` | [`WorkerSignupView.vue`](../../frontend/src/views/auth/WorkerSignupView.vue) | Guest only / Standalone |

## OWNER pages

OWNER tab pages render inside
[`OwnerTabLayout.vue`](../../frontend/src/layouts/OwnerTabLayout.vue).

| Page               | Path                                       | Route name               | View                                                                                                    | Access / layout    |
| ------------------ | ------------------------------------------ | ------------------------ | ------------------------------------------------------------------------------------------------------- | ------------------ |
| 사장 홈·지갑       | `/owner/home`                              | `owner-home`             | [`OwnerHomeView.vue`](../../frontend/src/views/owner/OwnerHomeView.vue)                                 | OWNER / Tab        |
| 근태관리           | `/owner/attendance`                        | `owner-attendance`       | [`OwnerAttendanceView.vue`](../../frontend/src/views/owner/OwnerAttendanceView.vue)                     | OWNER / Tab        |
| 사장 문서함        | `/owner/documents`                         | `owner-documents`        | [`OwnerDocumentsView.vue`](../../frontend/src/views/owner/OwnerDocumentsView.vue)                       | OWNER / Tab        |
| 출퇴근 QR          | `/owner/qr`                                | `owner-qr`               | [`OwnerQrView.vue`](../../frontend/src/views/owner/OwnerQrView.vue)                                     | OWNER / Tab        |
| 사업장 등록        | `/owner/workplaces/new`                    | `owner-workplace-new`    | [`OwnerWorkplaceNewView.vue`](../../frontend/src/views/owner/workplace/OwnerWorkplaceNewView.vue)       | OWNER / Standalone |
| 지갑 충전          | `/owner/wallet/charge`                     | `owner-charge`           | [`OwnerChargeView.vue`](../../frontend/src/views/owner/wallet/OwnerChargeView.vue)                      | OWNER / Standalone |
| 지갑 출금          | `/owner/wallet/withdraw`                   | `owner-withdraw`         | [`OwnerWithdrawView.vue`](../../frontend/src/views/owner/wallet/OwnerWithdrawView.vue)                  | OWNER / Standalone |
| 근무 포지션 등록   | `/owner/attendance/work-cases/new`         | `owner-work-case-new`    | [`OwnerWorkCaseNewView.vue`](../../frontend/src/views/owner/workCase/OwnerWorkCaseNewView.vue)          | OWNER / Standalone |
| 근무 상세          | `/owner/attendance/work-cases/:workCaseId` | `owner-work-case-detail` | [`OwnerWorkCaseDetailView.vue`](../../frontend/src/views/owner/workCase/OwnerWorkCaseDetailView.vue)    | OWNER / Standalone |
| 사장 문서 보기     | `/owner/documents/:documentId`             | `owner-document-viewer`  | [`OwnerDocumentViewerView.vue`](../../frontend/src/views/owner/OwnerDocumentViewerView.vue)             | OWNER / Standalone |
| 사장 마이페이지    | `/owner/mypage`                            | `owner-mypage`           | [`OwnerMyPageView.vue`](../../frontend/src/views/owner/OwnerMyPageView.vue)                             | OWNER / Standalone |
| 사업장 관리        | `/owner/mypage/workplaces`                 | `owner-workplaces`       | [`OwnerWorkplaceManageView.vue`](../../frontend/src/views/owner/workplace/OwnerWorkplaceManageView.vue) | OWNER / Standalone |
| 사장 회원정보 변경 | `/owner/mypage/profile`                    | `owner-profile`          | [`OwnerProfileEditView.vue`](../../frontend/src/views/owner/mypage/OwnerProfileEditView.vue)            | OWNER / Standalone |
| 사장 비밀번호 변경 | `/owner/mypage/password`                   | `owner-password`         | [`OwnerPasswordEditView.vue`](../../frontend/src/views/owner/mypage/OwnerPasswordEditView.vue)          | OWNER / Standalone |

## WORKER pages

WORKER tab pages render inside
[`WorkerTabLayout.vue`](../../frontend/src/layouts/WorkerTabLayout.vue).

| Page                 | Path                                         | Route name                | View                                                                                                    | Access / layout     |
| -------------------- | -------------------------------------------- | ------------------------- | ------------------------------------------------------------------------------------------------------- | ------------------- |
| 알바생 홈·안심지갑   | `/worker/home`                               | `worker-home`             | [`WorkerHomeView.vue`](../../frontend/src/views/worker/WorkerHomeView.vue)                              | WORKER / Tab        |
| 근로관리             | `/worker/work`                               | `worker-work`             | [`WorkerWorkView.vue`](../../frontend/src/views/worker/WorkerWorkView.vue)                              | WORKER / Tab        |
| QR 출퇴근            | `/worker/scan`                               | `worker-scan`             | [`WorkerScanView.vue`](../../frontend/src/views/worker/WorkerScanView.vue)                              | WORKER / Tab        |
| 알바생 문서함        | `/worker/documents`                          | `worker-documents`        | [`WorkerDocumentsView.vue`](../../frontend/src/views/worker/WorkerDocumentsView.vue)                    | WORKER / Tab        |
| 안심지갑 출금        | `/worker/wallet/withdraw`                    | `worker-withdraw`         | [`WorkerWithdrawView.vue`](../../frontend/src/views/worker/wallet/WorkerWithdrawView.vue)               | WORKER / Standalone |
| 알바생 근무 상세     | `/worker/work/work-cases/:workCaseId`        | `worker-work-case-detail` | [`WorkerWorkCaseDetailView.vue`](../../frontend/src/views/worker/workCase/WorkerWorkCaseDetailView.vue) | WORKER / Standalone |
| 임금분쟁 신고        | `/worker/work/work-cases/:workCaseId/report` | `worker-report`           | [`WorkerReportView.vue`](../../frontend/src/views/worker/workCase/WorkerReportView.vue)                 | WORKER / Standalone |
| 알바생 문서 보기     | `/worker/documents/:documentId`              | `worker-document-viewer`  | [`WorkerDocumentViewerView.vue`](../../frontend/src/views/worker/WorkerDocumentViewerView.vue)          | WORKER / Standalone |
| 알바생 마이페이지    | `/worker/mypage`                             | `worker-mypage`           | [`WorkerMyPageView.vue`](../../frontend/src/views/worker/WorkerMyPageView.vue)                          | WORKER / Standalone |
| 알바생 회원정보 변경 | `/worker/mypage/profile`                     | `worker-profile`          | [`WorkerProfileEditView.vue`](../../frontend/src/views/worker/mypage/WorkerProfileEditView.vue)         | WORKER / Standalone |
| 알바생 비밀번호 변경 | `/worker/mypage/password`                    | `worker-password`         | [`WorkerPasswordEditView.vue`](../../frontend/src/views/worker/mypage/WorkerPasswordEditView.vue)       | WORKER / Standalone |

## Deep-link and error pages

| Page           | Path                  | Route name       | View                                                                             | Access / layout                |
| -------------- | --------------------- | ---------------- | -------------------------------------------------------------------------------- | ------------------------------ |
| 근무 초대 확정 | `/invitations/:token` | `invite-confirm` | [`InviteConfirmView.vue`](../../frontend/src/views/invite/InviteConfirmView.vue) | WORKER invite / Standalone     |
| 접근 권한 없음 | `/forbidden`          | `forbidden`      | [`ForbiddenView.vue`](../../frontend/src/views/error/ForbiddenView.vue)          | Public / Standalone            |
| 페이지 없음    | `/:pathMatch(.*)*`    | `not-found`      | [`NotFoundView.vue`](../../frontend/src/views/error/NotFoundView.vue)            | Public / Standalone, catch-all |

The current role-mismatch guard redirects to the authenticated user's home instead of routing to
`/forbidden`. The `/forbidden` page remains available as a direct route.

## Navigation-only routes

| Entered path | Destination    | Access | Shell                                                                   |
| ------------ | -------------- | ------ | ----------------------------------------------------------------------- |
| `/owner`     | `/owner/home`  | OWNER  | [`OwnerTabLayout.vue`](../../frontend/src/layouts/OwnerTabLayout.vue)   |
| `/worker`    | `/worker/home` | WORKER | [`WorkerTabLayout.vue`](../../frontend/src/layouts/WorkerTabLayout.vue) |

These two child redirects have no route name and do not add to the 33-page count.

## Global navigation behavior

The global `beforeEach` guard currently applies these rules in order:

1. An authenticated user entering a Guest-only route is sent to the current role's home.
2. An unauthenticated user entering a protected route is sent to `/?redirect=<original>`.
3. An unauthenticated invitation visitor is instead sent to
   `/worker/login?redirect=<original>`.
4. An authenticated OWNER entering a WORKER invitation is returned to the OWNER home with a
   warning.
5. Any other authenticated role mismatch is returned to the current role's home with a warning.
6. An OWNER with `needsWorkplaceSetup=true` is forced from every `/owner` route except
   `/owner/workplaces/new` to the workplace registration page.

After a successful WORKER login, the login page consumes the preserved `redirect` query and returns
to `/invitations/:token`. `InviteConfirmView.vue` then loads that token and displays the inviting
OWNER's badge. The current data source is still a frontend mock; the real invitation lookup must
revalidate the token after authentication before returning the badge.

Every navigation resets scroll position to the top. Undefined paths render the 404 View and are
not hidden by a redirect to `/`.

## Maintenance rule

- Add, rename, or remove a row in the same change that modifies the corresponding router record.
- Keep requirements-only pages out of this current-state table until the route and View exist.
- Do not treat an unregistered View file, a frontend service, or a mock response as proof that a
  page route exists.
