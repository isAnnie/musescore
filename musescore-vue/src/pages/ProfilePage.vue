<template>
  <div class="container mx-auto px-4 py-8">
    <h1 class="text-3xl font-bold text-gray-800 mb-6">我的乐谱</h1>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <div class="lg:col-span-1">
        <div class="bg-white rounded-lg shadow-md p-6">
          <div class="text-center">
            <div class="mx-auto h-20 w-20 rounded-full bg-gray-300 flex items-center justify-center mb-4">
              <span class="text-2xl font-bold text-gray-600">{{ userInitial }}</span>
            </div>
            <h2 class="text-xl font-semibold text-gray-800">{{ userStore.user?.username || '未登录用户' }}</h2>
            <p class="text-gray-600 mt-1">{{ userStore.user?.email || '-' }}</p>
          </div>

          <div class="mt-6 space-y-3">
            <div class="flex justify-between">
              <span class="text-gray-600">乐谱数量</span>
              <span class="font-medium">{{ myScores.length }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-gray-600">草稿数量</span>
              <span class="font-medium">{{ draftCount }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-gray-600">最近更新</span>
              <span class="font-medium">{{ latestUpdatedAt }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="lg:col-span-2">
        <div class="bg-white rounded-lg shadow-md p-6">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-xl font-semibold text-gray-800">我的乐谱列表</h3>
            <button
              @click="refreshScores"
              class="px-3 py-1 text-sm bg-gray-100 text-gray-700 rounded hover:bg-gray-200 transition-colors"
            >
              刷新
            </button>
          </div>

          <p v-if="errorMessage" class="text-sm text-red-500 mb-4">{{ errorMessage }}</p>
          <p v-if="loading" class="text-gray-500">加载中...</p>

          <div v-else-if="myScores.length === 0" class="text-gray-500 py-10 text-center">
            暂无乐谱，点击下方按钮创建第一份乐谱
          </div>

          <div v-else class="space-y-4">
            <div
              v-for="score in myScores"
              :key="score.id"
              class="flex items-center justify-between p-4 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
            >
              <div class="min-w-0">
                <h4 class="font-medium text-gray-800 truncate">{{ score.title }}</h4>
                <p class="text-sm text-gray-600 mt-1">
                  {{ score.composer || '未知作曲' }} · {{ score.isDraft ? '草稿' : '已发布' }}
                </p>
                <p class="text-xs text-gray-500 mt-1">更新于 {{ formatDate(score.updatedAt) }}</p>
              </div>
              <div class="flex space-x-2 ml-4">
                <button
                  @click="editScore(score.id)"
                  class="px-3 py-1 text-sm bg-blue-100 text-blue-600 rounded hover:bg-blue-200 transition-colors"
                >
                  编辑
                </button>
                <button
                  @click="removeScore(score.id)"
                  class="px-3 py-1 text-sm bg-red-100 text-red-600 rounded hover:bg-red-200 transition-colors"
                >
                  删除
                </button>
              </div>
            </div>
          </div>

          <button
            @click="createNewScore"
            class="mt-6 w-full py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center justify-center"
          >
            <span class="mr-2">+</span>
            创建新乐谱
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/userStore'
import type { Score } from '@/types/score'
import { deleteScoreOnServer, listMyScores } from '@/services/scoreApi'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const errorMessage = ref('')
const myScores = ref<Score[]>([])

const userInitial = computed(() => {
  const username = userStore.user?.username || ''
  return username ? username.slice(0, 1).toUpperCase() : 'U'
})

const draftCount = computed(() => myScores.value.filter(item => item.isDraft).length)

const latestUpdatedAt = computed(() => {
  if (!myScores.value.length) return '-'
  const latest = [...myScores.value].sort((a, b) => b.updatedAt.getTime() - a.updatedAt.getTime())[0]
  return formatDate(latest.updatedAt)
})

const formatDate = (date: Date) => {
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const refreshScores = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    myScores.value = await listMyScores()
  } catch (error) {
    console.error('获取我的乐谱失败:', error)
    errorMessage.value = error instanceof Error ? error.message : '获取我的乐谱失败'
  } finally {
    loading.value = false
  }
}

const editScore = (id: string) => {
  router.push(`/editor/${id}`)
}

const removeScore = async (id: string) => {
  try {
    await deleteScoreOnServer(id)
    myScores.value = myScores.value.filter(score => score.id !== id)
  } catch (error) {
    console.error('删除乐谱失败:', error)
    errorMessage.value = error instanceof Error ? error.message : '删除乐谱失败'
  }
}

const createNewScore = () => {
  router.push('/editor')
}

onMounted(async () => {
  await refreshScores()
})
</script>
