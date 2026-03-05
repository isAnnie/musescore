<template>
  <div class="browse-page">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <div class="search-container">
        <div class="search-input-wrapper">
          <input
            v-model="searchQuery"
            type="text"
            placeholder="搜索乐谱、作曲家或标签..."
            class="search-input"
            @keyup.enter="performSearch"
          />
          <button class="search-btn" @click="performSearch">
            🔍
          </button>
        </div>
        
        <div class="search-filters">
          <select v-model="selectedCategory" class="filter-select">
            <option value="">所有分类</option>
            <option v-for="category in categories" :key="category.id" :value="category.id">
              {{ category.name }}
            </option>
          </select>
          
          <select v-model="selectedSort" class="filter-select">
            <option value="recent">最新</option>
            <option value="popular">热门</option>
            <option value="rating">评分</option>
            <option value="downloads">下载量</option>
          </select>
          
          <button class="filter-btn" @click="toggleAdvancedFilters">
            高级筛选
          </button>
        </div>
      </div>
      
      <!-- 高级筛选 -->
      <div v-if="showAdvancedFilters" class="advanced-filters">
        <div class="filter-grid">
          <div class="filter-group">
            <label class="filter-label">调号</label>
            <select v-model="filters.keySignature" class="filter-select">
              <option value="">全部</option>
              <option v-for="key in keySignatures" :key="key" :value="key">
                {{ key }}
              </option>
            </select>
          </div>
          
          <div class="filter-group">
            <label class="filter-label">拍号</label>
            <div class="time-signature-filters">
              <select v-model="filters.timeSignature.numerator" class="filter-select">
                <option value="">任意</option>
                <option v-for="n in [2,3,4,6]" :key="n" :value="n">{{ n }}</option>
              </select>
              <span>/</span>
              <select v-model="filters.timeSignature.denominator" class="filter-select">
                <option value="">任意</option>
                <option v-for="d in [2,4,8]" :key="d" :value="d">{{ d }}</option>
              </select>
            </div>
          </div>
          
          <div class="filter-group">
            <label class="filter-label">难度</label>
            <select v-model="filters.difficulty" class="filter-select">
              <option value="">全部</option>
              <option value="easy">简单</option>
              <option value="medium">中等</option>
              <option value="hard">困难</option>
            </select>
          </div>
          
          <div class="filter-group">
            <label class="filter-label">乐器</label>
            <select v-model="filters.instrument" class="filter-select">
              <option value="">全部</option>
              <option v-for="instrument in instruments" :key="instrument" :value="instrument">
                {{ instrument }}
              </option>
            </select>
          </div>
        </div>
        
        <div class="filter-actions">
          <button class="filter-reset" @click="resetFilters">
            重置筛选
          </button>
          <button class="filter-apply" @click="applyFilters">
            应用筛选
          </button>
        </div>
      </div>
    </div>

    <!-- 主要内容 -->
    <div class="main-content">
      <div class="content-header">
        <h1 class="page-title">浏览乐谱</h1>
        <div class="result-count">
          找到 {{ filteredScores.length }} 个结果
        </div>
      </div>

      <!-- 筛选标签 -->
      <div v-if="activeFilters.length > 0" class="active-filters">
        <div class="filter-tags">
          <span 
            v-for="filter in activeFilters"
            :key="filter.key"
            class="filter-tag"
          >
            {{ filter.label }}
            <button @click="removeFilter(filter.key)" class="filter-remove">×</button>
          </span>
        </div>
        <button @click="clearAllFilters" class="clear-all-btn">
          清除所有筛选
        </button>
      </div>

      <!-- 乐谱网格 -->
      <div class="scores-grid">
        <div 
          v-for="score in paginatedScores"
          :key="score.id"
          class="score-card"
        >
          <router-link :to="`/score/${score.id}`" class="score-link">
            <div class="score-image">
              <div class="score-preview">
                <!-- 乐谱预览图 -->
                <div class="staff-preview">
                  <div v-for="i in 5" :key="i" class="staff-line"></div>
                  <div 
                    v-for="note in score.previewNotes"
                    :key="note.id"
                    class="preview-note"
                    :style="note.style"
                  >
                    {{ note.symbol }}
                  </div>
                </div>
              </div>
              <div v-if="score.featured" class="featured-badge">
                🔥 热门
              </div>
            </div>
            
            <div class="score-info">
              <h3 class="score-title">{{ score.title }}</h3>
              <p class="score-composer">{{ score.composer }}</p>
              
              <div class="score-meta">
                <div class="meta-item">
                  <span class="meta-icon">🎹</span>
                  <span class="meta-text">{{ score.instrument }}</span>
                </div>
                <div class="meta-item">
                  <span class="meta-icon">⏱️</span>
                  <span class="meta-text">{{ score.duration }}</span>
                </div>
                <div class="meta-item">
                  <span class="meta-icon">🎵</span>
                  <span class="meta-text">{{ score.notesCount }} 音符</span>
                </div>
              </div>
              
              <div class="score-stats">
                <div class="stat">
                  <span class="stat-icon">👁️</span>
                  <span class="stat-value">{{ score.views }}</span>
                </div>
                <div class="stat">
                  <span class="stat-icon">💖</span>
                  <span class="stat-value">{{ score.likes }}</span>
                </div>
                <div class="stat">
                  <span class="stat-icon">💾</span>
                  <span class="stat-value">{{ score.downloads }}</span>
                </div>
              </div>
              
              <div class="score-tags">
                <span 
                  v-for="tag in score.tags"
                  :key="tag"
                  class="tag"
                >
                  {{ tag }}
                </span>
              </div>
            </div>
          </router-link>
          
          <div class="score-actions">
            <button class="action-btn" @click="playPreview(score)" title="预览">
              ▶️
            </button>
            <button class="action-btn" @click="downloadScore(score)" title="下载">
              ⬇️
            </button>
            <button class="action-btn" @click="likeScore(score)" title="喜欢">
              {{ score.liked ? '❤️' : '🤍' }}
            </button>
            <button class="action-btn" @click="bookmarkScore(score)" title="收藏">
              {{ score.bookmarked ? '⭐' : '☆' }}
            </button>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="filteredScores.length === 0" class="empty-state">
        <div class="empty-icon">🎵</div>
        <h3 class="empty-title">未找到乐谱</h3>
        <p class="empty-desc">尝试调整筛选条件或搜索关键词</p>
        <button class="empty-btn" @click="resetFilters">
          重置筛选条件
        </button>
      </div>

      <!-- 分页 -->
      <div v-if="filteredScores.length > itemsPerPage" class="pagination">
        <button 
          class="pagination-btn"
          :disabled="currentPage === 1"
          @click="prevPage"
        >
          上一页
        </button>
        
        <div class="pagination-pages">
          <button
            v-for="page in visiblePages"
            :key="page"
            class="pagination-page"
            :class="{ active: page === currentPage }"
            @click="goToPage(page)"
          >
            {{ page }}
          </button>
        </div>
        
        <button 
          class="pagination-btn"
          :disabled="currentPage === totalPages"
          @click="nextPage"
        >
          下一页
        </button>
      </div>
    </div>

    <!-- 侧边栏 -->
    <aside class="sidebar">
      <!-- 热门分类 -->
      <div class="sidebar-section">
        <h3 class="sidebar-title">热门分类</h3>
        <div class="category-list">
          <button
            v-for="category in popularCategories"
            :key="category.id"
            class="category-btn"
            :class="{ active: selectedCategory === category.id }"
            @click="selectCategory(category.id)"
          >
            <span class="category-icon">{{ category.icon }}</span>
            <span class="category-name">{{ category.name }}</span>
            <span class="category-count">{{ category.count }}</span>
          </button>
        </div>
      </div>

      <!-- 热门标签 -->
      <div class="sidebar-section">
        <h3 class="sidebar-title">热门标签</h3>
        <div class="tag-cloud">
          <span
            v-for="tag in popularTags"
            :key="tag.name"
            class="tag-cloud-item"
            :style="{ fontSize: tag.size }"
            @click="addTagFilter(tag.name)"
          >
            {{ tag.name }}
          </span>
        </div>
      </div>

      <!-- 排行榜 -->
      <div class="sidebar-section">
        <h3 class="sidebar-title">排行榜</h3>
        <div class="leaderboard">
          <div 
            v-for="(score, index) in topScores"
            :key="score.id"
            class="leaderboard-item"
          >
            <div class="leaderboard-rank">
              <span class="rank-number">{{ index + 1 }}</span>
            </div>
            <div class="leaderboard-info">
              <h4 class="leaderboard-title">{{ score.title }}</h4>
              <p class="leaderboard-composer">{{ score.composer }}</p>
            </div>
            <div class="leaderboard-stats">
              <span class="stat-icon">👁️</span>
              <span class="stat-value">{{ score.views }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 节日推荐 -->
      <div v-if="showFestivalSection" class="sidebar-section festival-section">
        <div class="festival-header">
          <span class="festival-icon">🎆</span>
          <h3 class="festival-title">新春推荐</h3>
        </div>
        <div class="festival-scores">
          <div 
            v-for="score in festivalScores"
            :key="score.id"
            class="festival-score"
            @click="viewFestivalScore(score)"
          >
            <div class="festival-score-preview">🐎</div>
            <div class="festival-score-info">
              <h4>{{ score.title }}</h4>
              <p>{{ score.description }}</p>
            </div>
          </div>
        </div>
      </div>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// 搜索和筛选状态
const searchQuery = ref('')
const selectedCategory = ref('')
const selectedSort = ref('recent')
const showAdvancedFilters = ref(false)
const currentPage = ref(1)
const itemsPerPage = 12

// 筛选器
const filters = ref({
  keySignature: '',
  timeSignature: { numerator: '', denominator: '' },
  difficulty: '',
  instrument: '',
  tags: [] as string[]
})

// 分类数据
const categories = [
  { id: 'classical', name: '古典音乐', icon: '🎼' },
  { id: 'pop', name: '流行音乐', icon: '🎤' },
  { id: 'jazz', name: '爵士乐', icon: '🎷' },
  { id: 'rock', name: '摇滚乐', icon: '🎸' },
  { id: 'folk', name: '民谣', icon: '🎵' },
  { id: 'festival', name: '节日音乐', icon: '🎆' },
  { id: 'piano', name: '钢琴曲', icon: '🎹' },
  { id: 'guitar', name: '吉他谱', icon: '🎸' }
]

// 调号选项
const keySignatures = ['C', 'G', 'D', 'A', 'E', 'B', 'F#', 'C#', 'F', 'Bb', 'Eb', 'Ab', 'Db', 'Gb', 'Cb']

// 乐器选项
const instruments = ['钢琴', '吉他', '小提琴', '长笛', '萨克斯', '鼓', '贝斯', '人声']

// 示例乐谱数据
const scores = ref<any[]>([
  {
    id: '1',
    title: '新春序曲',
    composer: '传统音乐',
    instrument: '钢琴',
    duration: '3:45',
    notesCount: 156,
    views: 1234,
    likes: 89,
    downloads: 45,
    featured: true,
    tags: ['春节', '马年', '传统'],
    previewNotes: [
      { id: 1, symbol: '𝅝', style: 'top: 20px; left: 30px;' },
      { id: 2, symbol: '𝅝', style: 'top: 20px; left: 100px;' }
    ],
    liked: false,
    bookmarked: false
  },
  // ... 更多示例数据
])

// 计算属性
const filteredScores = computed(() => {
  let result = [...scores.value]
  
  // 搜索过滤
  if (searchQuery.value.trim()) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter(score => 
      score.title.toLowerCase().includes(query) ||
      score.composer.toLowerCase().includes(query) ||
      score.tags.some((tag: string) => tag.toLowerCase().includes(query))
    )
  }
  
  // 分类过滤
  if (selectedCategory.value) {
    result = result.filter(score => 
      score.tags.includes(selectedCategory.value) ||
      score.category === selectedCategory.value
    )
  }
  
  // 调号过滤
  if (filters.value.keySignature) {
    result = result.filter(score => score.keySignature === filters.value.keySignature)
  }
  
  // 难度过滤
  if (filters.value.difficulty) {
    result = result.filter(score => score.difficulty === filters.value.difficulty)
  }
  
  // 乐器过滤
  if (filters.value.instrument) {
    result = result.filter(score => score.instrument === filters.value.instrument)
  }
  
  // 标签过滤
  if (filters.value.tags.length > 0) {
    result = result.filter(score => 
      filters.value.tags.every(tag => score.tags.includes(tag))
    )
  }
  
  // 排序
  switch (selectedSort.value) {
    case 'recent':
      result.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
      break
    case 'popular':
      result.sort((a, b) => b.views - a.views)
      break
    case 'rating':
      result.sort((a, b) => b.rating - a.rating)
      break
    case 'downloads':
      result.sort((a, b) => b.downloads - a.downloads)
      break
  }
  
  return result
})

const paginatedScores = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage
  const end = start + itemsPerPage
  return filteredScores.value.slice(start, end)
})

const totalPages = computed(() => {
  return Math.ceil(filteredScores.value.length / itemsPerPage)
})

const visiblePages = computed(() => {
  const pages: number[] = []
  const maxVisible = 5
  const half = Math.floor(maxVisible / 2)
  
  let start = Math.max(1, currentPage.value - half)
  let end = Math.min(totalPages.value, start + maxVisible - 1)
  
  if (end - start + 1 < maxVisible) {
    start = Math.max(1, end - maxVisible + 1)
  }
  
  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  
  return pages
})

const activeFilters = computed(() => {
  const filters: any[] = []
  
  if (selectedCategory.value) {
    const category = categories.find(c => c.id === selectedCategory.value)
    if (category) {
      filters.push({
        key: 'category',
        label: category.name
      })
    }
  }
  
  if (searchQuery.value.trim()) {
    filters.push({
      key: 'search',
      label: `搜索: ${searchQuery.value}`
    })
  }
  
  return filters
})

// 热门数据
const popularCategories = computed(() => {
  return categories.slice(0, 6).map(cat => ({
    ...cat,
    count: scores.value.filter(s => s.tags.includes(cat.id)).length
  }))
})

const popularTags = computed(() => {
  const tagMap = new Map()
  scores.value.forEach(score => {
    score.tags.forEach((tag: string) => {
      tagMap.set(tag, (tagMap.get(tag) || 0) + 1)
    })
  })
  
  return Array.from(tagMap.entries())
    .sort((a, b) => b[1] - a[1])
    .slice(0, 20)
    .map(([name, count]) => ({
      name,
      count,
      size: `${Math.min(20 + count * 2, 32)}px`
    }))
})

const topScores = computed(() => {
  return [...scores.value]
    .sort((a, b) => b.views - a.views)
    .slice(0, 5)
})

// 节日数据
const showFestivalSection = ref(true) // 根据时间判断
const festivalScores = computed(() => {
  return scores.value
    .filter(score => score.tags.includes('春节') || score.tags.includes('马年'))
    .slice(0, 3)
})

// 方法
const performSearch = () => {
  console.log('搜索:', searchQuery.value)
  currentPage.value = 1
}

const toggleAdvancedFilters = () => {
  showAdvancedFilters.value = !showAdvancedFilters.value
}

const resetFilters = () => {
  searchQuery.value = ''
  selectedCategory.value = ''
  selectedSort.value = 'recent'
  filters.value = {
    keySignature: '',
    timeSignature: { numerator: '', denominator: '' },
    difficulty: '',
    instrument: '',
    tags: []
  }
  currentPage.value = 1
}

const applyFilters = () => {
  currentPage.value = 1
  showAdvancedFilters.value = false
}

const selectCategory = (categoryId: string) => {
  selectedCategory.value = selectedCategory.value === categoryId ? '' : categoryId
  currentPage.value = 1
}

const addTagFilter = (tag: string) => {
  if (!filters.value.tags.includes(tag)) {
    filters.value.tags.push(tag)
  }
}

const removeFilter = (filterKey: string) => {
  if (filterKey === 'category') {
    selectedCategory.value = ''
  } else if (filterKey === 'search') {
    searchQuery.value = ''
  }
}

const clearAllFilters = () => {
  resetFilters()
}

const playPreview = (score: any) => {
  console.log('播放预览:', score.title)
}

const downloadScore = (score: any) => {
  console.log('下载乐谱:', score.title)
}

const likeScore = (score: any) => {
  score.liked = !score.liked
  score.likes += score.liked ? 1 : -1
}

const bookmarkScore = (score: any) => {
  score.bookmarked = !score.bookmarked
}

const viewFestivalScore = (score: any) => {
  router.push(`/score/${score.id}`)
}

const prevPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
  }
}

const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
  }
}

const goToPage = (page: number) => {
  currentPage.value = page
}

onMounted(() => {
  // 加载更多数据
  console.log('浏览页面加载')
})
</script>

<style scoped>
.browse-page {
  @apply min-h-screen bg-gray-50;
}

/* 搜索栏 */
.search-bar {
  @apply bg-white border-b shadow-sm;
}

.search-container {
  @apply max-w-7xl mx-auto px-4 py-6;
}

.search-input-wrapper {
  @apply relative mb-4;
}

.search-input {
  @apply w-full pl-4 pr-12 py-3 text-lg border rounded-xl 
         focus:outline-none focus:ring-2 focus:ring-blue-500 
         focus:border-transparent;
}

.search-btn {
  @apply absolute right-3 top-1/2 transform -translate-y-1/2 
         p-2 text-gray-500 hover:text-gray-700;
}

.search-filters {
  @apply flex items-center space-x-3;
}

.filter-select {
  @apply px-3 py-2 border rounded-lg focus:outline-none 
         focus:ring-2 focus:ring-blue-500 focus:border-transparent;
}

.filter-btn {
  @apply px-4 py-2 border rounded-lg hover:bg-gray-50 
         transition-colors;
}

/* 高级筛选 */
.advanced-filters {
  @apply border-t mt-4 pt-4;
}

.filter-grid {
  @apply grid grid-cols-1 md:grid-cols-4 gap-4;
}

.filter-group {
  @apply space-y-2;
}

.filter-label {
  @apply block text-sm font-medium text-gray-700;
}

.time-signature-filters {
  @apply flex items-center space-x-2;
}

.filter-actions {
  @apply flex justify-end space-x-3 mt-4;
}

.filter-reset {
  @apply px-4 py-2 border rounded-lg hover:bg-gray-50 
         transition-colors;
}

.filter-apply {
  @apply px-4 py-2 bg-blue-500 text-white rounded-lg 
         hover:bg-blue-600 transition-colors;
}

/* 主要内容 */
.main-content {
  @apply max-w-7xl mx-auto px-4 py-8;
}

.content-header {
  @apply flex justify-between items-center mb-6;
}

.page-title {
  @apply text-3xl font-bold text-gray-800;
}

.result-count {
  @apply text-gray-600;
}

/* 筛选标签 */
.active-filters {
  @apply mb-6 p-4 bg-blue-50 rounded-lg;
}

.filter-tags {
  @apply flex flex-wrap gap-2 mb-3;
}

.filter-tag {
  @apply inline-flex items-center px-3 py-1 bg-blue-100 
         text-blue-700 rounded-full text-sm;
}

.filter-remove {
  @apply ml-2 text-lg leading-none hover:text-blue-900;
}

.clear-all-btn {
  @apply text-sm text-blue-600 hover:text-blue-800;
}

/* 乐谱网格 */
.scores-grid {
  @apply grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6;
}

.score-card {
  @apply bg-white border rounded-xl overflow-hidden hover:shadow-lg 
         transition-all;
}

.score-link {
  @apply block;
}

.score-image {
  @apply relative h-48 bg-gradient-to-br from-blue-50 to-indigo-50 
         flex items-center justify-center p-4;
}

.score-preview {
  @apply w-full h-full bg-white rounded p-4;
}

.staff-preview {
  @apply relative w-full h-full;
}

.staff-line {
  @apply absolute left-4 right-4 h-px bg-gray-300;
}

.staff-line:nth-child(1) { top: 25%; }
.staff-line:nth-child(2) { top: 40%; }
.staff-line:nth-child(3) { top: 55%; }
.staff-line:nth-child(4) { top: 70%; }
.staff-line:nth-child(5) { top: 85%; }

.preview-note {
  @apply absolute text-xl;
}

.featured-badge {
  @apply absolute top-2 right-2 bg-red-500 text-white 
         px-2 py-1 rounded text-xs font-medium;
}

.score-info {
  @apply p-4;
}

.score-title {
  @apply text-lg font-semibold text-gray-800 mb-1 truncate;
}

.score-composer {
  @apply text-gray-600 text-sm mb-3 truncate;
}

.score-meta {
  @apply flex flex-wrap gap-2 mb-3;
}

.meta-item {
  @apply flex items-center space-x-1 text-sm text-gray-500;
}

.meta-icon {
  @apply text-xs;
}

.score-stats {
  @apply flex justify-between mb-3;
}

.stat {
  @apply flex items-center space-x-1;
}

.stat-icon {
  @apply text-sm;
}

.stat-value {
  @apply text-sm font-medium;
}

.score-tags {
  @apply flex flex-wrap gap-1;
}

.tag {
  @apply px-2 py-1 bg-gray-100 text-gray-600 text-xs rounded;
}

.score-actions {
  @apply flex justify-between p-4 border-t;
}

.action-btn {
  @apply p-2 hover:bg-gray-100 rounded transition-colors;
}

/* 空状态 */
.empty-state {
  @apply text-center py-12;
}

.empty-icon {
  @apply text-6xl mb-4;
}

.empty-title {
  @apply text-xl font-semibold text-gray-800 mb-2;
}

.empty-desc {
  @apply text-gray-600 mb-4;
}

.empty-btn {
  @apply px-6 py-2 bg-blue-500 text-white rounded-lg 
         hover:bg-blue-600 transition-colors;
}

/* 分页 */
.pagination {
  @apply flex items-center justify-between mt-8 pt-8 border-t;
}

.pagination-btn {
  @apply px-4 py-2 border rounded-lg hover:bg-gray-50 
         transition-colors disabled:opacity-50 disabled:cursor-not-allowed;
}

.pagination-pages {
  @apply flex items-center space-x-2;
}

.pagination-page {
  @apply w-10 h-10 flex items-center justify-center border 
         rounded-lg hover:bg-gray-50 transition-colors;
}

.pagination-page.active {
  @apply bg-blue-500 text-white border-blue-500 hover:bg-blue-600;
}

/* 侧边栏 */
.sidebar {
  @apply lg:col-span-1 space-y-6;
}

.sidebar-section {
  @apply bg-white border rounded-xl p-4;
}

.sidebar-title {
  @apply text-lg font-semibold text-gray-800 mb-4;
}

/* 分类列表 */
.category-list {
  @apply space-y-2;
}

.category-btn {
  @apply flex items-center justify-between w-full p-2 rounded-lg 
         hover:bg-gray-50 transition-colors;
}

.category-btn.active {
  @apply bg-blue-50 text-blue-600;
}

.category-icon {
  @apply text-lg;
}

.category-name {
  @apply flex-1 ml-3 text-left;
}

.category-count {
  @apply text-gray-500 text-sm;
}

/* 标签云 */
.tag-cloud {
  @apply flex flex-wrap gap-2;
}

.tag-cloud-item {
  @apply cursor-pointer text-gray-600 hover:text-blue-600 
         transition-colors;
}

/* 排行榜 */
.leaderboard {
  @apply space-y-3;
}

.leaderboard-item {
  @apply flex items-center space-x-3 p-2 rounded-lg hover:bg-gray-50 
         cursor-pointer transition-colors;
}

.leaderboard-rank {
  @apply w-8 h-8 flex items-center justify-center bg-gray-100 
         rounded-lg font-medium;
}

.leaderboard-info {
  @apply flex-1;
}

.leaderboard-title {
  @apply font-medium text-gray-800 truncate;
}

.leaderboard-composer {
  @apply text-sm text-gray-600 truncate;
}

.leaderboard-stats {
  @apply flex items-center space-x-1;
}

/* 节日推荐 */
.festival-section {
  @apply bg-gradient-to-br from-yellow-50 to-orange-50 border-yellow-200;
}

.festival-header {
  @apply flex items-center mb-4;
}

.festival-icon {
  @apply text-2xl mr-2;
}

.festival-title {
  @apply text-lg font-semibold text-yellow-800;
}

.festival-scores {
  @apply space-y-3;
}

.festival-score {
  @apply flex items-center space-x-3 p-3 bg-white bg-opacity-50 
         rounded-lg cursor-pointer hover:bg-white transition-colors;
}

.festival-score-preview {
  @apply text-2xl;
}

.festival-score-info h4 {
  @apply font-medium text-gray-800 truncate;
}

.festival-score-info p {
  @apply text-sm text-gray-600 truncate;
}

/* 响应式布局 */
@media (min-width: 1024px) {
  .browse-page {
    @apply grid grid-cols-4 gap-8;
  }
  
  .main-content {
    @apply col-span-3;
  }
  
  .sidebar {
    @apply col-span-1;
  }
}
</style>