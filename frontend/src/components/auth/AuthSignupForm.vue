<script setup>
/**
 * 사장/알바생 공용 회원가입 폼.
 * 필드: 아이디(중복확인)·비밀번호·비밀번호 확인·이름·이메일(중복확인)·전화번호(선택).
 *
 * 연계 API: GET /auth/check-login-id · GET /auth/check-email · POST /auth/signup
 *   → @/services/auth (checkLoginId, checkEmail, signup)
 * 성공 후: role 에 맞는 로그인 화면으로 이동.
 *
 * 실시간 검증(#238): useFieldValidation 이 형식 오류(errors)를 담당한다. validateAll() 은
 * 제출 시점에 모든 필드를 touched 로 올리므로, 그 뒤로는 아무 필드나 값이 바뀔 때마다
 * touched 된 필드가 전부 재검증된다 — 서버가 지목한 필드(예: loginId)의 형식 규칙은
 * 여전히 통과하므로, 관계없는 필드(예: name)를 고치는 순간 서버 메시지가 지워진다.
 * 그래서 서버만 아는 답(중복확인 성공/실패, 제출 실패 시 서버가 지목한 모든 필드 오류)은
 * 별도 슬롯(serverErrors)에 담는다 — errors 와 같은 키(규칙표의 모든 필드)를 가지며,
 * 각 슬롯 값은 그 필드 자신의 값이 바뀔 때만 지운다. 아이디·이메일의 중복확인은 서버에
 * 물어볼 수 있는 두 필드에 대한 이 슬롯의 특수화(AVAILABILITY_FIELDS)일 뿐이다. 템플릿은
 * 두 슬롯을 `errors.x || serverErrors.x` 로 합치고, 형식 오류가 항상 먼저 보인다.
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

// 서버 전용 오류 슬롯 — errors 와 분리해 형식 재검증이 서버 응답을 덮어쓰지 않게 한다.
// errors 와 같은 키(규칙표의 모든 필드)를 가진다. 아이디·이메일 중복확인 결과도 이
// 슬롯을 쓴다 — 서버만 아는 답이라는 점에서 같은 종류이기 때문이다(#238 Finding 1).
const serverErrors = reactive(Object.fromEntries(Object.keys(errors).map((name) => [name, ''])))
// 서버에 물어 확인할 수 있는(중복확인이 있는) 필드 — serverErrors 의 특수화만 표시한다.
const AVAILABILITY_FIELDS = new Set(['loginId', 'email'])

const loginIdCheck = reactive({ done: false, available: false })
const emailCheck = reactive({ done: false, available: false })
const submitting = ref(false)

// 중복확인을 통과한 상태 — 버튼 자리를 '확인완료' 표시로, 필드 아래를 성공 문구로 바꾼다.
// 값이 바뀌면 아래 watch 가 done 을 내려 자동으로 되돌아온다.
const loginIdVerified = computed(() => loginIdCheck.done && loginIdCheck.available)
const emailVerified = computed(() => emailCheck.done && emailCheck.available)

// 값이 바뀌면 이전 중복확인 결과(상태)를 무효화한다.
watch(
  () => form.loginId,
  () => {
    loginIdCheck.done = false
    loginIdCheck.available = false
  }
)
watch(
  () => form.email,
  () => {
    emailCheck.done = false
    emailCheck.available = false
  }
)

// 서버가 특정 필드에 대해 준 오류는 그 필드 자신의 값이 바뀔 때만 지운다 — 다른 필드를
// 고쳐도 남아있어야 한다(#238 Finding 1). errors 와 같은 키를 순회하므로 규칙표에 필드가
// 추가·삭제돼도 이 배선은 저절로 맞는다.
Object.keys(errors).forEach((name) => {
  watch(
    () => form[name],
    () => {
      serverErrors[name] = ''
    }
  )
})

// 서버가 이 필드를 거부하면(사유가 있으면) 아이디·이메일의 중복확인 완료 상태도 함께
// 지운다 — 그대로 두면 '확인완료' 배지와 서버 거부 메시지가 동시에 보인다(#238 Finding 5).
// 이미 성공/실패가 이번 라운드에서 막 정해진 중복확인 자체의 응답 처리(onCheckLoginId·
// onCheckEmail)는 done/available 을 스스로 올바르게 맞추므로 이 함수를 쓰지 않는다 —
// 여기서 다시 되돌리면 '이미 확인했지만 사용 중이었다'는 상태가 '아직 확인 안 함'으로
// 잘못 바뀐다. 이 함수는 그 두 곳이 모르는 경로(제출 시점의 뒤늦은 서버 거부)를 위한 것이다.
function setServerError(field, reason) {
  serverErrors[field] = reason
  if (!reason || !AVAILABILITY_FIELDS.has(field)) return
  if (field === 'loginId') {
    loginIdCheck.done = false
    loginIdCheck.available = false
  } else {
    emailCheck.done = false
    emailCheck.available = false
  }
}

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
    serverErrors.loginId = available ? '' : '이미 사용 중인 아이디입니다.'
  } catch (err) {
    if (form.loginId !== checkedValue) return
    const fieldMessage = fieldErrorMap(err).loginId
    if (fieldMessage) {
      serverErrors.loginId = fieldMessage
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
    serverErrors.email = available ? '' : '이미 사용 중인 이메일입니다.'
  } catch (err) {
    if (form.email !== checkedValue) return
    const fieldMessage = fieldErrorMap(err).email
    if (fieldMessage) {
      serverErrors.email = fieldMessage
    } else {
      ui.toast(err?.response?.data?.message || '이메일 중복확인에 실패했어요.', { type: 'danger' })
    }
  }
}

/**
 * 중복확인 완료 여부를 제출 시점에 확인한다. loginIdCheck/emailCheck 는
 * useFieldValidation 의 규칙표가 모르는, 이 컴포넌트만 가진 상태라 규칙에 넣지 않고
 * 여기서 따로 얹는다.
 *
 * 형식이 이미 무효(errors.x 가 채워진 상태)면 serverErrors.x 를 건드리지 않는다 — 호출
 * 순서에 기대지 않기 위해서다. 지금은 onSubmit 이 validateAll() 직후에만 이 함수를
 * 부르지만, 그 순서가 이 함수의 정확성 조건이 되면 안 된다. 다른 폼이 이 패턴을
 * 베낄 때(Task 4) 호출 순서가 달라져도 이 함수 자체가 스스로를 지켜야 한다.
 */
function checkAvailabilityBeforeSubmit() {
  let ok = true

  if (!errors.loginId) {
    if (!loginIdCheck.done) {
      serverErrors.loginId = '아이디 중복확인을 해주세요.'
      ok = false
    } else if (!loginIdCheck.available) {
      serverErrors.loginId = '이미 사용 중인 아이디입니다.'
      ok = false
    } else {
      serverErrors.loginId = ''
    }
  }

  if (!errors.email) {
    if (!emailCheck.done) {
      serverErrors.email = '이메일 중복확인을 해주세요.'
      ok = false
    } else if (!emailCheck.available) {
      serverErrors.email = '이미 사용 중인 이메일입니다.'
      ok = false
    } else {
      serverErrors.email = ''
    }
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
    // 확인·제출 사이 경합으로 서버가 특정 필드를 거부하면(409 등) fieldErrors 를 준다 —
    // 해당 입력 아래에 사유를 붙이고, 필드로 못 옮기는 오류만 토스트로 보여준다. 모든
    // 필드의 서버 거부를 serverErrors 로 보낸다 — errors 에 쓰면 관계없는 다른 필드를
    // 고치는 순간 재검증 watcher 가 지워버린다(#238 Finding 1).
    const fieldErrors = fieldErrorMap(err)
    const matchedField = Object.keys(form).some((field) => {
      const reason = fieldErrors[field]
      if (!reason) return false
      setServerError(field, reason)
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
      :error="errors.loginId || serverErrors.loginId"
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
      :error="errors.password || serverErrors.password"
      @blur="handleBlur('password')"
    />

    <AppField
      v-model="form.passwordConfirm"
      type="password"
      label="비밀번호 확인"
      placeholder="비밀번호를 한 번 더 입력"
      required
      :error="errors.passwordConfirm || serverErrors.passwordConfirm"
      @blur="handleBlur('passwordConfirm')"
    />

    <AppField
      v-model="form.name"
      label="이름"
      placeholder="이름"
      :maxlength="NAME_MAX_LENGTH"
      required
      :error="errors.name || serverErrors.name"
      @blur="handleBlur('name')"
    />

    <AppField
      v-model="form.email"
      type="email"
      label="이메일"
      placeholder="example@gighub.com"
      required
      :error="errors.email || serverErrors.email"
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
      :error="errors.phone || serverErrors.phone"
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
