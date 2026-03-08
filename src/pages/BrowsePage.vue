<template>
  <div class="publish-page">
    <section class="hero">
      <div>
        <h1>乐谱发布中心</h1>
        <p>
          发布内容来源于你在乐谱编辑页保存到数据库的作品。你可以发布、下架、查看和复制公开乐谱。
        </p>
      </div>
      <div class="hero-actions">
        <input
          v-model.trim="searchQuery"
          type="text"
          placeholder="搜索标题、作曲者、标签"
          class="search-input"
        />
        <select v-model="sortBy" class="sort-select">
          <option value="updated">按最近更新</option>
          <option value="created">按创建时间</option>
          <option value="title">按标题排序</option>
        </select>
        <button class="btn btn-secondary" :disabled="loading" @click="loadData">
          {{ loading ? '加载中...' : '刷新' }}
        </button>
      </div>
    </section>

    <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>

    <section v-if="userStore.isLoggedIn" class="card">
      <div class="card-head">
        <h2>我的数据库乐谱（发布管理）</h2>
        <p>以下均为你已保存到数据库的乐谱，可一键发布或下架。</p>
      </div>

      <div v-if="myScores.length === 0" class="empty-hint">
        你还没有已保存的乐谱，请先前往编辑页创建并保存作品。
      </div>

      <div v-else class="score-card-grid">
        <article v-for="score in myScores" :key="score.id" class="score-card mine">
          <div>
            <p class="score-title">{{ score.title }}</p>
            <p class="score-meta">
              {{ score.composer || '未知作曲' }} · {{ formatDate(score.updatedAt) }}
            </p>
            <p class="score-meta">
              状态：{{ isPublished(score) ? '已发布（公开）' : '未发布（私有/草稿）' }}
            </p>
          </div>
          <div class="score-actions">
            <button class="btn btn-secondary" @click="viewOwnScore(score.id)">进入编辑</button>
            <button
              v-if="isPublished(score)"
              class="btn btn-warning"
              :disabled="actionLoadingId === score.id"
              @click="unpublishScore(score)"
            >
              {{ actionLoadingId === score.id ? '处理中...' : '下架' }}
            </button>
            <button
              v-else
              class="btn btn-primary"
              :disabled="actionLoadingId === score.id"
              @click="publishScore(score)"
            >
              {{ actionLoadingId === score.id ? '处理中...' : '发布' }}
            </button>
          </div>
        </article>
      </div>
    </section>

    <section class="card">
      <div class="card-head">
        <h2>公开乐谱广场（{{ filteredPublicScores.length }}）</h2>
        <p>查看公开乐谱；可复制他人作品为你的草稿并继续编辑。</p>
      </div>

      <div v-if="filteredPublicScores.length === 0" class="empty-hint">
        暂无可展示的公开乐谱。
      </div>

      <div v-else class="score-card-grid">
        <article v-for="score in filteredPublicScores" :key="score.id" class="score-card">
          <div>
            <p class="score-title">{{ score.title }}</p>
            <p class="score-meta">
              {{ score.composer || '未知作曲' }} · {{ score.tempo }} BPM · {{ score.keySignature }}
            </p>
            <p class="score-meta">
              音符：{{ score.notes.length }} · 小节：{{ score.measures.length }}
            </p>
            <p class="score-tags">{{ (score.tags || []).slice(0, 4).join('，') || '暂无标签' }}</p>
          </div>
          <div class="score-actions">
            <button class="btn btn-secondary" @click="previewPublicScore(score.id)">查看详情</button>
            <template v-if="isMyScore(score.id)">
              <button
                class="btn btn-warning"
                :disabled="actionLoadingId === score.id"
                @click="unpublishById(score.id)"
              >
                {{ actionLoadingId === score.id ? '处理中...' : '下架我的乐谱' }}
              </button>
            </template>
            <template v-else>
              <button
                class="btn btn-primary"
                :disabled="actionLoadingId === score.id"
                @click="copyScore(score.id)"
              >
                {{ actionLoadingId === score.id ? '复制中...' : '复制为我的草稿' }}
              </button>
            </template>
          </div>
        </article>
      </div>
    </section>

    <div v-if="previewScore" class="modal-mask" @click.self="previewScore = null">
      <div class="modal">
        <div class="modal-header">
          <h3>{{ previewScore.title }}</h3>
          <button class="close-btn" @click="previewScore = null">×</button>
        </div>
        <p class="score-meta">
          作曲：{{ previewScore.composer || '未知' }} · 速度：{{ previewScore.tempo }} ·
          节拍：{{ previewScore.timeSignature.numerator }}/{{ previewScore.timeSignature.denominator }} ·
          调号：{{ previewScore.keySignature }}
        </p>
        <p class="score-meta">更新时间：{{ formatDate(previewScore.updatedAt) }}</p>
        <p class="score-meta">简介：{{ previewScore.description || '暂无简介' }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/userStore'
import type { Score } from '@/types/score'
import {
  createScoreOnServer,
  getPublicScoreById,
  listMyScores,
  listPublicScores,
  updateScoreOnServer
} from '@/services/scoreApi'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const actionLoadingId = ref<string | null>(null)
const errorMessage = ref('')
const searchQuery = ref('')
const sortBy = ref<'updated' | 'created' | 'title'>('updated')
const myScores = ref<Score[]>([])
const publicScores = ref<Score[]>([])
const previewScore = ref<Score | null>(null)

const myIdSet = computed(() => new Set(myScores.value.map((score) => score.id)))
const isMyScore = (scoreId: string) => myIdSet.value.has(scoreId)
const isPublished = (score: Score) => !score.isDraft && (score.visibility ?? 'private') === 'public'

const filteredPublicScores = computed(() => {
  const query = searchQuery.value.toLowerCase()
  let result = [...publicScores.value]

  if (query) {
    result = result.filter((score) => {
      const tags = (score.tags || []).join(' ').toLowerCase()
      return (
        score.title.toLowerCase().includes(query) ||
        score.composer.toLowerCase().includes(query) ||
        tags.includes(query)
      )
    })
  }

  if (sortBy.value === 'title') {
    result.sort((a, b) => a.title.localeCompare(b.title))
  } else if (sortBy.value === 'created') {
    result.sort((a, b) => b.createdAt.getTime() - a.createdAt.getTime())
  } else {
    result.sort((a, b) => b.updatedAt.getTime() - a.updatedAt.getTime())
  }
  return result
})

const formatDate = (date: Date) => new Date(date).toLocaleString('zh-CN')

const loadData = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const [publicResult, myResult] = await Promise.all([
      listPublicScores(),
      userStore.isLoggedIn ? listMyScores() : Promise.resolve([] as Score[])
    ])
    publicScores.value = publicResult
    myScores.value = myResult
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载乐谱数据失败'
  } finally {
    loading.value = false
  }
}

const viewOwnScore = (id: string) => {
  router.push(`/editor/${id}`)
}

const previewPublicScore = async (id: string) => {
  try {
    previewScore.value = await getPublicScoreById(id)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载乐谱详情失败'
  }
}

const publishScore = async (score: Score) => {
  actionLoadingId.value = score.id
  errorMessage.value = ''
  try {
    await updateScoreOnServer({
      ...score,
      isDraft: false,
      visibility: 'public'
    })
    await loadData()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '发布失败'
  } finally {
    actionLoadingId.value = null
  }
}

const unpublishScore = async (score: Score) => {
  actionLoadingId.value = score.id
  errorMessage.value = ''
  try {
    await updateScoreOnServer({
      ...score,
      isDraft: true,
      visibility: 'private'
    })
    await loadData()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '下架失败'
  } finally {
    actionLoadingId.value = null
  }
}

const unpublishById = async (id: string) => {
  const target = myScores.value.find((item) => item.id === id)
  if (!target) {
    errorMessage.value = '未找到该乐谱的归属记录'
    return
  }
  await unpublishScore(target)
}

const copyScore = async (id: string) => {
  if (!userStore.isLoggedIn) {
    errorMessage.value = '请先登录后再复制乐谱'
    return
  }
  actionLoadingId.value = id
  errorMessage.value = ''
  try {
    const source = await getPublicScoreById(id)
    const copied = await createScoreOnServer({
      ...source,
      id: `copy-${Date.now()}`,
      title: `${source.title}（副本）`,
      isDraft: true,
      visibility: 'private',
      createdAt: new Date(),
      updatedAt: new Date()
    })
    await loadData()
    router.push(`/editor/${copied.id}`)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '复制失败'
  } finally {
    actionLoadingId.value = null
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.publish-page {
  max-width: 1180px;
  margin: 0 auto;
  padding: 22px 14px 40px;
}

.hero {
  border: 1px solid #bfdbfe;
  border-radius: 16px;
  padding: 18px;
  background: linear-gradient(135deg, #eff6ff 0%, #ecfeff 100%);
  display: grid;
  gap: 12px;
}

.hero h1 {
  font-size: 30px;
  font-weight: 700;
  color: #0f172a;
}

.hero p {
  margin-top: 6px;
  color: #334155;
}

.hero-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.search-input,
.sort-select {
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  padding: 10px 12px;
  min-width: 190px;
  background: #ffffff;
}

.card {
  margin-top: 14px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 16px;
}

.card-head h2 {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.card-head p {
  margin-top: 4px;
  color: #64748b;
}

.score-card-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(270px, 1fr));
  gap: 12px;
}

.score-card {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.score-card.mine {
  border-color: #93c5fd;
  background: linear-gradient(180deg, #ffffff 0%, #eff6ff 100%);
}

.score-title {
  font-size: 17px;
  font-weight: 700;
  color: #0f172a;
}

.score-meta,
.score-tags {
  margin-top: 5px;
  color: #475569;
  font-size: 13px;
}

.score-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.btn {
  border: 0;
  border-radius: 10px;
  padding: 8px 12px;
  font-weight: 600;
  cursor: pointer;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary {
  background: #0284c7;
  color: #ffffff;
}

.btn-warning {
  background: #ea580c;
  color: #ffffff;
}

.btn-secondary {
  background: #e2e8f0;
  color: #0f172a;
}

.empty-hint {
  margin-top: 12px;
  color: #64748b;
}

.error-message {
  margin-top: 10px;
  color: #dc2626;
}

.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 90;
  padding: 16px;
}

.modal {
  width: 100%;
  max-width: 640px;
  background: #ffffff;
  border-radius: 14px;
  padding: 16px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.modal-header h3 {
  font-size: 20px;
  font-weight: 700;
}

.close-btn {
  width: 30px;
  height: 30px;
  border-radius: 9999px;
  border: 0;
  background: #e2e8f0;
  cursor: pointer;
}
</style>
