<script setup>
/**
 * [C] 사장 QR  ·  /owner/qr  ·  OWNER  (탭 화면)
 * 선택 지점 동적 QR 표시(30~60초 주기 갱신).
 * 지점 컨텍스트: useWorkplaceStore().selectedId 기준.
 * 연계 API: GET /workplaces/{id}/qr → { qrToken, expiresAt }
 *   →  @/services/workplaces (getWorkplaceQr)
 *
 * 토큰 발급·만료 판정은 서버가 한다. 프론트는 표시 + 만료 전 재발급만 담당한다.
 * TODO(담당 C): QR 렌더링 라이브러리 선정 후 아래 자리표시자를 실제 QR 이미지로 교체.
 *   (신규 의존성이라 팀 공유 후 도입 — 현재는 토큰 문자열만 노출)
 */
import { QrCode, RotateCcw } from 'lucide-vue-next'
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'

import EmptyState from '@/components/common/EmptyState.vue'
import { getWorkplaceQr } from '@/services/workplaces'
import { useUiStore } from '@/stores/ui'
import { useWorkplaceStore } from '@/stores/workplace'

/** 서버가 만료시각을 주지 않을 때 쓰는 기본 갱신 주기 */
const REFRESH_MS = 30_000

const ui = useUiStore()
const workplaceStore = useWorkplaceStore()

const qr = ref(null)
const loading = ref(false)
const nextRefreshAt = ref(0) // 다음 재발급 시각(ms). 0 이면 아직 발급 전
const remaining = ref(0) // 남은 초(표시용)

let tickTimer = null

const workplaceName = computed(() => workplaceStore.selected?.name ?? '')

/** QR 토큰 발급. 만료시각이 오면 tick 이 다시 호출한다. */
async function issue() {
  const workplaceId = workplaceStore.selectedId
  if (workplaceId == null) return

  loading.value = true
  try {
    qr.value = await getWorkplaceQr(workplaceId)

    // 서버 만료시각이 유효하면 그 시각에, 없으면 기본 주기 뒤에 재발급한다.
    const expiresMs = new Date(qr.value?.expiresAt ?? '').getTime()
    nextRefreshAt.value =
      Number.isFinite(expiresMs) && expiresMs > Date.now() ? expiresMs : Date.now() + REFRESH_MS
  } catch {
    ui.toast('QR을 발급하지 못했어요.', { type: 'danger' })
    // 실패 시 매초 재시도하지 않도록 다음 주기까지 미룬다.
    nextRefreshAt.value = Date.now() + REFRESH_MS
  } finally {
    loading.value = false
  }
}

/** 1초마다 남은 시간을 갱신하고, 만료되면 새 토큰을 받아온다. */
function tick() {
  if (nextRefreshAt.value === 0) return
  remaining.value = Math.max(0, Math.ceil((nextRefreshAt.value - Date.now()) / 1000))
  if (remaining.value === 0 && !loading.value) issue()
}

onMounted(() => {
  workplaceStore.load()
  tickTimer = setInterval(tick, 1000)
})

// 화면을 벗어나면 타이머를 반드시 정리한다(백그라운드 재발급 방지).
onUnmounted(() => clearInterval(tickTimer))

// 지점을 바꾸면 그 지점의 QR 로 다시 발급받는다.
watch(() => workplaceStore.selectedId, issue, { immediate: true })
</script>

<template>
  <div class="qr-screen">
    <EmptyState
      v-if="!workplaceStore.hasWorkplace && workplaceStore.loaded"
      message="등록된 사업장이 없습니다."
    >
      사업장을 먼저 등록하면 출퇴근 QR을 발급할 수 있어요.
    </EmptyState>

    <template v-else>
      <header class="head">
        <h2 class="title">출퇴근 QR</h2>
        <p class="desc">
          <strong>{{ workplaceName }}</strong> 알바생이 이 QR을 스캔하면 출퇴근이 기록됩니다.
        </p>
      </header>

      <!-- TODO(담당 C): QR 렌더링 라이브러리 도입 후 실제 QR 이미지로 교체 -->
      <div class="qr-box">
        <template v-if="qr">
          <QrCode :size="72" class="qr-icon" />
          <p class="qr-placeholder">QR 자리 (렌더링 라이브러리 도입 예정)</p>
          <p class="qr-token">{{ qr.qrToken }}</p>
        </template>
        <p v-else class="qr-placeholder">QR을 발급하는 중…</p>
      </div>

      <p class="countdown">
        <template v-if="loading">새 QR을 발급하는 중…</template>
        <template v-else-if="qr">{{ remaining }}초 후 자동 갱신</template>
      </p>

      <button type="button" class="refresh" :disabled="loading" @click="issue">
        <RotateCcw :size="16" />
        지금 새로 발급
      </button>
    </template>
  </div>
</template>

<style scoped>
.qr-screen {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-lg);
}

.head {
  text-align: center;
}
.title {
  font-size: var(--text-xl);
  font-weight: var(--weight-bold);
  color: var(--color-text);
}
.desc {
  margin-top: var(--space-xs);
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}

/* QR 자리표시자 — 실제 QR 도입 시 이 박스 안만 교체하면 된다 */
.qr-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-sm);
  width: 100%;
  max-width: 260px;
  aspect-ratio: 1;
  padding: var(--space-lg);
  background: var(--color-surface);
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-lg);
  text-align: center;
}
.qr-icon {
  color: var(--color-owner);
}
.qr-placeholder {
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}
.qr-token {
  font-size: var(--text-sm);
  color: var(--color-text);
  word-break: break-all;
}

.countdown {
  min-height: 21px;
  font-size: var(--text-md);
  color: var(--color-text-sub);
}

.refresh {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
  padding: var(--space-sm) var(--space-lg);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill);
  font-size: var(--text-md);
  color: var(--color-text);
}
.refresh:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
</style>
