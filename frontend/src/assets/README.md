# assets 사용법 & 에셋 맵

프론트 공통 자산(스타일 변수 · 폰트 · 로고 · 이미지) 폴더입니다.
이 폴더는 이미 `frontend/src/assets/` 에 놓여 있고 `src/main.js` 에서 불러오도록
연결까지 끝나 있습니다. **팀원은 아래 "팀원 준비" 3줄만 하면 바로 씁니다.**

## 팀원 준비 (딱 3가지)

```powershell
Set-Location frontend
npm ci                    # 1) 기본 의존성 설치 (package-lock 기준)
npm install lucide-vue-next   # 2) 아이콘 라이브러리 (에셋 맵에서 사용)
```

3) 끝. 전역 스타일·폰트는 이미 `main.js` 에서 불러오므로 추가 작업이 없습니다.
   - `src/main.js` 에 `import '@/assets/main.css'` 가 이미 있습니다.
   - 폰트(Pretendard)는 `fonts/` 에 파일이 함께 커밋되어 있어 **다운로드가 필요 없습니다.**

> SVG 로고에 CSS 색을 입히려면(권장) 추가로 아래 한 줄이 필요합니다. (§로고 참고)
> `npm install -D vite-svg-loader`

## 폴더 구성

```
assets/
├── base.css        # 색·간격·글자 크기 변수 + 기본 스타일  ← 값은 전부 여기서
├── fonts.css       # 폰트(@font-face) — 이미 fonts/ 파일에 연결됨
├── main.css        # 진입점 (base + fonts 를 불러옴). main.js 에서 import 됨
├── fonts/          # Pretendard 파일(커밋되어 있음, 받을 필요 없음)
└── images/
    ├── logo/       # GigHub 로고 (SVG)
    │   ├── logo-gighub.svg   # 워드마크(로고+글자). 색은 CSS로 지정
    │   └── logo-symbol.svg   # 헤더용 심볼(마크만)
    ├── badges/     # 신뢰 뱃지 (SVG, 역할 2 × 단계 3 = 6종)
    │   ├── badge-owner-lv1.svg  ~  badge-owner-lv3.svg    # 안심일터 1·2·3
    │   └── badge-worker-lv1.svg ~ badge-worker-lv3.svg    # 성실근로자 1·2·3
    └── banks/      # 은행 로고 — 아래 설명 참고
```

원칙 한 줄: **아이콘은 라이브러리(lucide)로, 브랜드 고유물(로고·뱃지)만 파일로.**

---

## 에셋 맵 — 어디서 무엇을 쓰나

| 화면 / 위치 | 필요한 것 | 형식·방법 | 출처 |
|---|---|---|---|
| 온보딩 역할선택 | GigHub 로고 (사장/알바생 2색) | `logo/logo-gighub.svg` **1개** + CSS 색 | 자체 제작(§로고) |
| 사장 글로벌 헤더 | 로고 심볼 | `logo/logo-symbol.svg` | 자체 제작 |
| 헤더 알림·마이 | 종·사람 아이콘 | lucide `Bell` · `CircleUser` | 라이브러리 |
| 사장 바텀 네비 | 홈·근태·문서·QR | lucide (아래 매핑) | 라이브러리 |
| 알바생 바텀 네비 | 안심지갑·근로·QR·문서 | lucide (문서·QR **재사용**) | 라이브러리 |
| 알바생 로고 | GigHub 로고 (앰버) | 위 `logo-gighub.svg` 재사용, 색만 | 자체 제작 |
| 충전 화면 은행 | 은행 표시 | lucide `Landmark` + 이름 + 색칩 (권장) | §은행 |
| 마이 이름 옆 | 프로필 대용 | lucide `CircleUser` (공통) | 라이브러리 |
| 사장 마이 뱃지 | 안심일터 1·2·3 | `badges/badge-owner-lv*.svg` | 파일(제공됨) |
| 알바생 마이 뱃지 | 성실근로자 1·2·3 | `badges/badge-worker-lv*.svg` | 파일(제공됨) |
| 송금상세·근태 리스트 | 상태 표시 | lucide 소형 아이콘 + 상태색 (아래) | 라이브러리 |

---

## 스타일 변수 쓰는 법 (base.css)

색·간격·글자 크기는 컴포넌트에 직접 값을 쓰지 말고 **`base.css` 의 변수만** 씁니다.

```vue
<style scoped>
.amount { color: var(--color-brand); font-size: var(--text-2xl); }
.card   { border-radius: var(--radius-md); box-shadow: var(--shadow-card); }
</style>
```

자주 쓰는 토큰: 색 `--color-text / --color-brand / --color-owner / --color-worker`,
상태색 `--color-success / --color-warning / --color-danger`(+`-bg` 배경쌍),
글자 `--text-md(본문) ~ --text-2xl(금액)`, 간격 `--space-sm ~ --space-xl`,
모서리 `--radius-sm ~ --radius-lg`. **없는 값이 필요하면 컴포넌트가 아니라 `base.css` 에 먼저 추가.**

---

## 로고 — SVG로, 파일 1개 + CSS 색 (권장)

**SVG를 쓰세요.** 로고는 확대해도 안 깨져야 하고, "색만 다른 두 버전"이 필요하니
색을 파일에 굽지 말고 **`fill="currentColor"` 한 파일**로 두고 CSS `color`로 칠합니다.
→ 파일 2개를 따로 관리할 필요가 없습니다. (제공된 `logo-gighub.svg`가 이미 이 방식)

```vue
<script setup>
import LogoGighub from '@/assets/images/logo/logo-gighub.svg'
</script>
<template>
  <LogoGighub class="logo logo--owner" />   <!-- 사장: 파랑 -->
  <LogoGighub class="logo logo--worker" />  <!-- 알바생: 앰버 -->
</template>
<style scoped>
.logo--owner  { color: var(--color-owner); }   /* #3b59d9 */
.logo--worker { color: var(--color-worker); }   /* #e6a800 */
</style>
```

> `.svg`를 위처럼 **컴포넌트로 import** 하려면 `vite-svg-loader` 가 필요합니다.
> 1) `npm install -D vite-svg-loader`
> 2) `frontend/vite.config.js` 의 `plugins` 에 추가:
> ```js
> import svgLoader from 'vite-svg-loader'
> // ...
> plugins: [vue(), svgLoader()],
> ```
> 설치가 부담되면 `<img src="@/assets/images/logo/logo-gighub.svg">` 로도 되지만
> 이땐 CSS 로 색을 못 바꿉니다(사장/알바생 2색 불가).

**어디서 구하나:** 로고는 우리 브랜드라 다운로드가 아니라 **직접 제작**입니다(Figma에서
디자인 → SVG export). 지금 넣어둔 파일은 **바로 쓸 수 있는 임시(placeholder)** 이니
최종 로고가 나오면 같은 파일명으로 교체하세요. 최종본은 글자를 아웃라인(path)으로
변환해 폰트 의존을 없애는 게 좋습니다.

**PNG는 언제?** 파비콘·카톡/OG 미리보기처럼 SVG가 안 되는 곳에만. 그 PNG는
`assets`가 아니라 `public/` 에 둡니다(예: `public/favicon.png`, `public/og-image.png`).

---

## 아이콘 — lucide 라이브러리 (역할 무관 재사용, 색만 CSS)

`npm install lucide-vue-next` 로 설치합니다. 바텀 네비·헤더·상태 아이콘은 파일로
관리하지 않고 lucide 컴포넌트로 씁니다.
**사장·알바생이 겹치는 아이콘(문서함·QR 등)은 같은 컴포넌트를 재사용하고, 색은
활성/비활성 상태에 따라 CSS로만 바꿉니다.** (역할별로 아이콘을 복제하지 마세요.)

### 바텀 네비 매핑

| 사장 탭 | 알바생 탭 | lucide |
|---|---|---|
| 홈(지갑) | 안심지갑(홈) | `Wallet` |
| 근태관리 | 근로관리 | `CalendarCheck` / `ClipboardList` |
| 문서함 | 문서함 | `FileText` ← **동일 재사용** |
| QR | QR | `QrCode` ← **동일 재사용** |

```vue
<script setup>
import { Wallet, FileText, QrCode, CalendarCheck } from 'lucide-vue-next'
</script>
<template>
  <button class="tab" :class="{ 'is-active': active==='home' }">
    <Wallet :size="20" /><span>홈</span>
  </button>
</template>
<style scoped>
.tab           { color: var(--color-text-sub); }  /* 비활성 */
.tab.is-active { color: var(--color-text); }        /* 선택됨 — 아이콘이 색을 따라옴 */
</style>
```

### 헤더·기타

| 용도 | lucide |
|---|---|
| 알림(종) | `Bell` |
| 마이페이지·프로필 대용 | `CircleUser` |
| 뒤로가기 / 목록 화살표 | `ChevronLeft` / `ChevronRight` |
| 충전 / 출금 | `Plus` / `ArrowUpRight` |
| 문의(물음표) | `CircleHelp` |

---

## 리스트 상태 표시 (송금상세 · 근태 리스트)

상태는 **작은 lucide 아이콘 + 상태색 토큰**으로 통일합니다. 아이콘은 선택(없어도 텍스트
뱃지로 충분)이지만, 넣으면 한눈에 구분됩니다.

| 상태 | 아이콘 | 색 토큰 |
|---|---|---|
| 근무전·모집중 | `Clock` | `--color-text-sub` |
| 근무중 | `Loader` | `--color-primary` |
| 완료·정산완료 | `CircleCheck` | `--color-success` |
| 예치중·예치완료 | `Lock` | `--color-brand` |
| 지각 | `TriangleAlert` | `--color-warning` |
| 노쇼 | `UserX` | `--color-danger` |
| 환불 | `RotateCcw` | `--color-text-sub` |

---

## 은행 로고 (충전 화면)

**실제 은행 로고는 상표라 함부로 넣지 않는 게 안전합니다.** 권장:

- `Landmark`(lucide) 아이콘 + 은행 이름 + 브랜드 색 칩 조합으로 표현
- 색 칩 예: KB `#FFCC00`, 신한 `#0046FF`, 우리 `#0067AC`, 카카오뱅크 `#FFE300`

`images/banks/` 에 임시 PNG 로고들이 들어 있으나, 정식 사용 가능 여부(상표/라이선스)를
확인하기 전에는 위 아이콘+이름 방식을 기본으로 씁니다.
(KB 후원 프로젝트라면 KB CI는 사용 가능 여부를 먼저 확인하세요.)

---

## 뱃지 (마이페이지 — 안심일터 / 성실근로자)

제공된 6개 SVG를 등급에 맞춰 보여줍니다. 색은 단계별로 브론즈/실버/골드입니다.

```vue
<script setup>
import { computed } from 'vue'
const props = defineProps({ role: String, level: Number }) // role: 'owner'|'worker'
const src = computed(() =>
  new URL(`@/assets/images/badges/badge-${props.role}-lv${props.level}.svg`, import.meta.url).href)
</script>
<template>
  <img :src="src" :alt="`뱃지 ${level}단계`" width="48" />
</template>
```

- 이름 옆 프로필은 `CircleUser`(lucide) 로 공통 처리 → 사장·알바생 동일.
- 뱃지 미달(이력 5건 미만)이면 뱃지 대신 회색 `CircleUser` + "이력 쌓는 중" 문구.
- 등급 색이 필요하면 `--tier-1/2/3` 토큰 사용. 최종 일러스트가 나오면 같은 파일명으로 교체.

---

## 규칙 (팀 공통)

- 색·px는 컴포넌트에 직접 쓰지 말고 `base.css` 변수 사용. 없으면 변수를 먼저 추가.
- 아이콘은 lucide 우선. 겹치는 아이콘은 재사용 + 색만 CSS로. 역할별 복제 금지.
- 브랜드 파일(로고·뱃지)만 `images/` 에 SVG로. PNG는 `public/`(파비콘 등)에만.
- 은행·타사 로고는 상표 확인 전 사용 금지 → 기본은 아이콘+이름.

---

## 폰트를 woff2 로 줄이고 싶다면 (선택)

지금은 `ttf` 가 커밋되어 있어 그대로도 동작합니다. 용량을 줄이려면 Pretendard woff2 를
받아 `fonts/` 에 같은 이름으로 넣으면 `fonts.css` 가 자동으로 woff2 를 우선 적용합니다.
받는 곳: <https://github.com/orioncactus/pretendard/releases> → 최신 zip → `public/static/woff2/`
