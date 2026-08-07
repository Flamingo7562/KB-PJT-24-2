---
patch_id: SPEC-162-01
status: accepted
issue: 162
base_spec_version: 4.1.0
targets:
  - requirement: ATT-001
  - operation: GET /api/workplaces/{workplaceId}/qr
  - operation: POST /api/workplaces/{workplaceId}/qr/reissue
  - decision: DEC-OPEN-ERROR-CATALOG
  - decision: DEC-OPEN-QR-REISSUE-IDEMPOTENCY
---

# SPEC-162-01: 사업장 고정 QR 최초 발급과 조회·재발급 계약

## 추가 사항

### 최초 발급

사업장 생성이 성공하면 같은 트랜잭션에서 그 사업장의 활성 고정 QR 한 건을 함께 만든다.
발급자는 사업장 소유자다. 조회는 QR을 만들지도 교체하지도 않는다.

`ACTIVE` 사업장에 활성 QR이 없는 상태는 정상 상태가 아니다. 조회는 이 상태를 빈 응답이나
자동 보정으로 감추지 않고 `500 INTERNAL_ERROR`와 `traceId`로 드러낸다. 소유자는 재발급으로
복구할 수 있으며, 재발급은 기존 활성 QR이 없어도 성공한다.

### Token 형식

Token은 다섯 세그먼트를 `.`으로 이은 문자열이다. 사업장 42, 키 식별자 `k1`의 예시는
다음과 같다.

```text
v1.k1.42.AQIDBAUGBwgJCgsMDQ4PEA.qZ7XcO1nB2sV4hK9pR0tYuI3wE5aM6dF7gH8jL2xN0c
```

| 순서 | 내용                                                                     |
| ---- | ------------------------------------------------------------------------ |
| 1    | 고정 문자열 `v1`                                                         |
| 2    | 서명 키 식별자. `[A-Za-z0-9_-]{1,16}`                                    |
| 3    | 사업장 식별자의 10진 표기                                                |
| 4    | 16바이트 nonce를 padding 없는 Base64 URL-safe로 인코딩한 값              |
| 5    | 앞 네 세그먼트를 `.`으로 이은 문자열의 HMAC-SHA256을 같은 방식으로 인코딩한 값 |

검증은 다섯 세그먼트가 모두 일치할 때만 성공한다. 다섯째 세그먼트가 앞 네 개 전체를
덮으므로 어느 세그먼트를 바꿔도 검증이 실패한다.

키 식별자를 Token에 담는 이유는 이 QR이 인쇄되어 매장에 부착되기 때문이다. 서명 키를
교체해도 구 식별자의 검증 키를 남겨두면 이미 배포된 인쇄물이 계속 동작한다.

HMAC 키는 저장소와 WAR에 포함하지 않고 외부 properties로만 주입한다. 키가 없으면
애플리케이션 기동이 실패한다. 완성 Token 원문과 키는 DB에 저장하지 않고 일반 로그와
분석에도 남기지 않는다. `token_nonce`는 공개 식별자이므로 이 금지 대상이 아니다.

같은 사업장의 활성 QR을 반복 조회하면 항상 같은 Token 문자열이 나온다.

### 잠금 순서

QR을 만들거나 바꾸는 모든 흐름은 `workplaces` → `qr_tokens` 순서로 잠근다. 재발급은
사업장 행을 잠근 뒤 기존 활성 QR을 `REVOKED`로 전이하고 새 nonce를 넣는다.

### 오류 계약

`DEC-OPEN-ERROR-CATALOG` 중 위 두 Operation의 범위만 확정한다.

| 상황                                 | Status | Code               |
| ------------------------------------ | ------ | ------------------ |
| OWNER 역할이 아님                    | 403    | `ROLE_MISMATCH`    |
| 없는 사업장 또는 다른 OWNER의 사업장 | 404    | `RESOURCE_NOT_FOUND` |
| `ACTIVE` 사업장인데 활성 QR 없음(조회) | 500  | `INTERNAL_ERROR`   |
| 동시 재발급 경쟁에서 패배            | 409    | `CONFLICT`         |

없는 사업장과 다른 OWNER의 사업장을 구분하지 않는다. 구분하면 사업장 식별자의 존재
여부가 비소유자에게 드러난다.

### 멱등 범위

이 기능 범위에서 QR 재발급은 `Idempotency-Key` Header를 요구하지 않는다.
`DEC-OPEN-QR-REISSUE-IDEMPOTENCY`는 열린 상태로 남는다.

재발급 응답은 언제나 "현재 활성 QR의 권위 있는 표현"이다. 요청이 몇 번 도달하든 활성
QR은 정확히 하나이며 마지막 응답이 진실이다. 이중 제출 방지는 화면의 확인 단계와 전송 중
비활성화가 담당한다.

## 이 Patch가 결정하지 않는 것

`READY` 진입 조건은 현재 정식 명세 어디에도 정의되어 있지 않다. `READY`는 상태 열거와
`READY→IN_PROGRESS` 전이로만 등장하며, `ACCEPTED`를 `READY`로 바꾸는 주체와 시점이 없다.
근태 스캔과 노쇼 판정은 이 결정 없이 구현할 수 없다. 이 Patch는 그 결정을 대신하지 않는다.

폐기된 QR을 실제로 거절하는 지점도 이 범위에 없다. 서명 검증은 Token이 우리 키로 서명된
것인지만 판정하므로, 폐기된 nonce의 Token도 서명 자체는 유효하다. 스캔이 nonce의 현재
상태를 확인해야 폐기가 사용자에게 관찰된다. 그 확인은 스캔 구현이 담당한다.

## 완료 조건

- [ ] 사업장을 새로 만들면 그 사업장의 활성 QR이 정확히 한 건 함께 생긴다.
- [ ] 같은 사업장의 QR을 반복 조회하면 항상 같은 Token 문자열이 나온다.
- [ ] 재발급은 이전 QR을 `REVOKED`와 폐기 시각으로 전이하고, 동시 요청이 겹쳐도 활성 QR은 정확히 한 건이며, 응답 Token은 방금 저장한 nonce로 서명된다.
- [ ] 다른 OWNER의 사업장 QR 조회가 `404 RESOURCE_NOT_FOUND`다.
- [ ] 다섯 세그먼트 중 하나라도 바뀐 Token은 모두 검증에 실패한다.
- [ ] `ACTIVE` 사업장에 활성 QR이 없으면 조회가 `500 INTERNAL_ERROR`와 `traceId`를 반환한다.
- [ ] HMAC 키 속성이 없으면 애플리케이션이 기동되지 않는다.
- [ ] 사장 QR 화면에서 재발급을 실행할 수 있고, 조회가 실패한 상태에서도 재발급 버튼을 누를 수 있다.
