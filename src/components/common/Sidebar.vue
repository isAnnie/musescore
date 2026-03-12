<template>
  <aside class="sidebar">
    <!-- 用户信息卡片 -->
    <div class="user-card" v-if="userStore.isLoggedIn">
      <div class="user-avatar">
        <User class="w-10 h-10" v-if="!userStore.user?.avatar" />
        <img v-else :src="userStore.user.avatar" :alt="userStore.user.username" />
      </div>
      <div class="user-info">
        <div class="user-name">{{ userStore.user?.username }}</div>
        <div class="user-role">{{ getUserRole() }}</div>
      </div>
      <div class="user-stats">
        <div class="stat-item">
          <div class="stat-value">{{ userStore.user?.scores?.length || 0 }}</div>
          <div class="stat-label">乐谱</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ followers }}</div>
          <div class="stat-label">粉丝</div>
        </div>
      </div>
    </div>

    <!-- 主要导航菜单 -->
    <nav class="nav-menu">
      <div class="menu-section">
        <h3 class="menu-title">创作</h3>
        <router-link to="/editor" class="menu-item" :class="{ active: isActive('/editor') }">
          <Edit class="w-5 h-5" />
          <span>乐谱编辑器</span>
          <span v-if="draftCount > 0" class="badge">{{ draftCount }}</span>
        </router-link>
        <router-link to="/my-scores" class="menu-item" :class="{ active: isActive('/my-scores') }">
          <FileMusic class="w-5 h-5" />
          <span>我的乐谱</span>
        </router-link>
        <!-- <router-link to="/arrangements" class="menu-item" :class="{ active: isActive('/arrangements') }">
          <Layers class="w-5 h-5" />
          <span>我的编曲</span>
        </router-link> -->
        <!-- <router-link to="/templates" class="menu-item" :class="{ active: isActive('/templates') }">
          <LayoutTemplate class="w-5 h-5" />
          <span>模板库</span>
        </router-link> -->
      </div>

      <div class="menu-section">
        <h3 class="menu-title">发现</h3>
        <!-- <router-link to="/discover" class="menu-item" :class="{ active: isActive('/discover') }">
          <Compass class="w-5 h-5" />
          <span>发现</span>
        </router-link> -->
        <router-link to="/featured" class="menu-item" :class="{ active: isActive('/featured') }">
          <Star class="w-5 h-5" />
          <span>精选乐谱</span>
        </router-link>
        <router-link to="/trending" class="menu-item" :class="{ active: isActive('/trending') }">
          <TrendingUp class="w-5 h-5" />
          <span>热门趋势</span>
        </router-link>
        <router-link to="/collections" class="menu-item" :class="{ active: isActive('/collections') }">
          <Bookmark class="w-5 h-5" />
          <span>精选集</span>
        </router-link>
      </div>

      <div class="menu-section">
        <h3 class="menu-title">社区</h3>
        <router-link to="/groups" class="menu-item" :class="{ active: isActive('/groups') }">
          <Users class="w-5 h-5" />
          <span>兴趣小组</span>
          <span v-if="groupNotifications > 0" class="badge">{{ groupNotifications }}</span>
        </router-link>
        <router-link to="/forums" class="menu-item" :class="{ active: isActive('/forums') }">
          <MessageSquare class="w-5 h-5" />
          <span>论坛</span>
        </router-link>
        <router-link to="/contests" class="menu-item" :class="{ active: isActive('/contests') }">
          <Award class="w-5 h-5" />
          <span>创作大赛</span>
          <span v-if="hasActiveContest" class="badge badge-live">进行中</span>
        </router-link>
        <router-link to="/collaborations" class="menu-item" :class="{ active: isActive('/collaborations') }">
          <GitBranch class="w-5 h-5" />
          <span>协作项目</span>
        </router-link>
      </div>

      <div class="menu-section">
        <h3 class="menu-title">工具</h3>
        <router-link to="/tuner" class="menu-item" :class="{ active: isActive('/tuner') }">
          <Music class="w-5 h-5" />
          <span>调音器</span>
        </router-link>
        <router-link to="/metronome" class="menu-item" :class="{ active: isActive('/metronome') }">
          <Activity class="w-5 h-5" />
          <span>节拍器</span>
        </router-link>
        <router-link to="/chord-chart" class="menu-item" :class="{ active: isActive('/chord-chart') }">
          <Hash class="w-5 h-5" />
          <span>和弦表</span>
        </router-link>
        <router-link to="/transpose" class="menu-item" :class="{ active: isActive('/transpose') }">
          <Repeat class="w-5 h-5" />
          <span>移调工具</span>
        </router-link>
      </div>

      <!-- 节日特辑 -->
      <div class="festival-section" v-if="showFestivalSection">
        <div class="festival-header">
          <Sparkles class="w-5 h-5 text-yellow-500" />
          <h3 class="festival-title">新春特辑</h3>
        </div>
        <router-link to="/festival/music" class="menu-item festival-item">
          <div class="festival-icon">🧧</div>
          <span>马年主题音乐</span>
        </router-link>
        <router-link to="/festival/scores" class="menu-item festival-item">
          <div class="festival-icon">🎼</div>
          <span>春节乐谱集</span>
        </router-link>
        <router-link to="/festival/contest" class="menu-item festival-item">
          <div class="festival-icon">🏆</div>
          <span>新春创作大赛</span>
          <span class="badge badge-live">火热进行</span>
        </router-link>
      </div>
    </nav>

    <!-- 快速操作 -->
    <div class="quick-actions">
      <h3 class="quick-actions-title">快速操作</h3>
      <button class="quick-action-btn" @click="createNewScore">
        <Plus class="w-5 h-5" />
        <span>新建乐谱</span>
      </button>
      <button class="quick-action-btn" @click="importScore">
        <Upload class="w-5 h-5" />
        <span>导入乐谱</span>
      </button>
      <button class="quick-action-btn" @click="printScore">
        <Printer class="w-5 h-5" />
        <span>打印乐谱</span>
      </button>
    </div>

    <!-- 最近访问 -->
    <div class="recent-visited" v-if="recentVisited.length > 0">
      <div class="recent-header">
        <History class="w-5 h-5" />
        <h3 class="recent-title">最近访问</h3>
        <button @click="clearRecent" class="clear-btn">
          <X class="w-4 h-4" />
        </button>
      </div>
      <div class="recent-list">
        <div 
          v-for="item in recentVisited"
          :key="item.id"
          class="recent-item"
          @click="visitRecent(item)"
        >
          <div class="recent-icon">
            <FileMusic class="w-4 h-4" />
          </div>
          <div class="recent-content">
            <div class="recent-name">{{ item.name }}</div>
            <div class="recent-time">{{ formatTime(item.time) }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部帮助 -->
    <div class="sidebar-footer">
      <a href="#" class="footer-link">
        <HelpCircle class="w-4 h-4" />
        <span>帮助中心</span>
      </a>
      <a href="#" class="footer-link">
        <Settings class="w-4 h-4" />
        <span>设置</span>
      </a>
      <div class="theme-toggle">
        <button @click="toggleTheme" class="theme-btn">
          <Moon v-if="theme === 'light'" class="w-4 h-4" />
          <Sun v-else class="w-4 h-4" />
          <span>{{ theme === 'light' ? '深色模式' : 'ǳɫģʽ' }}</span>
        </button>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/userStore'
import { useScoreStore } from '@/stores/scoreStore'
import {
  User,
  Edit,
  FileMusic,
  Layers,
  LayoutTemplate,
  Compass,
  Star,
  TrendingUp,
  Bookmark,
  Users,
  MessageSquare,
  Award,
  GitBranch,
  Music,
  Activity,
  Hash,
  Repeat,
  Plus,
  Upload,
  Printer,
  History,
  X,
  HelpCircle,
  Settings,
  Moon,
  Sun,
  Sparkles
} from 'lucide-vue-next'

const router = useRouter()
const userStore = useUserStore()
const scoreStore = useScoreStore()

// 主题状态
const theme = ref<'light' | 'dark'>(localStorage.getItem('theme') as 'light' | 'dark' || 'light')

// 最近访问记录
const recentVisited = ref([
  { id: 1, name: '新春序曲', type: 'score', time: new Date(Date.now() - 3600000) },
  { id: 2, name: '马年吉祥', type: 'score', time: new Date(Date.now() - 7200000) },
  { id: 3, name: '春节快乐合奏', type: 'arrangement', time: new Date(Date.now() - 86400000) },
  { id: 4, name: '古典音乐模板', type: 'template', time: new Date(Date.now() - 172800000) },
])

// 统计数据
const followers = ref(128)
const draftCount = computed(() => userStore.user?.scores?.filter(s => s.isDraft).length || 0)
const groupNotifications = ref(3)
const hasActiveContest = ref(true)

// 判断当前是否显示节日特辑
const showFestivalSection = computed(() => {
  const now = new Date()
  const newYearStart = new Date('2026-02-17') // 春节开始
  const newYearEnd = new Date('2026-03-03')   // 元宵节结束
  return now >= newYearStart && now <= newYearEnd
})

// 用户角色判断
const getUserRole = () => {
  const scoresCount = userStore.user?.scores?.length || 0
  if (scoresCount >= 50) return '资深作曲家'
  if (scoresCount >= 20) return '活跃创作者'
  if (scoresCount >= 5) return '初级创作者'
  return '新晋用户'
}

// 路径匹配
const isActive = (path: string) => {
  return router.currentRoute.value.path.startsWith(path)
}

// 快速操作函数
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

const printScore = () => {
  // 实现打印乐谱逻辑
  console.log('打印乐谱')
}

// 最近访问操作
const clearRecent = () => {
  recentVisited.value = []
  localStorage.removeItem('recentVisited')
}

const visitRecent = (item: any) => {
  switch (item.type) {
    case 'score':
      router.push(`/editor/${item.id}`)
      break
    case 'arrangement':
      router.push(`/arrangement/${item.id}`)
      break
    case 'template':
      router.push(`/template/${item.id}`)
      break
  }
}

// 格式化时间显示
const formatTime = (date: Date) => {
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))

  if (hours < 1) return '刚刚'
  if (hours < 24) return `${hours}Сʱǰ`
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString('zh-CN')
}

// 主题切换
const toggleTheme = () => {
  theme.value = theme.value === 'light' ? 'dark' : 'light'
  localStorage.setItem('theme', theme.value)
  document.documentElement.classList.toggle('dark', theme.value === 'dark')
}

// 初始化
onMounted(() => {
  // 从localStorage加载最近访问记录
  const saved = localStorage.getItem('recentVisited')
  if (saved) {
    recentVisited.value = JSON.parse(saved).map((item: any) => ({
      ...item,
      time: new Date(item.time)
    }))
  }
  
  // 应用主题
  document.documentElement.classList.toggle('dark', theme.value === 'dark')
})
</script>

<style scoped>
.sidebar {
  @apply w-64 bg-white border-r h-full overflow-y-auto 
         flex flex-col space-y-6 p-4;
  min-height: calc(100vh - 64px); /* 减去Header高度 */
}

/* 用户卡片样式 */
.user-card {
  @apply bg-gradient-to-r from-blue-50 to-indigo-50 rounded-xl p-4 
         border border-blue-100;
}

.user-avatar {
  @apply w-16 h-16 rounded-full bg-gradient-to-r from-blue-500 to-purple-600 
         flex items-center justify-center text-white mx-auto mb-3;
}

.user-info {
  @apply text-center mb-4;
}

.user-name {
  @apply font-semibold text-gray-800 text-lg;
}

.user-role {
  @apply text-sm text-gray-600 mt-1;
}

.user-stats {
  @apply flex justify-around border-t pt-3;
}

.stat-item {
  @apply text-center;
}

.stat-value {
  @apply font-bold text-gray-800 text-lg;
}

.stat-label {
  @apply text-xs text-gray-500 mt-1;
}

/* 菜单样式 */
.menu-section {
  @apply mb-6;
}

.menu-title {
  @apply text-xs uppercase text-gray-500 font-semibold mb-2 px-2;
}

.menu-item {
  @apply flex items-center space-x-3 px-3 py-2 rounded-lg 
         text-gray-700 hover:bg-gray-100 hover:text-blue-600 
         transition-colors mb-1 relative;
}

.menu-item.active {
  @apply bg-blue-50 text-blue-600 font-medium;
}

.menu-item svg {
  @apply text-gray-400;
}

.menu-item.active svg {
  @apply text-blue-500;
}

/* 徽章样式 */
.badge {
  @apply absolute right-3 px-2 py-1 bg-blue-100 text-blue-600 
         text-xs rounded-full;
}

.badge-live {
  @apply bg-red-100 text-red-600;
}

/* 节日特辑 */
.festival-section {
  @apply bg-gradient-to-r from-yellow-50 to-orange-50 border border-yellow-200 
         rounded-xl p-4 mb-6;
}

.festival-header {
  @apply flex items-center space-x-2 mb-3;
}

.festival-title {
  @apply font-semibold text-yellow-800;
}

.festival-item {
  @apply bg-white bg-opacity-50 border border-yellow-200 
         hover:bg-yellow-50 hover:border-yellow-300;
}

.festival-icon {
  @apply text-xl;
}

/* 快速操作 */
.quick-actions {
  @apply border border-gray-200 rounded-xl p-4 mb-6;
}

.quick-actions-title {
  @apply font-medium text-gray-700 mb-3;
}

.quick-action-btn {
  @apply w-full flex items-center space-x-3 px-3 py-2 rounded-lg 
         bg-gray-50 hover:bg-gray-100 text-gray-700 mb-2 
         transition-colors;
}

/* 最近访问 */
.recent-visited {
  @apply border border-gray-200 rounded-xl p-4;
}

.recent-header {
  @apply flex items-center justify-between mb-3;
}

.recent-title {
  @apply font-medium text-gray-700 flex-1 ml-2;
}

.clear-btn {
  @apply p-1 rounded hover:bg-gray-100 transition-colors;
}

.recent-list {
  @apply space-y-2;
}

.recent-item {
  @apply flex items-center space-x-3 p-2 rounded-lg 
         hover:bg-gray-50 cursor-pointer transition-colors;
}

.recent-icon {
  @apply w-8 h-8 bg-gray-100 rounded-lg flex items-center 
         justify-center text-gray-600;
}

.recent-content {
  @apply flex-1;
}

.recent-name {
  @apply text-sm font-medium text-gray-800 truncate;
}

.recent-time {
  @apply text-xs text-gray-500;
}

/* 侧边栏底部 */
.sidebar-footer {
  @apply border-t pt-4 mt-auto space-y-2;
}

.footer-link {
  @apply flex items-center space-x-2 px-2 py-1.5 text-gray-600 
         hover:text-blue-600 hover:bg-gray-50 rounded transition-colors;
}

.theme-toggle {
  @apply pt-2 mt-2 border-t;
}

.theme-btn {
  @apply flex items-center space-x-2 px-2 py-1.5 text-gray-600 
         hover:text-blue-600 hover:bg-gray-50 rounded transition-colors 
         w-full;
}

/* 暗色模式支持 */
.dark .sidebar {
  @apply bg-gray-900 border-gray-800;
}

.dark .user-card {
  @apply from-gray-800 to-gray-900 border-gray-700;
}

.dark .menu-title {
  @apply text-gray-400;
}

.dark .menu-item {
  @apply text-gray-300 hover:bg-gray-800 hover:text-blue-400;
}

.dark .menu-item.active {
  @apply bg-blue-900 bg-opacity-30 text-blue-400;
}

.dark .festival-section {
  @apply from-yellow-900 to-orange-900 border-yellow-800;
}

.dark .festival-title {
  @apply text-yellow-200;
}

.dark .festival-item {
  @apply bg-gray-800 bg-opacity-50 border-yellow-800 hover:bg-yellow-900;
}
</style>
