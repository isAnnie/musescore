<template>
  <div class="not-found-page flex items-center justify-center min-h-screen bg-gray-50">
    <div class="text-center max-w-md p-8">
      <div class="text-8xl mb-6">🎵</div>
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
</style>