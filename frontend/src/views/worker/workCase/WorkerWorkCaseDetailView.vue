<script setup>
/**
 * [F] 알바생 근무 정보 상세  ·  /worker/work/work-cases/:workCaseId  ·  WORKER(본인 근무)
 * 근무 정보 확인(제목·날짜·시간·휴게·일급·정산 상태).
 * 연계 API: GET /work-cases/{id} · GET /work-cases/{id}/workplace-contact  →  @/services/workCases
 * route.params.workCaseId 사용. 공통: StatusChip · 문의하기 시트 · 신고 진입.
 */
import { Phone } from 'lucide-vue-next'
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AppBackHeader from '@/components/common/AppBackHeader.vue'
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import StatusChip from '@/components/common/StatusChip.vue'
import { getOwnerContact, getWorkCase } from '@/services/workCases'
import { useUiStore } from '@/stores/ui'
import { formatDate, formatDuration, formatKRW, formatTimeRange } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const ui = useUiStore()

const workCaseId = route.params.workCaseId
const workCase = ref(null)
const loading = ref(true)

onMounted(async () => {
  try {
    workCase.value = await getWorkCase(workCaseId)
  } catch {
    ui.toast('근무 정보를 불러오지 못했습니다.', { type: 'danger' })
  } finally {
    loading.value = false
  }
})

/* ---- 문의하기 시트 ---- */
const contactOpen = ref(false)
const contact = ref(null)
const contactLoading = ref(false)

async function openContact() {
  contact.value = null
  contactOpen.value = true
  contactLoading.value = true
  try {
    contact.value = await getOwnerContact(workCaseId)
  } catch {
    ui.toast('연락처를 불러오지 못했습니다.', { type: 'warning' })
  } finally {
    contactLoading.value = false
  }
}

function goReport() {
  router.push(`/worker/work/work-cases/${workCaseId}/report`)
}
</script>

<template>
  <div class="sub-page">
    <AppBackHeader title="근무 정보" />
    <main class="screen-body">
      <p v-if="loading" class="loading">불러오는 중…</p>

      <EmptyState v-else-if="!workCase" message="근무 정보를 찾을 수 없습니다." />

      <template v-else>
        <section class="detail-card">
          <header class="head">
            <div>
              <p class="workplace">{{ workCase.workplaceName }}</p>
              <h1 class="title">{{ workCase.title }}</h1>
            </div>
            <div class="chips">
              <StatusChip :status="workCase.status" kind="workCase" />
              <StatusChip :status="workCase.settleStatus" kind="settle" />
            </div>
          </header>

          <dl class="info">
            <div class="row">
              <dt>근무일</dt>
              <dd>{{ formatDate(workCase.workDate) }}</dd>
            </div>
            <div class="row">
              <dt>근무 시간</dt>
              <dd>{{ formatTimeRange(workCase.startTime, workCase.endTime) }}</dd>
            </div>
            <div class="row">
              <dt>휴게 시간</dt>
              <dd>
                {{ formatDuration(workCase.breakMinutes) }}
                <span class="break-tag">{{ workCase.breakPaid ? '유급' : '무급' }}</span>
              </dd>
            </div>
            <div class="row">
              <dt>일급</dt>
              <dd class="wage">{{ formatKRW(workCase.dailyWage) }}</dd>
            </div>
          </dl>
        </section>

        <div class="actions">
          <BaseButton variant="secondary" size="lg" block @click="openContact">
            <Phone :size="18" />
            사장님께 문의
          </BaseButton>
          <BaseButton variant="danger" size="lg" block @click="goReport">
            임금분쟁 신고
          </BaseButton>
        </div>
      </template>
    </main>

    <BaseBottomSheet :open="contactOpen" title="사장님 연락처" @close="contactOpen = false">
      <p v-if="contactLoading" class="contact-loading">연락처 불러오는 중…</p>
      <a v-else-if="contact" class="contact-row" :href="`tel:${contact.phone}`">
        <Phone :size="18" />
        <span>{{ contact.ownerName }}</span>
        <strong>{{ contact.phone }}</strong>
      </a>
      <p v-else class="contact-loading">연락처를 불러오지 못했습니다.</p>
    </BaseBottomSheet>
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
.detail-card {
  padding: var(--space-lg);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}
.head {
  display: flex;
  align-items: flex-start;
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
.chips {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: var(--space-xs);
  flex-shrink: 0;
}
.info {
  margin-top: var(--space-lg);
  padding-top: var(--space-md);
  border-top: 1px solid var(--color-border);
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
.actions {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  margin-top: var(--space-lg);
}
.contact-loading {
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}
.contact-row {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text);
}
.contact-row strong {
  margin-left: auto;
  font-weight: var(--weight-bold);
  color: var(--color-worker);
}
</style>
