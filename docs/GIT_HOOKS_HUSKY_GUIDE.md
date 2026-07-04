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
| `pre-commit` | `.husky/pre-commit` | staged 파일에서 금지 기술 추가 여부를 검사 |
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

## 향후 확장 예시

프론트엔드와 백엔드 프로젝트가 생성된 뒤에는 `.husky/pre-commit`에 다음 검사를 추가할 수 있습니다.

```sh
npm --prefix frontend run lint
npm --prefix frontend run test
```

Spring legacy 프로젝트가 Maven 기반이라면 다음을 추가할 수 있습니다.

```sh
mvn test
```

Gradle 기반이라면 다음을 추가할 수 있습니다.

```sh
./gradlew test
```

Windows 팀원이 많다면 Git Bash, WSL, 또는 CI에서 동일 명령이 동작하는지 확인한 뒤 적용합니다.

## Hook 임시 비활성화

정말 필요한 경우에만 다음처럼 한 번의 커밋에서 Hook을 우회할 수 있습니다.

```sh
git commit --no-verify
```

우회한 이유는 PR 본문에 남깁니다.

## 참고 링크

- [Husky Docs - Get started](https://typicode.github.io/husky/get-started.html)
