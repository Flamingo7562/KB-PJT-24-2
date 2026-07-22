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
import { onMounted, ref } from 'vue'
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
      <section v-if="me" class="profile-summary">
        <p class="profile-name">{{ me.name }}</p>
        <p class="profile-sub">{{ me.email }}</p>
      </section>

      <section v-if="badge" class="badge-card">
        <TrustBadge
          role="owner"
          :level="badge.level"
          :size="64"
          show-remaining
          :remaining="badge.remainingToNextLevel"
          :criterion="badge.criterionLabel"
        />
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

.profile-summary {
  padding: var(--space-sm) 0 var(--space-lg);
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

.badge-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-xl) var(--space-lg);
  background: var(--color-owner-weak);
  border-radius: var(--radius-md);
  text-align: center;
}
.badge-desc {
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
