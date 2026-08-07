<script setup>
/**
 * 인증 WORKER가 초대 조건을 확인하고 Body 없는 POST로 최종 동의하는 화면입니다.
 * 수락 결과가 불명확한 동안에는 같은 멱등 Key를 유지하고, 성공 뒤에는 수락 API를 다시
 * 호출하지 않은 채 근무 상세·지갑 거래를 재조회해 계약 문서 Stream으로 연결합니다.
 */
import { CheckCircle2, FileText, RefreshCw } from 'lucide-vue-next'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AppBackHeader from '@/components/common/AppBackHeader.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import TrustBadge from '@/components/common/TrustBadge.vue'
import { serverDocumentFileUrl } from '@/services/documents'
import { confirmInvite, getInvite } from '@/services/invites'
import { newIdempotencyKey } from '@/services/http'
import { getWorkCase } from '@/services/workCases'
import { useUiStore } from '@/stores/ui'
import { useWalletStore } from '@/stores/wallet'
import { formatDuration, formatKRW, formatSeoulDateTime, formatSeoulTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const ui = useUiStore()
const walletStore = useWalletStore()

const token = String(route.params.token ?? '')
const invite = ref(null)
const loading = ref(true)
const errorMsg = ref('')
const consented = ref(false)
const confirming = ref(false)
const retryPending = ref(false)
const acceptKey = ref(null)
const acceptedResult = ref(null)
const acceptedWorkCase = ref(null)
const syncing = ref(false)
const syncError = ref(false)

const contractFileUrl = computed(() => {
  const documentId = acceptedWorkCase.value?.contract?.documentId
  return documentId ? serverDocumentFileUrl(documentId, 'view') : ''
})

onMounted(loadInvite)

async function loadInvite() {
  loading.value = true
  errorMsg.value = ''
  try {
    invite.value = await getInvite(token)
  } catch (error) {
    if (handleAccessError(error)) return
    errorMsg.value = invitationErrorMessage(error)
  } finally {
    loading.value = false
  }
}

function handleAccessError(error) {
  const status = error?.response?.status
  const code = error?.code ?? error?.response?.data?.code

  if (status === 401 || code === 'AUTH_REQUIRED') {
    router.replace({ path: '/worker/login', query: { redirect: route.fullPath } })
    return true
  }
  if (status === 403 || code === 'ROLE_MISMATCH' || code === 'FORBIDDEN') {
    router.replace('/forbidden')
    return true
  }
  return false
}

function invitationErrorMessage(error) {
  const code = error?.code ?? error?.response?.data?.code
  const messages = {
    RESOURCE_NOT_FOUND: '초대 링크를 찾을 수 없습니다.',
    INVITATION_EXPIRED: '초대 링크가 만료되었습니다.',
    INVITATION_REVOKED: '철회된 초대 링크입니다.',
    INVITATION_ALREADY_ACCEPTED: '이미 수락된 초대 링크입니다.',
    INVITATION_TERMS_CHANGED: '근무 조건이 변경되어 이 초대를 사용할 수 없습니다.',
    WORK_CASE_LOCKED: '현재 확정할 수 없는 근무입니다.'
  }
  return messages[code] ?? '초대 정보를 불러오지 못했습니다.'
}

function acceptanceErrorMessage(error) {
  const code = error?.code ?? error?.response?.data?.code
  const serverMessage = error?.response?.data?.message ?? ''
  if (code === 'CONFLICT') {
    return serverMessage.includes('잔액')
      ? '사장님의 예치 준비가 완료되지 않아 지금은 근무를 확정할 수 없습니다.'
      : '요청 결과를 확인하지 못했습니다. 같은 요청으로 다시 확인해 주세요.'
  }

  const messages = {
    RESOURCE_NOT_FOUND: '초대 링크를 찾을 수 없습니다.',
    INVITATION_EXPIRED: '초대 링크가 만료되었습니다.',
    INVITATION_REVOKED: '철회된 초대 링크입니다.',
    INVITATION_ALREADY_ACCEPTED: '이미 다른 요청에서 수락된 초대입니다.',
    INVITATION_TERMS_CHANGED: '근무 조건이 변경되어 이 초대를 수락할 수 없습니다.',
    WORK_CASE_LOCKED: '현재 확정할 수 없는 근무입니다.',
    IDEMPOTENCY_KEY_REUSED: '요청 조건이 달라졌습니다. 다시 눌러 새 요청으로 확인해 주세요.'
  }
  return messages[code] ?? '근무 확정 결과를 확인하지 못했습니다.'
}

function shouldKeepAcceptanceKey(error) {
  const status = error?.response?.status
  const code = error?.code ?? error?.response?.data?.code
  return status == null || status >= 500 || code === 'CONFLICT'
}

async function confirm() {
  if (confirming.value || acceptedResult.value) return
  if (!consented.value) {
    ui.toast('근무 조건과 최종 동의 내용을 확인해 주세요.', { type: 'warning' })
    return
  }

  // 첫 클릭부터 결과가 확정될 때까지 한 Key를 유지해 중복 계약·예치를 막는다.
  acceptKey.value ??= newIdempotencyKey()
  confirming.value = true
  retryPending.value = false

  let result
  try {
    result = await confirmInvite(token, { idempotencyKey: acceptKey.value })
  } catch (error) {
    if (handleAccessError(error)) return

    retryPending.value = shouldKeepAcceptanceKey(error)
    if (!retryPending.value) acceptKey.value = null
    ui.toast(acceptanceErrorMessage(error), {
      type: retryPending.value ? 'warning' : 'danger'
    })
    return
  } finally {
    confirming.value = false
  }

  // 200 최초 응답과 Replay 응답은 동일한 성공 Body이므로 같은 완료 경로로 처리한다.
  acceptedResult.value = result
  acceptKey.value = null
  consented.value = false
  ui.toast('근무가 확정되었습니다.', { type: 'success' })
  await syncAcceptedState()
}

async function syncAcceptedState() {
  if (!acceptedResult.value?.workCaseId || syncing.value) return

  syncing.value = true
  syncError.value = false
  const [workCaseResult, walletResult] = await Promise.allSettled([
    getWorkCase(acceptedResult.value.workCaseId),
    walletStore.loadTransactions()
  ])

  if (workCaseResult.status === 'fulfilled') {
    acceptedWorkCase.value = workCaseResult.value
  }
  syncError.value =
    workCaseResult.status === 'rejected' ||
    walletResult.status === 'rejected' ||
    !acceptedWorkCase.value?.contract?.documentId
  syncing.value = false
}

function goWorkCase() {
  router.replace(`/worker/work/work-cases/${acceptedResult.value.workCaseId}`)
}
</script>

<template>
  <div class="sub-page">
    <AppBackHeader title="근무 확정" to="/worker/home" />
    <main class="screen-body">
      <p v-if="loading" class="loading">불러오는 중…</p>

      <template v-else-if="acceptedResult">
        <section class="success-card" aria-live="polite">
          <CheckCircle2 :size="48" />
          <h1>근무가 확정되었습니다</h1>
          <p>
            근로계약서가 생성되었고 일급 예치 상태는
            <strong>{{ acceptedResult.escrowStatus === 'HELD' ? '예치 완료' : '확인 중' }}</strong
            >입니다.
          </p>
        </section>

        <section v-if="acceptedWorkCase" class="detail-card">
          <p class="workplace">{{ acceptedWorkCase.workplaceName }}</p>
          <h2 class="title">{{ acceptedWorkCase.title }}</h2>
          <dl class="info">
            <div class="row">
              <dt>근무 일정</dt>
              <dd>
                {{ formatSeoulDateTime(acceptedWorkCase.startsAt) }} ~
                {{ formatSeoulTime(acceptedWorkCase.endsAt) }}
              </dd>
            </div>
            <div class="row">
              <dt>계약 조건</dt>
              <dd>버전 {{ acceptedWorkCase.contract?.sourceTermsVersion }}</dd>
            </div>
          </dl>
        </section>

        <p v-if="syncing" class="sync-message">근무·지갑·계약 정보를 동기화하는 중…</p>
        <div v-else-if="syncError" class="sync-warning">
          <p>근무 확정은 완료됐지만 최신 정보를 모두 불러오지 못했습니다.</p>
          <button type="button" @click="syncAcceptedState">
            <RefreshCw :size="16" />
            다시 불러오기
          </button>
        </div>

        <div class="success-actions">
          <BaseButton variant="worker" size="lg" block @click="goWorkCase">
            근무 정보 보기
          </BaseButton>
          <a
            v-if="contractFileUrl"
            :href="contractFileUrl"
            class="contract-link"
            target="_blank"
            rel="noopener"
          >
            <FileText :size="18" />
            최종 근로계약서 보기
          </a>
        </div>
      </template>

      <div v-else-if="errorMsg" class="error-state">
        <EmptyState :message="errorMsg" />
        <BaseButton variant="secondary" block @click="router.replace('/worker/home')">
          홈으로
        </BaseButton>
      </div>

      <template v-else-if="invite">
        <section class="invite-head">
          <div class="head-info">
            <p class="workplace">{{ invite.workplaceName }}</p>
            <h1 class="title">{{ invite.title }}</h1>
          </div>
          <div class="badge-wrap">
            <TrustBadge
              v-if="invite.ownerBadge"
              role="owner"
              :level="invite.ownerBadge.level"
              :size="44"
            />
            <span v-else class="no-owner-badge">활성 안심 뱃지 없음</span>
          </div>
        </section>

        <section class="detail-card">
          <dl class="info">
            <div class="row">
              <dt>근무 일정</dt>
              <dd>
                {{ formatSeoulDateTime(invite.startsAt) }} ~ {{ formatSeoulTime(invite.endsAt) }}
              </dd>
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
            <div class="row">
              <dt>조건 버전</dt>
              <dd>{{ invite.termsVersion }}</dd>
            </div>
            <div class="row">
              <dt>초대 만료</dt>
              <dd>{{ formatSeoulDateTime(invite.expiresAt) }}</dd>
            </div>
          </dl>
        </section>

        <section class="consent-card">
          <h2>전자동의</h2>
          <p>
            위 조건으로 근로계약서를 생성하고, 로그인한 이름을 전자동의 증거로 기록하는 데
            동의합니다. 서명 이미지나 별도 파일은 전송되지 않습니다.
          </p>
          <label class="consent-check">
            <input v-model="consented" type="checkbox" />
            <span>근무 조건과 계약 확정에 동의합니다.</span>
          </label>
        </section>

        <p class="warn">
          근로계약서 날인 완료 시점부터는 근무 변경 및 취소가 불가합니다. 신중하게 날인해주세요.
        </p>

        <BaseButton
          variant="worker"
          size="lg"
          block
          :disabled="confirming || !consented"
          @click="confirm"
        >
          {{
            confirming
              ? '확정 중…'
              : retryPending
                ? '같은 요청으로 다시 확인'
                : '동의하고 근무 확정'
          }}
        </BaseButton>
      </template>
    </main>
  </div>
</template>

<style scoped>
.screen-body {
  padding: var(--space-lg);
}
.loading,
.sync-message {
  margin-top: var(--space-xl);
  text-align: center;
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}
.error-state,
.success-actions {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
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
.badge-wrap {
  flex-shrink: 0;
  text-align: center;
}
.no-owner-badge {
  display: inline-block;
  max-width: 72px;
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}
.detail-card,
.consent-card,
.success-card {
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
  gap: var(--space-md);
  padding: var(--space-sm) 0;
}
.row dt {
  flex-shrink: 0;
  font-size: var(--text-md);
  color: var(--color-text-sub);
}
.row dd {
  text-align: right;
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
.consent-card h2 {
  font-size: var(--text-lg);
  font-weight: var(--weight-bold);
  color: var(--color-text);
}
.consent-card p {
  margin-top: var(--space-sm);
  font-size: var(--text-sm);
  line-height: 1.6;
  color: var(--color-text-sub);
}
.consent-check {
  display: flex;
  align-items: flex-start;
  gap: var(--space-sm);
  margin-top: var(--space-md);
  font-size: var(--text-md);
  color: var(--color-text);
  cursor: pointer;
}
.consent-check input {
  width: 18px;
  height: 18px;
  margin-top: 2px;
  accent-color: var(--color-worker);
}
.warn {
  margin: var(--space-lg) 0;
  padding: var(--space-md);
  font-size: var(--text-sm);
  color: var(--color-danger);
  background: var(--color-danger-bg);
  border-radius: var(--radius-sm);
}
.success-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-sm);
  text-align: center;
  color: var(--color-worker);
}
.success-card h1 {
  font-size: var(--text-xl);
  font-weight: var(--weight-bold);
  color: var(--color-text);
}
.success-card p {
  font-size: var(--text-md);
  color: var(--color-text-sub);
}
.sync-warning {
  margin-top: var(--space-md);
  padding: var(--space-md);
  border-radius: var(--radius-sm);
  background: var(--color-bg);
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}
.sync-warning button {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
  margin-top: var(--space-sm);
  color: var(--color-worker);
  font-weight: var(--weight-medium);
}
.success-actions {
  margin-top: var(--space-lg);
}
.contract-link {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-xs);
  padding: var(--space-md) var(--space-lg);
  border: 1px solid var(--color-worker);
  border-radius: var(--radius-sm);
  color: var(--color-worker);
  font-size: var(--text-lg);
  font-weight: var(--weight-medium);
}
</style>
