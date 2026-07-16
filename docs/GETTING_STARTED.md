# 시작 가이드

이 문서는 팀원이 프로젝트 개발을 시작하기 전에 한 번씩 해야 하는 설정을 정리합니다.

## 1. 레포지토리 받기

```sh
git clone https://github.com/Flamingo7562/KB-PJT-24-2.git
cd KB-PJT-24-2
```

## 2. Node 의존성 설치

루트 `package.json`은 Husky와 공통 검사 스크립트를 관리합니다.

```sh
npm install
```

Windows PowerShell에서 `npm.ps1` 실행 정책 오류가 나면 `npm.cmd`를 사용합니다.

```sh
npm.cmd install
```

## 3. Husky 활성화

커밋할 때 기술 제약, lint, 커밋 메시지 규칙을 자동 검사하도록 설정합니다.

```sh
npm run prepare
```

PowerShell 오류가 나면:

```sh
npm.cmd run prepare
```

설정 확인:

```sh
git config --get core.hooksPath
```

정상 결과:

```text
.husky
```

## 4. 커밋 메시지 템플릿 설정

커밋 작성 시 컨벤션 예시가 자동으로 뜨게 합니다.

```sh
git config commit.template .gitmessage.txt
```

설정 확인:

```sh
git config --get commit.template
```

정상 결과:

```text
.gitmessage.txt
```

## 5. 공통 검사 실행

로컬 설정이 정상인지 한 번 확인합니다.

```sh
npm run check
```

PowerShell 오류가 나면:

```sh
npm.cmd run check
```

아직 실제 `frontend/package.json` 또는 `backend/build.gradle`이 없다면 lint가 skip될 수 있습니다. 실제 프로젝트가 생성되면 같은 명령이 자동으로 lint를 실행합니다.

## 6. 브랜치 생성

일반 작업 브랜치는 최신 `dev`에서 만듭니다. 이슈 번호를 포함하고 브랜치명에는 `#`를 넣지 않습니다.

```sh
git fetch origin
git switch dev
git pull --ff-only origin dev
git switch -c feature/12-login
git push -u origin feature/12-login
```

로컬에 `dev`가 아직 없으면 `git switch dev` 대신 최초 한 번 `git switch --track origin/dev`를 실행합니다.

예시:

```text
feature/12-login
fix/23-session-timeout
docs/5-project-guide
```

## 7. 커밋 메시지 작성

커밋 메시지는 다음 형식을 사용합니다.

```text
<type>: [AREA] 작업 요약 (#이슈번호)
```

예시:

```text
feat: [FE] 로그인 화면 구현 (#12)
feat: [BE] 로그인 API 구현 (#12)
feat: [DB] 사용자 조회 mapper 추가 (#12)
docs: [GITHUB] 이슈 템플릿 수정 (#5)
chore: [HOOK] Husky 설정 추가 (#7)
```

사용 가능한 AREA:

```text
[FE], [BE], [DB], [DOCS], [GITHUB], [HOOK], [INFRA], [COMMON]
```

## 8. 커밋 전 수동 검사

커밋 전에 미리 확인하고 싶으면 실행합니다.

```sh
npm run check
```

프론트엔드만 확인:

```sh
npm run lint:fe
```

백엔드만 확인:

```sh
npm run lint:be
```

PowerShell 오류가 나면 `npm` 대신 `npm.cmd`를 사용합니다.

## 9. 커밋

```sh
git add .
git commit
```

`git commit`을 사용하면 `.gitmessage.txt` 템플릿이 뜹니다. `git commit -m "..."`을 사용하면 템플릿은 뜨지 않지만 Husky 검사는 동일하게 실행됩니다.

## 10. PR 작성

PR 제목은 이슈/커밋과 같은 형식을 사용합니다.

```text
feat: [FE] 로그인 화면 구현
```

일반 작업 PR은 `dev`를 대상으로 만들고 본문에는 아래 문구를 넣습니다.

```text
Refs #12
```

배포·제출할 때는 `dev`에서 `main`으로 PR을 만들고, 해당 PR에 `Closes #12`를 적어 기본 브랜치 머지 시 이슈를 종료합니다.

## 참고 문서

- [프로젝트 관리 가이드](PROJECT_MANAGEMENT_GUIDE.md)
- [Monorepo 구조 가이드](MONOREPO_STRUCTURE_GUIDE.md)
- [이슈 작성 템플릿 가이드](ISSUE_WRITING_GUIDE.md)
- [커밋 컨벤션](COMMIT_CONVENTION.md)
- [Git Hook 및 Husky 가이드](GIT_HOOKS_HUSKY_GUIDE.md)
- [Lint 가이드](LINT_GUIDE.md)
