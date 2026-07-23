# Gig Hub Frontend

Gig Hub의 Vue.js SPA입니다. Vue 3, Vite, Vue Router, Pinia, Axios, Bootstrap을 사용하며 JavaScript로 작성합니다.

## 요구 환경

- Node.js `20.19.0` 이상, `25.0.0` 미만
- npm `11` 권장

팀 권장 버전은 Node.js `24.18.0` LTS이지만, 현재 개발 PC의 Node.js `20.19.0`도 Vite 8의 공식 요구사항을 충족합니다.

## 최초 설치

`package-lock.json`이 있으므로 팀원은 개별 패키지를 설치하지 않고 다음 명령을 사용합니다.

```powershell
Set-Location frontend
npm ci
```

`npm install vue`는 초기 의존성을 변경할 때만 필요하며, 일반적인 프로젝트 실행 과정에서는 사용하지 않습니다.

## 실행

```powershell
npm run dev
```

기본 개발 주소는 Vite가 터미널에 표시합니다. `/api` 요청은 `.env`의 `DEV_PROXY_TARGET`으로 전달되며 기본값은 `http://localhost:8080`입니다.

환경 파일은 다음과 같이 준비합니다.

```powershell
Copy-Item .env.example .env
```

## 검증

```powershell
npm run lint
npm run test:run
npm run build
```

Prettier 검사는 읽기 좋은 형식을 확인하고 싶을 때 선택적으로 실행합니다. 운영체제별
LF/CRLF는 모두 허용하며, 이 검사는 커밋 훅이나 루트 `npm run check`의 필수 통과 조건이
아닙니다.

```powershell
npm run format:check
```

저장소 루트에서는 다음 명령으로 전체 Guardrail과 lint를 실행합니다.

```powershell
Set-Location ..
npm run check
```

## 주요 구조

```text
frontend/
  src/
    assets/       공통 스타일
    components/   재사용 UI
    router/       화면 경로와 Guard
    services/     Axios 기반 API Client
    stores/       Pinia 상태
    views/        Route 화면
  .env.example
  eslint.config.js
  package.json
  vite.config.js
```

- Vue 상태는 화면 상태를 관리하며 서버 데이터의 최종 원본으로 취급하지 않습니다.
- Axios 공통 Client는 `/api`를 사용하고 세션 Cookie와 CSRF Header를 전송할 수 있게 구성되어 있습니다.
- 로그인·권한 검사는 프론트 Router Guard만 믿지 않고 Spring Service에서도 반드시 수행해야 합니다.
