<script setup>
/**
 * [H] 근무 확정(초대 링크)  ·  /invite/:token  ·  WORKER(로그인 필수, 딥링크)
 * 자동 입력된 근무 내역 확인 → 전자서명 → 근무 확정(=에스크로 예치). 사장 안심 뱃지 표시.
 * 만료·사용 토큰 410. OWNER 접근은 가드 G4가 차단.
 * 연계 API: GET /invites/{token} · POST /invites/{token}/confirm
 *   →  @/services/invites (getInvite, confirmInvite)
 * route.params.token 사용. 공통: TrustBadge(role='owner') · BaseButton
 * 고정 경고 문구(변경 금지):
 *   '근로계약서 날인 완료 시점부터는 근무 변경 및 취소가 불가합니다. 신중하게 날인해주세요.'
 * TODO(담당 H): 전자서명(canvas) 입력 → base64.
 */
import AppBackHeader from '@/components/common/AppBackHeader.vue'
import EmptyState from '@/components/common/EmptyState.vue'
</script>

<template>
  <div class="sub-page">
    <AppBackHeader title="근무 확정" to="/worker/home" />
    <main class="screen-body">
      <EmptyState message="근무 확정(전자서명) 화면 (TODO: 담당 H 구현)" />
    </main>
  </div>
</template>

<style scoped>
.screen-body {
  padding: var(--space-lg);
}
</style>
