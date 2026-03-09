<template>
  <div class="editor-page">
    <div v-if="loading" class="loading-container">
      <div class="loading-content">
        <div class="loading-spinner"></div>
        <div class="loading-text">加载乐谱中...</div>
      </div>
    </div>

    <div v-else-if="error" class="error-container">
      <div class="error-content">
        <div class="error-icon">!</div>
        <h3 class="error-title">加载失败</h3>
        <p class="error-message">{{ error }}</p>
        <button @click="retryLoading" class="error-btn">重试</button>
        <router-link to="/" class="error-link">返回首页</router-link>
      </div>
    </div>

    <div v-else class="editor-container">
      <FullScoreEditor
        v-if="currentScore"
        :key="scoreKey"
        :score="currentScore"
        @save="handleSave"
        @publish="handlePublish"
        @export="handleExport"
        @close="handleClose"
      />

      <div v-else class="empty-editor">
        <div class="empty-content">
          <div class="empty-icon">♫</div>
          <h3 class="empty-title">创建新乐谱</h3>
          <p class="empty-description">开始创作你的音乐作品</p>

          <div class="quick-start-options">
            <div class="quick-start-card" @click="createBlankScore">
              <div class="card-icon">+</div>
              <h4 class="card-title">空白乐谱</h4>
              <p class="card-desc">从零开始创建</p>
            </div>

            <div class="quick-start-card" @click="importFromFile">
              <div class="card-icon">⇪</div>
              <h4 class="card-title">导入文件</h4>
              <p class="card-desc">MIDI / MusicXML / MXL</p>
            </div>

            <div class="quick-start-card" @click="createTemplateScore">
              <div class="card-icon">◎</div>
              <h4 class="card-title">使用模板</h4>
              <p class="card-desc">快速开始</p>
            </div>

            <div class="quick-start-card" @click="openRecentScore">
              <div class="card-icon">↺</div>
              <h4 class="card-title">最近编辑</h4>
              <p class="card-desc">继续创作</p>
            </div>
          </div>

          <div v-if="showFestivalSection" class="festival-section">
            <div class="festival-header">
              <div class="festival-icon">✦</div>
              <h4 class="festival-title">活动推荐</h4>
            </div>
            <div class="festival-options">
              <button class="festival-btn" @click="createFestivalScore('新春主题音乐')">新春主题音乐</button>
              <button class="festival-btn" @click="createFestivalScore('节庆序曲')">节庆传统音乐</button>
              <button class="festival-btn" @click="joinFestivalContest">参加创作大赛</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useScoreStore } from '@/stores/scoreStore'
import { useUserStore } from '@/stores/userStore'
import { getScoreById, saveScoreOnServer } from '@/services/scoreApi'
import FullScoreEditor from '@/components/editor/FullScoreEditor.vue'
import type { Score } from '@/types/score'

type PublishPayload = {
  visibility: 'public' | 'private' | 'unlisted'
  description: string
  tags: string[]
}

const route = useRoute()
const router = useRouter()
const scoreStore = useScoreStore()
const userStore = useUserStore()

const loading = ref(false)
const error = ref<string | null>(null)
const scoreKey = ref(Date.now())
const hasUnsavedChanges = ref(false)
const initialized = ref(false)
const autoSaveTimer = ref<ReturnType<typeof setTimeout> | null>(null)
const savingInFlight = ref(false)
const saveQueued = ref(false)
const isSyncingFromServer = ref(false)
const lastSavedFingerprint = ref<string | null>(null)

const AUTO_SAVE_DELAY_MS = 1200

const currentScore = computed(() => scoreStore.currentScore)
const scoreId = computed(() => route.params.id as string | undefined)
const currentScoreFingerprint = computed(() => {
  if (!currentScore.value) return null
  const score = currentScore.value
  return JSON.stringify({
    id: score.id,
    title: score.title,
    composer: score.composer,
    tempo: score.tempo,
    timeSignature: score.timeSignature,
    keySignature: score.keySignature,
    notes: score.notes,
    measures: score.measures,
    isDraft: score.isDraft,
    tags: score.tags ?? [],
    description: score.description ?? '',
    visibility: score.visibility ?? 'private'
  })
})

const showFestivalSection = computed(() => {
  const now = new Date()
  const start = new Date('2026-01-01')
  const end = new Date('2026-12-31')
  return now >= start && now <= end
})

onMounted(() => {
  if (scoreId.value) {
    loadScore(scoreId.value)
  }
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onBeforeUnmount(() => {
  clearAutoSaveTimer()
  window.removeEventListener('beforeunload', handleBeforeUnload)
})

watch(
  () => route.params.id,
  (newId) => {
    if (typeof newId === 'string' && newId.length > 0) {
      loadScore(newId)
      return
    }
    clearAutoSaveTimer()
    lastSavedFingerprint.value = null
    hasUnsavedChanges.value = false
    scoreStore.currentScore = null
  }
)

watch(
  () => currentScoreFingerprint.value,
  (nextFingerprint) => {
    if (!nextFingerprint) {
      hasUnsavedChanges.value = false
      return
    }
    if (!initialized.value) return
    if (isSyncingFromServer.value) return

    const isSaved = nextFingerprint === lastSavedFingerprint.value
    hasUnsavedChanges.value = !isSaved
    if (!isSaved) {
      scheduleAutoSave()
    }
  },
  { flush: 'post' }
)

const handleBeforeUnload = (e: BeforeUnloadEvent) => {
  if (!hasUnsavedChanges.value) return
  e.preventDefault()
  e.returnValue = '你有未保存的更改，确定要离开吗？'
}

const clearAutoSaveTimer = () => {
  if (!autoSaveTimer.value) return
  clearTimeout(autoSaveTimer.value)
  autoSaveTimer.value = null
}

const scoreFingerprint = (score: Score) => JSON.stringify({
  id: score.id,
  title: score.title,
  composer: score.composer,
  tempo: score.tempo,
  timeSignature: score.timeSignature,
  keySignature: score.keySignature,
  notes: score.notes,
  measures: score.measures,
  isDraft: score.isDraft,
  tags: score.tags ?? [],
  description: score.description ?? '',
  visibility: score.visibility ?? 'private'
})

const syncSavedScore = (saved: Score) => {
  const index = scoreStore.scores.findIndex((item) => item.id === saved.id)
  if (index === -1) {
    scoreStore.scores.push(saved)
  } else {
    scoreStore.scores[index] = saved
  }

  isSyncingFromServer.value = true
  try {
    scoreStore.currentScore = saved
    lastSavedFingerprint.value = scoreFingerprint(saved)
    hasUnsavedChanges.value = false
  } finally {
    isSyncingFromServer.value = false
  }
}

const persistScore = async (options?: { showSuccessMessage?: string }) => {
  if (!currentScore.value) return
  if (!userStore.isLoggedIn) return

  if (savingInFlight.value) {
    saveQueued.value = true
    return
  }

  savingInFlight.value = true
  try {
    if (typeof currentScore.value.isDraft !== 'boolean') {
      currentScore.value.isDraft = true
    }
    currentScore.value.updatedAt = new Date()
    const saved = await saveScoreOnServer(currentScore.value)
    syncSavedScore(saved)
    if (options?.showSuccessMessage) {
      showSaveSuccess(options.showSuccessMessage)
    }
  } catch (err) {
    console.error('保存乐谱失败:', err)
    error.value = err instanceof Error ? err.message : '保存乐谱失败'
  } finally {
    savingInFlight.value = false
    if (saveQueued.value) {
      saveQueued.value = false
      scheduleAutoSave(0)
    }
  }
}

const scheduleAutoSave = (delay = AUTO_SAVE_DELAY_MS) => {
  if (!userStore.isLoggedIn) return
  if (!hasUnsavedChanges.value) return
  clearAutoSaveTimer()
  autoSaveTimer.value = setTimeout(() => {
    autoSaveTimer.value = null
    void persistScore()
  }, delay)
}

const loadScore = async (id: string) => {
  loading.value = true
  error.value = null
  initialized.value = false
  clearAutoSaveTimer()

  try {
    let score = scoreStore.scores.find((item) => item.id === id)
    if (!score) {
      score = await getScoreById(id)
      syncSavedScore(score)
    } else {
      scoreStore.currentScore = score
      lastSavedFingerprint.value = scoreFingerprint(score)
      hasUnsavedChanges.value = false
    }
    scoreKey.value = Date.now()
  } catch (err) {
    console.error('加载乐谱失败:', err)
    error.value = err instanceof Error ? err.message : '加载乐谱时发生错误'
  } finally {
    loading.value = false
    initialized.value = true
  }
}

const retryLoading = () => {
  if (!scoreId.value) return
  loadScore(scoreId.value)
}

const navigateToScore = (id: string) => {
  router.push(`/editor/${id}`)
}

const createBlankScore = () => {
  const now = new Date()
  const newScore: Score = {
    id: Date.now().toString(),
    title: '未命名乐谱',
    composer: userStore.user?.username || '',
    tempo: 120,
    timeSignature: { numerator: 4, denominator: 4 },
    keySignature: 'C',
    notes: [],
    measures: [],
    createdAt: now,
    updatedAt: now,
    isDraft: true,
    visibility: 'private',
    tags: [],
    description: ''
  }

  const created = scoreStore.createScore(newScore)
  initialized.value = true
  hasUnsavedChanges.value = true
  lastSavedFingerprint.value = null
  scheduleAutoSave()
  navigateToScore(created.id)
}

const createFestivalScore = (title: string) => {
  createBlankScore()
  if (!scoreStore.currentScore) return
  scoreStore.currentScore.title = title
  scoreStore.currentScore.keySignature = 'F'
}

const createTemplateScore = () => {
  createBlankScore()
}

const openRecentScore = () => {
  const recent = scoreStore.recentScores[0]
  if (!recent) return
  navigateToScore(recent.id)
}

const joinFestivalContest = () => {
  router.push('/contests/festival')
}

const importFromFile = () => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '.musicxml,.xml,.mxl,.mid,.midi'
  input.onchange = async () => {
    const file = input.files?.[0]
    if (!file) return

    try {
      loading.value = true
      const imported = await scoreStore.importScoreFile(file)
      hasUnsavedChanges.value = true
      lastSavedFingerprint.value = null
      scheduleAutoSave()
      navigateToScore(imported.id)
    } catch (err) {
      console.error('导入失败:', err)
      error.value = err instanceof Error ? err.message : '导入文件时发生错误'
    } finally {
      loading.value = false
    }
  }
  input.click()
}

const handleSave = async () => {
  if (!currentScore.value) return
  clearAutoSaveTimer()
  await persistScore({ showSuccessMessage: '保存成功' })
}

const handlePublish = async (payload: PublishPayload) => {
  if (!currentScore.value) return

  try {
    clearAutoSaveTimer()
    currentScore.value.isDraft = false
    currentScore.value.visibility = payload.visibility
    currentScore.value.description = payload.description
    currentScore.value.tags = payload.tags
    currentScore.value.updatedAt = new Date()
    await persistScore({ showSuccessMessage: '发布成功' })
  } catch (err) {
    console.error('发布作品失败:', err)
    error.value = err instanceof Error ? err.message : '发布作品失败'
  }
}

const handleExport = (format: string) => {
  if (!currentScore.value) return

  switch (format) {
    case 'midi':
      exportMIDI(currentScore.value)
      break
    case 'pdf':
      exportPDF(currentScore.value)
      break
    case 'musicxml':
      exportMusicXML(currentScore.value)
      break
    default:
      break
  }
}

const exportMIDI = (score: Score) => {
  const midiBlob = scoreStore.exportToMIDI(score)
  const url = URL.createObjectURL(midiBlob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${score.title}.mid`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

const exportPDF = (score: Score) => {
  console.log('导出 PDF:', score)
  alert('PDF 导出功能开发中...')
}

const exportMusicXML = (score: Score) => {
  const xmlContent = scoreStore.exportToMusicXML(score)
  const blob = new Blob([xmlContent], { type: 'application/xml' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${score.title}.musicxml`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

const handleClose = () => {
  if (hasUnsavedChanges.value) {
    const shouldLeave = window.confirm('有未保存更改，确定离开编辑器吗？')
    if (!shouldLeave) return
  }
  router.push('/')
}

const showSaveSuccess = (message = '保存成功') => {
  const successEl = document.createElement('div')
  successEl.className = 'save-success-toast'
  successEl.textContent = message
  document.body.appendChild(successEl)

  setTimeout(() => {
    successEl.classList.add('show')
  }, 10)

  setTimeout(() => {
    successEl.classList.remove('show')
    setTimeout(() => {
      document.body.removeChild(successEl)
    }, 300)
  }, 2000)
}
</script>

<style scoped>
.editor-page {
  @apply h-full;
}

.loading-container,
.error-container {
  @apply flex items-center justify-center h-full bg-gray-50;
}

.loading-content {
  @apply flex flex-col items-center;
}

.loading-spinner {
  @apply w-12 h-12 border-4 border-blue-500 border-t-transparent rounded-full animate-spin;
}

.loading-text {
  @apply mt-4 text-gray-600;
}

.error-content {
  @apply text-center max-w-md p-8 bg-white rounded-xl shadow-lg;
}

.error-icon {
  @apply text-4xl mb-4;
}

.error-title {
  @apply text-xl font-semibold text-gray-800 mb-2;
}

.error-message {
  @apply text-gray-600 mb-6;
}

.error-btn {
  @apply px-6 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors mr-3;
}

.error-link {
  @apply px-6 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors;
}

.editor-container {
  @apply h-full bg-gray-50;
}

.empty-editor {
  @apply flex items-center justify-center h-full bg-gray-50 p-8;
}

.empty-content {
  @apply text-center max-w-4xl;
}

.empty-icon {
  @apply text-7xl mb-4;
}

.empty-title {
  @apply text-3xl font-bold text-gray-800 mb-2;
}

.empty-description {
  @apply text-gray-600 text-lg mb-8;
}

.quick-start-options {
  @apply grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-10;
}

.quick-start-card {
  @apply p-6 bg-white rounded-xl border border-gray-200 hover:border-blue-300 hover:shadow-lg transition-all cursor-pointer;
}

.card-icon {
  @apply text-3xl mb-3;
}

.card-title {
  @apply text-lg font-semibold text-gray-800 mb-1;
}

.card-desc {
  @apply text-sm text-gray-600;
}

.festival-section {
  @apply bg-gradient-to-r from-yellow-50 to-orange-50 border border-yellow-200 rounded-xl p-6;
}

.festival-header {
  @apply flex items-center justify-center mb-4;
}

.festival-icon {
  @apply text-2xl mr-2;
}

.festival-title {
  @apply text-xl font-semibold text-yellow-800;
}

.festival-options {
  @apply flex flex-wrap justify-center gap-3;
}

.festival-btn {
  @apply px-4 py-2 bg-white border border-yellow-300 rounded-lg hover:bg-yellow-50 transition-colors;
}

:deep(.save-success-toast) {
  @apply fixed top-4 right-4 bg-green-500 text-white px-4 py-2 rounded-lg shadow-lg transform translate-x-full transition-transform duration-300 z-50;
}

:deep(.save-success-toast.show) {
  @apply translate-x-0;
}
</style>
