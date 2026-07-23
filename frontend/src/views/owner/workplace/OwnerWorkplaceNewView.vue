<script setup>
/**
 * [D] 사업장 등록  ·  /owner/workplaces/new  ·  OWNER
 * 폼: 사업자등록번호·상호명·대표자명·사업장 주소·사업장 전화번호.
 * 주소는 다음 우편번호 검색(모달 내 embed, @/utils/daumPostcode) 또는 직접 입력. 좌표 자동
 * 변환은 별도 지오코딩 API 필요(미구현). 인증 반경은 기본값(100).
 * 진입 경로 2가지: ① 첫 로그인(G7, 사업장 0개) 강제 진입 ② /owner/mypage/workplaces 에서 수동 추가.
 * 연계 API: POST /workplaces  →  @/services/workplaces (createWorkplace)
 * 등록 성공 후: useWorkplaceStore().load({force:true}) 갱신 →
 *   G7 강제 진입이었으면 needsWorkplaceSetup 을 false 로 갱신 후 /owner/home,
 *   수동 추가였으면 /owner/mypage/workplaces 로 복귀.
 *   (⚠ needsWorkplaceSetup 갱신을 빼먹으면 G7 가드가 /owner/home 이동을 즉시 되돌려 무한 루프)
 * 공통: AppField · BaseButton · @/utils/validators (isBusinessNumber, isPhone)
 */
import { nextTick, ref } from 'vue'
import { useRouter } from 'vue-router'

import AppBackHeader from '@/components/common/AppBackHeader.vue'
import AppField from '@/components/common/AppField.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import BaseModal from '@/components/common/BaseModal.vue'
import { createWorkplace } from '@/services/workplaces'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import { useWorkplaceStore } from '@/stores/workplace'
import { embedAddressSearch } from '@/utils/daumPostcode'
import { blockNonDigitKeydown, formatBusinessNumberInput, formatPhoneInput } from '@/utils/format'
import { isBusinessNumber, isPhone, isRequired } from '@/utils/validators'

const router = useRouter()
const authStore = useAuthStore()
const workplaceStore = useWorkplaceStore()
const ui = useUiStore()

// 진입 시점의 needsWorkplaceSetup 값으로 "G7 강제 진입"이었는지 기억해둔다.
// (등록 성공 후 이 값을 false 로 바꾸므로, 바뀌기 전에 미리 캡처해야 한다)
const cameFromForcedSetup = authStore.needsWorkplaceSetup

const businessNumber = ref('')
const name = ref('')
const representativeName = ref('')
const address = ref('')
const phone = ref('')

const businessNumberError = ref('')
const nameError = ref('')
const representativeNameError = ref('')
const addressError = ref('')
const phoneError = ref('')
const submitting = ref(false)

function onBusinessNumberInput(v) {
  businessNumber.value = formatBusinessNumberInput(v)
}

function onPhoneInput(v) {
  phone.value = formatPhoneInput(v)
}

const addressSearchOpen = ref(false)
const addressSearchContainer = ref(null)

async function searchAddress() {
  addressSearchOpen.value = true
  await nextTick()
  embedAddressSearch(
    addressSearchContainer.value,
    (result) => {
      address.value = result.address
      addressError.value = ''
      addressSearchOpen.value = false
    },
    () => {
      addressSearchOpen.value = false
      ui.toast('주소 검색을 불러오지 못했어요. 직접 입력해주세요.', { type: 'danger' })
    }
  )
}

function validate() {
  const checks = [
    [isBusinessNumber(businessNumber.value), businessNumberError],
    [isRequired(name.value, '상호명'), nameError],
    [isRequired(representativeName.value, '대표자명'), representativeNameError],
    [isRequired(address.value, '사업장 주소'), addressError],
    [isPhone(phone.value, { required: true }), phoneError]
  ]
  checks.forEach(([check, errorRef]) => {
    errorRef.value = check.valid ? '' : check.message
  })
  return checks.every(([check]) => check.valid)
}

async function handleSubmit() {
  if (!validate()) return

  submitting.value = true
  try {
    // 주소 → 좌표 자동 변환은 지오코딩 연동 전이라 미구현. 인증 반경은 기본값(100m)을 쓴다.
    await createWorkplace({
      businessNumber: businessNumber.value,
      name: name.value,
      representativeName: representativeName.value,
      address: address.value,
      phone: phone.value,
      radiusM: 100
    })
    await workplaceStore.load({ force: true })
    ui.toast('사업장을 등록했어요.', { type: 'success' })

    if (cameFromForcedSetup) {
      authStore.setUser({ ...authStore.user, needsWorkplaceSetup: false })
      router.push('/owner/home')
    } else {
      router.push('/owner/mypage/workplaces')
    }
  } catch (err) {
    ui.toast(err?.response?.data?.message || '사업장 등록에 실패했어요.', { type: 'danger' })
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="sub-page">
    <AppBackHeader title="사업장 등록" to="/owner/home" />
    <main class="screen-body">
      <form class="new-form" @submit.prevent="handleSubmit">
        <AppField
          :model-value="businessNumber"
          label="사업자등록번호 (숫자 10자리)"
          placeholder="000-00-00000"
          required
          maxlength="12"
          :error="businessNumberError"
          @keydown="blockNonDigitKeydown"
          @update:model-value="onBusinessNumberInput"
        />
        <AppField
          v-model="name"
          label="상호명"
          placeholder="상호명을 입력하세요"
          required
          :error="nameError"
        />
        <AppField
          v-model="representativeName"
          label="대표자명"
          placeholder="대표자명을 입력하세요"
          required
          :error="representativeNameError"
        />
        <AppField
          v-model="address"
          label="사업장 주소"
          placeholder="주소 검색을 이용하거나 직접 입력하세요"
          required
          :error="addressError"
        >
          <template #suffix>
            <BaseButton type="button" variant="secondary" @click="searchAddress">검색</BaseButton>
          </template>
        </AppField>
        <AppField
          :model-value="phone"
          label="사업장 전화번호 (지역번호 포함 9~11자리)"
          type="tel"
          placeholder="02-0000-0000"
          required
          maxlength="13"
          :error="phoneError"
          @keydown="blockNonDigitKeydown"
          @update:model-value="onPhoneInput"
        />

        <BaseButton type="submit" variant="owner" block :disabled="submitting">등록</BaseButton>
      </form>
    </main>

    <BaseModal :open="addressSearchOpen" title="주소 검색" @close="addressSearchOpen = false">
      <div ref="addressSearchContainer" class="postcode-embed"></div>
    </BaseModal>
  </div>
</template>

<style scoped>
.screen-body {
  padding: var(--space-lg);
}
.new-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}
.postcode-embed {
  height: 400px;
}
</style>
