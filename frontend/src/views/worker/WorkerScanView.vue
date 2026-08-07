<script setup>
/**
 * [F] 알바생 QR 스캔(탭)  ·  /worker/scan  ·  WORKER  (탭 화면)
 * 카메라 스캔 → GPS 검증 → 출근/퇴근 자동 판별·기록(단일 스캔 API).
 * 카메라·위치 권한 필요. 서버가 QR 유효성·GPS 반경·출퇴근 판별을 최종 검증한다.
 * QR 이 정적이라 시간 만료로 걸러지지 않는다 — 대리 출근 차단은 서버의 GPS 반경 검증이 전담한다.
 * 연계 API: POST /api/attendance/scans  →  @/services/worker (scan)
 * QR 디코딩은 브라우저 내장 BarcodeDetector만 사용한다. 직접 Token 입력은 대리출석 경로가
 * 되므로 제공하지 않고 미지원·권한 거부 시 지원 환경을 안내한다.
 */
import { QrCode, ScanLine } from 'lucide-vue-next'
import { nextTick, onBeforeUnmount, ref } from 'vue'

import BaseButton from '@/components/common/BaseButton.vue'
import BaseModal from '@/components/common/BaseModal.vue'
import { newIdempotencyKey } from '@/services/http'
import { scan } from '@/services/worker'
import { useUiStore } from '@/stores/ui'
import { SCAN_TYPE } from '@/utils/constants'
import { formatDateTime } from '@/utils/format'

const ui = useUiStore()

// phase: idle | scanning | processing | confirmation | result
const phase = ref('idle')
const errorMsg = ref('')
const result = ref(null)
const pendingQrToken = ref('')
const pendingIntent = ref(null)

const videoEl = ref(null)
const coords = ref(null)

let stream = null
let detector = null
let detectTimer = null

/** 현재 위치 조회(Promise 래핑) */
function getLocation() {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error('geolocation unavailable'))
      return
    }
    navigator.geolocation.getCurrentPosition(
      (pos) =>
        resolve({
          latitude: Number(pos.coords.latitude.toFixed(7)),
          longitude: Number(pos.coords.longitude.toFixed(7)),
          accuracyMeters: Number(pos.coords.accuracy.toFixed(2)),
          capturedAt: new Date(pos.timestamp).toISOString()
        }),
      (err) => reject(err),
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
    )
  })
}

async function startScan() {
  errorMsg.value = ''

  // 1) 위치 권한(출퇴근 반경 검증에 필요)
  try {
    coords.value = await getLocation()
  } catch {
    errorMsg.value = '위치 권한이 필요합니다. 브라우저에서 위치 접근을 허용해주세요.'
    ui.toast('위치 권한이 필요합니다.', { type: 'warning' })
    return
  }

  // 2) QR 디코더 지원 여부 — 직접 입력으로 우회하지 않는다.
  if (!('BarcodeDetector' in window)) {
    errorMsg.value =
      '이 브라우저는 QR 스캔을 지원하지 않아요. 지원되는 모바일 브라우저를 이용해주세요.'
    ui.toast(errorMsg.value, { type: 'warning' })
    return
  }

  // 3) 후면 카메라 열기
  try {
    stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } })
  } catch {
    errorMsg.value = '카메라 권한이 필요합니다. 브라우저 설정에서 카메라 접근을 허용해주세요.'
    ui.toast(errorMsg.value, { type: 'warning' })
    return
  }

  phase.value = 'scanning'
  await nextTick()
  if (videoEl.value) {
    videoEl.value.srcObject = stream
    await videoEl.value.play().catch(() => {})
  }

  try {
    detector = new window.BarcodeDetector({ formats: ['qr_code'] })
  } catch {
    detector = new window.BarcodeDetector()
  }
  detectTimer = setInterval(runDetect, 350)
}

async function runDetect() {
  if (phase.value !== 'scanning' || !videoEl.value) return
  try {
    const codes = await detector.detect(videoEl.value)
    if (codes && codes.length > 0) {
      stopCamera()
      await submitScan(codes[0].rawValue)
    }
  } catch {
    // 일시적 감지 실패는 무시하고 다음 틱에 재시도
  }
}

/**
 * 근태 스캔 실패를 서버 상태코드별로 구분해 안내한다(docs/rules/api.md·domain.md):
 *   410 폐기   → 사장이 교체해 더 이상 쓰지 않는 옛 QR (정적 QR 이라 시간 만료는 없다)
 *   409 상태충돌 → 이미 처리됐거나 지금 스캔 불가한 근무 상태
 *   422 검증거부 → 위치(GPS 반경)·QR 인증 실패 → 반경·QR 재확인
 * (400 등 그 외는 일반 안내) 상태는 서버가 최종 판단하므로 프론트는 문구만 분기한다.
 */
function scanErrorInfo(error) {
  const status = error?.response?.status
  if (status === 410) {
    return {
      message: '더 이상 사용하지 않는 QR이에요. 매장에 붙은 최신 QR을 스캔해주세요.',
      type: 'warning'
    }
  }
  if (status === 409) {
    return {
      message: '이미 처리됐거나 지금은 스캔할 수 없는 근무예요. 근무 상태를 확인해주세요.',
      type: 'warning'
    }
  }
  if (status === 422) {
    return {
      message: '위치·QR 인증에 실패했어요. 사업장 반경 안에서 올바른 QR을 다시 스캔해주세요.',
      type: 'danger'
    }
  }
  return {
    message: '스캔에 실패했어요. 잠시 후 QR·위치를 확인해 다시 시도해주세요.',
    type: 'danger'
  }
}

async function submitScan(qrToken, { confirmEarlyCheckout = false } = {}) {
  if (!qrToken) return
  const intent = {
    payload: {
      qrToken,
      latitude: coords.value?.latitude,
      longitude: coords.value?.longitude,
      accuracyMeters: coords.value?.accuracyMeters,
      capturedAt: coords.value?.capturedAt,
      confirmEarlyCheckout
    },
    idempotencyKey: newIdempotencyKey()
  }
  pendingIntent.value = intent
  await sendIntent(intent)
}

/** 응답 유실 재확인은 최초 Body와 Key를 그대로 사용해 CHECK_OUT으로 오판되지 않게 합니다. */
async function sendIntent(intent) {
  errorMsg.value = ''
  phase.value = 'processing'
  try {
    const res = await scan(intent.payload, { idempotencyKey: intent.idempotencyKey })
    pendingIntent.value = null
    result.value = res
    if (res.result === 'CONFIRMATION_REQUIRED') {
      pendingQrToken.value = intent.payload.qrToken
      phase.value = 'confirmation'
    } else {
      phase.value = 'result'
    }
  } catch (error) {
    const status = error?.response?.status
    const uncertain = status === undefined || status >= 500
    if (!uncertain) pendingIntent.value = null
    const info = uncertain
      ? {
          message: '처리 결과를 확인하지 못했어요. 같은 요청으로 결과를 다시 확인해주세요.',
          type: 'warning'
        }
      : scanErrorInfo(error)
    errorMsg.value = info.message
    ui.toast(info.message, { type: info.type })
    phase.value = 'idle'
  }
}

async function retryPendingIntent() {
  if (pendingIntent.value) await sendIntent(pendingIntent.value)
}

/** 카메라·감지 루프 정리 */
function stopCamera() {
  if (detectTimer) {
    clearInterval(detectTimer)
    detectTimer = null
  }
  if (stream) {
    stream.getTracks().forEach((track) => track.stop())
    stream = null
  }
  if (videoEl.value) videoEl.value.srcObject = null
}

function cancelScan() {
  stopCamera()
  phase.value = 'idle'
}

async function confirmEarlyCheckout() {
  try {
    coords.value = await getLocation()
  } catch {
    ui.toast('조기 퇴근 확인에는 현재 위치가 필요합니다.', { type: 'warning' })
    return
  }
  await submitScan(pendingQrToken.value, { confirmEarlyCheckout: true })
}

function reset() {
  result.value = null
  pendingQrToken.value = ''
  pendingIntent.value = null
  phase.value = 'idle'
}

const resultLabel = () => SCAN_TYPE[result.value?.scanType]?.label ?? '기록 완료'

onBeforeUnmount(stopCamera)
</script>

<template>
  <div class="worker-scan">
    <!-- 스캔 전 안내 -->
    <section v-if="phase === 'idle'" class="intro">
      <span class="intro-icon">
        <QrCode :size="56" />
      </span>
      <h1 class="intro-title">QR 출퇴근</h1>
      <p class="intro-desc">
        사업장 QR을 스캔하면 위치를 확인해 <strong>출근·퇴근이 자동으로</strong> 기록됩니다.
        카메라와 위치 권한이 필요해요.
      </p>
      <p v-if="errorMsg" class="error">{{ errorMsg }}</p>

      <BaseButton v-if="pendingIntent" variant="worker" size="lg" block @click="retryPendingIntent">
        같은 요청 결과 다시 확인
      </BaseButton>
      <BaseButton v-else variant="worker" size="lg" block @click="startScan">
        <ScanLine :size="20" />
        스캔 시작
      </BaseButton>
    </section>

    <!-- 카메라 스캔 중 -->
    <section v-else-if="phase === 'scanning'" class="scanning">
      <div class="viewport">
        <video ref="videoEl" class="video" playsinline muted></video>
        <div class="frame"></div>
      </div>
      <p class="hint">QR을 사각형 안에 맞춰주세요.</p>
      <BaseButton variant="secondary" size="lg" block @click="cancelScan">취소</BaseButton>
    </section>

    <!-- 서버 기록 중 -->
    <section v-else-if="phase === 'processing'" class="processing">
      <ScanLine :size="40" />
      <p>출퇴근을 기록하는 중…</p>
    </section>

    <!-- 결과 모달 -->
    <BaseModal :open="phase === 'result'" :closable="false" title="인증 완료">
      <div v-if="result" class="result">
        <p class="result-type">{{ resultLabel() }} 처리되었습니다.</p>
        <p class="result-time">{{ formatDateTime(result.recordedAt) }}</p>
        <p v-if="result.isLate" class="result-late">지각 {{ result.lateMinutes }}분으로 기록됨</p>
      </div>
      <template #footer>
        <BaseButton variant="worker" size="lg" block @click="reset">확인</BaseButton>
      </template>
    </BaseModal>

    <BaseModal :open="phase === 'confirmation'" :closable="false" title="조기 퇴근 확인">
      <p class="intro-desc">
        예정 종료 시각 {{ formatDateTime(result?.scheduledEndAt) }} 전입니다. 지금 퇴근으로
        기록할까요?
      </p>
      <template #footer>
        <BaseButton variant="secondary" block @click="reset">취소</BaseButton>
        <BaseButton variant="worker" block @click="confirmEarlyCheckout">퇴근 기록</BaseButton>
      </template>
    </BaseModal>
  </div>
</template>

<style scoped>
.intro,
.processing {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-xl) 0;
  text-align: center;
}
.intro-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 96px;
  height: 96px;
  border-radius: var(--radius-pill);
  background: var(--color-worker-weak);
  color: var(--color-worker);
}
.intro-title {
  font-size: var(--text-xl);
  font-weight: var(--weight-bold);
  color: var(--color-text);
}
.intro-desc {
  font-size: var(--text-md);
  color: var(--color-text-sub);
  line-height: 1.6;
}
.intro-desc strong {
  color: var(--color-text);
  font-weight: var(--weight-medium);
}
.intro .btn {
  margin-top: var(--space-md);
}
.error {
  font-size: var(--text-sm);
  color: var(--color-danger);
}
.scanning {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}
.viewport {
  position: relative;
  width: 100%;
  aspect-ratio: 1;
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: var(--color-primary);
}
.video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.frame {
  position: absolute;
  inset: 15%;
  border: 3px solid var(--color-on-primary);
  border-radius: var(--radius-md);
  box-shadow: 0 0 0 100vmax var(--color-overlay);
}
.hint {
  text-align: center;
  font-size: var(--text-md);
  color: var(--color-text-sub);
}
.processing {
  color: var(--color-text-sub);
}
.result {
  text-align: center;
}
.result-type {
  font-size: var(--text-lg);
  font-weight: var(--weight-bold);
  color: var(--color-text);
}
.result-time {
  margin-top: var(--space-xs);
  font-size: var(--text-md);
  color: var(--color-text-sub);
}
.result-late {
  margin-top: var(--space-sm);
  font-size: var(--text-sm);
  color: var(--color-warning);
}
</style>
