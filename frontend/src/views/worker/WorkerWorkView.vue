<script setup>
/**
 * [F] 알바생 근로관리  ·  /worker/work  ·  WORKER  (탭 화면)
 * 근무 히스토리 리스트(상태 뱃지, ? 아이콘 → 문의/신고).
 * 연계 API: GET /worker/work-cases  →  @/services/worker (listWorkerWorkCases)
 *   ? 아이콘 → 문의하기 시트(BaseBottomSheet, getOwnerContact) / 신고 → report 라우트
 * 공통: StatusChip(근무/정산 상태) · 항목 클릭 → /worker/work/work-cases/:workCaseId
 */
import { CircleHelp, Phone } from 'lucide-vue-next'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import StatusChip from '@/components/common/StatusChip.vue'
import { getOwnerContact } from '@/services/workCases'
import { listWorkerWorkCases } from '@/services/worker'
import { useUiStore } from '@/stores/ui'
import { formatDate, formatKRW } from '@/utils/format'

const router = useRouter()
const ui = useUiStore()

const workCases = ref([])
const loading = ref(true)

onMounted(load)

async function load() {
  loading.value = true
  try {
    const { content } = await listWorkerWorkCases()
    workCases.value = content ?? []
  } catch {
    ui.toast('근무 내역을 불러오지 못했습니다.', { type: 'danger' })
  } finally {
    loading.value = false
  }
}

function goDetail(workCase) {
  router.push(`/worker/work/work-cases/${workCase.workCaseId}`)
}

/* ---- 문의 · 신고 시트 ---- */
const helpOpen = ref(false)
const helpWorkCase = ref(null)
const contact = ref(null)
const contactLoading = ref(false)

async function openHelp(workCase) {
  helpWorkCase.value = workCase
  contact.value = null
  helpOpen.value = true
  contactLoading.value = true
  try {
    contact.value = await getOwnerContact(workCase.workCaseId)
  } catch {
    ui.toast('연락처를 불러오지 못했습니다.', { type: 'warning' })
  } finally {
    contactLoading.value = false
  }
}

function goReport() {
  const id = helpWorkCase.value?.workCaseId
  helpOpen.value = false
  if (id != null) router.push(`/worker/work/work-cases/${id}/report`)
}
</script>

<template>
  <div class="worker-work">
    <h1 class="page-title">근무 내역</h1>

    <p v-if="loading" class="loading">불러오는 중…</p>

    <EmptyState v-else-if="workCases.length === 0" message="아직 근무 내역이 없어요." />

    <ul v-else class="work-case-list">
      <li v-for="workCase in workCases" :key="workCase.workCaseId" class="work-case">
        <button type="button" class="work-case-main" @click="goDetail(workCase)">
          <div class="work-case-head">
            <span class="workplace">{{ workCase.workplaceName }}</span>
            <span class="date">{{ formatDate(workCase.workDate) }}</span>
          </div>
          <div class="work-case-sub">
            <span class="time">{{ workCase.time }}</span>
            <span class="wage">{{ formatKRW(workCase.dailyWage) }}</span>
          </div>
          <div class="work-case-status">
            <StatusChip :status="workCase.status" kind="workCase" />
            <StatusChip :status="workCase.settleStatus" kind="settle" />
          </div>
        </button>

        <button
          type="button"
          class="help-btn"
          aria-label="문의 또는 신고"
          @click="openHelp(workCase)"
        >
          <CircleHelp :size="20" />
        </button>
      </li>
    </ul>

    <BaseBottomSheet :open="helpOpen" title="문의 · 신고" @close="helpOpen = false">
      <div class="help-body">
        <p class="help-target">
          {{ helpWorkCase?.workplaceName }} · {{ formatDate(helpWorkCase?.workDate) }}
        </p>

        <section class="contact">
          <h2 class="contact-title">사장님께 문의</h2>
          <p v-if="contactLoading" class="contact-loading">연락처 불러오는 중…</p>
          <a v-else-if="contact" class="contact-row" :href="`tel:${contact.phone}`">
            <Phone :size="18" />
            <span>{{ contact.ownerName }}</span>
            <strong>{{ contact.phone }}</strong>
          </a>
          <p v-else class="contact-loading">연락처를 불러오지 못했습니다.</p>
        </section>
      </div>

      <template #footer>
        <BaseButton variant="danger" size="lg" block @click="goReport">
          임금분쟁 신고하기
        </BaseButton>
      </template>
    </BaseBottomSheet>
  </div>
</template>

<style scoped>
.page-title {
  font-size: var(--text-xl);
  font-weight: var(--weight-bold);
  color: var(--color-text);
}
.loading {
  margin-top: var(--space-lg);
  text-align: center;
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}
.work-case-list {
  margin-top: var(--space-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}
.work-case {
  display: flex;
  align-items: stretch;
  gap: var(--space-sm);
  padding: var(--space-lg);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}
.work-case-main {
  flex: 1;
  min-width: 0;
  text-align: left;
}
.work-case-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-sm);
}
.workplace {
  font-size: var(--text-lg);
  font-weight: var(--weight-medium);
  color: var(--color-text);
}
.date {
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}
.work-case-sub {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-top: var(--space-xs);
}
.time {
  font-size: var(--text-md);
  color: var(--color-text-sub);
}
.wage {
  font-size: var(--text-md);
  font-weight: var(--weight-bold);
  color: var(--color-text);
}
.work-case-status {
  display: flex;
  gap: var(--space-md);
  margin-top: var(--space-sm);
}
.help-btn {
  flex-shrink: 0;
  display: inline-flex;
  align-items: flex-start;
  color: var(--color-text-sub);
}
.help-body {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}
.help-target {
  font-size: var(--text-md);
  color: var(--color-text-sub);
}
.contact-title {
  font-size: var(--text-md);
  font-weight: var(--weight-medium);
  color: var(--color-text-sub);
}
.contact-loading {
  margin-top: var(--space-sm);
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}
.contact-row {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-top: var(--space-sm);
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
