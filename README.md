# KB-PJT-24-2

KB IT's Your Life 7기 24-2팀 프로젝트입니다.

Vue.js와 Spring Framework Legacy를 사용하는 Gig Hub 모노레포입니다. Frontend와 Backend는 별도로 실행하지만 Issue·PR과 공통 검증을 한 저장소에서 추적합니다.

## 시작하기

```sh
npm ci
npm --prefix frontend ci
npm run check
```

처음 개발 환경을 준비할 때 필요한 상세 절차는 아래 개발 문서 진입점에서 확인합니다.

## 문서 진입점

- [전체 개발 문서](docs/README.md)
- [프론트엔드 개발 안내](frontend/README.md)
- [백엔드 개발 안내](backend/README.md)

문서의 상태와 작업별 가이드 목록은 `docs/README.md`에서만 관리합니다.

## 프로젝트 구조

```text
KB-PJT-24-2/
  frontend/
  backend/
  docs/
  .github/
  .husky/
  scripts/
```

## 기술 기준

- Frontend: Vue.js, JavaScript 기본
- Backend: Java 17, Spring Framework 5, 외부 Tomcat 9
- Persistence: MyBatis, MySQL
- 조건부 도입: TypeScript는 명확한 필요와 팀 합의가 있을 때 허용
- 사용 금지: React, Spring Boot, JPA

세부 기술 경계와 의존성 변경 절차는 전체 개발 문서의 `기술 스택과 의존성` 항목에서 확인합니다.
