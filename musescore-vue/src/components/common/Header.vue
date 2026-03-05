<template>
  <header class="header">
    <!-- 顶部栏 -->
    <div class="top-bar bg-gray-900 text-white text-sm">
      <div class="container mx-auto px-4">
        <div class="flex justify-between items-center py-2">
          <!-- 左 -->
          <div class="flex items-center space-x-4">
            <a href="#" class="hover:text-blue-300">下载桌面版</a>
            <a href="#" class="hover:text-blue-300">乐谱商店</a>
            <a href="#" class="hover:text-blue-300">帮助中心</a>
          </div>
          
          <!-- 右 -->
          <div class="flex items-center space-x-4">
            <button class="hover:text-blue-300" @click="toggleTheme">
              <Moon class="w-4 h-4" v-if="theme === 'light'" />
              <Sun class="w-4 h-4" v-else />
            </button>
            <a href="#" class="hover:text-blue-300">语言</a>
            <span class="text-gray-400">|</span>
            <button 
              v-if="!userStore.isLoggedIn"
              @click="showLoginModal = true"
              class="hover:text-blue-300"
            >
              登录/注册
            </button>
            <div v-else class="flex items-center space-x-2">
              <div
                class="relative user-dropdown-container"
                @mouseenter="handleUserMenuMouseEnter"
                @mouseleave="handleUserMenuMouseLeave"
              >
                <button class="flex items-center space-x-2 hover:text-blue-300" @click.stop="toggleUserMenu">
                  <User class="w-4 h-4" />
                  <span>{{ userStore.user?.username }}</span>
                  <ChevronDown class="w-3 h-3" />
                </button>
                <div v-if="showUserMenu" class="user-dropdown">
                  <router-link to="/profile" class="dropdown-item" @click="showUserMenu = false">
                    <User class="w-4 h-4" />个人资料
                  </router-link>
                  <router-link to="/my-scores" class="dropdown-item" @click="showUserMenu = false">
                    <Music class="w-4 h-4" />我的乐谱
                  </router-link>
                  <router-link to="/settings" class="dropdown-item" @click="showUserMenu = false">
                    <Settings class="w-4 h-4" />设置
                  </router-link>
                  <hr class="my-1">
                  <button @click="logout" class="dropdown-item text-red-500">
                    <LogOut class="w-4 h-4" />退出登录
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 主导航栏 -->
    <div class="main-nav bg-white border-b">
      <div class="container mx-auto px-4">
        <div class="flex items-center justify-between py-3">
          <!-- Logo和品牌 -->
          <div class="flex items-center space-x-8">
            <router-link to="/" class="flex items-center space-x-2">
              <div class="logo-icon w-8 h-8 bg-gradient-to-r from-blue-500 to-purple-600 rounded-lg flex items-center justify-center">
                <Music class="w-5 h-5 text-white" />
              </div>
              <span class="text-2xl font-bold text-gray-800">MuseScore Vue</span>
            </router-link>

            <!-- 搜索框 -->
            <div class="search-box">
              <Search class="search-icon" />
              <input 
                type="text" 
                v-model="searchQuery"
                placeholder="搜索乐谱、用户或标签..."
                @keyup.enter="search"
                class="search-input"
              />
              <div v-if="searchSuggestions.length > 0" class="search-suggestions">
                <div 
                  v-for="suggestion in searchSuggestions"
                  :key="suggestion.id"
                  class="suggestion-item"
                  @click="selectSuggestion(suggestion)"
                >
                  <div class="flex items-center">
                    <div class="suggestion-type">{{ suggestion.type }}</div>
                    <div>
                      <div class="suggestion-title">{{ suggestion.title }}</div>
                      <div class="suggestion-subtitle">{{ suggestion.subtitle }}</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 主要操作按钮 -->
          <div class="flex items-center space-x-4">
            <!-- 新建乐谱按钮 -->
            <router-link 
              to="/editor"
              class="btn-primary flex items-center space-x-2"
            >
              <Plus class="w-5 h-5" />
              <span>新建乐谱</span>
            </router-link>

            <!-- 通知 -->
            <div class="relative notifications-container">
              <button 
                class="icon-btn"
                @click="toggleNotifications"
              >
                <Bell class="w-5 h-5" />
                <span v-if="unreadCount > 0" class="notification-badge">
                  {{ unreadCount > 9 ? '9+' : unreadCount }}
                </span>
              </button>
              <div v-if="showNotifications" class="notifications-panel">
                <div class="notifications-header">
                  <h3>通知</h3>
                  <button @click="markAllAsRead" class="text-sm text-blue-500">
                    全部标记为已读
                  </button>
                </div>
                <div class="notifications-list">
                  <div 
                    v-for="notification in notifications"
                    :key="notification.id"
                    :class="['notification-item', { unread: !notification.read }]"
                  >
                    <div class="notification-avatar">
                      <User class="w-4 h-4" />
                    </div>
                    <div class="notification-content">
                      <div class="notification-text">{{ notification.message }}</div>
                      <div class="notification-time">{{ notification.time }}</div>
                    </div>
                  </div>
                </div>
                <div class="notifications-footer">
                  <router-link to="/notifications" class="view-all">
                    查看所有通知
                  </router-link>
                </div>
              </div>
            </div>

            <!-- 上传按钮 -->
            <button class="icon-btn" @click="importScore">
              <Upload class="w-5 h-5" />
            </button>

            <!-- 更多菜单 -->
            <div class="relative more-menu-container">
              <button 
                class="icon-btn"
                @click="showMoreMenu = !showMoreMenu"
              >
                <MoreHorizontal class="w-5 h-5" />
              </button>
              <div v-if="showMoreMenu" class="more-menu">
                <router-link to="/features" class="menu-item">
                  <Zap class="w-4 h-4" /> 特色功能
                </router-link>
                <router-link to="/pricing" class="menu-item">
                  <DollarSign class="w-4 h-4" /> 定价
                </router-link>
                <router-link to="/blog" class="menu-item">
                  <BookOpen class="w-4 h-4" /> 博客
                </router-link>
                <router-link to="/forum" class="menu-item">
                  <MessageSquare class="w-4 h-4" /> 社区
                </router-link>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 二级导航 -->
    <div class="sub-nav bg-white border-b">
      <div class="container mx-auto px-4">
        <div class="flex space-x-6">
          <router-link 
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            :class="[
              'nav-item',
              { 'nav-item-active': $route.path === item.path }
            ]"
          >
            <component :is="item.icon" class="w-4 h-4 mr-2" />
            {{ item.label }}
          </router-link>
        </div>
      </div>
    </div>

    <!-- 登录模态框 -->
    <LoginModal 
      v-if="showLoginModal"
      @close="showLoginModal = false"
      @login-success="handleLoginSuccess"
    />
  </header>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/userStore'
import { useScoreStore } from '@/stores/scoreStore'
import LoginModal from '@/components/auth/LoginModal.vue'
import {
  Moon,
  Sun,
  User,
  ChevronDown,
  Music,
  Settings,
  LogOut,
  Search,
  Plus,
  Bell,
  Upload,
  MoreHorizontal,
  Home,
  Compass,
  Star,
  BookOpen,
  Zap,
  DollarSign,
  MessageSquare
} from 'lucide-vue-next'

const router = useRouter()
const userStore = useUserStore()
const scoreStore = useScoreStore()

// 主题切换
const theme = ref<'light' | 'dark'>('light')
const toggleTheme = () => {
  theme.value = theme.value === 'light' ? 'dark' : 'light'
  document.documentElement.classList.toggle('dark', theme.value === 'dark')
}

// 搜索功能
const searchQuery = ref('')
const searchSuggestions = ref<any[]>([])
const showSuggestions = ref(false)

const search = () => {
  if (searchQuery.value.trim()) {
    console.log('搜索:', searchQuery.value)
    // 实际应用中这里会调用搜索API
  }
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

// 通知功能
const showNotifications = ref(false)
const notifications = ref([
  { id: 1, message: '你的乐谱收到了新的评论', time: '2分钟前', read: false },
  { id: 2, message: '用户 "作曲家" 关注了你', time: '1小时前', read: true },
  { id: 3, message: '节日创作大赛已经开始', time: '昨天', read: false },
  { id: 4, message: '你的订阅即将到期', time: '3天前', read: true },
])

const unreadCount = computed(() => {
  return notifications.value.filter(n => !n.read).length
})

const toggleNotifications = () => {
  showNotifications.value = !showNotifications.value
}

const markAllAsRead = () => {
  notifications.value = notifications.value.map(n => ({ ...n, read: true }))
}

// 更多菜单
const showMoreMenu = ref(false)
const showUserMenu = ref(false)
let userMenuCloseTimer: ReturnType<typeof setTimeout> | null = null

// 登录模态框
const showLoginModal = ref(false)

// 导航项
const navItems = [
  { path: '/', label: '首页', icon: Home },
  { path: '/browse', label: '浏览', icon: Compass },
  { path: '/featured', label: '精选乐谱', icon: Star },
  { path: '/my-scores', label: '我的乐谱', icon: BookOpen },
]

// 搜索建议数据
const searchSuggestionsData = [
  { id: 1, type: '乐谱', title: '春节序曲', subtitle: '传统节日音乐' },
  { id: 2, type: '用户', title: '音乐家小明', subtitle: '专业作曲家' },
  { id: 3, type: '标签', title: '#马年新春', subtitle: '节日主题' },
  { id: 4, type: '乐谱', title: '新春快乐', subtitle: '现代编曲' },
]

// 点击外部关闭下拉菜单
const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  if (!target.closest('.user-dropdown-container')) {
    showUserMenu.value = false
  }
  if (!target.closest('.more-menu-container')) {
    showMoreMenu.value = false
  }
  if (!target.closest('.notifications-container')) {
    showNotifications.value = false
  }
}

const toggleUserMenu = () => {
  showUserMenu.value = !showUserMenu.value
}

const handleUserMenuMouseEnter = () => {
  if (userMenuCloseTimer) {
    clearTimeout(userMenuCloseTimer)
    userMenuCloseTimer = null
  }
  showUserMenu.value = true
}

const handleUserMenuMouseLeave = () => {
  if (userMenuCloseTimer) {
    clearTimeout(userMenuCloseTimer)
  }
  userMenuCloseTimer = setTimeout(() => {
    showUserMenu.value = false
    userMenuCloseTimer = null
  }, 140)
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  
  // 初始化主题
  const savedTheme = localStorage.getItem('theme')
  if (savedTheme) {
    theme.value = savedTheme as 'light' | 'dark'
    document.documentElement.classList.toggle('dark', theme.value === 'dark')
  }
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  if (userMenuCloseTimer) {
    clearTimeout(userMenuCloseTimer)
    userMenuCloseTimer = null
  }
})

// 搜索建议
watch(searchQuery, (newVal) => {
  if (newVal.trim()) {
    searchSuggestions.value = searchSuggestionsData.filter(item =>
      item.title.toLowerCase().includes(newVal.toLowerCase()) ||
      item.subtitle.toLowerCase().includes(newVal.toLowerCase())
    )
    showSuggestions.value = true
  } else {
    searchSuggestions.value = []
    showSuggestions.value = false
  }
})

// 选择搜索建议
const selectSuggestion = (suggestion: any) => {
  searchQuery.value = suggestion.title
  showSuggestions.value = false
  search()
}

// 登录成功处理
const handleLoginSuccess = () => {
  showLoginModal.value = false
}

// 退出登录
const logout = () => {
  userStore.logout()
}
</script>

<style scoped>
.header {
  @apply sticky top-0 z-50;
}

.top-bar {
  @apply py-1 px-4;
}

.logo-icon {
  @apply bg-gradient-to-r from-blue-500 to-purple-600;
}

/* 搜索框样式 */
.search-box {
  @apply relative w-96;
}

.search-input {
  @apply w-full pl-10 pr-4 py-2 border rounded-lg 
         focus:outline-none focus:ring-2 focus:ring-blue-500 
         focus:border-transparent bg-gray-50;
}

.search-icon {
  @apply absolute left-3 top-1/2 transform -translate-y-1/2 
         text-gray-400 w-4 h-4;
}

.search-suggestions {
  @apply absolute top-full left-0 right-0 bg-white border 
         rounded-lg shadow-lg mt-1 z-50 max-h-96 overflow-y-auto;
}

.suggestion-item {
  @apply px-4 py-3 hover:bg-gray-50 cursor-pointer border-b 
         last:border-b-0 transition-colors;
}

.suggestion-type {
  @apply inline-block px-2 py-1 bg-blue-100 text-blue-700 
         text-xs rounded mr-3;
}

.suggestion-title {
  @apply font-medium text-gray-800;
}

.suggestion-subtitle {
  @apply text-sm text-gray-500;
}

/* 按钮样式 */
.btn-primary {
  @apply px-4 py-2 bg-blue-500 text-white rounded-lg 
         hover:bg-blue-600 transition-colors font-medium;
}

.icon-btn {
  @apply p-2 rounded-full hover:bg-gray-100 transition-colors 
         relative;
}

/* 通知样式 */
.notification-badge {
  @apply absolute -top-1 -right-1 bg-red-500 text-white 
         text-xs rounded-full w-5 h-5 flex items-center 
         justify-center;
}

.notifications-panel {
  @apply absolute right-0 top-full mt-2 w-80 bg-white 
         border rounded-lg shadow-lg z-50;
}

.notifications-header {
  @apply px-4 py-3 border-b flex justify-between items-center;
}

.notifications-header h3 {
  @apply font-semibold text-gray-800;
}

.notifications-list {
  @apply max-h-96 overflow-y-auto;
}

.notification-item {
  @apply px-4 py-3 border-b hover:bg-gray-50;
}

.notification-item.unread {
  @apply bg-blue-50;
}

.notification-avatar {
  @apply w-8 h-8 bg-blue-100 rounded-full flex items-center 
         justify-center text-blue-600 mr-3;
}

.notification-content {
  @apply flex-1;
}

.notification-text {
  @apply text-sm text-gray-800;
}

.notification-time {
  @apply text-xs text-gray-500 mt-1;
}

.notifications-footer {
  @apply px-4 py-3 border-t text-center;
}

.view-all {
  @apply text-blue-500 hover:text-blue-600 text-sm;
}

/* 更多菜单 */
.more-menu {
  @apply absolute right-0 top-full mt-2 w-48 bg-white border 
         rounded-lg shadow-lg z-50;
}

.menu-item {
  @apply flex items-center space-x-2 px-4 py-2 hover:bg-gray-50 
         transition-colors;
}

/* 用户下拉菜单 */
.user-dropdown {
  @apply absolute right-0 top-full mt-0 w-48 bg-white border text-gray-700
         rounded-lg shadow-lg z-50 py-2;
}

.dropdown-item {
  @apply flex items-center space-x-2 px-4 py-2 text-gray-700 hover:bg-gray-50 
         transition-colors text-sm;
}

/* 导航项样式 */
.nav-item {
  @apply flex items-center py-3 px-2 text-gray-600 hover:text-blue-500 
         border-b-2 border-transparent hover:border-blue-500 transition-colors;
}

.nav-item-active {
  @apply text-blue-600 border-blue-600 font-medium;
}

.sub-nav {
  @apply shadow-sm;
}
</style>
