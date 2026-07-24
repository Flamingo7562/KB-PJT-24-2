<script setup>
/**
 * [H] 근무 확정(초대 링크)  ·  /invitations/:token  ·  WORKER(로그인 필수, 딥링크)
 * 자동 입력된 근무 내역 확인 → 전자서명 → 근무 확정(=에스크로 예치). 사장 안심 뱃지 표시.
 * 만료·사용 토큰 410. OWNER 접근은 가드 G4가 차단.
 * 연계 API: GET /invitations/{token} · POST /invitations/{token}/accept
 *   →  @/services/invites (getInvite, confirmInvite)
 * route.params.token 사용. 공통: TrustBadge(role='owner') · BaseButton
 * 고정 경고 문구(변경 금지):
 *   '근로계약서 날인 완료 시점부터는 근무 변경 및 취소가 불가합니다. 신중하게 날인해주세요.'
 */
import { Eraser } from 'lucide-vue-next'
import { nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AppBackHeader from '@/components/common/AppBackHeader.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import TrustBadge from '@/components/common/TrustBadge.vue'
import { confirmInvite, getInvite } from '@/services/invites'
import { useUiStore } from '@/stores/ui'
import { formatDate, formatDuration, formatKRW, formatTimeRange } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const ui = useUiStore()

const token = route.params.token
const invite = ref(null)
const loading = ref(true)
const errorMsg = ref('')
const confirming = ref(false)

/* ---- 전자서명 canvas ---- */
const canvasRef = ref(null)
const hasSignature = ref(false)
let ctx = null
let drawing = false

onMounted(async () => {
  try {
    invite.value = await getInvite(token)
    await nextTick()
    setupCanvas()
  } catch (e) {
    errorMsg.value =
      e?.response?.status === 410
        ? '만료되었거나 이미 사용된 초대 링크예요.'
        : '초대 정보를 불러오지 못했어요.'
  } finally {
    loading.value = false
  }
})

function setupCanvas() {
  const canvas = canvasRef.value
  if (!canvas) return
  // width 설정이 컨텍스트를 초기화하므로 크기 지정 후 스타일을 잡는다.
  canvas.width = canvas.offsetWidth
  canvas.height = canvas.offsetHeight
  ctx = canvas.getContext('2d')
  ctx.lineWidth = 2
  ctx.lineCap = 'round'
  ctx.lineJoin = 'round'
  const ink = window
    .getComputedStyle(document.documentElement)
    .getPropertyValue('--color-text')
    .trim()
  if (ink) ctx.strokeStyle = ink
}

function pointPos(e) {
  const rect = canvasRef.value.getBoundingClientRect()
  return { x: e.clientX - rect.left, y: e.clientY - rect.top }
}

function startDraw(e) {
  if (!ctx) return
  drawing = true
  canvasRef.value.setPointerCapture(e.pointerId)
  const { x, y } = pointPos(e)
  ctx.beginPath()
  ctx.moveTo(x, y)
}

function draw(e) {
  if (!drawing || !ctx) return
  const { x, y } = pointPos(e)
  ctx.lineTo(x, y)
  ctx.stroke()
  hasSignature.value = true
}

function endDraw(e) {
  if (!drawing) return
  drawing = false
  try {
    canvasRef.value.releasePointerCapture(e.pointerId)
  } catch {
    // pointer capture 미지원/이미 해제된 경우 무시
  }
}

function clearSignature() {
  if (!ctx) return
  ctx.clearRect(0, 0, canvasRef.value.width, canvasRef.value.height)
  hasSignature.value = false
}

async function confirm() {
  if (!hasSignature.value) {
    ui.toast('서명을 입력해 주세요.', { type: 'warning' })
    return
  }
  confirming.value = true
  try {
    const signatureImage = canvasRef.value.toDataURL('image/png')
    await confirmInvite(token, { signatureImage })
    ui.toast('근무가 확정됐어요.', { type: 'success' })
    router.replace('/worker/home')
  } catch (e) {
    const status = e?.response?.status
    if (status === 410) {
      ui.toast('만료되었거나 이미 사용된 초대예요.', { type: 'warning' })
    } else if (status === 409) {
      ui.toast('사장님 잔액이 부족해 확정할 수 없어요.', { type: 'warning' })
    } else {
      ui.toast('근무 확정에 실패했어요.', { type: 'danger' })
    }
  } finally {
    confirming.value = false
  }
}
</script>

<template>
  <div class="sub-page">
    <AppBackHeader title="근무 확정" to="/worker/home" />
    <main class="screen-body">
      <p v-if="loading" class="loading">불러오는 중…</p>

      <EmptyState v-else-if="errorMsg" :message="errorMsg" />

      <template v-else>
        <section class="invite-head">
          <div class="head-info">
            <p class="workplace">{{ invite.workplaceName }}</p>
            <h1 class="title">{{ invite.title }}</h1>
          </div>
          <TrustBadge role="owner" :level="invite.ownerBadge?.level ?? 0" :size="44" />
        </section>

        <section class="detail-card">
          <dl class="info">
            <div class="row">
              <dt>근무일</dt>
              <dd>{{ formatDate(invite.workDate) }}</dd>
            </div>
            <div class="row">
              <dt>근무 시간</dt>
              <dd>{{ formatTimeRange(invite.startTime, invite.endTime) }}</dd>
            </div>
            <div class="row">
              <dt>휴게 시간</dt>
              <dd>
                {{ formatDuration(invite.breakMinutes) }}
                <span class="break-tag">{{ invite.breakPaid ? '유급' : '무급' }}</span>
              </dd>
            </div>
            <div class="row">
              <dt>일급</dt>
              <dd class="wage">{{ formatKRW(invite.dailyWage) }}</dd>
            </div>
          </dl>
        </section>

        <section class="sign-section">
          <div class="sign-head">
            <h2 class="sign-title">전자서명</h2>
            <button type="button" class="clear-btn" @click="clearSignature">
              <Eraser :size="15" />
              지우기
            </button>
          </div>
          <canvas
            ref="canvasRef"
            class="sign-canvas"
            @pointerdown="startDraw"
            @pointermove="draw"
            @pointerup="endDraw"
            @pointercancel="endDraw"
          ></canvas>
          <p class="sign-hint">위 영역에 손가락(또는 마우스)으로 서명해 주세요.</p>
        </section>

        <p class="warn">
          근로계약서 날인 완료 시점부터는 근무 변경 및 취소가 불가합니다. 신중하게 날인해주세요.
        </p>

        <BaseButton variant="worker" size="lg" block :disabled="confirming" @click="confirm">
          {{ confirming ? '확정 중…' : '서명하고 근무 확정' }}
        </BaseButton>
      </template>
    </main>
  </div>
</template>

<style scoped>
.screen-body {
  padding: var(--space-lg);
}
.loading {
  margin-top: var(--space-xl);
  text-align: center;
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}

.invite-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
}
.workplace {
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}
.title {
  margin-top: var(--space-xs);
  font-size: var(--text-xl);
  font-weight: var(--weight-bold);
  color: var(--color-text);
}

.detail-card {
  margin-top: var(--space-lg);
  padding: var(--space-lg);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}
.row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: var(--space-sm) 0;
}
.row dt {
  font-size: var(--text-md);
  color: var(--color-text-sub);
}
.row dd {
  font-size: var(--text-md);
  color: var(--color-text);
}
.break-tag {
  margin-left: var(--space-xs);
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}
.wage {
  font-weight: var(--weight-bold);
}

.sign-section {
  margin-top: var(--space-lg);
}
.sign-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.sign-title {
  font-size: var(--text-md);
  font-weight: var(--weight-medium);
  color: var(--color-text-sub);
}
.clear-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}
.sign-canvas {
  width: 100%;
  height: 180px;
  margin-top: var(--space-sm);
  background: var(--color-surface);
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-sm);
  touch-action: none;
  cursor: crosshair;
}
.sign-hint {
  margin-top: var(--space-xs);
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}

.warn {
  margin: var(--space-lg) 0;
  padding: var(--space-md);
  font-size: var(--text-sm);
  color: var(--color-danger);
  background: var(--color-danger-bg);
  border-radius: var(--radius-sm);
}
</style>
