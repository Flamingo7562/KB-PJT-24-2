<script setup>
/**
 * [C] 근무 상세  ·  /owner/attendance/shifts/:shiftId  ·  OWNER
 * 근무 상세 + 매칭 알바생 성실 뱃지. 수정·삭제·연결 링크 복사는 매칭전(OPEN)만.
 * 확정(날인) 후 수정·삭제 버튼 숨김 — 서버도 409.
 * 연계 API: GET /shifts/{id} · PATCH /shifts/{id} · DELETE /shifts/{id} · POST /shifts/{id}/invites
 *   →  @/services/shifts (getShift, updateShift, deleteShift, createInvite)
 * route.params.shiftId 사용. 공통: TrustBadge(알바생 뱃지) · StatusChip · BaseModal(삭제 확인)
 */
import { Link2, Pencil, Trash2 } from 'lucide-vue-next'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AppBackHeader from '@/components/common/AppBackHeader.vue'
import AppField from '@/components/common/AppField.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import BaseModal from '@/components/common/BaseModal.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import StatusChip from '@/components/common/StatusChip.vue'
import TrustBadge from '@/components/common/TrustBadge.vue'
import { createInvite, deleteShift, getShift, updateShift } from '@/services/shifts'
import { useUiStore } from '@/stores/ui'
import { copyText } from '@/utils/clipboard'
import { formatDate, formatDuration, formatKRW, formatTimeRange } from '@/utils/format'
import { isPositiveAmount, isRequired } from '@/utils/validators'

const route = useRoute()
const router = useRouter()
const ui = useUiStore()

const shift = ref(null)
const loading = ref(true)
const editing = ref(false)
const submitting = ref(false)
const deleteOpen = ref(false)
const copying = ref(false) // 연결 링크 생성 중(중복 클릭 방지)

/**
 * 매칭전(OPEN)에서만 수정·삭제·연결 링크 생성이 가능하다.
 * 알바생이 확정(날인)하면 에스크로 예치가 걸리므로 서버가 409 로 막는다 — 화면도 버튼을 숨긴다.
 */
const canModify = computed(() => shift.value?.status === 'OPEN')

const form = reactive({
  title: '',
  workDate: '',
  startTime: '',
  endTime: '',
  breakMinutes: '',
  breakPaid: false,
  dailyWage: ''
})
const errors = reactive({ title: '', workDate: '', startTime: '', endTime: '', dailyWage: '' })

async function load() {
  loading.value = true
  try {
    shift.value = await getShift(route.params.shiftId)
  } catch {
    ui.toast('근무 정보를 불러오지 못했어요.', { type: 'danger' })
  } finally {
    loading.value = false
  }
}

onMounted(load)

function startEdit() {
  const s = shift.value
  form.title = s.title ?? ''
  form.workDate = s.workDate ?? ''
  form.startTime = s.startTime ?? ''
  form.endTime = s.endTime ?? ''
  form.breakMinutes = s.breakMinutes ?? ''
  form.breakPaid = s.breakPaid ?? false
  form.dailyWage = s.dailyWage ?? ''
  Object.keys(errors).forEach((key) => (errors[key] = ''))
  editing.value = true
}

function validate() {
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

  submitting.value = true
  try {
    await updateShift(shift.value.shiftId, {
      title: form.title.trim(),
      workDate: form.workDate,
      startTime: form.startTime,
      endTime: form.endTime,
      breakMinutes: Number(form.breakMinutes || 0),
      breakPaid: form.breakPaid,
      dailyWage: Number(form.dailyWage)
    })
    ui.toast('근무 정보를 수정했어요.', { type: 'success' })
    editing.value = false
    await load()
  } catch {
    // 확정 이후 수정 요청은 서버가 409 로 거절한다.
    ui.toast('수정하지 못했어요. 이미 확정된 근무일 수 있어요.', { type: 'danger' })
  } finally {
    submitting.value = false
  }
}

async function onDelete() {
  submitting.value = true
  try {
    await deleteShift(shift.value.shiftId)
    ui.toast('근무를 삭제했어요.', { type: 'success' })
    // 모달을 닫지 않은 채로 화면을 떠난다.
    // 닫기 트랜지션 도중에 이 화면이 unmount 되면 Teleport 로 body 에 붙은
    // 오버레이가 남아(opacity:0) 이후 화면의 클릭을 통째로 가로챈다.
    await router.push('/owner/attendance')
  } catch {
    ui.toast('삭제하지 못했어요. 이미 확정된 근무일 수 있어요.', { type: 'danger' })
    deleteOpen.value = false
  } finally {
    submitting.value = false
  }
}

async function onCopyInvite() {
  copying.value = true
  try {
    const { inviteUrl } = await createInvite(shift.value.shiftId)
    if (await copyText(inviteUrl)) {
      ui.toast('연결 링크를 복사했어요.', { type: 'success' })
    } else {
      // 브라우저가 복사를 막은 경우 — 링크를 띄워 직접 복사할 수 있게 한다.
      ui.toast(`복사가 막혔어요. 링크: ${inviteUrl}`, { type: 'warning', duration: 6000 })
    }
  } catch {
    ui.toast('링크를 만들지 못했어요.', { type: 'danger' })
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

      <EmptyState v-else-if="!shift" message="근무 정보를 찾을 수 없습니다." />

      <template v-else>
        <!-- ---- 보기 모드 ---- -->
        <template v-if="!editing">
          <header class="head">
            <h2 class="title">{{ shift.title }}</h2>
            <StatusChip :status="shift.status" kind="shift" />
          </header>
          <p class="place">{{ shift.workplaceName }}</p>

          <dl class="detail">
            <div class="detail-row">
              <dt>근무 날짜</dt>
              <dd>{{ formatDate(shift.workDate) }}</dd>
            </div>
            <div class="detail-row">
              <dt>근무 시간</dt>
              <dd>{{ formatTimeRange(shift.startTime, shift.endTime) }}</dd>
            </div>
            <div class="detail-row">
              <dt>휴게시간</dt>
              <dd>
                {{ formatDuration(shift.breakMinutes) }}
                <span class="sub">({{ shift.breakPaid ? '유급' : '무급' }})</span>
              </dd>
            </div>
            <div class="detail-row">
              <dt>일급</dt>
              <dd class="wage">{{ formatKRW(shift.dailyWage) }}</dd>
            </div>
            <div class="detail-row">
              <dt>정산 상태</dt>
              <dd><StatusChip :status="shift.settleStatus" kind="settle" /></dd>
            </div>
          </dl>

          <section v-if="shift.worker" class="worker">
            <h3 class="section-title">매칭된 알바생</h3>
            <div class="worker-card">
              <TrustBadge role="worker" :level="shift.worker.badgeLevel" :size="40" />
              <span class="worker-name">{{ shift.worker.name }}</span>
            </div>
          </section>

          <!-- 매칭전(OPEN)에서만 노출 — 확정 후에는 서버도 409 로 막는다 -->
          <section v-if="canModify" class="actions">
            <BaseButton variant="secondary" block :disabled="copying" @click="onCopyInvite">
              <Link2 :size="16" />
              {{ copying ? '링크 만드는 중…' : '연결 링크 복사' }}
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
