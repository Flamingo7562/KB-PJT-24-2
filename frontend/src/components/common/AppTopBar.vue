<script setup>
/**
 * 공통 상단바 — 로고 + (사장) 지점 select + 알림 + 마이페이지.
 *
 * 탭 화면 레이아웃(OwnerTabLayout·WorkerTabLayout)이 렌더한다. view 는 본문만 작성한다.
 * - 지점 select: OWNER 전용. workplace 스토어를 원본으로 근태·문서·QR 이 참조한다.
 * - 알림 종: 안읽음 배지 표시 + 클릭 시 notifications 스토어로 알림 모달 열기.
 */
import { Bell, CircleUser } from 'lucide-vue-next'
import { storeToRefs } from 'pinia'
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'

import LogoSymbol from '@/assets/images/logo/logo-symbol.svg'
import { useNotificationsStore } from '@/stores/notifications'
import { useWorkplaceStore } from '@/stores/workplace'

const props = defineProps({
  role: { type: String, required: true } // 'OWNER' | 'WORKER'
})

const router = useRouter()
const isOwner = computed(() => props.role === 'OWNER')

const workplace = useWorkplaceStore()
const { workplaces, selectedId } = storeToRefs(workplace)

const notifications = useNotificationsStore()
const { unreadCount } = storeToRefs(notifications)

// select v-model — 선택 시 스토어에 반영
const branch = computed({
  get: () => selectedId.value,
  set: (id) => workplace.select(Number(id))
})

onMounted(() => {
  if (isOwner.value) workplace.load()
  notifications.load()
})

function goMyPage() {
  router.push(isOwner.value ? '/owner/mypage' : '/worker/mypage')
}
</script>

<template>
  <header class="topbar">
    <span class="brand">
      <LogoSymbol
        class="brand-logo"
        :class="isOwner ? 'is-owner' : 'is-worker'"
        aria-label="Gig Hub"
      />
      <span class="brand-name">Gig Hub</span>
    </span>

    <div class="right">
      <select
        v-if="isOwner && workplaces.length"
        v-model="branch"
        class="branch"
        aria-label="지점 선택"
      >
        <option v-for="w in workplaces" :key="w.workplaceId" :value="w.workplaceId">
          {{ w.name }}
        </option>
      </select>

      <button type="button" class="icon-btn" aria-label="알림" @click="notifications.open()">
        <Bell :size="22" />
        <span v-if="unreadCount > 0" class="badge">{{ unreadCount > 9 ? '9+' : unreadCount }}</span>
      </button>

      <button type="button" class="icon-btn" aria-label="마이페이지" @click="goMyPage">
        <CircleUser :size="22" />
      </button>
    </div>
  </header>
</template>

<style scoped>
.topbar {
  position: sticky;
  top: 0;
  z-index: var(--z-tabbar);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-sm);
  padding: var(--space-md) var(--space-lg);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
}
.brand {
  display: inline-flex;
  align-items: center;
  gap: var(--space-sm);
  min-width: 0;
}
.brand-logo {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
}
.brand-logo.is-owner {
  color: var(--color-owner);
}
.brand-logo.is-worker {
  color: var(--color-worker);
}
.brand-name {
  font-size: var(--text-lg);
  font-weight: var(--weight-bold);
  color: var(--color-text);
}
.right {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}
.branch {
  max-width: 110px;
  padding: 4px var(--space-sm);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: var(--text-sm);
  color: var(--color-text);
  background: var(--color-surface);
}
.icon-btn {
  position: relative;
  color: var(--color-text);
}
.badge {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: var(--radius-pill);
  background: var(--color-danger);
  color: var(--color-on-primary);
  font-size: 10px;
  line-height: 16px;
  text-align: center;
}
</style>
