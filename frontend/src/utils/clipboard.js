/**
 * 클립보드 복사 유틸.
 *
 * `navigator.clipboard.writeText` 는 권한·포커스 상태에 따라 resolve 도 reject 도 하지
 * 않고 멈추는 경우가 있다. 그대로 await 하면 호출한 화면의 버튼이 로딩 상태로 굳으므로
 * 타임아웃을 걸어 항상 성공/실패가 결정되게 한다.
 */

/**
 * 텍스트를 클립보드에 복사한다.
 * @param {string} text 복사할 문자열
 * @param {object} options timeoutMs(기본 2000)
 * @returns {Promise<boolean>} 복사 성공 여부(실패해도 예외를 던지지 않는다)
 */
export async function copyText(text, { timeoutMs = 2000 } = {}) {
  if (!navigator.clipboard?.writeText) return false

  try {
    await Promise.race([
      navigator.clipboard.writeText(text),
      new Promise((_, reject) => {
        setTimeout(() => reject(new Error('clipboard timeout')), timeoutMs)
      })
    ])
    return true
  } catch {
    return false
  }
}
