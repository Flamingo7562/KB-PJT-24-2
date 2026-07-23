# Lint 가이드

이 프로젝트는 프론트엔드와 백엔드 lint를 각각의 생태계 도구로 관리합니다.

| 영역 | 도구 | 실행 기준 |
| --- | --- | --- |
| Frontend | ESLint, eslint-plugin-vue, Prettier | `frontend/package.json` |
| Backend | Gradle `check`, Checkstyle | `backend/build.gradle` |
| 공통 | Husky, root npm scripts | 루트 `package.json` |

## 루트 명령

```sh
npm run lint
npm run lint:fe
npm run lint:be
npm run format:staged
npm run check
```

Windows PowerShell에서 `npm.ps1` 실행 정책 오류가 나면 `npm.cmd`로 실행합니다.

```sh
npm.cmd run lint
npm.cmd run check
```

아직 `frontend/package.json` 또는 `backend/build.gradle`이 없으면 해당 lint는 건너뜁니다. 실제 프로젝트가 생성되면 같은 명령이 자동으로 동작합니다.

## Frontend

Vue 프로젝트 생성 후 `frontend/package.json`에 다음 스크립트를 둡니다.

```json
{
  "scripts": {
    "lint": "eslint src --max-warnings=0",
    "lint:fix": "eslint src --fix",
    "format": "prettier . --write",
    "format:check": "prettier . --check"
  }
}
```

VSCode에서는 ESLint 확장과 다음 설정을 권장합니다.

```json
{
  "eslint.validate": ["javascript", "typescript", "vue"],
  "editor.codeActionsOnSave": {
    "source.fixAll.eslint": "explicit"
  }
}
```

## Backend

백엔드는 Gradle `check` 태스크를 기준으로 lint를 실행합니다. Spring Framework legacy 프로젝트가 생성되면 `backend/build.gradle`에 Checkstyle 플러그인을 추가합니다.

```gradle
plugins {
    id 'java'
    id 'war'
    id 'checkstyle'
}

checkstyle {
    configFile = file('config/checkstyle/checkstyle.xml')
}
```

루트 Gradle wrapper를 사용할 경우:

```sh
# Windows
.\gradlew.bat -p backend check

# macOS / Linux
./gradlew -p backend check
```

`backend/` 내부 Gradle wrapper를 사용할 경우:

```sh
cd backend

# Windows
.\gradlew.bat check

# macOS / Linux
./gradlew check
```

## Husky 적용 기준

`pre-commit`에서는 빠른 검사를 실행합니다.

```sh
npm run check:precommit
```

이 명령은 다음을 실행합니다.

- 기술 제약 검사: React, Spring Boot, JPA 추가 방지
- 스테이징된 프런트엔드 JS, Vue, JSON, CSS, Markdown, HTML, YAML 파일에 Prettier 자동 적용
- 프론트엔드 lint: `frontend/package.json`의 `lint` 스크립트가 있을 때 실행
- 백엔드 lint: `backend/build.gradle`이 있고 Gradle wrapper가 있을 때 `check` 실행

Prettier는 `lint-staged`를 통해 스테이징된 파일만 수정하고 변경 결과를 같은 커밋에 다시
포함합니다. 프로젝트 전체 `format:check`는 필요할 때 수동으로 실행하는 선택 검사이며,
커밋 훅은 관계없는 기존 파일을 일괄 포맷하지 않습니다.

백엔드 전체 테스트가 느려지면 `pre-commit`에서는 프론트엔드 lint만 실행하고, 백엔드 `check`는 `pre-push` 또는 GitHub Actions에서 실행하도록 조정할 수 있습니다.
