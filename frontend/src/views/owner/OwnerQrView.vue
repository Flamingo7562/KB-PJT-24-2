<script setup>
/**
 * [C] 사장 QR  ·  /owner/qr  ·  OWNER  (탭 화면)
 * 선택 지점의 정적 QR 표시(값 고정 — 출력해 매장에 부착하는 용도).
 * 지점 컨텍스트: useWorkplaceStore().selectedId 기준.
 * 연계 API: GET /workplaces/{id}/qr → { qrToken }
 *   →  @/services/workplaces (getWorkplaceQr)
 *
 * 토큰 발급·검증은 서버가 한다. 만료·재발급 주기가 없으므로 프론트는 표시만 담당하고,
 * 지점이 바뀔 때만 다시 조회한다.
 * TODO(담당 C): QR 렌더링 라이브러리 선정 후 아래 자리표시자를 실제 QR 이미지로 교체.
 *   (신규 의존성이라 팀 공유 후 도입 — 현재는 토큰 문자열만 노출)
 */
import { QrCode } from 'lucide-vue-next'
import { computed, onMounted, ref, watch } from 'vue'

import EmptyState from '@/components/common/EmptyState.vue'
import { getWorkplaceQr } from '@/services/workplaces'
import { useUiStore } from '@/stores/ui'
import { useWorkplaceStore } from '@/stores/workplace'

const ui = useUiStore()
const workplaceStore = useWorkplaceStore()

const qr = ref(null)
const loading = ref(false)

const workplaceName = computed(() => workplaceStore.selected?.name ?? '')

/** 선택 지점의 QR 조회. 값이 고정이라 지점 전환 시에만 호출된다. */
async function load() {
  const workplaceId = workplaceStore.selectedId
  if (workplaceId == null) return

  loading.value = true
  try {
    qr.value = await getWorkplaceQr(workplaceId)
  } catch {
    qr.value = null
    ui.toast('QR을 불러오지 못했어요.', { type: 'danger' })
  } finally {
    loading.value = false
  }
}

onMounted(() => workplaceStore.load())

// 지점을 바꾸면 그 지점의 QR 을 다시 불러온다.
watch(() => workplaceStore.selectedId, load, { immediate: true })
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
        <p class="desc">QR은 바뀌지 않으니 출력해서 매장에 붙여두고 계속 사용하세요.</p>
      </header>

      <!-- TODO(담당 C): QR 렌더링 라이브러리 도입 후 실제 QR 이미지로 교체 -->
      <div class="qr-box">
        <template v-if="qr">
          <QrCode :size="72" class="qr-icon" />
          <p class="qr-placeholder">QR 자리 (렌더링 라이브러리 도입 예정)</p>
          <p class="qr-token">{{ qr.qrToken }}</p>
        </template>
        <p v-else class="qr-placeholder">
          <template v-if="loading">QR을 불러오는 중…</template>
          <template v-else>표시할 QR이 없어요.</template>
        </p>
      </div>
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
</style>
