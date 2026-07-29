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
npm run test:harness
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

`pre-commit`에서는 staged 경로에 비례한 검사를 실행합니다.

```sh
npm run check:precommit
```

이 명령은 항상 staged Guardrail과 `lint-staged`를 먼저 실행한 뒤 다음처럼 애플리케이션 검사 범위를 선택합니다.

| 변경 범위                           | 애플리케이션 검사                      |
| ----------------------------------- | -------------------------------------- |
| 문서·GitHub 템플릿·메타데이터만    | 생략                                   |
| Frontend 애플리케이션 파일          | Frontend ESLint                        |
| Backend 애플리케이션 파일           | Backend Gradle `check`                 |
| 두 영역의 애플리케이션 파일         | 두 영역 모두                           |
| 공통 Hook·스크립트·알 수 없는 경로 | 하네스 테스트와 두 영역 모두           |

Prettier는 `lint-staged`를 통해 스테이징된 파일만 수정하고 변경 결과를 같은 커밋에 다시
포함합니다. 프로젝트 전체 `format:check`는 필요할 때 수동으로 실행하는 선택 검사이며,
커밋 훅은 관계없는 기존 파일을 일괄 포맷하지 않습니다.
현재 자동 format 대상은 `frontend/` 아래에 설정된 확장자이며, 공유 Markdown은 별도의
format·상대 링크·Git 추적 검증을 사용합니다.

실행 계획만 확인하려면 파일을 변경하지 않는 dry-run을 사용합니다.

```sh
node scripts/run-precommit.js --dry-run
```

## PR 전 전체 검사

애플리케이션 코드, 의존성, build·test 설정, 공통 검증 자동화 또는 여러 영역을 변경했다면 PR 전 다음 명령을 한 번 실행합니다.

```sh
npm run check
```

전체 검사는 전체 Guardrail, 하네스 단위 테스트, Frontend ESLint와 Backend Gradle `check`를 실행합니다. 성공 후 문서처럼 결과를 무효화하지 않는 변경만 추가됐다면 반복하지 않습니다. Markdown-only 변경은 format·상대 링크·Git 추적 상태를 확인하고 전체 검사를 생략하며 PR에 사유를 남깁니다.
