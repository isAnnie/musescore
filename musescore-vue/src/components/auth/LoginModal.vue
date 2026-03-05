<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-content">
      <div class="modal-header">
        <h2 class="modal-title">{{ isRegisterMode ? '注册 MuseScore Vue' : '登录 MuseScore Vue' }}</h2>
        <button @click="$emit('close')" class="modal-close">
          <X class="w-5 h-5" />
        </button>
      </div>

      <div class="modal-body">
        <form @submit.prevent="handleSubmit" class="space-y-4">
          <div>
            <label class="form-label">用户名或邮箱</label>
            <input
              v-model="form.account"
              type="text"
              required
              class="form-input"
              placeholder="请输入用户名或邮箱"
            />
          </div>

          <div v-if="isRegisterMode">
            <label class="form-label">邮箱</label>
            <input
              v-model="form.email"
              type="email"
              required
              class="form-input"
              placeholder="请输入邮箱"
            />
          </div>

          <div>
            <label class="form-label">密码</label>
            <input
              v-model="form.password"
              type="password"
              required
              class="form-input"
              placeholder="请输入密码"
            />
          </div>

          <div v-if="!isRegisterMode" class="flex items-center justify-between">
            <label class="flex items-center">
              <input type="checkbox" v-model="form.remember" class="mr-2" />
              <span class="text-sm">记住我</span>
            </label>
            <a href="#" class="text-sm text-blue-500 hover:text-blue-600">
              忘记密码？
            </a>
          </div>

          <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

          <button
            type="submit"
            :disabled="loading"
            class="btn-login"
          >
            <span v-if="!loading">{{ isRegisterMode ? '注册' : '登录' }}</span>
            <span v-else>{{ isRegisterMode ? '注册中...' : '登录中...' }}</span>
          </button>
        </form>

        <div class="register-link">
          <span>{{ isRegisterMode ? '已有账号？' : '还没有账号？' }}</span>
          <button type="button" class="mode-switch-btn" @click="toggleMode">
            {{ isRegisterMode ? '去登录' : '立即注册' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useUserStore } from '@/stores/userStore'
import { X } from 'lucide-vue-next'

const emit = defineEmits(['close', 'login-success'])

const userStore = useUserStore()
const loading = ref(false)
const isRegisterMode = ref(false)
const errorMessage = ref('')

const form = ref({
  account: '',
  email: '',
  password: '',
  remember: false
})

const toggleMode = () => {
  isRegisterMode.value = !isRegisterMode.value
  errorMessage.value = ''
}

const handleSubmit = async () => {
  if (!form.value.account || !form.value.password) return
  if (isRegisterMode.value && !form.value.email) return

  loading.value = true
  errorMessage.value = ''
  try {
    if (isRegisterMode.value) {
      await userStore.register(form.value.account, form.value.email, form.value.password)
    } else {
      await userStore.login(form.value.account, form.value.password)
    }
    emit('login-success')
    emit('close')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '操作失败，请重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.modal-overlay {
  @apply fixed inset-0 bg-black bg-opacity-50 flex items-center 
         justify-center z-50;
}

.modal-content {
  @apply bg-white rounded-xl w-full max-w-md;
}

.modal-header {
  @apply flex items-center justify-between p-6 border-b;
}

.modal-title {
  @apply text-xl font-semibold text-gray-800;
}

.modal-close {
  @apply p-2 rounded-full hover:bg-gray-100 transition-colors;
}

.modal-body {
  @apply p-6;
}

.form-label {
  @apply block text-sm font-medium text-gray-700 mb-1;
}

.form-input {
  @apply w-full px-3 py-2 border rounded-lg focus:outline-none 
         focus:ring-2 focus:ring-blue-500 focus:border-transparent;
}

.btn-login {
  @apply w-full py-3 bg-blue-500 text-white rounded-lg 
         hover:bg-blue-600 transition-colors font-medium
         disabled:opacity-50 disabled:cursor-not-allowed;
}

.error-text {
  @apply text-sm text-red-500;
}

.register-link {
  @apply mt-6 text-center text-gray-600 space-x-2;
}

.mode-switch-btn {
  @apply text-blue-500 hover:text-blue-600 font-medium;
}
</style>
