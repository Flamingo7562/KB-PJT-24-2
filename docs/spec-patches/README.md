# 명세 Patch 운영 가이드

`docs/spec-patches/`는 정식 명세 변경을 제안하고 그 처리 이력을 남기는 **비규범 영역**이다. Patch가 `accepted` 상태여도 제품 계약은 아니며, Controller가 Patch를 정식 명세 릴리스에 적용한 뒤에도 계약의 단일 원본은 오직 [`docs/specs/**`](../specs/)이다. 개발자와 구현 Agent는 실행 시점에 정식 명세와 Patch를 합성해 계약으로 해석하지 않는다.

이 문서에서는 Git의 `HEAD`와 혼동되는 “HEAD 명세”라는 표현을 사용하지 않는다.

- **정식 명세(Canonical/Released Spec)**: PM/Repository Admin이 릴리스하고 `SPEC_LOCK.json`으로 잠근 `docs/specs/**`
- **명세 Patch**: 정식 명세 변경을 요청하는 비규범 제안·승인·감사 기록
- **Controller**: Patch 승인과 정식 명세 통합을 담당하는 PM/Repository Admin `Flamingo7562`

## 역할과 책임

### 제안자

- 최신 `origin/dev`와 현재 정식 명세를 기준으로, 독립적으로 승인 가능한 최소 기능 단위마다 Patch 하나를 작성한다.
- 같은 기능 변경 때문에 요구사항, API, 결정, 추적성이 함께 달라져야 한다면 그 전체 영향을 하나의 Patch에 적는다.
- 정식 명세를 직접 만들거나 수정·삭제·이동·복구하지 않는다.
- 기준 명세와 활성 Patch가 바뀌면 대상 계약의 의미 호환성을 다시 확인하고 필요한 재검토를 요청한다.

### Controller

- 기준 명세 버전과 Commit, 대상 계약 식별자, 의존 Patch, 활성 Patch와의 중복·충돌을 확인한다.
- Patch의 승인 여부와 적용 순서를 결정하고 `accepted`, `rejected`, `superseded`, `applied` 전환을 관리한다.
- 승인된 내용을 최신 정식 명세에 편집 통합하고 릴리스 메타데이터와 Lock을 원자적으로 갱신한다.
- 자동 검증 결과를 참고하되 제품 의미, 호환성, 적용 순서의 최종 판단을 직접 수행한다.

### 구현자와 리뷰어

- `accepted` Patch를 기준으로 구현을 병행할 수는 있지만, 해당 Patch가 `applied`된 정식 명세 버전을 구현 PR에 기록하기 전에는 병합하지 않는다.
- Patch PR의 기술 검토는 가능하지만 Controller의 제품 승인이나 정식 명세 릴리스를 대신하지 않는다.

## 디렉터리

```text
docs/specs/                  # 정식 명세: PM/Admin 전용
docs/spec-patches/
  README.md                  # 이 비규범 영역의 운영 절차
  TEMPLATE.md                # Patch 작성 템플릿
  proposed/                  # draft, proposed, accepted Patch
  archive/                   # applied, rejected, superseded Patch
```

활성 Patch는 [`proposed/`](proposed/)에, 종료된 감사 기록은 [`archive/`](archive/)에 둔다. 상태를 종료 상태로 바꿀 때는 파일 이동과 메타데이터 갱신을 같은 변경에서 처리한다. `README.md`와 [`TEMPLATE.md`](TEMPLATE.md)는 작성 지원 문서이며 Patch가 아니다.

## 파일명과 리비전

Patch 파일명은 다음 형식을 따른다.

```text
<github-id>_issue-<number>_<kebab-summary>_patch_v<revision>.md
```

예시:

```text
github-id_issue-207_spec-patch-governance_patch_v1.md
```

기계 검증 기준은 다음 정규식과 같다.

```regex
^[a-z0-9]+(?:-[a-z0-9]+)*_issue-[1-9][0-9]*_[a-z0-9]+(?:-[a-z0-9]+)*_patch_v[1-9][0-9]*\.md$
```

- GitHub ID와 요약은 영문 소문자, 숫자, 하이픈만 사용한다.
- 파일명의 Issue 번호는 `issue` 메타데이터와 같아야 한다.
- `v1`, `v2`는 Patch 문서의 리비전이며 정식 명세 SemVer와 무관하다.
- 승인 전 수정은 같은 리비전에서 할 수 있다.
- `accepted` 이후 제품 의미가 바뀌면 기존 문서를 덮어쓰지 않는다. 새 리비전을 만들고 이전 리비전을 `superseded`로 전환하며 `supersedes`와 `superseded_by`에 양방향 참조를 남긴다.

## 메타데이터

Patch는 파일 첫 줄부터 YAML front matter를 가져야 한다. 필드의 순서와 기본 구조는 [`TEMPLATE.md`](TEMPLATE.md)를 따른다.

| 필드                 | 규칙                                                                                                                                                     |
| -------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `patch_id`           | 저장소 전체에서 유일한 `SPEC-<issue>-<2자리 순번>` 형식이다. Issue 번호는 `issue`와 같아야 한다.                                                         |
| `author`             | 파일명과 같은 소문자 GitHub ID이다.                                                                                                                      |
| `status`             | `draft`, `proposed`, `accepted`, `applied`, `rejected`, `superseded` 중 하나이다.                                                                        |
| `issue`              | Patch를 추적하는 양의 GitHub Issue 번호이며 파일명과 일치한다.                                                                                           |
| `created_at`         | `YYYY-MM-DD` 형식의 최초 작성일이다. 리비전을 수정해도 조용히 바꾸지 않는다.                                                                             |
| `base_spec_version`  | 작성·재검토 기준이 된 정식 명세의 정확한 SemVer이다.                                                                                                     |
| `base_commit`        | 그 정식 명세를 포함한 최신 `origin/dev`의 축약 없는 소문자 40자리 Commit SHA이다.                                                                        |
| `change_type`        | `additive`, `clarification`, `breaking` 중 하나이다.                                                                                                     |
| `targets`            | 줄 번호가 아닌 안정적인 계약 식별자를 한 개 이상 적는다. 각 항목은 `requirement`, `decision`, `rest_operation` 등 식별자 종류 하나와 값 하나로 구성한다. |
| `depends_on`         | 먼저 검토·적용되어야 하는 Patch ID 목록이며 없으면 `[]`이다.                                                                                             |
| `supersedes`         | 이 리비전이 대체하는 Patch ID이며 없으면 `null`이다.                                                                                                     |
| `superseded_by`      | 이 리비전을 대체하는 Patch ID이며 없으면 `null`이다. 두 Patch가 서로를 가리켜야 한다.                                                                    |
| `applied_in_version` | `applied` 전에는 `null`, 적용 후에는 정식 명세 릴리스 SemVer이다.                                                                                        |
| `applied_by_pr`      | `applied` 전에는 `null`, 적용 후에는 정식 명세 릴리스 PR의 양의 번호이다.                                                                                |

`change_type`의 의미는 다음과 같다.

- `additive`: 기존 계약과 호환되는 기능 추가
- `clarification`: 제품 의미를 바꾸지 않는 명확화
- `breaking`: 기존 외부 동작, 데이터 의미 또는 호환성을 바꾸는 변경

Patch 본문은 변경 요약과 필요성, 현재 명세와 문제, 최종 규범 문장 또는 Before/After, 모든 영향 영역, 검증 가능한 수용 조건, 미결 사항, 관련 Issue·PR·의존 Patch를 빠짐없이 포함한다. 대상은 줄 번호 대신 요구사항 ID, 결정 ID, REST Operation처럼 변경 후에도 추적 가능한 식별자를 사용한다.

## 상태 수명주기

허용 전이는 아래 여섯 가지뿐이다.

```text
draft → proposed → accepted → applied
                  ↘ rejected
                  ↘ superseded
accepted ─────────→ superseded
```

| 상태         | 의미와 계약 효력                                                            | 위치        | 다음 상태                            |
| ------------ | --------------------------------------------------------------------------- | ----------- | ------------------------------------ |
| `draft`      | 작성 중인 비규범 초안                                                       | `proposed/` | `proposed`                           |
| `proposed`   | Patch 전용 PR에서 검토 중인 비규범 제안                                     | `proposed/` | `accepted`, `rejected`, `superseded` |
| `accepted`   | Controller가 내용을 승인했지만 아직 정식 명세에 적용하지 않은 비규범 기록   | `proposed/` | `applied`, `superseded`              |
| `applied`    | Controller 릴리스에 적용된 감사 기록. 계약 효력은 갱신된 정식 명세에만 있음 | `archive/`  | 없음                                 |
| `rejected`   | 채택하지 않은 사유를 보존하는 종료 기록                                     | `archive/`  | 없음                                 |
| `superseded` | 새 Patch가 대체하고 양방향 참조를 남긴 종료 기록                            | `archive/`  | 없음                                 |

상태를 건너뛰거나 되돌리지 않는다. `accepted`는 “승인됨”이고 `applied`는 “정식 명세 릴리스에 반영됨”이므로 서로 바꿔 쓸 수 없다. `accepted` Patch의 `applied_in_version`과 `applied_by_pr`는 여전히 `null`이어야 하며, 미결 사항이나 Placeholder가 남아 있어서는 안 된다.

## Patch 제출 절차

1. 작업 시작 전에 최신 `origin/dev`를 확인하고 그 Commit의 정식 명세 버전을 읽는다.
2. Patch 전용 브랜치에서 [`TEMPLATE.md`](TEMPLATE.md)를 `proposed/` 아래 올바른 파일명으로 복사한다.
3. 하나의 최소 기능 단위와 함께 바뀌어야 할 계약 영향을 모두 작성한다. 서로 독립적으로 승인할 수 있는 변경은 별도 Patch로 나눈다.
4. 최초 기준의 `base_spec_version`과 축약하지 않은 `base_commit`을 기록하고, 모든 안내문과 Placeholder를 실제 값 또는 명시적인 “영향 없음”·“미결 사항 없음”으로 바꾼다.
5. PR을 열기 전에 상태를 `proposed`로 바꾸고 `dev`를 대상으로 Patch 전용 PR을 만든다. PR에는 `Spec Patch: SPEC-...`와 `Base Spec: x.y.z`를 적는다.
6. Controller는 기준선, 대상, 의존성, 다른 활성 Patch와의 중복, 호환성을 확인한다. 기준이 오래되었거나 대상이 겹치면 자동 적용하지 않고 재검토 절차를 따른다.
7. Controller가 승인하면 Patch를 `accepted`로 전환한다. 구현은 병행할 수 있지만 정식 명세 적용 전에는 구현 PR을 병합하지 않는다.

Patch 전용 제안 PR은 여기서 끝난다. `accepted` Patch 기록을 `dev`에 병합하는 PR이며, 정식 명세·릴리스 메타데이터·`SPEC_LOCK.json`이나 제품 구현을 포함하지 않는다. 이후 Controller가 별도의 정식 명세 릴리스 PR을 만든다.

## 혼합 변경 금지

Patch PR에는 Patch 파일과 명시적으로 범위에 포함된 Patch 작성 지원 문서만 둔다. 다음 변경을 섞지 않는다.

- Frontend 또는 Backend 애플리케이션 코드와 테스트
- Flyway Migration, DDL, 통합 Schema
- `docs/specs/**` 정식 명세와 `SPEC_LOCK.json`
- Patch가 제안하는 제품 기능이나 REST 계약의 구현
- Patch 운영과 무관한 문서·자동화·설정

거버넌스 자체를 도입하거나 변경하는 관리 PR은 승인된 범위에서 지원 문서와 Guardrail을 함께 바꿀 수 있지만, 개별 제품 Patch나 제품 구현을 같은 PR에 포함하지 않는다.

## 기준선 변경, 충돌과 재검토

다음 이벤트에서는 막연히 “수시로 pull”하는 대신 최신 `origin/dev`와 정식 명세를 명시적으로 확인한다.

- 기능 또는 Patch 작업 시작 전
- Patch 최종 승인 직전
- 정식 명세 릴리스 직후
- 구현 PR을 Ready로 전환하기 직전
- 구현 PR 병합 직전

`base_spec_version` 또는 `base_commit`이 최신 기준과 다르면 Git 충돌이 없더라도 대상 계약의 의미 호환성을 다시 검토한다.

- 기준 변경이 대상 의미에 영향을 주지 않으면 기준 메타데이터를 최신 값으로 갱신하고 Controller의 명시적 재검토를 받는다.
- 승인 전에 제안 의미를 고치면 같은 리비전을 갱신할 수 있다.
- `accepted` 이후 의미를 고쳐야 하면 새 리비전을 만들고 기존 리비전을 `superseded` 처리한 뒤 다시 승인받는다.
- 다른 활성 Patch가 같은 계약 ID 또는 REST Operation을 대상으로 하면 텍스트 병합으로 해결하지 않는다. Patch 간 의존성과 적용 순서를 명시하고 Controller가 통합, 분리, 대체 또는 재승인을 결정한다.
- 충돌을 해소하며 기준 Commit만 조용히 바꾸거나 기존 승인을 그대로 승계하지 않는다.

## Controller 정식 명세 릴리스

1. `accepted` Patch 전용 PR이 병합된 최신 `dev`에서 Controller 릴리스 브랜치를 만들고 적용할 Patch의 의존성과 순서를 다시 확인한다.
2. 보호 파일이나 제품 의미를 바꾸지 않는 빈 번호 예약 커밋으로 브랜치를 Push하고 `dev` 대상 **Draft 정식 명세 릴리스 PR**을 먼저 열어 PR 번호를 확보한다. 이 PR은 Patch 전용 제안 PR과 별개다.
3. 각 Patch의 추가·교체·삭제 의도와 다른 활성 Patch의 의미 충돌을 검토하며 최신 정식 명세에 편집 통합한다. 문장을 단순히 이어 붙이지 않는다.
4. 영향받는 정식 명세, 릴리스 버전·승인일·소유자, 변경 이력, 필요한 호환 기준을 같은 릴리스로 정합화한다.
5. 확보한 Draft PR의 양의 번호를 Patch의 `applied_by_pr`에, 릴리스 SemVer를 `applied_in_version`에 기록한다. `SPEC_LOCK.json`을 최종 정식 명세 파일 집합으로 재생성하고 Patch를 `applied`로 바꾸어 `archive/`로 이동한다.
6. 3~5단계의 정식 명세, 릴리스 메타데이터, Lock, Patch 상태와 위치를 **하나의 최종 원자 커밋**으로 만들고 Push한다. 부분 상태를 중간 커밋이나 중간 Push로 공개하지 않는다.
7. 릴리스 PR을 검토·병합한 뒤에는 갱신된 `docs/specs/**`만 계약으로 사용한다. 보관 Patch는 감사와 추적 용도일 뿐 두 번째 규범 원본이 아니다.

## 거절, 대체와 Revert

- `rejected` 전환에는 본문에 Controller의 사유를 남기고 `archive/`로 이동한다.
- `superseded` 전환에는 새·이전 Patch의 `supersedes`와 `superseded_by`를 함께 갱신하고 이전 파일을 `archive/`로 이동한다.
- 이미 `applied`된 변경을 되돌릴 때 과거 정식 명세 파일이나 Lock을 직접 복원하지 않는다. 원래 Patch와 릴리스를 관련 항목에 연결한 별도 Revert Patch를 제출하고, 동일한 승인과 Controller 릴리스 절차를 거친다. 기존 `applied` 감사 기록의 상태는 바꾸지 않는다.

## 제출 전 점검

- [ ] 파일명, `patch_id`, `author`, `issue`가 서로 일치한다.
- [ ] 기준 명세 버전과 축약하지 않은 최신 `origin/dev` Commit을 기록했다.
- [ ] 안정적인 대상 계약 식별자와 의존 Patch를 빠짐없이 적었다.
- [ ] 모든 영향 영역과 검증 가능한 수용 조건을 작성했다.
- [ ] `accepted` 전환 전에 미결 사항과 Placeholder를 해소했다.
- [ ] 같은 대상을 다루는 활성 Patch와 기준선 변경을 재검토했다.
- [ ] Patch PR에 애플리케이션 코드, Migration, DDL 또는 보호 명세 변경을 섞지 않았다.
- [ ] `accepted`와 `applied`를 구분하고 상태에 맞는 디렉터리를 사용했다.
