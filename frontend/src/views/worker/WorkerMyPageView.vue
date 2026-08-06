<script setup>
/**
 * [H] 알바생 마이페이지  ·  /worker/mypage  ·  WORKER
 * 성실근로자 뱃지 카드(등급 + 성실근로 N건 남음 + 정의 소자) + 회원정보/비밀번호 변경 진입
 * + 로그아웃 + 회원 탈퇴.
 * 연계 API: GET /users/me · GET /users/me/badge · DELETE /users/me
 *   →  @/services/users (getMe, getBadge, deleteMe)
 * 진입: /worker/mypage/{profile,password}. 공통: TrustBadge(role='worker').
 */
import { ChevronRight, KeyRound, UserRound } from 'lucide-vue-next'
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

import AppBackHeader from '@/components/common/AppBackHeader.vue'
import AppField from '@/components/common/AppField.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import BaseModal from '@/components/common/BaseModal.vue'
import TrustBadge from '@/components/common/TrustBadge.vue'
import { fieldErrorMap } from '@/services/http'
import { deleteMe, getBadge, getMe } from '@/services/users'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import { isRequired } from '@/utils/validators'

const router = useRouter()
const authStore = useAuthStore()
const ui = useUiStore()

const me = ref(null)
const badge = ref(null)

// 진행률: 등급 판정 기준(최근 15건) 대비 최근 실적 비율.
const progressPercent = computed(() => {
  const recent = badge.value?.recentCount ?? 0
  return Math.min(100, Math.round((recent / 15) * 100))
})

const nextLevelLabel = computed(() => {
  const level = badge.value?.level ?? 0
  return level >= 3 ? '최고 등급' : `Lv.${level + 1}`
})

const menuItems = [
  { label: '회원정보 변경', to: '/worker/mypage/profile', icon: UserRound },
  { label: '비밀번호 변경', to: '/worker/mypage/password', icon: KeyRound }
]

const withdrawOpen = ref(false)
const withdrawPassword = ref('')
const withdrawError = ref('')
const withdrawing = ref(false)
const loggingOut = ref(false)

// getMe·getBadge 는 서로 독립된 요청이다. 뱃지 Endpoint(#182) 하나가 404 를 내도
// 프로필 카드 자체는 보여줘야 하므로 Promise.all 로 묶어 함께 실패시키지 않는다.
onMounted(async () => {
  try {
    me.value = await getMe()
  } catch {
    // me 가 비어 있으면 프로필 카드 전체가 v-if 로 자연히 숨는다.
  }
  try {
    badge.value = await getBadge()
  } catch {
    // 뱃지 없이도 나머지 프로필 정보는 그대로 보여준다.
  }
})

/**
 * 로그아웃. authStore.logout() 은 서버 호출 결과와 무관하게 로컬 상태를 비우므로,
 * 이 호출이 끝난 시점에 앱은 이미 로그아웃 상태다. 실패해도 화면에 남으면 상태와
 * 어긋나고 다음 이동에서 G1 가드가 어차피 튕겨낸다 — 그래서 이동은 finally 에서 한다.
 * 다만 서버 세션이 살아있을 가능성은 숨기지 않고 오류로 알린다.
 */
async function handleLogout() {
  loggingOut.value = true
  try {
    await authStore.logout()
  } catch (err) {
    ui.toast(err?.response?.data?.message || '로그아웃 요청이 서버에 전달되지 않았어요.', {
      type: 'danger'
    })
  } finally {
    loggingOut.value = false
    router.push('/')
  }
}

function openWithdraw() {
  withdrawPassword.value = ''
  withdrawError.value = ''
  withdrawOpen.value = true
}

async function confirmWithdraw() {
  const check = isRequired(withdrawPassword.value, '비밀번호')
  if (!check.valid) {
    withdrawError.value = check.message
    return
  }

  withdrawing.value = true
  withdrawError.value = ''
  try {
    await deleteMe({ password: withdrawPassword.value })
    withdrawOpen.value = false
    ui.toast('회원 탈퇴가 완료됐어요.', { type: 'success' })
    await authStore.logout()
    router.push('/')
  } catch (err) {
    // 서버가 실제로 지목한 필드에만 사유를 붙인다 — 잔액·진행 근무 등 무관한 사유를
    // 비밀번호 필드 아래 지어내 보여주지 않는다.
    const errors = fieldErrorMap(err)
    if (errors.password) {
      withdrawError.value = errors.password
    } else if (err?.response?.data?.message) {
      ui.toast(err.response.data.message, { type: 'danger' })
    } else {
      ui.toast('탈퇴 처리 중 오류가 발생했어요.', { type: 'danger' })
    }
  } finally {
    withdrawing.value = false
  }
}
</script>

<template>
  <div class="sub-page">
    <AppBackHeader title="마이페이지" />

    <main class="screen-body">
      <!-- badge 는 별도 Endpoint(#182) 라 me 조회는 성공했는데 badge 만 실패할 수 있다.
           그 경우에도 프로필 카드 자체는 보여줘야 하므로 badge 관련 조각만 따로 게이팅한다. -->
      <section v-if="me" class="profile-card">
        <div class="profile-top">
          <!-- 승인 프로필 응답에 사진 필드가 없어 기본 아이콘만 노출한다. -->
          <span class="avatar">
            <UserRound :size="24" />
          </span>

          <div class="profile-info">
            <p class="profile-name">{{ me.name }}</p>
            <p class="profile-sub">{{ me.loginId }} | {{ me.email }}</p>
          </div>

          <div v-if="badge" class="badge-slot">
            <TrustBadge role="worker" :level="badge.level" :size="40" />
          </div>
        </div>

        <template v-if="badge">
          <div
            class="bar"
            role="progressbar"
            :aria-valuenow="progressPercent"
            aria-valuemin="0"
            aria-valuemax="100"
          >
            <div class="bar__fill" :style="{ width: progressPercent + '%' }"></div>
          </div>

          <p class="level-remaining">
            다음 레벨 {{ nextLevelLabel }}까지 {{ badge.criterionLabel }}
            {{ badge.remainingToNextLevel }}건 남음 (최근 15건 기준)
          </p>

          <p class="badge-desc">{{ badge.criterionDesc }}</p>
        </template>
      </section>

      <nav class="menu-list">
        <RouterLink v-for="item in menuItems" :key="item.to" :to="item.to" class="menu-item">
          <component :is="item.icon" :size="20" class="menu-item__icon" />
          <span class="menu-item__label">{{ item.label }}</span>
          <ChevronRight :size="18" class="menu-item__chevron" />
        </RouterLink>
      </nav>

      <section class="account-actions">
        <BaseButton variant="secondary" block :disabled="loggingOut" @click="handleLogout">
          로그아웃
        </BaseButton>
        <button type="button" class="withdraw-link" @click="openWithdraw">회원 탈퇴</button>
      </section>
    </main>

    <BaseModal :open="withdrawOpen" title="회원 탈퇴" @close="withdrawOpen = false">
      <p class="withdraw-desc">
        탈퇴하면 되돌릴 수 없어요. 잔액·예치금이 있거나 진행 중인 근무가 있으면 탈퇴할 수 없어요.
      </p>
      <AppField
        v-model="withdrawPassword"
        label="비밀번호"
        type="password"
        placeholder="비밀번호를 입력하세요"
        :error="withdrawError"
      />
      <template #footer>
        <BaseButton variant="secondary" block :disabled="withdrawing" @click="withdrawOpen = false">
          취소
        </BaseButton>
        <BaseButton variant="danger" block :disabled="withdrawing" @click="confirmWithdraw">
          탈퇴하기
        </BaseButton>
      </template>
    </BaseModal>
  </div>
</template>

<style scoped>
.screen-body {
  padding: var(--space-lg);
}

.profile-card {
  margin: var(--space-sm) 0 var(--space-lg);
  padding: var(--space-lg);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.profile-top {
  display: flex;
  align-items: center;
  gap: var(--space-md);
}

.avatar {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  overflow: hidden;
  color: var(--color-worker);
  background: var(--color-worker-weak);
  border-radius: var(--radius-pill);
}
.profile-info {
  flex: 1;
  min-width: 0;
}
.profile-name {
  font-size: var(--text-xl);
  font-weight: var(--weight-bold);
  color: var(--color-text);
}
.profile-sub {
  margin-top: var(--space-xs);
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}

.badge-slot {
  flex-shrink: 0;
}

.bar {
  height: 8px;
  margin-top: var(--space-lg);
  overflow: hidden;
  background: var(--color-bg);
  border-radius: var(--radius-pill);
}
.bar__fill {
  height: 100%;
  background: var(--color-worker);
  border-radius: var(--radius-pill);
}

.level-remaining {
  margin-top: var(--space-sm);
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  color: var(--color-text);
}
.badge-desc {
  margin-top: var(--space-xs);
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}

.menu-list {
  display: flex;
  flex-direction: column;
  margin-top: var(--space-xl);
  border-top: 1px solid var(--color-border);
}
.menu-item {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-lg) var(--space-sm);
  border-bottom: 1px solid var(--color-border);
  color: var(--color-text);
}
.menu-item__icon {
  color: var(--color-text-sub);
}
.menu-item__label {
  flex: 1;
  font-size: var(--text-md);
  font-weight: var(--weight-medium);
}
.menu-item__chevron {
  color: var(--color-text-sub);
}

.account-actions {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-md);
  margin-top: var(--space-lg);
}

.withdraw-link {
  display: block;
  width: 100%;
  padding: var(--space-sm) 0;
  font-size: var(--text-sm);
  color: var(--color-text-sub);
  text-align: center;
  text-decoration: underline;
}

.withdraw-desc {
  margin-bottom: var(--space-lg);
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}
</style>
