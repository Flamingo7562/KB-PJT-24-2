/**
 * localStorage 안전 래퍼 — "화면 표시 취향"만 저장한다.
 *
 * 저장해도 되는 것: 마지막으로 고른 뷰 모드처럼 **잃어버려도 그만인 UI 설정**.
 * 저장하면 안 되는 것: 세션·토큰·개인정보·금액 등 서버가 권위를 갖는 데이터
 * (인증은 세션 쿠키로만 다룬다 — CLAUDE.md '서버가 최종 권위').
 *
 * 사파리 프라이빗 모드 등에서 localStorage 접근 자체가 예외를 던질 수 있어
 * 모든 접근을 try/catch 로 감싸고, 실패하면 기본값으로 조용히 넘어간다.
 */

/** 저장된 값을 읽는다. 없거나 접근이 막히면 fallback 을 돌려준다. */
export function readPreference(key, fallback = null) {
  try {
    return localStorage.getItem(key) ?? fallback
  } catch {
    return fallback
  }
}

/** 값을 저장한다. 실패해도 화면 동작에는 영향이 없으므로 조용히 무시한다. */
export function writePreference(key, value) {
  try {
    localStorage.setItem(key, value)
  } catch {
    // 저장 실패(용량 초과·프라이빗 모드) — 이번 세션 동안만 유지되면 충분하다.
  }
}
