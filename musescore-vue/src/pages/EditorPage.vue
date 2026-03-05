<template>
  <div class="editor-page">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <div class="loading-content">
        <div class="loading-spinner"></div>
        <div class="loading-text">加载乐谱中...</div>
      </div>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-container">
      <div class="error-content">
        <div class="error-icon">❌</div>
        <h3 class="error-title">加载失败</h3>
        <p class="error-message">{{ error }}</p>
        <button @click="retryLoading" class="error-btn">重试</button>
        <router-link to="/" class="error-link">返回首页</router-link>
      </div>
    </div>

    <!-- 编辑器主界面 -->
    <div v-else class="editor-container">
      <!-- 乐谱编辑器 -->
      <FullScoreEditor
        v-if="currentScore"
        :key="scoreKey"
        :score="currentScore"
        @save="handleSave"
        @export="handleExport"
        @close="handleClose"
      />
      
      <!-- 空状态（新建乐谱） -->
      <div v-else class="empty-editor">
        <div class="empty-content">
          <div class="empty-icon">🎵</div>
          <h3 class="empty-title">创建新乐谱</h3>
          <p class="empty-description">开始创作您的音乐作品</p>
          
          <div class="quick-start-options">
            <div class="quick-start-card" @click="createBlankScore">
              <div class="card-icon">📄</div>
              <h4 class="card-title">空白乐谱</h4>
              <p class="card-desc">从零开始创建</p>
            </div>
            
            <div class="quick-start-card" @click="importFromFile">
              <div class="card-icon">📥</div>
              <h4 class="card-title">导入文件</h4>
              <p class="card-desc">MIDI, MusicXML, MXL</p>
            </div>
            
            <div class="quick-start-card" @click="useTemplate">
              <div class="card-icon">🎨</div>
              <h4 class="card-title">使用模板</h4>
              <p class="card-desc">快速开始</p>
            </div>
            
            <div class="quick-start-card" @click="openRecentScores">
              <div class="card-icon">⏰</div>
              <h4 class="card-title">最近编辑</h4>
              <p class="card-desc">继续创作</p>
            </div>
          </div>
          
          <!-- 节日特辑 -->
          <div v-if="showFestivalSection" class="festival-section">
            <div class="festival-header">
              <div class="festival-icon">🎆</div>
              <h4 class="festival-title">新春特辑</h4>
            </div>
            <div class="festival-options">
              <button class="festival-btn" @click="createFestivalScore('马年新春')">
                <span class="btn-icon">🐎</span>
                <span>马年主题音乐</span>
              </button>
              <button class="festival-btn" @click="createFestivalScore('春节序曲')">
                <span class="btn-icon">🎶</span>
                <span>春节传统音乐</span>
              </button>
              <button class="festival-btn" @click="joinFestivalContest">
                <span class="btn-icon">🏆</span>
                <span>新春创作大赛</span>
              </button>
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
// import TemplateModal from '@/components/modals/TemplateModal.vue'
// import ImportModal from '@/components/modals/ImportModal.vue'
// import RecentScoresDrawer from '@/components/drawers/RecentScoresDrawer.vue'
// import SavePromptModal from '@/components/modals/SavePromptModal.vue'
import type { Score } from '@/types/score'

const route = useRoute()
const router = useRouter()
const scoreStore = useScoreStore()
const userStore = useUserStore()

// 页面状态
const loading = ref(false)
const error = ref<string | null>(null)
const scoreKey = ref(Date.now()) // 用于强制重新渲染编辑器
const hasUnsavedChanges = ref(false)

// 模态框状态
const showTemplateModal = ref(false)
const showImportModal = ref(false)
const showRecentDrawer = ref(false)
const showSavePrompt = ref(false)

// 计算属性
const currentScore = computed(() => scoreStore.currentScore)
const scoreId = computed(() => route.params.id as string)

// 判断是否显示节日特辑
const showFestivalSection = computed(() => {
  const now = new Date()
  const newYearStart = new Date('2026-02-17')
  const newYearEnd = new Date('2026-03-03')
  return now >= newYearStart && now <= newYearEnd
})

// 生命周期
onMounted(() => {
  // 加载乐谱
  if (scoreId.value) {
    loadScore(scoreId.value)
  }
  
  // 添加路由守卫提示
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
})

// 离开页面提示
const handleBeforeUnload = (e: BeforeUnloadEvent) => {
  if (hasUnsavedChanges.value) {
    e.preventDefault()
    e.returnValue = '您有未保存的更改，确定要离开吗？'
  }
}

// 监听路由变化
watch(
  () => route.params.id,
  (newId) => {
    if (newId) {
      loadScore(newId as string)
    } else {
      scoreStore.currentScore = null
    }
  }
)

// 加载乐谱
const loadScore = async (id: string) => {
  loading.value = true
  error.value = null
  
  try {
    let score = scoreStore.scores.find(s => s.id === id)
    if (!score) {
      try {
        score = await getScoreById(id)
        const existingIndex = scoreStore.scores.findIndex(item => item.id === score.id)
        if (existingIndex === -1) {
          scoreStore.scores.push(score)
        } else {
          scoreStore.scores[existingIndex] = score
        }
      } catch {
        score = undefined
      }
    }
    
    if (score) {
      scoreStore.currentScore = score
      scoreKey.value = Date.now() // 重新渲染编辑器
    } else {
      error.value = '未找到该乐谱'
    }
  } catch (err) {
    console.error('加载乐谱失败:', err)
    error.value = '加载乐谱时发生错误'
  } finally {
    loading.value = false
  }
}

// 重试加载
const retryLoading = () => {
  if (scoreId.value) {
    loadScore(scoreId.value)
  }
}

// 创建空白乐谱
const createBlankScore = () => {
  const newScore: Score = {
    id: Date.now().toString(),
    title: '未命名乐谱',
    composer: userStore.user?.username || '',
    tempo: 120,
    timeSignature: { numerator: 4, denominator: 4 },
    keySignature: 'C',
    notes: [],
    measures: [],
    createdAt: new Date(),
    updatedAt: new Date(),
    isDraft: true
  }
  
  const createdScore = scoreStore.createScore(newScore)
  navigateToScore(createdScore.id)
}

// 创建节日主题乐谱
const createFestivalScore = (title: string) => {
  const newScore: Score = {
    id: Date.now().toString(),
    title,
    composer: userStore.user?.username || '',
    tempo: 100,
    timeSignature: { numerator: 4, denominator: 4 },
    keySignature: 'F',
    notes: [
      {
        id: '1',
        type: 'quarter',
        pitch: 'C4',
        duration: 1,
        position: { x: 100, y: 150 }
      },
      {
        id: '2',
        type: 'quarter',
        pitch: 'D4',
        duration: 1,
        position: { x: 150, y: 150 }
      },
      {
        id: '3',
        type: 'quarter',
        pitch: 'E4',
        duration: 1,
        position: { x: 200, y: 150 }
      },
      {
        id: '4',
        type: 'quarter',
        pitch: 'F4',
        duration: 1,
        position: { x: 250, y: 150 }
      }
    ],
    measures: [],
    createdAt: new Date(),
    updatedAt: new Date(),
    isDraft: true,
    tags: ['春节', '马年', '节日']
  }
  
  const createdScore = scoreStore.createScore(newScore)
  navigateToScore(createdScore.id)
}

// 导入文件
const importFromFile = () => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '.musicxml,.xml,.mxl,.mid,.midi'
  input.onchange = async () => {
    const file = input.files?.[0]
    if (!file) return
    await handleFileImport(file)
  }
  input.click()
}

// 处理文件导入
const handleFileImport = async (file: File) => {
  try {
    loading.value = true
    const importedScore = await scoreStore.importScoreFile(file)
    navigateToScore(importedScore.id)
  } catch (err) {
    console.error('导入失败:', err)
    const message = err instanceof Error ? err.message : '导入文件时发生错误'
    error.value = `导入失败：${message}`
  } finally {
    loading.value = false
  }
}

// 使用模板
const useTemplate = () => {
  showTemplateModal.value = true
}

// 应用模板
const applyTemplate = (template: any) => {
  const newScore: Score = {
    id: Date.now().toString(),
    title: template.name,
    composer: userStore.user?.username || '',
    tempo: template.tempo || 120,
    timeSignature: template.timeSignature || { numerator: 4, denominator: 4 },
    keySignature: template.keySignature || 'C',
    notes: template.notes || [],
    measures: [],
    createdAt: new Date(),
    updatedAt: new Date(),
    isDraft: true
  }
  
  const createdScore = scoreStore.createScore(newScore)
  navigateToScore(createdScore.id)
  showTemplateModal.value = false
}

// 打开最近乐谱
const openRecentScores = () => {
  showRecentDrawer.value = true
}

// 加载选中的乐谱
const loadSelectedScore = (scoreId: string) => {
  navigateToScore(scoreId)
  showRecentDrawer.value = false
}

// 参加新春创作大赛
const joinFestivalContest = () => {
  router.push('/contests/festival')
}

// 保存处理
const handleSave = async () => {
  if (!currentScore.value) return

  try {
    currentScore.value.isDraft = false
    currentScore.value.updatedAt = new Date()
    const saved = await saveScoreOnServer(currentScore.value)
    const scoreIndex = scoreStore.scores.findIndex(item => item.id === saved.id)
    if (scoreIndex === -1) {
      scoreStore.scores.push(saved)
    } else {
      scoreStore.scores[scoreIndex] = saved
    }
    scoreStore.currentScore = saved
    hasUnsavedChanges.value = false
    showSaveSuccess()
  } catch (err) {
    console.error('保存乐谱失败:', err)
    const message = err instanceof Error ? err.message : '保存乐谱失败'
    error.value = message
  }
}

// 导出处理
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
  }
}

// 导出MIDI
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

// 导出PDF
const exportPDF = (score: Score) => {
  // 这里可以集成PDF生成库
  console.log('导出PDF:', score)
  alert('PDF导出功能开发中...')
}

// 导出MusicXML
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

// 关闭编辑器
const handleClose = () => {
  if (hasUnsavedChanges.value) {
    showSavePrompt.value = true
  } else {
    router.push('/')
  }
}

// 确认保存
const confirmSave = async () => {
  await handleSave()
  showSavePrompt.value = false
  router.push('/')
}

// 不保存
const discardChanges = () => {
  hasUnsavedChanges.value = false
  showSavePrompt.value = false
  router.push('/')
}

// 取消关闭
const cancelClose = () => {
  showSavePrompt.value = false
}

// 导航到乐谱
const navigateToScore = (id: string) => {
  router.push(`/editor/${id}`)
}

// 显示保存成功提示
const showSaveSuccess = () => {
  const successEl = document.createElement('div')
  successEl.className = 'save-success-toast'
  successEl.textContent = '✓ 保存成功'
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

/* 加载状态 */
.loading-container {
  @apply flex items-center justify-center h-full bg-gray-50;
}

.loading-content {
  @apply flex flex-col items-center;
}

.loading-spinner {
  @apply w-12 h-12 border-4 border-blue-500 border-t-transparent 
         rounded-full animate-spin;
}

.loading-text {
  @apply mt-4 text-gray-600;
}

/* 错误状态 */
.error-container {
  @apply flex items-center justify-center h-full bg-gray-50;
}

.error-content {
  @apply text-center max-w-md p-8 bg-white rounded-xl shadow-lg;
}

.error-icon {
  @apply text-6xl mb-4;
}

.error-title {
  @apply text-xl font-semibold text-gray-800 mb-2;
}

.error-message {
  @apply text-gray-600 mb-6;
}

.error-btn {
  @apply px-6 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 
         transition-colors mr-3;
}

.error-link {
  @apply px-6 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 
         transition-colors;
}

/* 空状态 */
.empty-editor {
  @apply flex items-center justify-center h-full bg-gray-50 p-8;
}

.empty-content {
  @apply text-center max-w-4xl;
}

.empty-icon {
  @apply text-8xl mb-6;
}

.empty-title {
  @apply text-3xl font-bold text-gray-800 mb-2;
}

.empty-description {
  @apply text-gray-600 text-lg mb-8;
}

/* 快速开始选项 */
.quick-start-options {
  @apply grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-12;
}

.quick-start-card {
  @apply p-6 bg-white rounded-xl border border-gray-200 
         hover:border-blue-300 hover:shadow-lg transition-all 
         cursor-pointer;
}

.quick-start-card:hover .card-icon {
  @apply transform scale-110;
}

.card-icon {
  @apply text-4xl mb-3 transition-transform;
}

.card-title {
  @apply text-lg font-semibold text-gray-800 mb-1;
}

.card-desc {
  @apply text-sm text-gray-600;
}

/* 节日特辑 */
.festival-section {
  @apply bg-gradient-to-r from-yellow-50 to-orange-50 border 
         border-yellow-200 rounded-xl p-6;
}

.festival-header {
  @apply flex items-center justify-center mb-4;
}

.festival-icon {
  @apply text-3xl mr-3;
}

.festival-title {
  @apply text-xl font-semibold text-yellow-800;
}

.festival-options {
  @apply flex flex-wrap justify-center gap-3;
}

.festival-btn {
  @apply flex items-center space-x-2 px-4 py-2 bg-white border 
         border-yellow-300 rounded-lg hover:bg-yellow-50 
         transition-colors;
}

.btn-icon {
  @apply text-xl;
}

/* 编辑器容器 */
.editor-container {
  @apply h-full bg-gray-50;
}

/* 保存成功提示样式 */
:deep(.save-success-toast) {
  @apply fixed top-4 right-4 bg-green-500 text-white px-4 py-2 
         rounded-lg shadow-lg transform translate-x-full 
         transition-transform duration-300 z-50;
}

:deep(.save-success-toast.show) {
  @apply translate-x-0;
}
</style>
