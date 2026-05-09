<template>
  <div class="home-page">
    <!-- 节日横幅 -->
    <div class="festival-banner mb-8">
      <div class="bg-gradient-to-r from-red-500 to-orange-500 rounded-xl p-6 text-white">
        <h2 class="text-2xl font-bold mb-2">🎵 新春特辑：马年音乐创作大赛</h2>
        <p class="opacity-90">创作属于你的马年主题音乐，赢取丰厚奖品！</p>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- 快速开始 -->
      <div class="lg:col-span-2">
        <div class="bg-white rounded-xl shadow p-6">
          <h3 class="text-xl font-semibold mb-4">快速开始</h3>
          <div class="space-y-4">
            <button 
              @click="createNewScore"
              class="w-full py-3 px-4 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition"
            >
              新建乐谱
            </button>
            <button 
              @click="importScore"
              class="w-full py-3 px-4 border border-gray-300 rounded-lg hover:bg-gray-50 transition"
            >
              导入乐谱
            </button>
          </div>
        </div>
      </div>

      <!-- 最近乐谱 -->
      <div class="lg:col-span-1">
        <div class="bg-white rounded-xl shadow p-6">
          <h3 class="text-xl font-semibold mb-4">最近编辑</h3>
          <div class="space-y-3">
            <div 
              v-for="score in recentScores"
              :key="score.id"
              class="p-3 border rounded-lg hover:bg-gray-50 cursor-pointer"
              @click="openScore(score)"
            >
              <div class="font-medium">{{ score.title }}</div>
              <div class="text-sm text-gray-500">{{ formatDate(score.updatedAt) }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 模板库 -->
    <div class="mt-8">
      <h3 class="text-xl font-semibold mb-4">节日模板</h3>
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div 
          v-for="template in festivalTemplates"
          :key="template.id"
          class="bg-white rounded-lg shadow p-4 hover:shadow-lg transition cursor-pointer"
          @click="useTemplate(template)"
        >
          <div class="text-lg font-medium mb-2">{{ template.title }}</div>
          <div class="text-sm text-gray-600">{{ template.description }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useScoreStore } from '@/stores/scoreStore'
import type { Score } from '@/types/score'

const router = useRouter()
const scoreStore = useScoreStore()

const recentScores = computed(() => scoreStore.recentScores)

const festivalTemplates = [
  {
    id: '1',
    title: '马年新春',
    description: '马年主题节日音乐模板',
    timeSignature: { numerator: 4, denominator: 4 },
    keySignature: 'F'
  },
  {
    id: '2',
    title: '春节序曲',
    description: '传统春节音乐改编模板',
    timeSignature: { numerator: 2, denominator: 4 },
    keySignature: 'C'
  }
]

const buildNewScore = (title: string): Score => ({
  id: Date.now().toString(),
  title,
  composer: '',
  tempo: 120,
  timeSignature: { numerator: 4, denominator: 4 },
  keySignature: 'C',
  notes: [],
  measures: [],
  createdAt: new Date(),
  updatedAt: new Date(),
  isDraft: true
})

const createNewScore = () => {
  router.push('/editor')
}

const importScore = () => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '.musicxml,.xml,.mxl,.mid,.midi'
  input.onchange = async () => {
    const file = input.files?.[0]
    if (!file) return
    try {
      const importedScore = await scoreStore.importScoreFile(file)
      router.push(`/editor/${importedScore.id}`)
    } catch (error) {
      console.error('导入失败:', error)
      const message = error instanceof Error ? error.message : '导入文件时发生错误'
      alert(`导入失败：${message}`)
    }
  }
  input.click()
}

const openScore = (score: any) => {
  router.push(`/editor/${score.id}`)
}

const useTemplate = (template: any) => {
  const score = scoreStore.createScore(buildNewScore(template.title))
  score.timeSignature = template.timeSignature
  score.keySignature = template.keySignature
  router.push(`/editor/${score.id}`)
}

const formatDate = (date: Date) => {
  return date.toLocaleDateString('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped>
.home-page {
  max-width: 1180px;
  margin: 0 auto;
  padding: 22px 14px 40px;
}

.festival-banner > div {
  border: 1px solid #bfdbfe;
  border-radius: 16px;
  background: linear-gradient(135deg, #eff6ff 0%, #ecfeff 100%) !important;
  color: #0f172a !important;
  box-shadow: var(--shadow-card);
}

.festival-banner p {
  color: #334155;
}

.bg-white.rounded-xl,
.bg-white.rounded-lg {
  border: 1px solid #e2e8f0;
  box-shadow: none !important;
}

.bg-white.rounded-xl {
  border-radius: 14px;
}

.bg-white.rounded-lg {
  border-radius: 12px;
}

h3 {
  color: #0f172a;
}

button.bg-blue-600 {
  background: #0284c7 !important;
  border-radius: 10px;
  font-weight: 600;
}

button.bg-blue-600:hover {
  background: #0369a1 !important;
}

button.border {
  border-color: #cbd5e1 !important;
  background: #e2e8f0;
  color: #0f172a;
  border-radius: 10px;
  font-weight: 600;
}

button.border:hover {
  background: #cbd5e1 !important;
  border-color: #cbd5e1 !important;
}

.cursor-pointer:hover {
  background: #f0fdfa !important;
  border-color: #99f6e4;
  box-shadow: 0 8px 20px rgba(15, 118, 110, 0.08) !important;
}
</style>
