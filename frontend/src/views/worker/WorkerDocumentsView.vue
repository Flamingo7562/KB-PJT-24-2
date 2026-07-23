<script setup>
/**
 * [G] 알바생 문서함  ·  /worker/documents  ·  WORKER  (탭 화면)
 * 근로계약서(자동 저장) + 보건증(업로드·발급일 수정·삭제·공유·공유 취소).
 * 업로드=보건증만. 계약서 삭제=근무 종료 후. 카드에 공유중 지점 표시.
 * 연계 API: GET/POST/PATCH/DELETE /documents · GET/POST/DELETE /documents/{id}/shares
 *   →  @/services/documents (전부)
 * 공통: BaseBottomSheet(보건증 등록/공유 모달) · 카드 클릭 → /worker/documents/:documentId
 */
import {
  BadgeCheck,
  ChevronRight,
  FileText,
  MapPin,
  Pencil,
  Share2,
  Trash2,
  Upload
} from 'lucide-vue-next'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import AppField from '@/components/common/AppField.vue'
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import BaseModal from '@/components/common/BaseModal.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import {
  deleteDocument,
  getDocumentShares,
  listDocuments,
  revokeShare,
  shareDocument,
  updateDocumentIssuedDate,
  uploadDocument
} from '@/services/documents'
import { listWorkerWorkplaces } from '@/services/worker'
import { useUiStore } from '@/stores/ui'
import { formatDate } from '@/utils/format'

const router = useRouter()
const ui = useUiStore()

const ALLOWED_EXT = ['jpg', 'jpeg', 'png', 'pdf']

const docs = ref([])
const loading = ref(true)

const contracts = computed(() => docs.value.filter((d) => d.docType === 'CONTRACT'))
const healthCerts = computed(() => docs.value.filter((d) => d.docType === 'HEALTH_CERT'))

onMounted(load)

async function load() {
  loading.value = true
  try {
    const { content } = await listDocuments()
    const list = content ?? []
    // 카드에 '공유중 지점' 을 표시하려면 보건증별 공유 현황이 필요하다.
    await Promise.all(
      list
        .filter((d) => d.docType === 'HEALTH_CERT')
        .map(async (d) => {
          try {
            d.shares = await getDocumentShares(d.documentId)
          } catch {
            d.shares = []
          }
        })
    )
    docs.value = list
  } catch {
    ui.toast('문서를 불러오지 못했어요.', { type: 'danger' })
  } finally {
    loading.value = false
  }
}

function goViewer(doc) {
  router.push(`/worker/documents/${doc.documentId}`)
}

/* ---- 보건증 등록(업로드) 시트 ---- */
const registerOpen = ref(false)
const file = ref(null)
const fileName = ref('')
const fileError = ref('')
const issuedDate = ref('')
const issuedError = ref('')
const submitting = ref(false)

function openRegister() {
  file.value = null
  fileName.value = ''
  fileError.value = ''
  issuedDate.value = ''
  issuedError.value = ''
  registerOpen.value = true
}

function onFile(e) {
  fileError.value = ''
  const picked = e.target.files?.[0]
  if (!picked) {
    file.value = null
    fileName.value = ''
    return
  }
  const ext = picked.name.split('.').pop()?.toLowerCase() ?? ''
  if (!ALLOWED_EXT.includes(ext)) {
    fileError.value = 'jpg · png · pdf 파일만 등록할 수 있어요.'
    file.value = null
    fileName.value = ''
    e.target.value = ''
    return
  }
  file.value = picked
  fileName.value = picked.name
}

async function submitUpload() {
  fileError.value = ''
  issuedError.value = ''
  if (!file.value) {
    fileError.value = '보건증 파일을 선택해 주세요.'
    return
  }
  if (!issuedDate.value) {
    issuedError.value = '발급일을 입력해 주세요.'
    return
  }
  submitting.value = true
  try {
    const formData = new FormData()
    formData.append('docType', 'HEALTH_CERT')
    formData.append('file', file.value)
    formData.append('issuedDate', issuedDate.value)
    await uploadDocument(formData)
    ui.toast('보건증을 등록했어요.', { type: 'success' })
    registerOpen.value = false
    await load()
  } catch {
    ui.toast('보건증 등록에 실패했어요.', { type: 'danger' })
  } finally {
    submitting.value = false
  }
}

/* ---- 발급일 수정 모달 ---- */
const editDoc = ref(null)
const editDate = ref('')
const editOpen = ref(false)
const editSaving = ref(false)

function openEdit(doc) {
  editDoc.value = doc
  editDate.value = doc.issuedDate ?? ''
  editOpen.value = true
}

async function saveEdit() {
  if (!editDate.value) {
    ui.toast('발급일을 입력해 주세요.', { type: 'warning' })
    return
  }
  editSaving.value = true
  try {
    await updateDocumentIssuedDate(editDoc.value.documentId, { issuedDate: editDate.value })
    ui.toast('발급일을 수정했어요.', { type: 'success' })
    editOpen.value = false
    await load()
  } catch {
    ui.toast('발급일 수정에 실패했어요.', { type: 'danger' })
  } finally {
    editSaving.value = false
  }
}

/* ---- 삭제 확인 모달 (계약서·보건증 공용) ---- */
const deleteDoc = ref(null)
const deleteOpen = ref(false)
const deleting = ref(false)

function openDelete(doc) {
  deleteDoc.value = doc
  deleteOpen.value = true
}

async function confirmDelete() {
  deleting.value = true
  try {
    await deleteDocument(deleteDoc.value.documentId)
    ui.toast('문서를 삭제했어요.', { type: 'success' })
    deleteOpen.value = false
    await load()
  } catch (e) {
    // 계약서는 근무 종료 후에만 삭제 가능 — 서버가 최종 검증(409).
    if (e?.response?.status === 409) {
      ui.toast('진행 중인 근무가 있어 계약서를 삭제할 수 없어요.', { type: 'warning' })
    } else {
      ui.toast('삭제에 실패했어요.', { type: 'danger' })
    }
  } finally {
    deleting.value = false
  }
}

/* ---- 보건증 공유 관리 시트 ---- */
const shareDoc = ref(null)
const shareOpen = ref(false)
const shareTargets = ref([])
const shareLoading = ref(false)
const shareBusy = ref(false)

const sharedIds = computed(() => new Set((shareDoc.value?.shares ?? []).map((s) => s.workplaceId)))
const availableTargets = computed(() =>
  shareTargets.value.filter((w) => !sharedIds.value.has(w.workplaceId))
)

async function openShare(doc) {
  shareDoc.value = doc
  shareOpen.value = true
  shareLoading.value = true
  try {
    const [targets, shares] = await Promise.all([
      listWorkerWorkplaces(),
      getDocumentShares(doc.documentId)
    ])
    shareTargets.value = targets
    shareDoc.value.shares = shares
  } catch {
    ui.toast('공유 정보를 불러오지 못했어요.', { type: 'warning' })
  } finally {
    shareLoading.value = false
  }
}

async function doShare(workplace) {
  shareBusy.value = true
  try {
    await shareDocument(shareDoc.value.documentId, { workplaceId: workplace.workplaceId })
    ui.toast(`${workplace.workplaceName}에 공유했어요.`, { type: 'success' })
    shareDoc.value.shares = await getDocumentShares(shareDoc.value.documentId)
  } catch (e) {
    if (e?.response?.status === 409) {
      ui.toast('이미 공유한 지점이에요.', { type: 'warning' })
    } else {
      ui.toast('공유에 실패했어요.', { type: 'danger' })
    }
  } finally {
    shareBusy.value = false
  }
}

async function doRevoke(share) {
  shareBusy.value = true
  try {
    await revokeShare(shareDoc.value.documentId, share.workplaceId)
    ui.toast('공유를 취소했어요.', { type: 'success' })
    shareDoc.value.shares = await getDocumentShares(shareDoc.value.documentId)
  } catch {
    ui.toast('공유 취소에 실패했어요.', { type: 'danger' })
  } finally {
    shareBusy.value = false
  }
}
</script>

<template>
  <div class="worker-docs">
    <header class="head">
      <h1 class="page-title">문서함</h1>
      <BaseButton variant="worker" @click="openRegister">
        <Upload :size="16" />
        보건증 등록
      </BaseButton>
    </header>

    <p v-if="loading" class="loading">불러오는 중…</p>

    <template v-else>
      <EmptyState v-if="docs.length === 0" message="아직 등록된 문서가 없어요.">
        보건증을 등록하거나, 근무를 시작하면 근로계약서가 자동 저장돼요.
      </EmptyState>

      <template v-else>
        <section v-if="contracts.length" class="doc-section">
          <h2 class="section-title">근로계약서</h2>
          <ul class="doc-list">
            <li v-for="doc in contracts" :key="doc.documentId" class="doc-card">
              <button type="button" class="doc-main" @click="goViewer(doc)">
                <FileText :size="22" class="doc-icon" />
                <div class="doc-info">
                  <p class="doc-name">{{ doc.fileName }}</p>
                  <p class="doc-meta">발급일 {{ formatDate(doc.issuedDate) }}</p>
                </div>
                <ChevronRight :size="18" class="chevron" />
              </button>
              <div class="doc-actions">
                <button type="button" class="act act--danger" @click="openDelete(doc)">
                  <Trash2 :size="15" />
                  삭제
                </button>
              </div>
            </li>
          </ul>
        </section>

        <section v-if="healthCerts.length" class="doc-section">
          <h2 class="section-title">보건증</h2>
          <ul class="doc-list">
            <li v-for="doc in healthCerts" :key="doc.documentId" class="doc-card">
              <button type="button" class="doc-main" @click="goViewer(doc)">
                <BadgeCheck :size="22" class="doc-icon" />
                <div class="doc-info">
                  <p class="doc-name">{{ doc.fileName }}</p>
                  <p class="doc-meta">
                    발급일 {{ formatDate(doc.issuedDate) }}
                    <span v-if="doc.expiryDate"> · 만료 {{ formatDate(doc.expiryDate) }}</span>
                  </p>
                  <p class="doc-share">
                    <Share2 :size="13" />
                    <template v-if="doc.shares?.length">
                      공유중 · {{ doc.shares.map((s) => s.workplaceName).join(', ') }}
                    </template>
                    <template v-else>공유 안 함</template>
                  </p>
                </div>
                <ChevronRight :size="18" class="chevron" />
              </button>
              <div class="doc-actions">
                <button type="button" class="act" @click="openShare(doc)">
                  <Share2 :size="15" />
                  공유 관리
                </button>
                <button type="button" class="act" @click="openEdit(doc)">
                  <Pencil :size="15" />
                  발급일
                </button>
                <button type="button" class="act act--danger" @click="openDelete(doc)">
                  <Trash2 :size="15" />
                  삭제
                </button>
              </div>
            </li>
          </ul>
        </section>
      </template>
    </template>

    <!-- 보건증 등록 -->
    <BaseBottomSheet :open="registerOpen" title="보건증 등록" @close="registerOpen = false">
      <div class="upload-form">
        <div class="file-field">
          <label class="file-drop">
            <input type="file" accept=".jpg,.jpeg,.png,.pdf" class="file-input" @change="onFile" />
            <Upload :size="18" />
            <span class="file-name">{{ fileName || '파일 선택 (jpg · png · pdf)' }}</span>
          </label>
          <p v-if="fileError" class="field-err">{{ fileError }}</p>
        </div>
        <AppField v-model="issuedDate" label="발급일" type="date" required :error="issuedError" />
      </div>
      <template #footer>
        <BaseButton variant="worker" size="lg" block :disabled="submitting" @click="submitUpload">
          {{ submitting ? '등록 중…' : '등록' }}
        </BaseButton>
      </template>
    </BaseBottomSheet>

    <!-- 보건증 공유 관리 -->
    <BaseBottomSheet :open="shareOpen" title="보건증 공유" @close="shareOpen = false">
      <div class="share-body">
        <p v-if="shareLoading" class="muted">불러오는 중…</p>
        <template v-else>
          <section class="share-sec">
            <h3 class="share-h">공유중인 지점</h3>
            <ul v-if="shareDoc?.shares?.length" class="share-list">
              <li v-for="s in shareDoc.shares" :key="s.workplaceId" class="share-row">
                <span class="wp"><MapPin :size="15" /> {{ s.workplaceName }}</span>
                <BaseButton variant="ghost" :disabled="shareBusy" @click="doRevoke(s)">
                  공유 취소
                </BaseButton>
              </li>
            </ul>
            <p v-else class="muted">아직 공유한 지점이 없어요.</p>
          </section>

          <section class="share-sec">
            <h3 class="share-h">공유할 지점</h3>
            <ul v-if="availableTargets.length" class="share-list">
              <li v-for="w in availableTargets" :key="w.workplaceId" class="share-row">
                <span class="wp">
                  <MapPin :size="15" /> {{ w.workplaceName }}
                  <small v-if="w.ownerName"> · {{ w.ownerName }}</small>
                </span>
                <BaseButton variant="worker" :disabled="shareBusy" @click="doShare(w)">
                  공유
                </BaseButton>
              </li>
            </ul>
            <p v-else class="muted">공유할 수 있는 근무 예정 지점이 없어요.</p>
          </section>
        </template>
      </div>
    </BaseBottomSheet>

    <!-- 발급일 수정 -->
    <BaseModal :open="editOpen" title="발급일 수정" @close="editOpen = false">
      <AppField v-model="editDate" label="발급일" type="date" />
      <template #footer>
        <BaseButton variant="secondary" block @click="editOpen = false">취소</BaseButton>
        <BaseButton variant="worker" block :disabled="editSaving" @click="saveEdit"
          >저장</BaseButton
        >
      </template>
    </BaseModal>

    <!-- 삭제 확인 -->
    <BaseModal :open="deleteOpen" title="삭제할까요?" @close="deleteOpen = false">
      <p class="del-msg">
        <strong>{{ deleteDoc?.fileName }}</strong> 문서를 삭제합니다.
      </p>
      <p v-if="deleteDoc?.docType === 'CONTRACT'" class="del-note">
        근무가 종료된 계약서만 삭제할 수 있어요.
      </p>
      <p v-else class="del-note">공유한 지점에서도 함께 삭제돼요.</p>
      <template #footer>
        <BaseButton variant="secondary" block @click="deleteOpen = false">취소</BaseButton>
        <BaseButton variant="danger" block :disabled="deleting" @click="confirmDelete"
          >삭제</BaseButton
        >
      </template>
    </BaseModal>
  </div>
</template>

<style scoped>
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
}
.page-title {
  font-size: var(--text-xl);
  font-weight: var(--weight-bold);
  color: var(--color-text);
}
.loading {
  margin-top: var(--space-xl);
  text-align: center;
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}

.doc-section {
  margin-top: var(--space-lg);
}
.section-title {
  font-size: var(--text-md);
  font-weight: var(--weight-medium);
  color: var(--color-text-sub);
}
.doc-list {
  margin-top: var(--space-sm);
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}
.doc-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
}
.doc-main {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  width: 100%;
  padding: var(--space-lg);
  text-align: left;
}
.doc-icon {
  flex-shrink: 0;
  color: var(--color-worker);
}
.doc-info {
  flex: 1;
  min-width: 0;
}
.doc-name {
  font-size: var(--text-lg);
  font-weight: var(--weight-medium);
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.doc-meta {
  margin-top: var(--space-xs);
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}
.doc-share {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: var(--space-xs);
  font-size: var(--text-sm);
  color: var(--color-worker);
}
.chevron {
  flex-shrink: 0;
  color: var(--color-text-sub);
}
.doc-actions {
  display: flex;
  border-top: 1px solid var(--color-border);
}
.act {
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: var(--space-md);
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}
.act + .act {
  border-left: 1px solid var(--color-border);
}
.act--danger {
  color: var(--color-danger);
}

/* 등록 시트 */
.upload-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}
.file-drop {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-lg);
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text-sub);
  cursor: pointer;
}
.file-input {
  display: none;
}
.file-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.field-err {
  margin-top: var(--space-xs);
  font-size: var(--text-sm);
  color: var(--color-danger);
}

/* 공유 시트 */
.share-body {
  display: flex;
  flex-direction: column;
  gap: var(--space-xl);
}
.share-h {
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  color: var(--color-text-sub);
}
.share-list {
  margin-top: var(--space-sm);
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.share-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-sm);
}
.wp {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
  font-size: var(--text-md);
  color: var(--color-text);
}
.wp small {
  color: var(--color-text-sub);
}
.muted {
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}

/* 삭제 모달 */
.del-msg {
  font-size: var(--text-md);
  color: var(--color-text);
}
.del-note {
  margin-top: var(--space-sm);
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}
</style>
