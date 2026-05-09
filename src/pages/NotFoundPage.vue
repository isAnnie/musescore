<template>
  <div class="not-found-page flex items-center justify-center min-h-screen bg-gray-50">
    <div class="text-center max-w-md p-8">
      <div class="text-8xl mb-6">🎼</div>
      <h1 class="text-3xl font-bold text-gray-800 mb-4">页面未找到</h1>
      <p class="text-gray-600 mb-8">
        抱歉，您访问的页面不存在。可能是因为：
      </p>
      
      <ul class="text-left text-gray-600 mb-8 space-y-2">
        <li class="flex items-start">
          <span class="text-blue-500 mr-2">•</span>
          <span>页面已被移除或重命名</span>
        </li>
        <li class="flex items-start">
          <span class="text-blue-500 mr-2">•</span>
          <span>网址输入有误</span>
        </li>
        <li class="flex items-start">
          <span class="text-blue-500 mr-2">•</span>
          <span>该功能尚未实现</span>
        </li>
      </ul>
      
      <div class="space-y-3">
        <button 
          @click="goHome"
          class="w-full py-3 px-4 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition"
        >
          返回首页
        </button>
        <button 
          @click="goBack"
          class="w-full py-3 px-4 border border-gray-300 rounded-lg hover:bg-gray-50 transition"
        >
          返回上一页
        </button>
      </div>
      
      <div class="mt-8 pt-6 border-t border-gray-200">
        <p class="text-sm text-gray-500">
          需要帮助？<a href="#" class="text-blue-600 hover:underline">联系客服</a>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
  router.push('/')
}

const goBack = () => {
  router.go(-1)
}
</script>

<style scoped>
.not-found-page {
  background: linear-gradient(135deg, #eff6ff 0%, #ecfeff 100%);
  color: #0f172a;
}

.not-found-page > div {
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: var(--shadow-card);
}

.not-found-page button.bg-blue-600 {
  background: #0284c7 !important;
}

.not-found-page button.bg-blue-600:hover {
  background: #0369a1 !important;
}

.not-found-page .text-blue-500,
.not-found-page .text-blue-600 {
  color: #0284c7 !important;
}
</style>
