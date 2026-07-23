<script setup>
/**
 * [E] 사장 마이페이지  ·  /owner/mypage  ·  OWNER
 * 안심일터 뱃지 카드(등급 + 안심거래 N건 남음 + 정의 소자) + 회원정보/비밀번호 변경 진입
 * + 사업장 관리 진입 + 회원 탈퇴.
 * 연계 API: GET /users/me · GET /users/me/badge · DELETE /users/me
 *   →  @/services/users (getMe, getBadge, deleteMe)
 * 진입: /owner/mypage/{profile,password,workplaces}. 공통: TrustBadge(role='owner')
 */
import { Building2, ChevronRight, KeyRound, UserRound } from 'lucide-vue-next'
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

import AppBackHeader from '@/components/common/AppBackHeader.vue'
import AppField from '@/components/common/AppField.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import BaseModal from '@/components/common/BaseModal.vue'
import TrustBadge from '@/components/common/TrustBadge.vue'
import { deleteMe, getBadge, getMe } from '@/services/users'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import { isRequired } from '@/utils/validators'

const router = useRouter()
const authStore = useAuthStore()
const ui = useUiStore()

const me = ref(null)
const badge = ref(null)

// TODO: 최근 구인/신고/정산 통계 API 명세 확정 전 임시 0값. 연동 시 API 응답으로 교체한다.
const recentJobCount = ref(0)
const reportCount = ref(0)
const settlementRate = ref(0)

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
  { label: '회원정보 변경', to: '/owner/mypage/profile', icon: UserRound },
  { label: '비밀번호 변경', to: '/owner/mypage/password', icon: KeyRound },
  { label: '사업장 관리', to: '/owner/mypage/workplaces', icon: Building2 }
]

const withdrawOpen = ref(false)
const withdrawPassword = ref('')
const withdrawError = ref('')
const withdrawing = ref(false)

onMounted(async () => {
  const [meRes, badgeRes] = await Promise.all([getMe(), getBadge()])
  me.value = meRes
  badge.value = badgeRes
})

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
    withdrawError.value = err?.response?.data?.message || '탈퇴 처리 중 오류가 발생했어요.'
  } finally {
    withdrawing.value = false
  }
}
</script>

<template>
  <div class="sub-page">
    <AppBackHeader title="마이페이지" />

    <main class="screen-body">
      <section v-if="me && badge" class="profile-card">
        <div class="profile-top">
          <span class="avatar">
            <img
              v-if="me.profileImageUrl"
              :src="me.profileImageUrl"
              :alt="`${me.name} 프로필 사진`"
              class="avatar__img"
            />
            <UserRound v-else :size="24" />
          </span>

          <div class="profile-info">
            <p class="profile-name">{{ me.name }}</p>
            <p class="profile-sub">{{ me.loginId }} | {{ me.email }}</p>
          </div>

          <div class="badge-slot">
            <TrustBadge role="owner" :level="badge.level" :size="40" />
          </div>
        </div>

        <div class="stats-row">
          <span>최근 구인 {{ recentJobCount }}건</span>
          <span class="stats-row__divider">|</span>
          <span>신고 {{ reportCount }}건</span>
          <span class="stats-row__divider">|</span>
          <span>정상 정산 {{ settlementRate }}%</span>
        </div>

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
      </section>

      <nav class="menu-list">
        <RouterLink v-for="item in menuItems" :key="item.to" :to="item.to" class="menu-item">
          <component :is="item.icon" :size="20" class="menu-item__icon" />
          <span class="menu-item__label">{{ item.label }}</span>
          <ChevronRight :size="18" class="menu-item__chevron" />
        </RouterLink>
      </nav>

      <button type="button" class="withdraw-link" @click="openWithdraw">회원 탈퇴</button>
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
  color: var(--color-owner);
  background: var(--color-owner-weak);
  border-radius: var(--radius-pill);
}
.avatar__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
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

.stats-row {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-top: var(--space-lg);
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}
.stats-row__divider {
  color: var(--color-border);
}

.bar {
  height: 8px;
  margin-top: var(--space-sm);
  overflow: hidden;
  background: var(--color-bg);
  border-radius: var(--radius-pill);
}
.bar__fill {
  height: 100%;
  background: var(--color-owner);
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

.withdraw-link {
  display: block;
  width: 100%;
  margin-top: var(--space-xl);
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
