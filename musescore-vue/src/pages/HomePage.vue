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
  const score = scoreStore.createScore(buildNewScore('未命名乐谱'))
  router.push(`/editor/${score.id}`)
}

const importScore = () => {
  // 实现导入功能
  console.log('导入乐谱')
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
