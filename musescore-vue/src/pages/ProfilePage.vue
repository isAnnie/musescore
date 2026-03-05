<template>
  <div class="container mx-auto px-4 py-8">
    <h1 class="text-3xl font-bold text-gray-800 mb-6">个人中心</h1>
    
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- 用户信息卡片 -->
      <div class="lg:col-span-1">
        <div class="bg-white rounded-lg shadow-md p-6">
          <div class="text-center">
            <div class="mx-auto h-20 w-20 rounded-full bg-gray-300 flex items-center justify-center mb-4">
              <span class="text-2xl font-bold text-gray-600">U</span>
            </div>
            <h2 class="text-xl font-semibold text-gray-800">用户名</h2>
            <p class="text-gray-600 mt-1">user@example.com</p>
          </div>
          
          <div class="mt-6 space-y-3">
            <div class="flex justify-between">
              <span class="text-gray-600">创建的乐谱</span>
              <span class="font-medium">12</span>
            </div>
            <div class="flex justify-between">
              <span class="text-gray-600">收藏的乐谱</span>
              <span class="font-medium">24</span>
            </div>
            <div class="flex justify-between">
              <span class="text-gray-600">关注者</span>
              <span class="font-medium">45</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 内容区域 -->
      <div class="lg:col-span-2">
        <div class="bg-white rounded-lg shadow-md p-6">
          <h3 class="text-xl font-semibold text-gray-800 mb-4">我的乐谱</h3>
          
          <div class="space-y-4">
            <div 
              v-for="score in userScores" 
              :key="score.id"
              class="flex items-center justify-between p-4 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
            >
              <div>
                <h4 class="font-medium text-gray-800">{{ score.title }}</h4>
                <p class="text-sm text-gray-600 mt-1">{{ score.lastModified }}</p>
              </div>
              <div class="flex space-x-2">
                <button 
                  @click="editScore(score.id)"
                  class="px-3 py-1 text-sm bg-blue-100 text-blue-600 rounded hover:bg-blue-200 transition-colors"
                >
                  编辑
                </button>
                <button 
                  @click="deleteScore(score.id)"
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
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

interface UserScore {
  id: number
  title: string
  lastModified: string
}

const userScores = ref<UserScore[]>([
  {
    id: 1,
    title: '我的第一首曲子',
    lastModified: '2024-01-15'
  },
  {
    id: 2,
    title: '练习曲',
    lastModified: '2024-01-10'
  },
  {
    id: 3,
    title: '原创作品',
    lastModified: '2024-01-05'
  }
])

const editScore = (id: number) => {
  router.push(`/editor/${id}`)
}

const deleteScore = (id: number) => {
  userScores.value = userScores.value.filter(score => score.id !== id)
}

const createNewScore = () => {
  router.push('/editor')
}
</script>

<style scoped>
/* ProfilePage 样式 */
</style>