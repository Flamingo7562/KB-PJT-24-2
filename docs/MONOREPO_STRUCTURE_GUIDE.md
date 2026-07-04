# Monorepo 구조 가이드

이 저장소는 프론트엔드와 백엔드를 하나의 레포지토리에서 관리합니다.

```text
KB-PJT-24-2/
  frontend/
  backend/
  docs/
  .github/
  .husky/
  scripts/
  package.json
```

## 왜 하나의 레포로 관리하나요?

- 이슈, PR, GitHub Projects를 한 곳에서 관리할 수 있습니다.
- 화면 변경과 API 변경을 같은 이슈와 PR에서 추적할 수 있습니다.
- 커밋 컨벤션, Husky, lint, 문서를 공통으로 관리할 수 있습니다.
- 팀 프로젝트 규모에서는 설정과 히스토리가 한곳에 모이는 편이 단순합니다.

## 서버 실행 방식

레포지토리는 하나지만 서버는 따로 실행합니다.

```sh
# frontend
cd frontend
npm run dev
```

```sh
# backend
.\gradlew.bat -p backend war
```

백엔드 Gradle wrapper를 `backend/` 폴더 안에 둘 경우에는 `backend` 폴더에서 실행합니다.

## 권장 브랜치 예시

```text
feature/12-login
fix/23-session-timeout
docs/5-project-guide
```

한 이슈에서 프론트엔드와 백엔드가 함께 바뀌어도 브랜치는 하나를 사용합니다.

## 커밋 예시

```text
feat: [FE] 로그인 화면 구현 (#12)
feat: [BE] 로그인 API 구현 (#12)
feat: [DB] 사용자 조회 mapper 추가 (#12)
```

