<template>
  <div class="profile-page">
    <h1 class="page-title">个人中心</h1>

    <div class="layout">
      <aside class="sidebar-card">
        <div class="avatar">{{ userInitial }}</div>
        <h2 class="user-name">{{ userStore.user?.username || '未登录用户' }}</h2>
        <p class="user-email">{{ userStore.user?.email || '-' }}</p>

        <div class="quick-stats">
          <div class="stat-row">
            <span>作品总数</span>
            <strong>{{ myScores.length }}</strong>
          </div>
          <div class="stat-row">
            <span>草稿数量</span>
            <strong>{{ draftCount }}</strong>
          </div>
          <div class="stat-row">
            <span>最近更新</span>
            <strong>{{ latestUpdatedAt }}</strong>
          </div>
        </div>
      </aside>

      <main class="main-content">
        <section class="card">
          <div class="card-head">
            <h3>数据统计分析</h3>
            <div class="head-actions">
              <div class="range-switch">
                <button :class="{ active: trendRange === 7 }" @click="trendRange = 7">近7天</button>
                <button :class="{ active: trendRange === 30 }" @click="trendRange = 30">近30天</button>
              </div>
              <button class="refresh-btn" @click="refreshAll" :disabled="loading">
                {{ loading ? '刷新中...' : '刷新统计' }}
              </button>
              <button class="refresh-btn" @click="exportReportCsv">导出CSV</button>
              <button class="refresh-btn" @click="exportReportJson">导出JSON</button>
            </div>
          </div>

          <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

          <div class="analytics-grid">
            <article class="analytics-card">
              <p class="analytics-kicker">用户行为统计</p>
              <div class="analytics-metric">
                <span>页面访问总次数</span>
                <strong>{{ behaviorStats.totalPageViews }}</strong>
              </div>
              <div class="analytics-metric">
                <span>访问页面种类</span>
                <strong>{{ behaviorStats.uniquePages }}</strong>
              </div>
              <div class="analytics-metric">
                <span>近30天活跃天数</span>
                <strong>{{ behaviorStats.activeDays30 }}</strong>
              </div>
              <div class="analytics-metric">
                <span>近7天作品更新数</span>
                <strong>{{ behaviorStats.recentScoreUpdates7 }}</strong>
              </div>
            </article>

            <article class="analytics-card">
              <p class="analytics-kicker">作品传播统计</p>
              <div class="analytics-metric">
                <span>已发布作品数</span>
                <strong>{{ spreadStats.publishedCount }}</strong>
              </div>
              <div class="analytics-metric">
                <span>公开率</span>
                <strong>{{ spreadStats.publishRate }}</strong>
              </div>
              <div class="analytics-metric">
                <span>作品总音符数</span>
                <strong>{{ spreadStats.totalNotes }}</strong>
              </div>
              <div class="analytics-metric">
                <span>作品总小节数</span>
                <strong>{{ spreadStats.totalMeasures }}</strong>
              </div>
            </article>

            <article class="analytics-card">
              <p class="analytics-kicker">社区活跃度统计</p>
              <div class="analytics-metric">
                <span>我的讨论帖</span>
                <strong>{{ communityStats.myDiscussions }}</strong>
              </div>
              <div class="analytics-metric">
                <span>我的提问</span>
                <strong>{{ communityStats.myQuestions }}</strong>
              </div>
              <div class="analytics-metric">
                <span>我的回答</span>
                <strong>{{ communityStats.myAnswers }}</strong>
              </div>
              <div class="analytics-metric">
                <span>被采纳回答</span>
                <strong>{{ communityStats.acceptedAnswers }}</strong>
              </div>
              <div class="analytics-metric">
                <span>讨论帖累计点赞</span>
                <strong>{{ communityStats.discussionLikes }}</strong>
              </div>
            </article>
          </div>

          <div class="trend-grid">
            <article class="trend-card">
              <h4>用户行为趋势</h4>
              <p class="trend-sub">按日访问次数</p>
              <div class="bars">
                <div v-for="point in behaviorTrend" :key="point.key" class="bar-item">
                  <div class="bar-label">{{ point.label }}</div>
                  <div class="bar-track">
                    <div class="bar-fill behavior" :style="{ width: `${point.ratio}%` }"></div>
                  </div>
                  <div class="bar-value">{{ point.value }}</div>
                </div>
              </div>
            </article>

            <article class="trend-card">
              <h4>作品传播趋势</h4>
              <p class="trend-sub">按日公开作品更新次数</p>
              <div class="bars">
                <div v-for="point in spreadTrend" :key="point.key" class="bar-item">
                  <div class="bar-label">{{ point.label }}</div>
                  <div class="bar-track">
                    <div class="bar-fill spread" :style="{ width: `${point.ratio}%` }"></div>
                  </div>
                  <div class="bar-value">{{ point.value }}</div>
                </div>
              </div>
            </article>

            <article class="trend-card">
              <h4>社区活跃趋势</h4>
              <p class="trend-sub">按日社区发言次数（讨论+提问+回答）</p>
              <div class="bars">
                <div v-for="point in communityTrend" :key="point.key" class="bar-item">
                  <div class="bar-label">{{ point.label }}</div>
                  <div class="bar-track">
                    <div class="bar-fill community" :style="{ width: `${point.ratio}%` }"></div>
                  </div>
                  <div class="bar-value">{{ point.value }}</div>
                </div>
              </div>
            </article>
          </div>
        </section>

        <section class="card">
          <div class="card-head">
            <h3>我的乐谱列表</h3>
          </div>

          <p v-if="loading" class="muted-text">加载中...</p>
          <div v-else-if="myScores.length === 0" class="empty-box">暂无乐谱，点击下方按钮创建第一份作品。</div>

          <div v-else class="score-list">
            <div v-for="score in myScores" :key="score.id" class="score-item">
              <div class="score-info">
                <h4>{{ score.title }}</h4>
                <p>{{ score.composer || '未知作曲' }} · {{ score.isDraft ? '草稿' : '已发布' }}</p>
                <p>更新于 {{ formatDate(score.updatedAt) }}</p>
              </div>
              <div class="score-actions">
                <button class="action-btn edit" @click="editScore(score.id)">编辑</button>
                <button class="action-btn remove" @click="removeScore(score.id)">删除</button>
              </div>
            </div>
          </div>

          <button class="create-btn" @click="createNewScore">+ 创建新乐谱</button>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/userStore'
import type { Score } from '@/types/score'
import { deleteScoreOnServer, listMyScores } from '@/services/scoreApi'
import { listDiscussions, listQuestions, type DiscussionItem, type QuestionItem } from '@/services/communityApi'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const errorMessage = ref('')
const myScores = ref<Score[]>([])
const discussions = ref<DiscussionItem[]>([])
const questions = ref<QuestionItem[]>([])
const trendRange = ref<7 | 30>(7)

const userInitial = computed(() => {
  const username = userStore.user?.username || ''
  return username ? username.slice(0, 1).toUpperCase() : 'U'
})

const draftCount = computed(() => myScores.value.filter((item) => item.isDraft).length)

const latestUpdatedAt = computed(() => {
  if (!myScores.value.length) return '-'
  const latest = [...myScores.value].sort((a, b) => b.updatedAt.getTime() - a.updatedAt.getTime())[0]
  return formatDate(latest.updatedAt)
})

const currentUsername = computed(() => userStore.user?.username || '')

const behaviorStats = computed(() => {
  const visits = getPageVisits()
  const now = Date.now()
  const days30 = 30 * 24 * 3600 * 1000
  const days7 = 7 * 24 * 3600 * 1000

  const activeDaySet = new Set(
    visits
      .filter((v) => now - new Date(v.timestamp).getTime() <= days30)
      .map((v) => toDayKey(new Date(v.timestamp)))
  )

  const recentScoreUpdates7 = myScores.value.filter((s) => now - s.updatedAt.getTime() <= days7).length

  return {
    totalPageViews: visits.length,
    uniquePages: new Set(visits.map((v) => v.path)).size,
    activeDays30: activeDaySet.size,
    recentScoreUpdates7
  }
})

const spreadStats = computed(() => {
  const total = myScores.value.length
  const publishedCount = myScores.value.filter((s) => !s.isDraft && (s.visibility ?? 'private') === 'public').length
  const publishRate = total > 0 ? `${Math.round((publishedCount / total) * 100)}%` : '0%'
  const totalNotes = myScores.value.reduce((sum, s) => sum + (s.notes?.length || 0), 0)
  const totalMeasures = myScores.value.reduce((sum, s) => sum + (s.measures?.length || 0), 0)
  return {
    publishedCount,
    publishRate,
    totalNotes,
    totalMeasures
  }
})

const communityStats = computed(() => {
  const username = currentUsername.value
  if (!username) {
    return {
      myDiscussions: 0,
      myQuestions: 0,
      myAnswers: 0,
      acceptedAnswers: 0,
      discussionLikes: 0
    }
  }

  const myDiscussionList = discussions.value.filter((d) => d.author === username)
  const myQuestionList = questions.value.filter((q) => q.author === username)
  const allMyAnswers = questions.value.flatMap((q) => q.answers.filter((a) => a.author === username))
  const myAnswerIdSet = new Set(allMyAnswers.map((a) => a.id))
  const acceptedAnswers = questions.value.filter((q) => q.acceptedAnswerId && myAnswerIdSet.has(q.acceptedAnswerId)).length
  const discussionLikes = myDiscussionList.reduce((sum, d) => sum + (d.likes || 0), 0)

  return {
    myDiscussions: myDiscussionList.length,
    myQuestions: myQuestionList.length,
    myAnswers: allMyAnswers.length,
    acceptedAnswers,
    discussionLikes
  }
})

type DayPoint = {
  key: string
  label: string
}

type TrendPoint = {
  key: string
  label: string
  value: number
  ratio: number
}

const daySeries = computed<DayPoint[]>(() => getRecentDays(trendRange.value))

const behaviorTrend = computed<TrendPoint[]>(() => {
  const visits = getPageVisits()
  const countMap = new Map<string, number>()
  for (const visit of visits) {
    const key = toDayKey(new Date(visit.timestamp))
    countMap.set(key, (countMap.get(key) || 0) + 1)
  }
  return toTrend(daySeries.value, countMap)
})

const spreadTrend = computed<TrendPoint[]>(() => {
  const countMap = new Map<string, number>()
  for (const score of myScores.value) {
    if (score.isDraft || (score.visibility ?? 'private') !== 'public') continue
    const key = toDayKey(score.updatedAt)
    countMap.set(key, (countMap.get(key) || 0) + 1)
  }
  return toTrend(daySeries.value, countMap)
})

const communityTrend = computed<TrendPoint[]>(() => {
  const countMap = new Map<string, number>()
  const username = currentUsername.value
  if (!username) return toTrend(daySeries.value, countMap)

  for (const d of discussions.value) {
    if (d.author !== username) continue
    const key = toDayKey(new Date(d.createdAt))
    countMap.set(key, (countMap.get(key) || 0) + 1)
  }
  for (const q of questions.value) {
    if (q.author === username) {
      const key = toDayKey(new Date(q.createdAt))
      countMap.set(key, (countMap.get(key) || 0) + 1)
    }
    for (const a of q.answers) {
      if (a.author !== username) continue
      const key = toDayKey(new Date(a.createdAt))
      countMap.set(key, (countMap.get(key) || 0) + 1)
    }
  }
  return toTrend(daySeries.value, countMap)
})

const toTrend = (days: DayPoint[], countMap: Map<string, number>): TrendPoint[] => {
  const values = days.map((d) => countMap.get(d.key) || 0)
  const max = Math.max(1, ...values)
  return days.map((d) => {
    const value = countMap.get(d.key) || 0
    return {
      key: d.key,
      label: d.label,
      value,
      ratio: Math.round((value / max) * 100)
    }
  })
}

const toDayKey = (date: Date) => {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

const toDayLabel = (date: Date) => `${String(date.getMonth() + 1).padStart(2, '0')}/${String(date.getDate()).padStart(2, '0')}`

const getRecentDays = (days: number): DayPoint[] => {
  const list: DayPoint[] = []
  const now = new Date()
  now.setHours(0, 0, 0, 0)
  for (let i = days - 1; i >= 0; i -= 1) {
    const date = new Date(now)
    date.setDate(now.getDate() - i)
    list.push({ key: toDayKey(date), label: toDayLabel(date) })
  }
  return list
}

const formatDate = (date: Date) =>
  date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })

type PageVisit = {
  path: string
  timestamp: string
}

const getPageVisits = (): PageVisit[] => {
  try {
    const raw = localStorage.getItem('pageVisits')
    if (!raw) return []
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return []
    return parsed
      .filter((item) => item && typeof item.path === 'string' && typeof item.timestamp === 'string')
      .map((item) => ({ path: item.path, timestamp: item.timestamp }))
  } catch {
    return []
  }
}

const exportReportCsv = () => {
  const rows: string[] = []
  rows.push('模块,指标,值')
  rows.push(`用户行为,页面访问总次数,${behaviorStats.value.totalPageViews}`)
  rows.push(`用户行为,访问页面种类,${behaviorStats.value.uniquePages}`)
  rows.push(`用户行为,近30天活跃天数,${behaviorStats.value.activeDays30}`)
  rows.push(`用户行为,近7天作品更新数,${behaviorStats.value.recentScoreUpdates7}`)
  rows.push(`作品传播,已发布作品数,${spreadStats.value.publishedCount}`)
  rows.push(`作品传播,公开率,${spreadStats.value.publishRate}`)
  rows.push(`作品传播,作品总音符数,${spreadStats.value.totalNotes}`)
  rows.push(`作品传播,作品总小节数,${spreadStats.value.totalMeasures}`)
  rows.push(`社区活跃,我的讨论帖,${communityStats.value.myDiscussions}`)
  rows.push(`社区活跃,我的提问,${communityStats.value.myQuestions}`)
  rows.push(`社区活跃,我的回答,${communityStats.value.myAnswers}`)
  rows.push(`社区活跃,被采纳回答,${communityStats.value.acceptedAnswers}`)
  rows.push(`社区活跃,讨论帖累计点赞,${communityStats.value.discussionLikes}`)
  rows.push('')
  rows.push('日期,用户行为趋势,作品传播趋势,社区活跃趋势')
  for (let i = 0; i < daySeries.value.length; i += 1) {
    rows.push(`${daySeries.value[i].key},${behaviorTrend.value[i].value},${spreadTrend.value[i].value},${communityTrend.value[i].value}`)
  }
  downloadText(rows.join('\n'), `profile-analytics-${Date.now()}.csv`, 'text/csv;charset=utf-8;')
}

const exportReportJson = () => {
  const payload = {
    generatedAt: new Date().toISOString(),
    rangeDays: trendRange.value,
    summary: {
      behavior: behaviorStats.value,
      spread: spreadStats.value,
      community: communityStats.value
    },
    trend: daySeries.value.map((d, i) => ({
      date: d.key,
      behavior: behaviorTrend.value[i].value,
      spread: spreadTrend.value[i].value,
      community: communityTrend.value[i].value
    }))
  }
  downloadText(JSON.stringify(payload, null, 2), `profile-analytics-${Date.now()}.json`, 'application/json')
}

const downloadText = (text: string, filename: string, mime: string) => {
  const blob = new Blob([text], { type: mime })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

const refreshAll = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const [scoreList, discussionList, questionList] = await Promise.all([
      listMyScores(),
      listDiscussions(),
      listQuestions(false)
    ])
    myScores.value = scoreList
    discussions.value = discussionList
    questions.value = questionList
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '获取统计数据失败'
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
    myScores.value = myScores.value.filter((score) => score.id !== id)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '删除乐谱失败'
  }
}

const createNewScore = () => {
  router.push('/editor')
}

onMounted(async () => {
  await refreshAll()
})
</script>

<style scoped>
.profile-page {
  max-width: 1240px;
  margin: 0 auto;
  padding: 20px 12px 36px;
}

.page-title {
  font-size: 30px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 14px;
}

.layout {
  display: grid;
  grid-template-columns: 290px 1fr;
  gap: 14px;
}

.sidebar-card,
.card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 16px;
}

.avatar {
  width: 80px;
  height: 80px;
  border-radius: 9999px;
  background: #cbd5e1;
  color: #334155;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 700;
  margin: 0 auto 10px;
}

.user-name {
  text-align: center;
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.user-email {
  text-align: center;
  font-size: 14px;
  color: #64748b;
  margin-top: 4px;
}

.quick-stats {
  margin-top: 14px;
  display: grid;
  gap: 8px;
}

.stat-row {
  display: flex;
  justify-content: space-between;
  color: #334155;
}

.main-content {
  display: grid;
  gap: 14px;
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.card-head h3 {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.head-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}

.range-switch {
  display: flex;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  overflow: hidden;
}

.range-switch button {
  border: 0;
  padding: 8px 10px;
  background: #f8fafc;
  color: #334155;
  cursor: pointer;
}

.range-switch button.active {
  background: #0f766e;
  color: #ffffff;
}

.refresh-btn {
  border: 1px solid #cbd5e1;
  background: #f8fafc;
  color: #334155;
  border-radius: 10px;
  padding: 8px 12px;
  cursor: pointer;
}

.refresh-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.analytics-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.analytics-card {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px;
}

.analytics-kicker {
  font-size: 13px;
  font-weight: 700;
  color: #0f766e;
  margin-bottom: 8px;
}

.analytics-metric {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  padding: 6px 0;
  border-bottom: 1px dashed #e2e8f0;
  color: #334155;
}

.analytics-metric:last-child {
  border-bottom: 0;
}

.analytics-metric strong {
  color: #0f172a;
}

.trend-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.trend-card {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px;
}

.trend-card h4 {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.trend-sub {
  margin-top: 3px;
  font-size: 12px;
  color: #64748b;
}

.bars {
  margin-top: 10px;
  display: grid;
  gap: 6px;
}

.bar-item {
  display: grid;
  grid-template-columns: 46px 1fr 34px;
  gap: 6px;
  align-items: center;
}

.bar-label,
.bar-value {
  font-size: 12px;
  color: #475569;
}

.bar-track {
  height: 8px;
  border-radius: 9999px;
  background: #e2e8f0;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 9999px;
}

.bar-fill.behavior {
  background: linear-gradient(90deg, #0ea5e9, #0284c7);
}

.bar-fill.spread {
  background: linear-gradient(90deg, #22c55e, #16a34a);
}

.bar-fill.community {
  background: linear-gradient(90deg, #f97316, #ea580c);
}

.score-list {
  margin-top: 10px;
  display: grid;
  gap: 8px;
}

.score-item {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px;
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.score-info h4 {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.score-info p {
  font-size: 13px;
  color: #64748b;
  margin-top: 3px;
}

.score-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.action-btn,
.create-btn {
  border: 0;
  border-radius: 10px;
  padding: 8px 12px;
  font-weight: 600;
  cursor: pointer;
}

.action-btn.edit {
  background: #dbeafe;
  color: #1d4ed8;
}

.action-btn.remove {
  background: #fee2e2;
  color: #dc2626;
}

.create-btn {
  margin-top: 12px;
  width: 100%;
  background: #2563eb;
  color: #ffffff;
}

.error-text {
  margin-top: 10px;
  color: #dc2626;
}

.muted-text,
.empty-box {
  margin-top: 10px;
  color: #64748b;
}

@media (max-width: 1200px) {
  .analytics-grid,
  .trend-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1024px) {
  .layout {
    grid-template-columns: 1fr;
  }
}
</style>
