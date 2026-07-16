# 커밋 컨벤션

커밋 메시지는 Conventional Commits 형식을 사용합니다.

형식을 지키기 위해서 Husky를 사용하고 있습니다.

## 형식

```text
<type>: [AREA] <summary> (#issue)
```

예시:

```text
feat: [FE] 로그인 화면 라우팅 추가 (#12)
fix: [BE] 세션 만료 처리 보정 (#23)
docs: [GITHUB] 이슈 작성 가이드 추가 (#5)
chore: [HOOK] 커밋 메시지 검증 스크립트 추가
```

## Type

| type       | 의미                                    |
| ---------- | --------------------------------------- |
| `feat`     | 새로운 기능                             |
| `fix`      | 버그 수정                               |
| `docs`     | 문서 변경                               |
| `style`    | 포맷팅, 세미콜론 등 동작 변화 없는 변경 |
| `refactor` | 기능 변화 없는 구조 개선                |
| `test`     | 테스트 추가 또는 수정                   |
| `chore`    | 빌드, 설정, 패키지 관리                 |
| `build`    | 빌드 시스템, 의존성 변경                |
| `ci`       | CI 설정 변경                            |
| `perf`     | 성능 개선                               |
| `revert`   | 이전 커밋 되돌리기                      |

## Area Tag

작업 영역을 빠르게 구분하기 위해 summary 앞에 대문자 태그를 붙입니다.

| 영역     | area tag   | 예시                                             |
| -------- | ---------- | ------------------------------------------------ |
| Frontend | `[FE]`     | `feat: [FE] 로그인 화면 구현 (#12)`              |
| Backend  | `[BE]`     | `fix: [BE] 로그인 API 예외 처리 (#13)`           |
| Database | `[DB]`     | `feat: [DB] 사용자 조회 mapper 추가 (#14)`       |
| Docs     | `[DOCS]`   | `docs: [DOCS] API 명세 보강 (#15)`               |
| GitHub   | `[GITHUB]` | `docs: [GITHUB] 이슈 템플릿 수정 (#16)`          |
| Hook     | `[HOOK]`   | `chore: [HOOK] Husky commit-msg 검증 추가 (#17)` |
| Infra    | `[INFRA]`  | `chore: [INFRA] 배포 설정 정리 (#18)`            |
| Common   | `[COMMON]` | `refactor: [COMMON] 공통 예외 응답 정리 (#19)`   |

`(frontend)` 같은 scope는 사용하지 않습니다. 작업 영역은 `[FE]`, `[BE]` 같은 area tag로만 표현합니다.

## 작성 규칙

- 첫 줄은 72자 이내로 작성합니다.
- type은 소문자로 작성합니다.
- summary는 반드시 area tag로 시작합니다.
- summary는 변경 내용을 명령형 또는 결과 중심으로 짧게 씁니다.
- 이슈가 있는 작업은 첫 줄 끝에 `(#이슈번호)`를 붙이는 것을 권장합니다.
- 본문이 필요하면 한 줄을 비우고 변경 이유와 영향 범위를 적습니다.
- 관련 이슈는 본문에 `Refs #이슈번호`를 적어도 됩니다.
- `dev` 대상 작업 PR 본문에는 `Refs #이슈번호`를 적습니다.
- `main` 대상 배포·제출 또는 긴급 수정 PR에는 종료할 이슈를 `Closes #이슈번호`로 적습니다.

## GitHub 이슈 번호 연결

GitHub에서는 `#12`처럼 `#` 뒤에 이슈 번호를 쓰면 해당 이슈로 자동 링크됩니다.

| 목적               | 권장 표현                           | 결과                                  |
| ------------------ | ----------------------------------- | ------------------------------------- |
| 단순 참조          | `Refs #12`                          | 이슈와 연결만 됨                      |
| `dev` 대상 작업 PR | `Refs #12`                          | 이슈를 참조하고 작업 중임을 표시       |
| `main` 머지 시 종료 | `Closes #12`                        | 기본 브랜치에 머지되면 이슈 종료       |
| 제목에서 빠른 식별 | `feat: [FE] 로그인 화면 구현 (#12)` | 커밋 목록에서 이슈 번호 확인 가능     |

브랜치명에는 `#` 대신 숫자만 사용합니다.

```text
feature/12-login-page
fix/23-session-timeout
docs/5-project-guide
```

## 커밋 템플릿 적용

```sh
git config commit.template .gitmessage.txt
```

전역으로 적용하려면 다음 명령을 사용합니다.

```sh
git config --global commit.template "$(pwd)/.gitmessage.txt"
```
