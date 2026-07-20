<script setup>
const features = [
  {
    title: '근로 문서함',
    description: '승인된 합성 계약서와 양측이 공유하는 최종 서명본을 관리합니다.'
  },
  {
    title: '출퇴근 인증',
    description: '짧은 수명의 QR과 브라우저 위치 권한으로 출퇴근을 기록합니다.'
  },
  {
    title: '임금·정산 원장',
    description: 'Mock 계좌 기반 선예치 상태와 근로자의 정산 이력을 확인합니다.'
  }
]
</script>

<template>
  <section class="card hero-card">
    <div class="card-body p-4 p-md-5">
      <p class="mb-2 text-primary fw-semibold">Vue 프론트엔드 준비 완료</p>
      <h1 class="display-6 fw-bold">근로생활을 한곳에서 관리하는 Gig Hub</h1>
      <p class="col-lg-9 mt-3 mb-0 text-secondary fs-5">
        이 화면은 기능 구현을 시작하기 위한 기본 템플릿입니다. 실제 계약·근태·정산 데이터는 Spring
        API와 연결한 뒤 표시합니다.
      </p>

      <div class="row g-3 mt-4">
        <div v-for="feature in features" :key="feature.title" class="col-md-4">
          <article class="feature-item p-4">
            <h2 class="h5 fw-bold">{{ feature.title }}</h2>
            <p class="mb-0 text-secondary">{{ feature.description }}</p>
          </article>
        </div>
      </div>
    </div>
  </section>
</template>
