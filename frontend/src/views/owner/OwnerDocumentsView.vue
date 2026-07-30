<script setup>
/**
 * [D] 사장 문서함  ·  /owner/documents  ·  OWNER  (탭 화면)
 * 지점 문서: 자동 생성 계약서 + 공유받은 보건증. 읽기 전용(업로드·삭제 없음).
 * 지점 컨텍스트: useWorkplaceStore().selectedId (AppTopBar 의 전역 지점 select 를 그대로 구독).
 * 연계 API: GET /documents?workplaceId  →  @/services/documents (listDocuments)
 * 규칙: 계약서는 근무 확정 시 자동 생성되며 근무일로부터 3년 보존(삭제 불가). 보건증은 알바생이 공유한 것을
 *   열람만 하고, 근무 종료 시 서버가 공유를 자동 해제해 목록에서 빠진다.
 * 공통: 카드 클릭 → /owner/documents/:documentId
 */
import { FileText, Image as ImageIcon } from 'lucide-vue-next'
import { storeToRefs } from 'pinia'
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import EmptyState from '@/components/common/EmptyState.vue'
import { listDocuments } from '@/services/documents'
import { useWorkplaceStore } from '@/stores/workplace'
import { formatDate } from '@/utils/format'

const router = useRouter()
const workplaceStore = useWorkplaceStore()
const { selectedId } = storeToRefs(workplaceStore)

const TABS = [
  { value: 'ALL', label: '전체' },
  { value: 'CONTRACT', label: '근로계약서' },
  { value: 'HEALTH_CERT', label: '보건증' }
]

const documents = ref([])
const loading = ref(false)
const activeTab = ref('ALL')

// mock 은 params 를 무시하고 전체를 돌려주므로(다른 서비스와 동일한 관행), 지점 필터는 여기서 한 번 더 건다.
// 실제 API 연동 시 서버가 workplaceId 로 이미 필터링해 내려주므로 이 filter 는 그대로 둬도 안전하다.
const documentsInWorkplace = computed(() =>
  documents.value.filter((d) => d.workplaceId === selectedId.value)
)
const filteredDocuments = computed(() => {
  if (activeTab.value === 'ALL') return documentsInWorkplace.value
  return documentsInWorkplace.value.filter((d) => d.docType === activeTab.value)
})

async function load() {
  loading.value = true
  try {
    const res = await listDocuments({ workplaceId: selectedId.value })
    documents.value = res.content
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(selectedId, load)

function openViewer(documentId) {
  router.push(`/owner/documents/${documentId}`)
}
</script>

<template>
  <div class="documents">
    <div class="tabs" role="tablist" aria-label="문서 유형">
      <button
        v-for="tab in TABS"
        :key="tab.value"
        type="button"
        class="tab"
        :class="{ active: activeTab === tab.value }"
        @click="activeTab = tab.value"
      >
        {{ tab.label }}
      </button>
    </div>

    <EmptyState v-if="!loading && filteredDocuments.length === 0" message="표시할 문서가 없어요." />

    <ul v-else class="doc-list">
      <li v-for="doc in filteredDocuments" :key="doc.documentId" class="doc-card">
        <button type="button" class="doc-main" @click="openViewer(doc.documentId)">
          <span class="thumb">
            <ImageIcon v-if="['jpg', 'jpeg', 'png'].includes(doc.fileExt)" :size="20" />
            <FileText v-else :size="20" />
          </span>

          <span class="doc-info">
            <span class="doc-name">{{ doc.fileName }}</span>
            <!-- 발급일(documents.issued_on) · 만료 예정일(documents.expires_on) -->
            <span class="doc-meta">
              {{ formatDate(doc.issuedDate) }} ·
              {{ doc.docType === 'CONTRACT' ? '근로계약서' : '보건증' }}
            </span>
            <span v-if="doc.docType === 'HEALTH_CERT'" class="doc-expiry">
              만료 예정 {{ formatDate(doc.expiryDate) }}
            </span>
          </span>
        </button>
      </li>
    </ul>

    <p class="notice">
      근로계약서는 근무 확정 시 자동 저장되며 근무일로부터 3년간 보관돼요 · 보건증은 알바생이 공유한
      문서로, 근무가 끝나면 공유가 자동으로 해제돼요
    </p>
  </div>
</template>

<style scoped>
.documents {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}

.tabs {
  display: inline-flex;
  align-self: flex-start;
  gap: var(--space-xs);
  padding: 4px;
  background: var(--color-bg);
  border-radius: var(--radius-pill);
}
.tab {
  padding: var(--space-xs) var(--space-md);
  border-radius: var(--radius-pill);
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  color: var(--color-text-sub);
}
.tab.active {
  background: var(--color-primary);
  color: var(--color-on-primary);
}

.doc-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.doc-card {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-md);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}
.doc-main {
  display: flex;
  flex: 1;
  min-width: 0;
  align-items: center;
  gap: var(--space-md);
  text-align: left;
}
.thumb {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  color: var(--color-owner);
  background: var(--color-owner-weak);
  border-radius: var(--radius-sm);
}
.doc-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.doc-name {
  overflow: hidden;
  font-size: var(--text-md);
  font-weight: var(--weight-medium);
  color: var(--color-text);
  text-overflow: ellipsis;
  white-space: nowrap;
}
.doc-meta {
  font-size: var(--text-sm);
  color: var(--color-text-sub);
  word-break: keep-all;
}
/* 만료일은 놓치면 안 되는 정보라 한 줄 내려 주의 색으로 강조한다. */
.doc-expiry {
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  color: var(--color-warning);
  word-break: keep-all;
}

.notice {
  padding: var(--space-md);
  background: var(--color-bg);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}
</style>
