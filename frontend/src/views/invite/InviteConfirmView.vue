<script setup>
/**
 * WORKER 초대 조건 확인과 Body 없는 최종 동의 화면.
 *
 * Token은 현재 경로에서만 사용하고 저장·로그하지 않는다. 한 번의 수락 의도에는 화면 메모리의
 * 같은 멱등 Key를 사용하며, 결과가 불확실한 재확인에도 Key를 유지한다.
 */
import { CheckCircle2, FileText, RefreshCw } from 'lucide-vue-next'
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AppBackHeader from '@/components/common/AppBackHeader.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import TrustBadge from '@/components/common/TrustBadge.vue'
import { contractFileUrl } from '@/services/documents'
import { newIdempotencyKey } from '@/services/http'
import { confirmInvite, getInvite } from '@/services/invites'
import { getWorkCase } from '@/services/workCases'
import { useWalletStore } from '@/stores/wallet'
import { useUiStore } from '@/stores/ui'
import {
  formatDuration,
  formatKRW,
  formatSeoulDateTime,
  formatSeoulTimeRange
} from '@/utils/format'
import {
  invitationErrorMessage,
  isInvitationForbidden,
  shouldRetainAcceptanceKey
} from '@/utils/invitation'

const route = useRoute()
const router = useRouter()
const ui = useUiStore()
const walletStore = useWalletStore()

const token = computed(() => String(route.params.token ?? ''))
const invite = ref(null)
const loading = ref(true)
const errorMsg = ref('')
const agreed = ref(false)
const confirming = ref(false)
const retryAvailable = ref(false)
const accepted = ref(null)
const acceptedWorkCase = ref(null)
const synchronizing = ref(false)
const syncError = ref(false)

// 비밀값을 반응형 Store나 Web Storage에 넣지 않는다. 이 화면 인스턴스가 사라지면 함께 폐기된다.
let acceptanceKey = null
let inviteLoadSequence = 0

const workDateText = computed(() => formatSeoulDateTime(invite.value?.startsAt).split(' ')[0])
const contractDocumentId = computed(() => acceptedWorkCase.value?.contract?.documentId ?? null)
const contractViewUrl = computed(() =>
  contractDocumentId.value ? contractFileUrl(contractDocumentId.value, 'view') : ''
)
const contractDownloadUrl = computed(() =>
  contractDocumentId.value ? contractFileUrl(contractDocumentId.value, 'download') : ''
)

onMounted(loadInvite)

watch(token, () => {
  // 같은 컴포넌트에서 다른 초대 경로로 이동하면 이전 Token의 의도와 응답을 완전히 버린다.
  acceptanceKey = null
  invite.value = null
  agreed.value = false
  confirming.value = false
  retryAvailable.value = false
  accepted.value = null
  acceptedWorkCase.value = null
  syncError.value = false
  void loadInvite()
})

async function loadInvite() {
  const requestSequence = ++inviteLoadSequence
  const requestToken = token.value
  loading.value = true
  errorMsg.value = ''
  try {
    const result = await getInvite(requestToken)
    if (requestSequence !== inviteLoadSequence) return
    invite.value = result
  } catch (error) {
    if (requestSequence !== inviteLoadSequence) return
    if (isInvitationForbidden(error)) {
      await router.replace('/forbidden')
      return
    }
    errorMsg.value = invitationErrorMessage(error)
  } finally {
    if (requestSequence === inviteLoadSequence) loading.value = false
  }
}

/** 수락 성공 뒤 권위 있는 근무·지갑·계약 연결을 다시 읽는다. */
async function synchronizeAcceptedState() {
  synchronizing.value = true
  syncError.value = false

  const [workCaseResult] = await Promise.allSettled([
    getWorkCase(accepted.value.workCaseId),
    walletStore.loadHome()
  ])

  if (workCaseResult.status === 'fulfilled') {
    acceptedWorkCase.value = workCaseResult.value
  }

  // 수락 Aggregate는 계약 문서까지 원자 생성한다. 식별자가 없으면 성공을 되돌리지 않고
  // 동기화 실패로만 표시해 사용자가 같은 수락을 새 Key로 다시 보내지 않게 한다.
  syncError.value =
    workCaseResult.status === 'rejected' ||
    !acceptedWorkCase.value?.contract?.documentId ||
    Boolean(walletStore.error)
  synchronizing.value = false
}

async function confirm() {
  if (confirming.value || accepted.value) return
  if (!agreed.value) {
    ui.toast('근무 조건과 근로계약서 자동 생성에 동의해주세요.', { type: 'warning' })
    return
  }

  if (!acceptanceKey) acceptanceKey = newIdempotencyKey()
  const requestToken = token.value
  confirming.value = true
  try {
    const result = await confirmInvite(requestToken, { idempotencyKey: acceptanceKey })
    if (requestToken !== token.value) return
    accepted.value = result
    acceptanceKey = null
    retryAvailable.value = false
    ui.toast('근무가 확정됐어요.', { type: 'success' })
    await synchronizeAcceptedState()
  } catch (error) {
    if (requestToken !== token.value) return
    retryAvailable.value = shouldRetainAcceptanceKey(error)
    if (!retryAvailable.value) acceptanceKey = null

    if (isInvitationForbidden(error)) {
      await router.replace('/forbidden')
      return
    }
    ui.toast(invitationErrorMessage(error), {
      type: retryAvailable.value ? 'warning' : 'danger',
      duration: 6000
    })
  } finally {
    confirming.value = false
  }
}

function goWorkCase() {
  router.push(`/worker/work/work-cases/${accepted.value.workCaseId}`)
}
</script>

<template>
  <div class="sub-page">
    <AppBackHeader :title="accepted ? '근무 확정 완료' : '근무 확정'" to="/worker/home" />
    <main class="screen-body">
      <p v-if="loading" class="loading">불러오는 중…</p>

      <EmptyState v-else-if="errorMsg" :message="errorMsg" />

      <template v-else-if="accepted">
        <section class="success-card">
          <CheckCircle2 :size="48" aria-hidden="true" />
          <h1>근무가 확정됐어요</h1>
          <p>근무와 임금 예치 상태를 서버 기준으로 다시 확인했습니다.</p>
          <strong>{{ accepted.escrowStatus === 'HELD' ? '임금 예치 완료' : '확정 완료' }}</strong>
        </section>

        <p v-if="synchronizing" class="loading">근무·지갑·계약을 최신화하는 중…</p>

        <section v-else-if="acceptedWorkCase" class="accepted-detail">
          <h2>{{ acceptedWorkCase.title }}</h2>
          <p>{{ acceptedWorkCase.workplaceName }} · {{ formatKRW(acceptedWorkCase.dailyWage) }}</p>
          <p>{{ formatSeoulTimeRange(acceptedWorkCase.startsAt, acceptedWorkCase.endsAt) }}</p>
        </section>

        <section v-if="contractViewUrl" class="contract-section">
          <div class="section-head">
            <h2>최종 근로계약서</h2>
            <span>서명 완료본</span>
          </div>
          <iframe :src="contractViewUrl" title="최종 근로계약서" class="contract-frame" />
          <a :href="contractDownloadUrl" class="download-link">
            <FileText :size="18" />
            계약서 다운로드
          </a>
        </section>

        <div v-if="syncError" class="sync-warning">
          <p>근무는 확정됐지만 최신 정보를 모두 불러오지 못했어요.</p>
          <BaseButton
            variant="secondary"
            block
            :disabled="synchronizing"
            @click="synchronizeAcceptedState"
          >
            <RefreshCw :size="17" />
            다시 불러오기
          </BaseButton>
        </div>

        <BaseButton variant="worker" size="lg" block @click="goWorkCase">
          근무 상세 확인
        </BaseButton>
      </template>

      <template v-else-if="invite">
        <section class="invite-head">
          <div class="head-info">
            <p class="workplace">{{ invite.workplaceName }}</p>
            <h1 class="title">{{ invite.title }}</h1>
          </div>
          <TrustBadge
            v-if="invite.ownerBadge"
            role="owner"
            :level="invite.ownerBadge.level"
            :size="44"
          />
          <span v-else class="badge-empty">등록된 배지 없음</span>
        </section>

        <section class="detail-card">
          <dl class="info">
            <div class="row">
              <dt>근무일</dt>
              <dd>{{ workDateText }}</dd>
            </div>
            <div class="row">
              <dt>근무 시간</dt>
              <dd>{{ formatSeoulTimeRange(invite.startsAt, invite.endsAt) }}</dd>
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
              <dd>v{{ invite.termsVersion }}</dd>
            </div>
            <div class="row">
              <dt>초대 만료</dt>
              <dd>{{ formatSeoulDateTime(invite.expiresAt) }}</dd>
            </div>
          </dl>
        </section>

        <label class="consent">
          <input v-model="agreed" type="checkbox" />
          <span>위 근무 조건과 서버가 최종 근로계약서를 자동 생성하는 것에 동의합니다.</span>
        </label>
        <p class="consent-note">
          확정 버튼을 누르면 현재 로그인한 알바생의 이름으로 최종 동의가 기록됩니다. 서명 이미지나
          별도 파일은 전송하지 않습니다.
        </p>

        <p class="warn">
          근무 확정 시점부터는 근무 조건 변경 및 취소가 불가합니다. 내용을 신중하게 확인해주세요.
        </p>

        <BaseButton variant="worker" size="lg" block :disabled="confirming" @click="confirm">
          {{
            confirming
              ? '확정 중…'
              : retryAvailable
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
.badge-empty {
  flex-shrink: 0;
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}

.detail-card,
.accepted-detail,
.contract-section {
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
.row dt,
.accepted-detail p {
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

.consent {
  display: flex;
  align-items: flex-start;
  gap: var(--space-sm);
  margin-top: var(--space-lg);
  padding: var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text);
  cursor: pointer;
}
.consent input {
  width: 18px;
  height: 18px;
  margin-top: 2px;
  accent-color: var(--color-worker);
}
.consent-note {
  margin-top: var(--space-sm);
  font-size: var(--text-sm);
  line-height: 1.6;
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

.success-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-xl) var(--space-lg);
  text-align: center;
  color: var(--color-worker);
  background: var(--color-worker-weak);
  border-radius: var(--radius-md);
}
.success-card h1,
.accepted-detail h2,
.section-head h2 {
  font-size: var(--text-lg);
  font-weight: var(--weight-bold);
  color: var(--color-text);
}
.success-card p {
  color: var(--color-text-sub);
}
.accepted-detail {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-md);
}
.section-head span {
  font-size: var(--text-sm);
  color: var(--color-worker);
}
.contract-frame {
  width: 100%;
  height: 55vh;
  border: 0;
  border-radius: var(--radius-sm);
  background: var(--color-bg);
}
.download-link {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-xs);
  margin-top: var(--space-md);
  color: var(--color-worker);
  font-weight: var(--weight-medium);
}
.sync-warning {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  margin: var(--space-lg) 0;
  padding: var(--space-md);
  color: var(--color-text-sub);
  background: var(--color-bg);
  border-radius: var(--radius-sm);
}
.screen-body > .btn {
  margin-top: var(--space-lg);
}
</style>
