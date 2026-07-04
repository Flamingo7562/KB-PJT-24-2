# KB-PJT-24-2

KB IT's Your Life 7기 24-2팀 프로젝트입니다.

## 프로젝트 운영 문서

- [시작 가이드](docs/GETTING_STARTED.md)
- [프로젝트 관리 가이드](docs/PROJECT_MANAGEMENT_GUIDE.md)
- [Monorepo 구조 가이드](docs/MONOREPO_STRUCTURE_GUIDE.md)
- [이슈 작성 템플릿 가이드](docs/ISSUE_WRITING_GUIDE.md)
- [커밋 컨벤션](docs/COMMIT_CONVENTION.md)
- [Git Hook 및 Husky 가이드](docs/GIT_HOOKS_HUSKY_GUIDE.md)
- [Lint 가이드](docs/LINT_GUIDE.md)
- [GitHub Projects 패널 운영 가이드](docs/GITHUB_PROJECTS_PANEL_GUIDE.md)
- [커밋 amend 가이드](docs/AMEND_GUIDE.md)

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

## 기술 제약

- Frontend: Vue.js
- Backend: Spring Framework legacy
- Persistence: MyBatis
- 사용 금지: React, Spring Boot, JPA

## 공통 명령

```sh
npm install
npm run prepare
npm run check
npm run lint
```
