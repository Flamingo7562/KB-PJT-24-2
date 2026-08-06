<script setup>
/**
 * 사장/알바생 공용 회원가입 폼.
 * 필드: 아이디(중복확인)·비밀번호·비밀번호 확인·이름·이메일(중복확인)·전화번호(선택).
 *
 * 연계 API: GET /auth/check-login-id · GET /auth/check-email · POST /auth/signup
 *   → @/services/auth (checkLoginId, checkEmail, signup)
 * 성공 후: role 에 맞는 로그인 화면으로 이동.
 *
 * 실시간 검증(#238): useFieldValidation 이 형식 오류(errors)를 담당한다. 아이디·이메일의
 * 중복확인 결과(성공/실패, 서버가 준 fieldErrors 포함)는 서버만 아는 답이라 별도 슬롯
 * (checkErrors)에 둔다 — 같은 슬롯을 쓰면 값을 고쳐 형식이 재검증될 때 "이미 사용
 * 중입니다" 같은 서버 응답을 지워버리기 때문이다. 템플릿은 두 슬롯을
 * `errors.x || checkErrors.x` 로 합치고, 형식 오류가 항상 먼저 보인다.
 */
import { Check } from 'lucide-vue-next'
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import AppField from '@/components/common/AppField.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import { useFieldValidation } from '@/composables/useFieldValidation'
import { checkEmail, checkLoginId, signup } from '@/services/auth'
import { fieldErrorMap } from '@/services/http'
import { useUiStore } from '@/stores/ui'
import { blockNonDigitKeydown, formatPhoneInput } from '@/utils/format'
import {
  isEmail,
  isPhone,
  loginIdRule,
  NAME_MAX_LENGTH,
  nameRule,
  PASSWORD_MAX_LENGTH,
  PASSWORD_MIN_LENGTH,
  passwordRule,
  passwordsMatch
} from '@/utils/validators'

const props = defineProps({
  role: { type: String, required: true } // 'OWNER' | 'WORKER'
})

const router = useRouter()
const ui = useUiStore()

const form = reactive({
  loginId: '',
  password: '',
  passwordConfirm: '',
  name: '',
  email: '',
  phone: ''
})

// 형식 오류 전용. 규칙은 기존 validate() 가 쓰던 것을 그대로 옮겼다 — 규칙 자체는 불변.
const { errors, handleBlur, validateAll } = useFieldValidation(() => form, {
  loginId: (v) => loginIdRule(v.loginId),
  password: (v) => passwordRule(v.password),
  passwordConfirm: (v) => passwordsMatch(v.password, v.passwordConfirm),
  name: (v) => nameRule(v.name),
  email: (v) => isEmail(v.email),
  phone: (v) => isPhone(v.phone)
})

// 중복확인(서버) 전용 슬롯 — errors 와 분리해 형식 재검증이 서버 응답을 덮어쓰지 않게 한다.
// 아이디·이메일만 서버에 물어볼 수 있으므로 이 두 키만 가진다.
const checkErrors = reactive({ loginId: '', email: '' })
const AVAILABILITY_FIELDS = new Set(['loginId', 'email'])

const loginIdCheck = reactive({ done: false, available: false })
const emailCheck = reactive({ done: false, available: false })
const submitting = ref(false)

// 중복확인을 통과한 상태 — 버튼 자리를 '확인완료' 표시로, 필드 아래를 성공 문구로 바꾼다.
// 값이 바뀌면 아래 watch 가 done 을 내려 자동으로 되돌아온다.
const loginIdVerified = computed(() => loginIdCheck.done && loginIdCheck.available)
const emailVerified = computed(() => emailCheck.done && emailCheck.available)

// 값이 바뀌면 이전 중복확인 결과(상태 + 표시 메시지)를 모두 무효화한다.
watch(
  () => form.loginId,
  () => {
    loginIdCheck.done = false
    loginIdCheck.available = false
    checkErrors.loginId = ''
  }
)
watch(
  () => form.email,
  () => {
    emailCheck.done = false
    emailCheck.available = false
    checkErrors.email = ''
  }
)

// 입력 중 하이픈을 자동으로 채운다(010-1234-5678). 숫자 외 입력은 AppField 의 digits-only 가 막는다.
// v-model 은 입력값을 그대로 대입해 가공할 틈이 없다. 그래서 템플릿에서 축약을 풀어
// :model-value + @update:model-value 로 쓰고, 대입 자리에 이 함수를 끼워 포맷을 거치게 한다.
function onPhoneInput(v) {
  form.phone = formatPhoneInput(v)
}

async function onCheckLoginId() {
  // 포커스가 버튼으로 넘어가며 native blur 로 handleBlur 가 이미 한 번 불렸을 수 있다.
  // handleBlur 는 같은 값이면 같은 결과를 내는 멱등 함수라 두 번 불러도 안전하다.
  handleBlur('loginId')
  if (errors.loginId) return

  // 응답이 오는 동안 사용자가 값을 바꿀 수 있다. 그 사이 값이 달라지면 이 응답은 지금
  // 화면에 있는 값에 대한 답이 아니므로(watch 가 이미 상태를 무효화했다) 버린다.
  const checkedValue = form.loginId
  try {
    const { available } = await checkLoginId(checkedValue)
    if (form.loginId !== checkedValue) return
    loginIdCheck.done = true
    loginIdCheck.available = available
    checkErrors.loginId = available ? '' : '이미 사용 중인 아이디입니다.'
  } catch (err) {
    if (form.loginId !== checkedValue) return
    const fieldMessage = fieldErrorMap(err).loginId
    if (fieldMessage) {
      checkErrors.loginId = fieldMessage
    } else {
      ui.toast(err?.response?.data?.message || '아이디 중복확인에 실패했어요.', { type: 'danger' })
    }
  }
}

async function onCheckEmail() {
  // 포커스가 버튼으로 넘어가며 native blur 로 handleBlur 가 이미 한 번 불렸을 수 있다.
  // handleBlur 는 같은 값이면 같은 결과를 내는 멱등 함수라 두 번 불러도 안전하다.
  handleBlur('email')
  if (errors.email) return

  // 응답이 오는 동안 사용자가 값을 바꿀 수 있다. 그 사이 값이 달라지면 이 응답은 지금
  // 화면에 있는 값에 대한 답이 아니므로(watch 가 이미 상태를 무효화했다) 버린다.
  const checkedValue = form.email
  try {
    const { available } = await checkEmail(checkedValue)
    if (form.email !== checkedValue) return
    emailCheck.done = true
    emailCheck.available = available
    checkErrors.email = available ? '' : '이미 사용 중인 이메일입니다.'
  } catch (err) {
    if (form.email !== checkedValue) return
    const fieldMessage = fieldErrorMap(err).email
    if (fieldMessage) {
      checkErrors.email = fieldMessage
    } else {
      ui.toast(err?.response?.data?.message || '이메일 중복확인에 실패했어요.', { type: 'danger' })
    }
  }
}

/**
 * 중복확인 완료 여부를 제출 시점에 확인한다. loginIdCheck/emailCheck 는
 * useFieldValidation 의 규칙표가 모르는, 이 컴포넌트만 가진 상태라 규칙에 넣지 않고
 * 여기서 따로 얹는다. 형식이 이미 무효여도 이 함수는 그대로 checkErrors 를 채우지만,
 * 화면은 항상 `errors.x || checkErrors.x` 로 errors 를 우선 보여주므로 형식 오류가
 * 있는 동안은 이 메시지가 가려진다.
 */
function checkAvailabilityBeforeSubmit() {
  let ok = true

  if (!loginIdCheck.done) {
    checkErrors.loginId = '아이디 중복확인을 해주세요.'
    ok = false
  } else if (!loginIdCheck.available) {
    checkErrors.loginId = '이미 사용 중인 아이디입니다.'
    ok = false
  } else {
    checkErrors.loginId = ''
  }

  if (!emailCheck.done) {
    checkErrors.email = '이메일 중복확인을 해주세요.'
    ok = false
  } else if (!emailCheck.available) {
    checkErrors.email = '이미 사용 중인 이메일입니다.'
    ok = false
  } else {
    checkErrors.email = ''
  }

  return ok
}

async function onSubmit() {
  const formatOk = validateAll()
  const availabilityOk = checkAvailabilityBeforeSubmit()
  if (!formatOk || !availabilityOk) {
    ui.toast('입력 내용을 확인해주세요.', { type: 'warning' })
    return
  }

  submitting.value = true
  try {
    // 화면은 하이픈이 들어간 표시 형식을 유지하고, 전송 값 정규화는 서비스 계층이 담당한다.
    await signup({
      loginId: form.loginId,
      password: form.password,
      passwordConfirm: form.passwordConfirm,
      name: form.name,
      email: form.email,
      phone: form.phone,
      role: props.role
    })
    ui.toast('회원가입이 완료되었습니다. 로그인해주세요.', { type: 'success' })
    router.push(props.role === 'OWNER' ? '/owner/login' : '/worker/login')
  } catch (err) {
    // 확인·제출 사이 경합으로 아이디·이메일이 막 선점되면(409) 서버가 fieldErrors 를 준다 —
    // 해당 입력 아래에 사유를 붙이고, 필드로 못 옮기는 오류만 토스트로 보여준다.
    // 아이디·이메일의 서버 거부는 checkErrors 로(형식 재검증이 지우지 못하게), 나머지
    // 필드는 그대로 errors 로 보낸다.
    const fieldErrors = fieldErrorMap(err)
    const matchedField = Object.keys(form).some((field) => {
      const reason = fieldErrors[field]
      if (!reason) return false
      if (AVAILABILITY_FIELDS.has(field)) {
        checkErrors[field] = reason
      } else {
        errors[field] = reason
      }
      return true
    })
    if (!matchedField) {
      ui.toast(err?.response?.data?.message || '회원가입에 실패했어요.', { type: 'danger' })
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <form class="signup-form" @submit.prevent="onSubmit">
    <AppField
      v-model="form.loginId"
      label="아이디"
      placeholder="4~20자 영문·숫자"
      required
      :error="errors.loginId || checkErrors.loginId"
      :success="loginIdVerified ? '사용 가능한 아이디입니다.' : ''"
      @blur="handleBlur('loginId')"
    >
      <template #suffix>
        <span v-if="loginIdVerified" class="verified"><Check :size="16" /> 확인완료</span>
        <BaseButton v-else type="button" variant="secondary" @click="onCheckLoginId">
          중복확인
        </BaseButton>
      </template>
    </AppField>

    <AppField
      v-model="form.password"
      type="password"
      label="비밀번호"
      :placeholder="`${PASSWORD_MIN_LENGTH}~${PASSWORD_MAX_LENGTH}자`"
      :maxlength="PASSWORD_MAX_LENGTH"
      required
      :error="errors.password"
      @blur="handleBlur('password')"
    />

    <AppField
      v-model="form.passwordConfirm"
      type="password"
      label="비밀번호 확인"
      placeholder="비밀번호를 한 번 더 입력"
      required
      :error="errors.passwordConfirm"
      @blur="handleBlur('passwordConfirm')"
    />

    <AppField
      v-model="form.name"
      label="이름"
      placeholder="이름"
      :maxlength="NAME_MAX_LENGTH"
      required
      :error="errors.name"
      @blur="handleBlur('name')"
    />

    <AppField
      v-model="form.email"
      type="email"
      label="이메일"
      placeholder="example@gighub.com"
      required
      :error="errors.email || checkErrors.email"
      :success="emailVerified ? '사용 가능한 이메일입니다.' : ''"
      @blur="handleBlur('email')"
    >
      <template #suffix>
        <span v-if="emailVerified" class="verified"><Check :size="16" /> 확인완료</span>
        <BaseButton v-else type="button" variant="secondary" @click="onCheckEmail">
          중복확인
        </BaseButton>
      </template>
    </AppField>

    <AppField
      :model-value="form.phone"
      type="tel"
      label="전화번호"
      placeholder="010-0000-0000 (선택)"
      digits-only
      maxlength="13"
      :error="errors.phone"
      @keydown="blockNonDigitKeydown"
      @update:model-value="onPhoneInput"
      @blur="handleBlur('phone')"
    />

    <BaseButton
      type="submit"
      :variant="role === 'OWNER' ? 'owner' : 'worker'"
      size="lg"
      block
      :disabled="submitting"
    >
      회원가입
    </BaseButton>
  </form>
</template>

<style scoped>
.signup-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}
/* 중복확인 완료 표시 — 버튼과 같은 높이를 유지해 입력란 우측 정렬이 흔들리지 않게 한다. */
.verified {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0 var(--space-md);
  border-radius: var(--radius-sm);
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  color: var(--color-success);
  background: var(--color-success-bg);
  white-space: nowrap;
}
</style>
