# Git Hook 및 Husky 가이드

이 저장소는 Husky를 이용해 커밋 전 기본 검사를 실행하도록 준비되어 있습니다.

## 목적

- 잘못된 커밋 메시지를 줄입니다.
- React, Spring Boot, JPA가 실수로 추가되는 것을 조기에 막습니다.
- 팀원이 같은 기준으로 커밋하도록 만듭니다.

## 설치

레포지토리 루트에서 실행합니다.

```sh
npm install
npm run prepare
```

Windows PowerShell에서 `npm.ps1` 실행 정책 오류가 나면 `npm.cmd`를 사용합니다.

```sh
npm.cmd install
npm.cmd run prepare
```

Husky 공식 문서는 새 프로젝트에서 `npx husky init` 사용을 권장합니다. 다만 이 저장소에는 이미 `.husky/pre-commit`, `.husky/commit-msg` 템플릿이 있으므로 덮어쓰지 않도록 아래 수동 절차를 사용합니다.

```sh
npm install --save-dev husky
npm pkg set scripts.prepare="husky"
npm run prepare
```

설치 후 다음 값이 `.husky`인지 확인합니다.

```sh
git config core.hooksPath
```

macOS 또는 Linux에서 Hook 실행 권한이 빠져 있다면 다음 명령을 한 번 실행합니다.

```sh
chmod +x .husky/pre-commit .husky/commit-msg
```

## 포함된 Hook

| Hook         | 파일                | 역할                                       |
| ------------ | ------------------- | ------------------------------------------ |
| `pre-commit` | `.husky/pre-commit` | staged 경로에 맞는 기술 제약·format·lint   |
| `commit-msg` | `.husky/commit-msg` | 커밋 메시지가 컨벤션을 지키는지 검사       |

## 커밋 메시지 검사

허용되는 형식:

```text
feat: [FE] 로그인 화면 라우팅 추가 (#12)
fix: [BE] 세션 만료 처리 보정 (#23)
docs: [GITHUB] 이슈 작성 가이드 추가 (#5)
```

검사는 `scripts/validate-commit-msg.js`에서 수행합니다.

## 프로젝트 기술 제약 검사

`scripts/check-project-guardrails.js`는 다음 두 범위를 구분해 검사합니다.

```sh
# pre-commit: Git index에 staged된 실제 내용만 검사
npm run check:guardrails:staged

# PR 전 전체 검사: 추적 파일과 무시되지 않은 작업 트리 파일 검사
npm run check:guardrails
```

검사 항목은 다음과 같습니다.

- `react`, `react-dom`, `@vitejs/plugin-react`
- `spring-boot`, `org.springframework.boot`
- `JpaRepository`, `@Entity`, `javax.persistence`, `jakarta.persistence`

문서와 GitHub 템플릿은 검사 대상에서 제외합니다. 기술 제약을 설명하기 위해 금지 기술 이름을 문서에 적을 수 있어야 하기 때문입니다. staged 검사는 작업 트리가 아니라 Git index의 내용을 읽으므로 부분 스테이징한 커밋도 실제 커밋 대상과 동일하게 검사합니다.

## 변경 경로별 pre-commit 검사

`pre-commit`은 다음 명령을 실행합니다.

```sh
npm run check:precommit
```

`scripts/run-precommit.js`는 삭제와 이름 변경의 이전·이후 경로를 포함한 staged 경로를 분류합니다. 문서와 메타데이터를 제외한 알 수 없는 경로는 검사를 생략하지 않고 전체 검사로 처리합니다.

| staged 변경                                      | 실행                                                                          |
| ------------------------------------------------ | ----------------------------------------------------------------------------- |
| 문서·GitHub 템플릿·저장소 메타데이터만          | staged Guardrail, `lint-staged` 일치 파일 format; 애플리케이션 lint 생략      |
| Frontend 애플리케이션 파일만                    | staged Guardrail, `lint-staged` 일치 파일 format, Frontend ESLint             |
| Backend 애플리케이션 파일만                     | staged Guardrail, `lint-staged` 일치 파일 format, Backend Gradle `check`      |
| Frontend와 Backend 애플리케이션 파일 모두       | staged Guardrail, `lint-staged` 일치 파일 format, 하네스 테스트, 두 영역 검사 |
| Hook·검사 스크립트·루트 설정·분류되지 않은 경로 | staged Guardrail, `lint-staged` 일치 파일 format, 하네스 테스트, 전체 검사    |

문서와 함께 애플리케이션 파일이 변경되면 문서는 검사 범위를 넓히지 않고 해당 애플리케이션 영역을 검사합니다. Frontend 또는 Backend 파일을 삭제하거나 다른 영역으로 이동해도 기존 경로를 기준으로 필요한 검사를 유지합니다.

Frontend와 Backend 검사는 삭제와 파일 간 영향을 놓치지 않도록 선택된 영역 전체를 검사합니다. 따라서 같은 영역에 커밋하지 않은 오류가 남아 있으면 staged 파일과 무관하더라도 Hook을 통과하지 못할 수 있습니다.

현재 staged 파일에 대한 실행 계획만 확인하고 실제 검사나 format을 실행하지 않으려면 다음 명령을 사용합니다.

```sh
node scripts/run-precommit.js --dry-run
```

아직 `frontend/package.json` 또는 `backend/build.gradle`이 없으면 해당 영역 검사는 건너뜁니다.

## Frontend Lint

Vue 프로젝트가 생성되면 `frontend/package.json`에 `lint` 스크립트를 둡니다.

```json
{
  "scripts": {
    "lint": "eslint src --max-warnings=0",
    "lint:fix": "eslint src --fix"
  }
}
```

루트에서는 다음 명령으로 실행합니다.

```sh
npm run lint:fe
```

## Backend Lint

백엔드는 Gradle `check` 태스크를 기준으로 lint를 실행합니다. Gradle 프로젝트가 생성되면 `backend/build.gradle`에 Checkstyle 플러그인을 추가합니다.

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

Git 실행 권한을 추적하지 못하는 환경에서도 동작하도록 macOS와 Linux에서는 wrapper를 `sh`로 실행합니다.

## 전체 검사 실행 기준

다음 변경은 PR을 넘기기 전에 루트 전체 검사를 한 번 실행합니다.

```sh
npm run check
```

- Frontend 또는 Backend 애플리케이션 코드
- 의존성, build, test 설정
- Hook, 검사 스크립트와 루트 검증 자동화
- Frontend와 Backend를 함께 변경한 작업
- 경로 영향 범위를 확실히 분류할 수 없는 작업

전체 검사는 전체 Guardrail, 하네스 단위 테스트, Frontend ESLint와 Backend Gradle `check`를 순서대로 실행합니다. Markdown-only 변경은 문서 format·링크·Git 추적 상태를 확인하고 전체 애플리케이션 검사를 생략할 수 있습니다. 생략 사유는 PR에 기록합니다.

## Hook 임시 비활성화

정말 필요한 경우에만 다음처럼 한 번의 커밋에서 Hook을 우회할 수 있습니다.

```sh
git commit --no-verify
```

우회한 이유는 PR 본문에 남깁니다.

## 참고 링크

- [Husky Docs - Get started](https://typicode.github.io/husky/get-started.html)
