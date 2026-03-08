<template>
  <div class="community-page">
    <section class="hero">
      <div>
        <p class="hero-kicker">COMMUNITY</p>
        <h1>专业讨论 + 问答社区</h1>
        <p class="hero-desc">
          围绕作曲、编曲、配器、演奏和音乐制作展开交流，沉淀可复用的音乐实践经验。
        </p>
      </div>
      <div class="hero-metrics">
        <div class="metric-card">
          <p class="metric-value">{{ discussionPosts.length }}</p>
          <p class="metric-label">讨论主题数</p>
        </div>
        <div class="metric-card">
          <p class="metric-value">{{ questions.length }}</p>
          <p class="metric-label">问答主题数</p>
        </div>
      </div>
    </section>

    <p v-if="errorMessage" class="error-banner">{{ errorMessage }}</p>

    <section class="tabs">
      <button class="tab-btn" :class="{ active: activeTab === 'discussion' }" @click="activeTab = 'discussion'">
        专业讨论区
      </button>
      <button class="tab-btn" :class="{ active: activeTab === 'qa' }" @click="activeTab = 'qa'">
        问答区
      </button>
    </section>

    <section v-if="activeTab === 'discussion'" class="module-card">
      <header class="module-header">
        <h2>专业讨论区</h2>
        <p>发布技术观点、方案设计和实践复盘，支持图片与 MXL 附件。</p>
      </header>

      <form class="editor" @submit.prevent="submitDiscussion">
        <input v-model.trim="discussionDraft.title" type="text" placeholder="请输入讨论标题" required />
        <textarea
          v-model.trim="discussionDraft.content"
          rows="4"
          placeholder="请描述你的观点、上下文与权衡点"
          required
        />
        <div class="editor-actions">
          <select v-model="discussionDraft.tag">
            <option value="作曲">作曲</option>
            <option value="编曲">编曲</option>
            <option value="和声">和声</option>
            <option value="配器">配器</option>
            <option value="演奏技巧">演奏技巧</option>
            <option value="音乐制作">音乐制作</option>
            <option value="音乐理论">音乐理论</option>
          </select>
        </div>

        <div class="upload-grid">
          <div class="upload-card" :class="{ 'has-file': !!discussionDraft.imageDataUrl }">
            <p class="upload-title">上传图片</p>
            <input id="upload-image-input" ref="imageInputRef" class="upload-input" type="file" accept="image/*" @change="onImageSelect" />
            <label for="upload-image-input" class="upload-trigger">选择图片</label>
            <p v-if="discussionDraft.imageName" class="upload-meta file-chip">{{ discussionDraft.imageName }}</p>
            <img v-if="discussionDraft.imageDataUrl" :src="discussionDraft.imageDataUrl" class="preview-image" alt="预览图" />
            <button v-if="discussionDraft.imageDataUrl" class="btn btn-secondary" type="button" @click="clearImage">
              清除图片
            </button>
          </div>

          <div class="upload-card" :class="{ 'has-file': !!discussionDraft.mxlDataBase64 }">
            <p class="upload-title">上传 MXL 文件</p>
            <input id="upload-mxl-input" ref="mxlInputRef" class="upload-input" type="file" accept=".mxl" @change="onMxlSelect" />
            <label for="upload-mxl-input" class="upload-trigger">选择 MXL</label>
            <p v-if="discussionDraft.mxlFileName" class="upload-meta file-chip">{{ discussionDraft.mxlFileName }}</p>
            <div class="upload-actions">
              <button
                v-if="discussionDraft.mxlDataBase64"
                class="btn btn-secondary"
                type="button"
                @click="downloadMxlFile(discussionDraft.mxlFileName || 'score.mxl', discussionDraft.mxlDataBase64)"
              >
                下载附件
              </button>
              <button v-if="discussionDraft.mxlDataBase64" class="btn btn-secondary" type="button" @click="clearMxl">
                清除附件
              </button>
            </div>
          </div>
        </div>

        <div class="editor-actions">
          <button class="btn btn-primary" type="submit" :disabled="loading">
            {{ loading ? '提交中...' : '发布讨论' }}
          </button>
        </div>
      </form>

      <div v-if="discussionPosts.length === 0" class="empty">暂无讨论内容。</div>
      <div v-else class="list">
        <article v-for="post in discussionPosts" :key="post.id" class="item">
          <div class="item-head">
            <div>
              <h3>{{ post.title }}</h3>
              <p class="item-meta">{{ post.author }} · {{ formatDate(post.createdAt) }}</p>
            </div>
            <span class="tag">{{ post.tag }}</span>
          </div>
          <p class="item-content">{{ post.content }}</p>

          <img v-if="post.imageDataUrl" :src="post.imageDataUrl" alt="讨论图片" class="post-image" />

          <div v-if="post.mxlDataBase64" class="mxl-row">
            <span class="item-meta">附件：{{ post.mxlFileName || 'score.mxl' }}</span>
            <button class="btn btn-secondary" @click="downloadMxlFile(post.mxlFileName || 'score.mxl', post.mxlDataBase64 || '')">
              下载 MXL
            </button>
          </div>

          <div class="item-actions">
            <button class="btn btn-secondary" @click="onLikeDiscussion(post.id)">
              {{ post.likedByMe ? '取消点赞' : '点赞' }} {{ post.likes }}
            </button>
            <button class="btn btn-secondary" @click="onBookmarkDiscussion(post.id)">
              {{ post.bookmarkedByMe ? '取消收藏' : '收藏' }} {{ post.bookmarks }}
            </button>
          </div>
        </article>
      </div>
    </section>

    <section v-else class="module-card">
      <header class="module-header">
        <h2>问答区</h2>
        <p>提出具体问题，沉淀高质量回答并标记采纳方案。</p>
      </header>

      <form class="editor" @submit.prevent="submitQuestion">
        <input v-model.trim="qaDraft.title" type="text" placeholder="请输入问题标题" required />
        <textarea
          v-model.trim="qaDraft.content"
          rows="4"
          placeholder="你尝试了什么？期望结果是什么？"
          required
        />
        <div class="editor-actions">
          <select v-model="qaDraft.level">
            <option value="初级">初级</option>
            <option value="中级">中级</option>
            <option value="高级">高级</option>
          </select>
          <button class="btn btn-primary" type="submit" :disabled="loading">{{ loading ? '提交中...' : '发布提问' }}</button>
        </div>
      </form>

      <div class="qa-filter">
        <label>
          <input v-model="showOnlyOpenQuestions" type="checkbox" />
          仅显示未解决问题
        </label>
      </div>

      <div v-if="filteredQuestions.length === 0" class="empty">暂无符合条件的问题。</div>
      <div v-else class="list">
        <article v-for="q in filteredQuestions" :key="q.id" class="item">
          <div class="item-head">
            <div>
              <h3>{{ q.title }}</h3>
              <p class="item-meta">
                {{ q.author }} · {{ q.level }} · {{ formatDate(q.createdAt) }} · {{ q.resolved ? '已解决' : '待解决' }}
              </p>
            </div>
          </div>
          <p class="item-content">{{ q.content }}</p>

          <div class="answers">
            <h4>回答（{{ q.answers.length }}）</h4>
            <div v-if="q.answers.length === 0" class="empty-inline">暂无回答。</div>
            <div v-else class="answer-list">
              <div
                v-for="answer in q.answers"
                :key="answer.id"
                class="answer-item"
                :class="{ accepted: q.acceptedAnswerId === answer.id }"
              >
                <p class="item-meta">{{ answer.author }} · {{ formatDate(answer.createdAt) }}</p>
                <p>{{ answer.content }}</p>
                <div class="answer-actions">
                  <button class="btn btn-secondary" @click="onUpvoteAnswer(q.id, answer.id)">
                    赞同 {{ answer.upvotes }}
                  </button>
                  <button
                    v-if="q.acceptedAnswerId !== answer.id"
                    class="btn btn-success"
                    @click="onAcceptAnswer(q.id, answer.id)"
                  >
                    采纳
                  </button>
                </div>
              </div>
            </div>
          </div>

          <form class="answer-editor" @submit.prevent="submitAnswer(q.id)">
            <textarea v-model.trim="answerDraftMap[q.id]" rows="3" placeholder="请输入你的回答" required />
            <div class="editor-actions">
              <button class="btn btn-primary" type="submit">发布回答</button>
              <button class="btn btn-warning" type="button" @click="onToggleResolved(q)">
                {{ q.resolved ? '标记为待解决' : '标记为已解决' }}
              </button>
            </div>
          </form>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import {
  acceptAnswer,
  bookmarkDiscussion,
  createAnswer,
  createDiscussion,
  createQuestion,
  likeDiscussion,
  listDiscussions,
  listQuestions,
  setQuestionResolved,
  upvoteAnswer,
  type DiscussionItem,
  type QuestionItem
} from '@/services/communityApi'

type TabMode = 'discussion' | 'qa'

const activeTab = ref<TabMode>('discussion')
const showOnlyOpenQuestions = ref(false)
const loading = ref(false)
const errorMessage = ref('')
const imageInputRef = ref<HTMLInputElement | null>(null)
const mxlInputRef = ref<HTMLInputElement | null>(null)

const discussionDraft = reactive({
  title: '',
  content: '',
  tag: '作曲',
  imageName: null as string | null,
  imageDataUrl: null as string | null,
  mxlFileName: null as string | null,
  mxlDataBase64: null as string | null
})

const qaDraft = reactive({
  title: '',
  content: '',
  level: '初级'
})

const answerDraftMap = reactive<Record<string, string>>({})

const discussionPosts = ref<DiscussionItem[]>([])
const questions = ref<QuestionItem[]>([])

const filteredQuestions = computed(() =>
  showOnlyOpenQuestions.value ? questions.value.filter((q) => !q.resolved) : questions.value
)

const formatDate = (date: string) => new Date(date).toLocaleString()

const loadDiscussions = async () => {
  discussionPosts.value = await listDiscussions()
}

const loadQuestions = async () => {
  questions.value = await listQuestions(showOnlyOpenQuestions.value)
}

const loadAll = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    await Promise.all([loadDiscussions(), loadQuestions()])
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载社区数据失败'
  } finally {
    loading.value = false
  }
}

const readFileAsDataUrl = (file: File): Promise<string> =>
  new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(new Error('读取文件失败'))
    reader.readAsDataURL(file)
  })

const arrayBufferToBase64 = (buffer: ArrayBuffer) => {
  let binary = ''
  const bytes = new Uint8Array(buffer)
  const chunk = 0x8000
  for (let i = 0; i < bytes.length; i += chunk) {
    binary += String.fromCharCode(...bytes.subarray(i, i + chunk))
  }
  return btoa(binary)
}

const base64ToBlob = (base64: string, mime = 'application/octet-stream') => {
  const binary = atob(base64)
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i += 1) {
    bytes[i] = binary.charCodeAt(i)
  }
  return new Blob([bytes], { type: mime })
}

const downloadMxlFile = (fileName: string, base64: string) => {
  if (!base64) return
  const blob = base64ToBlob(base64, 'application/vnd.recordare.musicxml')
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = fileName.endsWith('.mxl') ? fileName : `${fileName}.mxl`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

const clearImage = () => {
  discussionDraft.imageName = null
  discussionDraft.imageDataUrl = null
  if (imageInputRef.value) imageInputRef.value.value = ''
}

const clearMxl = () => {
  discussionDraft.mxlFileName = null
  discussionDraft.mxlDataBase64 = null
  if (mxlInputRef.value) mxlInputRef.value.value = ''
}

const onImageSelect = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    errorMessage.value = '请选择图片文件'
    clearImage()
    return
  }
  try {
    discussionDraft.imageName = file.name
    discussionDraft.imageDataUrl = await readFileAsDataUrl(file)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '读取图片失败'
    clearImage()
  }
}

const onMxlSelect = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (!file.name.toLowerCase().endsWith('.mxl')) {
    errorMessage.value = '请上传 .mxl 文件'
    clearMxl()
    return
  }
  try {
    const buffer = await file.arrayBuffer()
    discussionDraft.mxlFileName = file.name
    discussionDraft.mxlDataBase64 = arrayBufferToBase64(buffer)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '读取 MXL 失败'
    clearMxl()
  }
}

const submitDiscussion = async () => {
  if (!discussionDraft.title || !discussionDraft.content) return
  loading.value = true
  errorMessage.value = ''
  try {
    await createDiscussion({
      title: discussionDraft.title,
      content: discussionDraft.content,
      tag: discussionDraft.tag,
      imageName: discussionDraft.imageName,
      imageDataUrl: discussionDraft.imageDataUrl,
      mxlFileName: discussionDraft.mxlFileName,
      mxlDataBase64: discussionDraft.mxlDataBase64
    })
    discussionDraft.title = ''
    discussionDraft.content = ''
    discussionDraft.tag = '作曲'
    clearImage()
    clearMxl()
    await loadDiscussions()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '发布讨论失败'
  } finally {
    loading.value = false
  }
}

const onLikeDiscussion = async (id: string) => {
  try {
    const updated = await likeDiscussion(id)
    const idx = discussionPosts.value.findIndex((item) => item.id === id)
    if (idx !== -1) discussionPosts.value[idx] = updated
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '点赞失败'
  }
}

const onBookmarkDiscussion = async (id: string) => {
  try {
    const updated = await bookmarkDiscussion(id)
    const idx = discussionPosts.value.findIndex((item) => item.id === id)
    if (idx !== -1) discussionPosts.value[idx] = updated
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '收藏失败'
  }
}

const submitQuestion = async () => {
  if (!qaDraft.title || !qaDraft.content) return
  loading.value = true
  errorMessage.value = ''
  try {
    await createQuestion({
      title: qaDraft.title,
      content: qaDraft.content,
      level: qaDraft.level
    })
    qaDraft.title = ''
    qaDraft.content = ''
    qaDraft.level = '初级'
    await loadQuestions()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '发布问题失败'
  } finally {
    loading.value = false
  }
}

const submitAnswer = async (questionId: string) => {
  const content = (answerDraftMap[questionId] || '').trim()
  if (!content) return
  errorMessage.value = ''
  try {
    await createAnswer(questionId, { content })
    answerDraftMap[questionId] = ''
    await loadQuestions()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '发布回答失败'
  }
}

const onUpvoteAnswer = async (questionId: string, answerId: string) => {
  errorMessage.value = ''
  try {
    await upvoteAnswer(questionId, answerId)
    await loadQuestions()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '赞同失败'
  }
}

const onAcceptAnswer = async (questionId: string, answerId: string) => {
  errorMessage.value = ''
  try {
    await acceptAnswer(questionId, answerId)
    await loadQuestions()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '采纳回答失败'
  }
}

const onToggleResolved = async (question: QuestionItem) => {
  errorMessage.value = ''
  try {
    await setQuestionResolved(question.id, !question.resolved)
    await loadQuestions()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '更新问题状态失败'
  }
}

watch(showOnlyOpenQuestions, () => {
  loadQuestions().catch((error) => {
    errorMessage.value = error instanceof Error ? error.message : '刷新问题列表失败'
  })
})

onMounted(() => {
  loadAll()
})
</script>

<style scoped>
.community-page {
  max-width: 1180px;
  margin: 0 auto;
  padding: 20px 12px 36px;
}

.hero {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 12px;
  background: linear-gradient(135deg, #0f172a 0%, #134e4a 100%);
  color: #f8fafc;
  border-radius: 16px;
  padding: 18px;
}

.hero-kicker {
  font-size: 12px;
  letter-spacing: 0.14em;
  color: #99f6e4;
}

.hero h1 {
  margin-top: 6px;
  font-size: 30px;
  font-weight: 700;
}

.hero-desc {
  margin-top: 10px;
  max-width: 720px;
  color: #d1fae5;
}

.hero-metrics {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.metric-card {
  border: 1px solid rgba(255, 255, 255, 0.26);
  border-radius: 12px;
  padding: 14px;
  background: rgba(255, 255, 255, 0.06);
}

.metric-value {
  font-size: 28px;
  font-weight: 700;
}

.metric-label {
  margin-top: 4px;
  font-size: 12px;
  color: #ccfbf1;
}

.error-banner {
  margin-top: 12px;
  border: 1px solid #fecaca;
  border-radius: 10px;
  padding: 10px 12px;
  color: #b91c1c;
  background: #fef2f2;
}

.tabs {
  display: flex;
  gap: 8px;
  margin-top: 14px;
}

.tab-btn {
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  background: #ffffff;
  color: #0f172a;
  padding: 9px 14px;
  font-weight: 600;
}

.tab-btn.active {
  background: #0f766e;
  border-color: #0f766e;
  color: #ffffff;
}

.module-card {
  margin-top: 12px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  border-radius: 14px;
  padding: 16px;
}

.module-header h2 {
  font-size: 21px;
  font-weight: 700;
  color: #0f172a;
}

.module-header p {
  margin-top: 4px;
  color: #475569;
}

.editor {
  margin-top: 12px;
  display: grid;
  gap: 8px;
}

.editor input,
.editor textarea,
.editor select,
.answer-editor textarea {
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  padding: 10px 12px;
}

.upload-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.upload-card {
  border: 1px dashed #94a3b8;
  border-radius: 10px;
  padding: 12px;
  display: grid;
  gap: 8px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  transition: all 0.2s ease;
}

.upload-card:hover {
  border-color: #14b8a6;
  box-shadow: 0 8px 22px rgba(15, 118, 110, 0.08);
}

.upload-card.has-file {
  border-color: #14b8a6;
  background: linear-gradient(180deg, #ffffff 0%, #ecfeff 100%);
}

.upload-title {
  font-size: 13px;
  font-weight: 700;
  color: #334155;
}

.upload-input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  pointer-events: none;
}

.upload-trigger {
  display: inline-flex;
  width: fit-content;
  align-items: center;
  justify-content: center;
  border: 1px solid #99f6e4;
  border-radius: 8px;
  background: #f0fdfa;
  color: #0f766e;
  font-size: 13px;
  font-weight: 700;
  padding: 7px 12px;
  cursor: pointer;
}

.upload-trigger:hover {
  background: #ccfbf1;
}

.upload-meta {
  font-size: 12px;
  color: #64748b;
}

.file-chip {
  display: inline-flex;
  width: fit-content;
  max-width: 100%;
  padding: 4px 8px;
  border: 1px solid #bfdbfe;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-image {
  width: 100%;
  max-height: 190px;
  object-fit: contain;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
}

.upload-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.list {
  margin-top: 12px;
  display: grid;
  gap: 10px;
}

.item {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px;
}

.item-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.item-head h3 {
  font-size: 17px;
  font-weight: 700;
  color: #0f172a;
}

.item-meta {
  margin-top: 3px;
  font-size: 12px;
  color: #64748b;
}

.item-content {
  margin-top: 8px;
  color: #334155;
}

.post-image {
  margin-top: 10px;
  max-width: 100%;
  max-height: 340px;
  object-fit: contain;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
}

.mxl-row {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.item-actions,
.answer-actions,
.editor-actions {
  margin-top: 10px;
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: flex-end;
}

.tag {
  font-size: 12px;
  font-weight: 600;
  color: #0f766e;
  background: #ccfbf1;
  border-radius: 999px;
  padding: 6px 10px;
  height: fit-content;
}

.answers {
  margin-top: 10px;
  border-top: 1px solid #e2e8f0;
  padding-top: 10px;
}

.answers h4 {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.answer-list {
  margin-top: 8px;
  display: grid;
  gap: 8px;
}

.answer-item {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 10px;
}

.answer-item.accepted {
  border-color: #16a34a;
  background: #f0fdf4;
}

.answer-editor {
  margin-top: 10px;
  display: grid;
  gap: 8px;
}

.qa-filter {
  margin-top: 10px;
  color: #334155;
}

.empty {
  margin-top: 12px;
  color: #64748b;
}

.empty-inline {
  margin-top: 8px;
  color: #64748b;
  font-size: 13px;
}

.btn {
  border: 0;
  border-radius: 9px;
  padding: 8px 12px;
  font-weight: 600;
  cursor: pointer;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary {
  background: #0f766e;
  color: #ffffff;
}

.btn-secondary {
  background: #e2e8f0;
  color: #0f172a;
}

.btn-success {
  background: #16a34a;
  color: #ffffff;
}

.btn-warning {
  background: #f59e0b;
  color: #ffffff;
}

@media (max-width: 900px) {
  .hero,
  .upload-grid {
    grid-template-columns: 1fr;
  }
}
</style>
