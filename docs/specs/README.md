# 제품 명세 안내

| 항목             | 값                         |
| ---------------- | -------------------------- |
| 명세 릴리스      | `3.0.1`                    |
| 승인일           | 2026-08-05                 |
| 소유자           | PM/Admin Master            |
| 최소 호환 스키마 | Flyway `202608051337` 이상 |

이 디렉터리는 Gig Hub의 제품 요구와 외부 REST 계약을 보관하는 규범 문서 영역입니다.
구현 코드, Swagger 화면, 작업 이력은 이 문서의 근거가 될 수 있지만 이 문서를 자동으로
바꾸는 권한은 갖지 않습니다.

최소 호환 스키마는 이 명세가 요구하는 최초 DB 구조를 뜻하며 현재 Flyway Head나 구현
진행률을 뜻하지 않습니다. 이후 Migration이 제품 의미나 외부 계약을 바꾸지 않고 호환성을
유지한다면 이 명세 릴리스를 갱신하지 않습니다.

`3.0.1`은 제품 의미와 외부 REST 계약을 바꾸지 않고 명세 Patch 제안·통합 운영 절차를
명확히 한 호환 Patch 릴리스입니다. 최소 호환선은 `3.0.0`과 동일하게
[Issue #205](https://github.com/Flamingo7562/KB-PJT-24-2/issues/205)에서 구현하고
[PR #208](https://github.com/Flamingo7562/KB-PJT-24-2/pull/208)로 병합한
`V202608051337__replace_mock_bank_account_user_with_pin.sql`입니다. 이 Migration보다 앞선
스키마는 사용자 귀속 없는 Mock 계좌와 충전 PIN 계약을 충족하지 않습니다.

## 문서 라우팅

| 알고 싶은 내용                          | 문서                                         |
| --------------------------------------- | -------------------------------------------- |
| 제품이 제공해야 하는 기능과 수용 기준   | [REQUIREMENTS.md](REQUIREMENTS.md)           |
| 승인된 REST 경로, 요청, 응답, 오류 계약 | [API_SPEC.md](API_SPEC.md)                   |
| 승인·미결·폐기된 제품 결정              | [DECISIONS.md](DECISIONS.md)                 |
| 요구사항과 API·도메인의 연결            | [SPEC_TRACEABILITY.md](SPEC_TRACEABILITY.md) |

과거 개발 현황 중심 라우팅 표는
[2026-07-31 아카이브](../archive/specs/DEVELOPMENT_ROUTING_TABLE-2026-07-31.md)로
이동했습니다. 아카이브는 규범 계약이 아닙니다.

## 권위와 충돌 해결

제품 의미와 외부 계약의 우선순위는 다음과 같습니다.

1. [REQUIREMENTS.md](REQUIREMENTS.md)의 승인된 요구사항과 수용 기준
2. [API_SPEC.md](API_SPEC.md)의 승인된 REST 계약
3. [DECISIONS.md](DECISIONS.md)의 승인 결정
4. [SPEC_TRACEABILITY.md](SPEC_TRACEABILITY.md)의 연결 정보

동일 순위에서 문장이 충돌하면 더 최근에 승인된 명세 릴리스를 따릅니다. 명세와 코드 또는
Swagger가 충돌하면 이를 명세 변경으로 간주하지 않고
[명세 Patch](../spec-patches/README.md)로 Controller에게 변경을 제안합니다. DB의 물리 구조는
해당 릴리스가 선언한 Flyway 호환 기준을 따르며, DDL과 Migration의 변경 권한은 별도 소유자
정책을 따릅니다.

## 편집 권한

- Controller인 PM/Repository Admin `Flamingo7562`만 이 디렉터리의 규범 문서를
  승인·편집·배포합니다.
- 개발자와 구현 Agent는 이 디렉터리를 읽기 전용으로 취급합니다.
- 구현 중 계약의 누락, 모순, 변경 필요성을 발견하면 코드를 기준으로 명세를 직접 고치지
  않고 [명세 Patch 작성 절차](../spec-patches/README.md)에 따라 비규범 제안을 남깁니다.
- 승인되지 않은 추정값, 구현 편의를 위한 필드, 임시 Endpoint를 규범 계약에 추가하지
  않습니다.
- 진행률, 임시 데이터 사용 여부, 구현 세부, 테스트 결과, 작업 목록은 이 디렉터리에 기록하지
  않습니다.

## 변경 절차

1. 제안자는 최신 `origin/dev`와 이 디렉터리의 정식 명세 버전을 확인하고, 독립적으로 승인할
   수 있는 최소 기능 단위마다 [명세 Patch](../spec-patches/README.md) 하나를 작성합니다.
2. Patch에는 기준 명세 버전과 Commit, 안정적인 대상 계약 ID, 변경 유형, 전체 영향, 수용 조건,
   의존성과 미결 사항을 기록합니다. Patch는 `applied`되기 전까지 제품 계약이 아닙니다.
3. Controller는 `proposed` Patch의 기준선, 대상 중복, 의존성, 호환성과 의미 충돌을 검토해
   `accepted`, `rejected`, 또는 `superseded`로 판정합니다. 오래된 기준선이나 의미 충돌은 새
   리비전 또는 명시적 재승인으로 해결합니다.
4. `accepted` Patch를 바탕으로 구현을 병행할 수 있지만, 구현 PR은 Patch가 정식 명세에
   `applied`되고 적용된 명세 버전을 기록하기 전에는 병합하지 않습니다.
5. Controller는 최신 `origin/dev`에서 영향받는 요구사항, REST 계약, 결정, 추적표를 편집
   통합하고 릴리스 메타데이터, Changelog, 필요한 호환 기준, `SPEC_LOCK.json`, Patch의
   `applied` 상태와 아카이브 이동을 하나의 원자적 명세 릴리스 PR로 처리합니다.
6. 호환되지 않는 변경은 Major, 호환되는 기능 추가는 Minor, 제품 의미를 바꾸지 않는 명확화는
   Patch Version을 올립니다. 되돌림도 과거 파일 복원이 아니라 별도 Revert Patch와 새 릴리스로
   처리합니다.
7. 개발팀은 배포된 정식 명세만을 기준으로 코드와 테스트를 변경하고, Swagger는 그 결과를
   보여주는 실행 계약 화면으로 생성합니다. 실행 시점에 Patch를 정식 명세와 합성하지 않습니다.

정적 OpenAPI YAML을 별도의 규범 원본으로 병행 관리하지 않습니다. 사람이 검토하는 계약의
원본은 이 디렉터리의 Markdown이며, Swagger/OpenAPI는 승인 계약을 코드에 반영한 결과를
검증하는 용도입니다.

## 릴리스 기록

| Version | 승인일     | 요약                                                                                                                                                                                 |
| ------- | ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `3.0.1` | 2026-08-05 | 제품 의미와 REST 계약을 유지하면서 명세 Patch 제안·Controller 통합 절차와 원자적 릴리스 기준을 확정한 Patch 릴리스 ([#207](https://github.com/Flamingo7562/KB-PJT-24-2/issues/207))  |
| `3.0.0` | 2026-08-05 | 비귀속 Demo Mock 계좌, 충전 PIN 인증, PIN 없는 출금 입금, 금융 오류·거래 표시·멱등 Claim 계약을 확정한 Major 릴리스 ([#202](https://github.com/Flamingo7562/KB-PJT-24-2/issues/202)) |
| `2.1.1` | 2026-08-05 | 아이디·이메일 정규화 결과의 필수·최대 길이 경계를 확정한 Patch 릴리스                                                                                                                |
| `2.1.0` | 2026-08-04 | 예상하지 못한 서버 오류의 공통 `500 INTERNAL_ERROR` 외부 계약을 승인한 Minor 릴리스                                                                                                  |
| `2.0.0` | 2026-08-04 | M2 인증·입력·OWNER 온보딩 계약을 확정하고 `employer_profiles`를 제거한 Major 릴리스                                                                                                  |
| `1.0.0` | 2026-07-31 | 팀 합의 사항을 제품 요구, REST 계약, 결정 기록, 추적표로 분리한 최초 규범 릴리스                                                                                                     |
