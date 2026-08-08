<script setup>
/**
 * [C] 근무 상세  ·  /owner/attendance/work-cases/:workCaseId  ·  OWNER
 * 근무 상세 + 매칭 알바생 성실 뱃지. 수정·삭제·연결 링크 발급은 수락 전(DRAFT)만.
 * 확정(날인) 후 수정·삭제 버튼 숨김 — 서버도 409 WORK_CASE_LOCKED.
 * 연계 API: GET /work-cases/{id} · PATCH /work-cases/{id} · DELETE /work-cases/{id} · POST /work-cases/{id}/invitations
 *   →  @/services/workCases (getWorkCase, updateWorkCase, deleteWorkCase, createInvite)
 * route.params.workCaseId 사용. 공통: TrustBadge(알바생 뱃지) · StatusChip · BaseModal(삭제 확인)
 */
import { FileText, Link2, Pencil, RefreshCw, Trash2 } from 'lucide-vue-next'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AppBackHeader from '@/components/common/AppBackHeader.vue'
import AppField from '@/components/common/AppField.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import BaseModal from '@/components/common/BaseModal.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import StatusChip from '@/components/common/StatusChip.vue'
import TrustBadge from '@/components/common/TrustBadge.vue'
import {
  canIssueInvitation,
  invitationStatusLabel,
  isDraft,
  isInvitationUsable
} from '@/constants/workCaseStatus'
import { contractFileUrl } from '@/services/documents'
import { fieldErrorMap } from '@/services/http'
import {
  createInvite,
  deleteWorkCase,
  getWorkCase,
  reissueInvite,
  updateWorkCase
} from '@/services/workCases'
import { useUiStore } from '@/stores/ui'
import { copyText } from '@/utils/clipboard'
import { escrowStatusLabel } from '@/utils/constants'
import {
  formatDate,
  formatDuration,
  formatKRW,
  formatSeoulDateTime,
  formatSeoulTime,
  formatSeoulTimeRange
} from '@/utils/format'
import { isPositiveAmount, isRequired } from '@/utils/validators'

const route = useRoute()
const router = useRouter()
const ui = useUiStore()

const workCase = ref(null)
const loading = ref(true)
const editing = ref(false)
const submitting = ref(false)
const deleteOpen = ref(false)
const reissueOpen = ref(false) // 재발급은 이전 링크를 무효화하므로 확인을 받는다
const copying = ref(false) // 연결 링크 생성 중(중복 클릭 방지)

/** 수정·삭제는 서버와 같이 DRAFT 만 허용한다. */
const canModify = computed(() => isDraft(workCase.value?.status))

/** 초대 발급은 DRAFT 보다 좁다 — 미매칭·시작 전까지 함께 본다(@/constants/workCaseStatus). */
const canIssueInviteLink = computed(() => canIssueInvitation(workCase.value))

const latestInvitation = computed(() => workCase.value?.latestInvitation ?? null)

/** 지금 유효한 링크가 있어야 "교체(재발급)"가 의미 있다. */
const canReissueInviteLink = computed(
  () => canIssueInviteLink.value && isInvitationUsable(latestInvitation.value)
)

/**
 * 초대 이력이 있으면 서버 DELETE 는 행을 지우지 않고 CANCELED 로 전이한다
 * (WorkCaseServiceImpl.delete → countInvitations > 0). 두 결과의 안내가 달라야 해서
 * 이미 받아 둔 상세로 미리 판별한다 — 두 경우 모두 응답은 204 라 구분할 수 없다.
 */
const deleteKeepsHistory = computed(() => latestInvitation.value != null)
const contractViewUrl = computed(() => {
  const documentId = workCase.value?.contract?.documentId
  return documentId ? contractFileUrl(documentId, 'view') : ''
})

/** 계약·예치·근태처럼 "이 근무가 왜 잠겼는지"를 보여주는 근거가 하나라도 있는지. */
const hasProgressInfo = computed(
  () =>
    !!latestInvitation.value ||
    !!workCase.value?.contract ||
    !!workCase.value?.escrow ||
    !!workCase.value?.settlement ||
    !!workCase.value?.attendance?.checkedInAt ||
    !!workCase.value?.attendance?.checkedOutAt
)

const invitationText = computed(() => {
  const invitation = latestInvitation.value
  if (!invitation) return ''
  const label = invitationStatusLabel(invitation.status)
  // PENDING 이어도 만료 시각이 지나면 쓸 수 없다 — 상태만 보여주면 사장이 오해한다.
  if (invitation.status === 'PENDING' && !isInvitationUsable(invitation)) {
    return `${label} (기한 지남)`
  }
  return label
})

const form = reactive({
  title: '',
  workDate: '',
  startTime: '',
  endTime: '',
  breakMinutes: '',
  breakPaid: false,
  dailyWage: ''
})
const errors = reactive({
  title: '',
  workDate: '',
  startTime: '',
  endTime: '',
  breakMinutes: '',
  dailyWage: ''
})

async function load() {
  loading.value = true
  try {
    workCase.value = await getWorkCase(route.params.workCaseId)
  } catch {
    ui.toast('근무 정보를 불러오지 못했어요.', { type: 'danger' })
  } finally {
    loading.value = false
  }
}

onMounted(load)

function startEdit() {
  const s = workCase.value
  form.title = s.title ?? ''
  form.workDate = s.workDate ?? ''
  // 응답은 startsAt/endsAt(UTC Instant)만 준다. 저장 시 서버는 이 값을 Asia/Seoul 벽시계로
  // 다시 읽으므로, 편집 폼도 같은 기준으로 채워야 브라우저 TZ 와 무관하게 왕복이 보존된다.
  form.startTime = formatSeoulTime(s.startsAt)
  form.endTime = formatSeoulTime(s.endsAt)
  form.breakMinutes = s.breakMinutes ?? ''
  form.breakPaid = s.breakPaid ?? false
  form.dailyWage = s.dailyWage ?? ''
  Object.keys(errors).forEach((key) => (errors[key] = ''))
  editing.value = true
}

function validate() {
  // 직전 서버 검증 오류가 다음 제출을 막지 않도록 다시 검증할 때 비운다.
  errors.breakMinutes = ''
  errors.title = isRequired(form.title, '제목').message
  errors.workDate = isRequired(form.workDate, '근무 날짜').message
  errors.startTime = isRequired(form.startTime, '시작시간').message
  errors.endTime = isRequired(form.endTime, '종료시간').message
  if (!errors.endTime && form.startTime && form.endTime <= form.startTime) {
    errors.endTime = '종료시간은 시작시간보다 늦어야 합니다.'
  }
  errors.dailyWage = isPositiveAmount(form.dailyWage).message

  return Object.values(errors).every((message) => message === '')
}

async function onSave() {
  if (!validate()) {
    ui.toast('입력값을 다시 확인해주세요.', { type: 'warning' })
    return
  }

  // 서버는 조건을 바꾸면 PENDING 초대를 REVOKED 로 철회한다(revokePendingInvitations).
  // 이미 보낸 링크가 조용히 죽으므로, 저장 전에 유효한 링크가 있었는지 기억해 두고 알린다.
  const hadUsableInvitation = isInvitationUsable(latestInvitation.value)

  submitting.value = true
  try {
    await updateWorkCase(workCase.value.workCaseId, {
      title: form.title.trim(),
      workDate: form.workDate,
      startTime: form.startTime,
      endTime: form.endTime,
      breakMinutes: Number(form.breakMinutes || 0),
      breakPaid: form.breakPaid,
      dailyWage: Number(form.dailyWage)
    })
    ui.toast('근무 정보를 수정했어요.', { type: 'success' })
    if (hadUsableInvitation) {
      ui.toast('조건이 바뀌어 이전 연결 링크는 철회됐어요. 새 링크를 발급해 다시 보내주세요.', {
        type: 'warning',
        duration: 6000
      })
    }
    editing.value = false
    await load()
  } catch (err) {
    const serverErrors = fieldErrorMap(err)
    if (Object.keys(serverErrors).length > 0) {
      Object.assign(errors, serverErrors)
    } else if (err?.code === 'WORK_CASE_LOCKED') {
      ui.toast('이미 확정된 근무는 수정할 수 없어요.', { type: 'danger' })
    } else {
      ui.toast('수정하지 못했어요. 이미 확정된 근무일 수 있어요.', { type: 'danger' })
    }
  } finally {
    submitting.value = false
  }
}

async function onDelete() {
  submitting.value = true
  try {
    const keepsHistory = deleteKeepsHistory.value
    await deleteWorkCase(workCase.value.workCaseId)
    // 두 경로 모두 204 라 응답으로는 구분할 수 없다. 초대 이력 유무로 결과를 안내한다.
    ui.toast(
      keepsHistory
        ? '초대 이력이 있어 근무를 취소 처리했어요. 목록에 취소 상태로 남습니다.'
        : '근무를 삭제했어요.',
      { type: 'success' }
    )
    // 모달을 닫지 않은 채로 화면을 떠난다.
    // 닫기 트랜지션 도중에 이 화면이 unmount 되면 Teleport 로 body 에 붙은
    // 오버레이가 남아(opacity:0) 이후 화면의 클릭을 통째로 가로챈다.
    await router.push('/owner/attendance')
  } catch (err) {
    const message =
      err?.code === 'WORK_CASE_LOCKED'
        ? '이미 확정된 근무는 삭제할 수 없어요.'
        : '삭제하지 못했어요. 이미 확정된 근무일 수 있어요.'
    ui.toast(message, { type: 'danger' })
    deleteOpen.value = false
  } finally {
    submitting.value = false
  }
}

/** 초대 발급 실패를 승인 오류 Code 별로 구분해 안내한다. */
function inviteErrorMessage(err) {
  switch (err?.code) {
    case 'WORK_CASE_LOCKED':
      // 서버는 DRAFT·미매칭·시작 전 세 조건을 하나의 오류로 합친다(정보 노출 방지).
      return '이미 수락됐거나 시작 시각이 지난 근무는 링크를 발급할 수 없어요.'
    case 'CONFLICT':
      return '링크를 발급할 수 없는 상태예요. 화면을 새로고침한 뒤 다시 시도해주세요.'
    case 'ROLE_MISMATCH':
    case 'FORBIDDEN':
      return '이 근무의 링크를 발급할 권한이 없어요.'
    case 'RESOURCE_NOT_FOUND':
      return '근무를 찾을 수 없어요. 목록에서 다시 열어주세요.'
    default:
      return '링크를 만들지 못했어요. 잠시 후 다시 시도해주세요.'
  }
}

/** 발급·재발급 결과를 복사하고 만료 시각까지 알린다. */
async function copyIssuedInvite({ inviteUrl, expiresAt }, reissued) {
  // 만료 시각은 근무 시작 시각이다 — 언제까지 유효한지 알려야 사장이 전송 시점을 판단한다.
  const expiryText = expiresAt ? ` ${formatSeoulDateTime(expiresAt)}까지 유효해요.` : ''

  if (await copyText(inviteUrl)) {
    ui.toast(
      reissued
        ? `새 연결 링크를 복사했어요. 이전 링크는 더 이상 쓸 수 없어요.${expiryText}`
        : `연결 링크를 복사했어요.${expiryText}`,
      { type: 'success', duration: 6000 }
    )
    return
  }
  // 브라우저가 복사를 막은 경우 — 링크를 띄워 직접 복사할 수 있게 한다.
  // Token 은 화면에만 노출하고 Console·Analytics 로는 내보내지 않는다.
  ui.toast(`복사가 막혔어요. 링크: ${inviteUrl}`, { type: 'warning', duration: 8000 })
}

/**
 * 발급에 성공해도 발급 가능 여부를 로컬에서 내리지 않는다 — 복사가 막혔거나 링크를
 * 잘못 보낸 경우 다시 받을 수 있어야 한다. 권위는 서버이고, 재조회 시 갱신된다.
 *
 * 활성 초대가 이미 있으면 서버가 같은 링크를 그대로 돌려준다(교체가 아니다).
 */
async function onCopyInvite() {
  copying.value = true
  try {
    await copyIssuedInvite(await createInvite(workCase.value.workCaseId), false)
    await load()
  } catch (err) {
    ui.toast(inviteErrorMessage(err), { type: 'danger' })
  } finally {
    copying.value = false
  }
}

/** 현재 링크를 철회하고 새 Token 으로 교체한다. 이전 링크는 즉시 무효가 된다. */
async function onReissueInvite() {
  reissueOpen.value = false
  copying.value = true
  try {
    await copyIssuedInvite(await reissueInvite(workCase.value.workCaseId), true)
    await load()
  } catch (err) {
    ui.toast(inviteErrorMessage(err), { type: 'danger' })
  } finally {
    copying.value = false
  }
}
</script>

<template>
  <div class="sub-page">
    <AppBackHeader title="근무 상세" />
    <main class="screen-body">
      <p v-if="loading" class="loading">불러오는 중…</p>

      <EmptyState v-else-if="!workCase" message="근무 정보를 찾을 수 없습니다." />

      <template v-else>
        <!-- ---- 보기 모드 ---- -->
        <template v-if="!editing">
          <header class="head">
            <h2 class="title">{{ workCase.title }}</h2>
            <StatusChip :status="workCase.status" kind="workCase" />
          </header>
          <p class="place">{{ workCase.workplaceName }}</p>

          <dl class="detail">
            <div class="detail-row">
              <dt>근무 날짜</dt>
              <dd>{{ formatDate(workCase.workDate) }}</dd>
            </div>
            <div class="detail-row">
              <dt>근무 시간</dt>
              <dd>{{ formatSeoulTimeRange(workCase.startsAt, workCase.endsAt) }}</dd>
            </div>
            <div class="detail-row">
              <dt>휴게시간</dt>
              <dd>
                {{ formatDuration(workCase.breakMinutes) }}
                <span class="sub">({{ workCase.breakPaid ? '유급' : '무급' }})</span>
              </dd>
            </div>
            <div class="detail-row">
              <dt>일급</dt>
              <dd class="wage">{{ formatKRW(workCase.dailyWage) }}</dd>
            </div>
            <!--
              termsVersion 은 화면에 내보내지 않는다. 조건 변경을 감지해 이전 링크를 철회하고
              계약서가 어떤 조건을 박제했는지 추적하는 내부 장치이며(WORK-005), 사용자에게
              'v3' 는 아무 의미가 없다. 서버 응답과 컬럼은 그대로 둔다.
            -->
          </dl>

          <!--
            계약·예치·근태 근거 — "이 근무가 왜 수정·삭제되지 않는지"를 설명한다.
            각 객체는 근거 행이 없으면 통째로 null 이라 v-if 로 행 단위로 감춘다.
          -->
          <section v-if="hasProgressInfo" class="progress">
            <h3 class="section-title">진행 현황</h3>
            <dl class="detail">
              <div v-if="workCase.latestInvitation" class="detail-row">
                <dt>연결 링크</dt>
                <dd>
                  {{ invitationText }}
                  <span v-if="workCase.latestInvitation.expiresAt" class="sub">
                    · {{ formatSeoulDateTime(workCase.latestInvitation.expiresAt) }}까지
                  </span>
                </dd>
              </div>
              <div v-if="workCase.contract" class="detail-row">
                <dt>계약 확정</dt>
                <dd>
                  {{ formatSeoulDateTime(workCase.contract.acceptedAt) }}
                  <a
                    v-if="contractViewUrl"
                    :href="contractViewUrl"
                    target="_blank"
                    rel="noopener"
                    class="contract-link"
                  >
                    <FileText :size="15" />
                    최종본 보기
                  </a>
                </dd>
              </div>
              <div v-if="workCase.escrow" class="detail-row">
                <dt>임금 예치</dt>
                <dd>
                  {{ formatKRW(workCase.escrow.amount) }}
                  <span class="sub">({{ escrowStatusLabel(workCase.escrow.status) }})</span>
                </dd>
              </div>
              <div v-if="workCase.attendance?.checkedInAt" class="detail-row">
                <dt>출근</dt>
                <dd>{{ formatSeoulDateTime(workCase.attendance.checkedInAt) }}</dd>
              </div>
              <div v-if="workCase.attendance?.checkedOutAt" class="detail-row">
                <dt>퇴근</dt>
                <dd>{{ formatSeoulDateTime(workCase.attendance.checkedOutAt) }}</dd>
              </div>
              <!-- settlement은 근거 행(정산 예약)이 있을 때만 온다 — 계약 확정 전에는 null. -->
              <div v-if="workCase.settlement" class="detail-row">
                <dt>정산 상태</dt>
                <dd><StatusChip :status="workCase.settlement.status" kind="settle" /></dd>
              </div>
            </dl>
          </section>

          <!-- 뱃지 등급은 배지 API(M7) 범위라 여기서는 기본(미부여)만 보여준다. -->
          <section v-if="workCase.worker" class="worker">
            <h3 class="section-title">매칭된 알바생</h3>
            <div class="worker-card">
              <TrustBadge role="worker" :size="40" />
              <span class="worker-name">{{ workCase.worker.name }}</span>
            </div>
          </section>

          <!-- 수정·삭제는 DRAFT, 링크 발급은 그보다 좁은 조건(미매칭·시작 전)까지 본다 -->
          <section v-if="canModify" class="actions">
            <BaseButton
              v-if="canIssueInviteLink"
              variant="secondary"
              block
              :disabled="copying"
              @click="onCopyInvite"
            >
              <Link2 :size="16" />
              {{ copying ? '링크 만드는 중…' : '연결 링크 발급·복사' }}
            </BaseButton>
            <BaseButton
              v-if="canReissueInviteLink"
              variant="secondary"
              block
              :disabled="copying"
              @click="reissueOpen = true"
            >
              <RefreshCw :size="16" />
              새 링크로 교체
            </BaseButton>
            <BaseButton variant="owner" block @click="startEdit">
              <Pencil :size="16" />
              수정
            </BaseButton>
            <BaseButton variant="danger" block @click="deleteOpen = true">
              <Trash2 :size="16" />
              삭제
            </BaseButton>
          </section>
          <p v-else class="locked">알바생이 확정한 근무는 변경·취소할 수 없습니다.</p>
        </template>

        <!-- ---- 수정 모드 ---- -->
        <form v-else class="form" @submit.prevent="onSave">
          <AppField v-model="form.title" label="제목" required :error="errors.title" />
          <AppField
            v-model="form.workDate"
            type="date"
            label="근무 날짜"
            required
            :error="errors.workDate"
          />
          <div class="field-row">
            <AppField
              v-model="form.startTime"
              type="time"
              label="시작시간"
              required
              :error="errors.startTime"
            />
            <AppField
              v-model="form.endTime"
              type="time"
              label="종료시간"
              required
              :error="errors.endTime"
            />
          </div>
          <AppField
            v-model="form.breakMinutes"
            type="number"
            label="휴게시간(분)"
            placeholder="0"
            :error="errors.breakMinutes"
          />

          <div class="field">
            <span class="field-label">휴게시간 급여</span>
            <div class="toggle" role="group" aria-label="휴게시간 급여 여부">
              <button
                type="button"
                class="toggle-btn"
                :class="{ active: !form.breakPaid }"
                @click="form.breakPaid = false"
              >
                무급
              </button>
              <button
                type="button"
                class="toggle-btn"
                :class="{ active: form.breakPaid }"
                @click="form.breakPaid = true"
              >
                유급
              </button>
            </div>
          </div>

          <AppField
            v-model="form.dailyWage"
            type="number"
            label="일급"
            required
            :hint="form.dailyWage ? formatKRW(form.dailyWage) : ''"
            :error="errors.dailyWage"
          />

          <div class="actions">
            <BaseButton type="submit" variant="owner" size="lg" block :disabled="submitting">
              저장
            </BaseButton>
            <BaseButton variant="secondary" block :disabled="submitting" @click="editing = false">
              취소
            </BaseButton>
          </div>
        </form>
      </template>
    </main>

    <BaseModal :open="reissueOpen" title="새 링크로 교체할까요?" @close="reissueOpen = false">
      지금 링크는 즉시 사용할 수 없게 됩니다. 이미 보낸 링크로는 알바생이 수락할 수 없어요.
      <template #footer>
        <BaseButton variant="secondary" block @click="reissueOpen = false">취소</BaseButton>
        <BaseButton variant="owner" block :disabled="copying" @click="onReissueInvite">
          교체하기
        </BaseButton>
      </template>
    </BaseModal>

    <BaseModal :open="deleteOpen" title="근무를 삭제할까요?" @close="deleteOpen = false">
      삭제하면 되돌릴 수 없습니다. 매칭전 근무만 삭제할 수 있어요.
      <template #footer>
        <BaseButton variant="secondary" block @click="deleteOpen = false">취소</BaseButton>
        <BaseButton variant="danger" block :disabled="submitting" @click="onDelete">
          삭제
        </BaseButton>
      </template>
    </BaseModal>
  </div>
</template>

<style scoped>
.screen-body {
  padding: var(--space-lg);
}
.loading {
  padding: var(--space-xl) 0;
  text-align: center;
  color: var(--color-text-sub);
}

.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-sm);
}
.title {
  font-size: var(--text-xl);
  font-weight: var(--weight-bold);
  color: var(--color-text);
}
.place {
  margin-top: var(--space-xs);
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}

/* ---- 상세 정보 ---- */
.detail {
  margin-top: var(--space-lg);
  padding: var(--space-md) var(--space-lg);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}
.detail-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-sm);
  padding: var(--space-sm) 0;
}
.detail-row + .detail-row {
  border-top: 1px solid var(--color-border);
}
.detail-row dt {
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}
.detail-row dd {
  font-size: var(--text-md);
  color: var(--color-text);
}
.detail-row dd.wage {
  font-weight: var(--weight-bold);
  color: var(--color-owner);
}
.sub {
  color: var(--color-text-sub);
}
.contract-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-left: var(--space-xs);
  color: var(--color-owner);
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
}

/* ---- 진행 현황(초대·계약·예치·근태) ---- */
.progress {
  margin-top: var(--space-xl);
}
.progress .detail {
  margin-top: var(--space-sm);
}

/* ---- 매칭 알바생 ---- */
.section-title {
  font-size: var(--text-lg);
  font-weight: var(--weight-bold);
  color: var(--color-text);
}
.worker {
  margin-top: var(--space-xl);
}
.worker-card {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  margin-top: var(--space-sm);
  padding: var(--space-md) var(--space-lg);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}
.worker-name {
  font-size: var(--text-md);
  font-weight: var(--weight-medium);
}

/* ---- 액션 ---- */
.actions {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  margin-top: var(--space-xl);
}
.locked {
  margin-top: var(--space-xl);
  padding: var(--space-md) var(--space-lg);
  background: var(--color-bg);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  color: var(--color-text-sub);
  text-align: center;
}

/* ---- 수정 폼 ---- */
.form {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}

/* 휴게시간·일급은 화살표(스피너) 없이 숫자만 직접 입력한다 */
.form :deep(input[type='number']) {
  appearance: textfield;
  -moz-appearance: textfield;
}
.form :deep(input[type='number'])::-webkit-outer-spin-button,
.form :deep(input[type='number'])::-webkit-inner-spin-button {
  -webkit-appearance: none;
  appearance: none;
  margin: 0;
}

/* Bootstrap 에 .row 가 있어 이름을 피한다(음수 margin 이 새어 들어온다) */
.field-row {
  display: flex;
  gap: var(--space-sm);
}
.field-row > * {
  flex: 1;
  min-width: 0;
}
.field {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.field-label {
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  color: var(--color-text-sub);
}
.toggle {
  display: flex;
  gap: var(--space-sm);
}
.toggle-btn {
  flex: 1;
  padding: var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  font-weight: var(--weight-medium);
  color: var(--color-text-sub);
}
.toggle-btn.active {
  border-color: var(--color-owner);
  color: var(--color-owner);
}
</style>
