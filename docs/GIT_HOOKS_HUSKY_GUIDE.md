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
| `pre-commit` | `.husky/pre-commit` | 기술 제약 검사와 lint 실행                 |
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

`scripts/check-project-guardrails.js`는 staged 파일에서 다음 항목을 검사합니다.

- `react`, `react-dom`, `@vitejs/plugin-react`
- `spring-boot`, `org.springframework.boot`
- `JpaRepository`, `@Entity`, `javax.persistence`, `jakarta.persistence`

문서와 GitHub 템플릿은 검사 대상에서 제외합니다. 기술 제약을 설명하기 위해 금지 기술 이름을 문서에 적을 수 있어야 하기 때문입니다.

## Lint 검사

`pre-commit`은 다음 명령을 실행합니다.

```sh
npm run check:precommit
```

이 명령은 기술 제약 검사를 먼저 실행하고, 이어서 프론트엔드와 백엔드 lint를 실행합니다.

```sh
npm run check:guardrails
npm run lint
```

아직 `frontend/package.json` 또는 `backend/build.gradle`이 없으면 해당 lint는 건너뜁니다.

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

## Hook 임시 비활성화

정말 필요한 경우에만 다음처럼 한 번의 커밋에서 Hook을 우회할 수 있습니다.

```sh
git commit --no-verify
```

우회한 이유는 PR 본문에 남깁니다.

## 참고 링크

- [Husky Docs - Get started](https://typicode.github.io/husky/get-started.html)
