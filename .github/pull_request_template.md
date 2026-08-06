## 요약

-

## 관련 이슈

- Refs #이슈번호
- Spec Patch: `SPEC-NNN-NN` 또는 `N/A`
- Base Spec: `x.y.z` 또는 `N/A`
- Delivery Mode: `implementation_bundled`, `spec_first` 또는 `N/A`
- Contract Change: `additive`, `clarification`, `breaking` 또는 `N/A`

<!-- dev 대상 작업 PR은 Refs를 사용하고 우측 Development에서 이슈를 수동 연결합니다. main 대상 배포·긴급 수정 PR에서 이슈를 종료할 때는 Closes를 사용합니다. API 경계 코드가 바뀌지만 계약 변경이 없다면 Spec Patch·Delivery Mode·Contract Change에 N/A와 근거를 적습니다. -->

## 변경 사항

-

## 확인 방법

-

## 체크리스트

- [ ] 이슈의 완료 조건을 충족했습니다.
- [ ] 관련 이슈를 PR의 Development 항목에 연결했습니다.
- [ ] Vue.js, Spring Framework legacy, MyBatis 제약을 지켰습니다.
- [ ] React, Spring Boot, JPA를 추가하지 않았습니다.
- [ ] 필요한 테스트 또는 수동 검증 결과를 남겼습니다.
- [ ] API 경계 변경에 연결된 Spec Patch를 기록하거나 계약 변경이 없는 이유를 `N/A`로 설명했습니다.
- [ ] `implementation_bundled` Patch는 Controller가 `accepted`한 뒤 병합하고, `spec_first` 구현은 Patch가 `applied`된 뒤 병합합니다.
- [ ] 승인·운영 릴리스라면 `npm run check:guardrails:release`로 미적용 구현 동반 Patch가 없음을 확인했습니다.
- [ ] PR 대상이 작업 통합은 `dev`, 배포·긴급 수정은 `main`으로 올바르게 설정되었습니다.
- [ ] GitHub Projects 상태를 `In Review` 또는 적절한 상태로 갱신했습니다.
