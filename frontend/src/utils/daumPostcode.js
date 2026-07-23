/**
 * 다음(카카오) 우편번호 서비스 — 도로명주소 검색.
 * 무료, API 키 불필요. 외부 CDN 스크립트를 최초 1회만 로드해 재사용한다.
 * 팝업 창(window.open) 대신 embed 모드로 화면 안(모달 등)에 직접 렌더링한다 —
 * 팝업 차단 설정의 영향을 받지 않기 위함.
 * 좌표(위도/경도) 변환은 이 서비스 범위 밖 — 별도 지오코딩 API 연동이 필요하다(미구현).
 */
const SCRIPT_URL = 'https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js'

let scriptPromise = null

function loadScript() {
  if (window.daum?.Postcode) return Promise.resolve()
  if (scriptPromise) return scriptPromise

  scriptPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = SCRIPT_URL
    script.onload = () => resolve()
    script.onerror = () => {
      scriptPromise = null
      reject(new Error('주소 검색 스크립트를 불러오지 못했어요.'))
    }
    document.head.appendChild(script)
  })
  return scriptPromise
}

/**
 * 주소 검색 위젯을 지정한 컨테이너 엘리먼트 안에 그려 넣는다.
 * @param {HTMLElement} container
 * @param {(result: { address: string, zonecode: string }) => void} onComplete
 * @param {(err: Error) => void} [onError]
 */
export async function embedAddressSearch(container, onComplete, onError) {
  try {
    await loadScript()
    container.innerHTML = ''
    new window.daum.Postcode({
      oncomplete(data) {
        onComplete({
          address: data.roadAddress || data.jibunAddress,
          zonecode: data.zonecode
        })
      },
      width: '100%',
      height: '100%'
    }).embed(container)
  } catch (err) {
    onError?.(err)
  }
}
