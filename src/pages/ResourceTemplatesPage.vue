<template>
  <div class="resource-library-page">
    <section class="hero">
      <div class="hero-copy">
        <p class="hero-kicker">RESOURCE LIBRARY</p>
        <h1 class="hero-title">资源模板库</h1>
        <p class="hero-desc">
          面向创作起步场景统一提供常用乐谱模板、和声片段、节奏型素材和示例工程，
          用户可按乐器类型、音乐风格或使用场景快速筛选，并一键导入编辑器作为创作基础。
        </p>

        <div class="hero-actions">
          <button class="btn btn-primary" @click="importResource(recommendedResource)">
            {{ importActionText }}
          </button>
          <button class="btn btn-secondary" @click="router.push('/editor')">
            打开编辑器
          </button>
        </div>
      </div>

      <div class="hero-stats">
        <article class="stat-card">
          <span class="stat-value">{{ resources.length }}</span>
          <span class="stat-label">资源总数</span>
        </article>
        <article class="stat-card">
          <span class="stat-value">{{ resourceTypes.length - 1 }}</span>
          <span class="stat-label">资源类型</span>
        </article>
        <article class="stat-card">
          <span class="stat-value">{{ instruments.length - 1 }}</span>
          <span class="stat-label">乐器维度</span>
        </article>
        <article class="stat-card stat-card-highlight">
          <span class="stat-value">{{ currentScore ? '当前草稿' : '新建草稿' }}</span>
          <span class="stat-label">导入模式</span>
        </article>
      </div>
    </section>

    <section class="summary-grid">
      <article
        v-for="group in resourceGroups"
        :key="group.type"
        class="summary-card"
      >
        <div class="summary-top">
          <span class="summary-badge">{{ group.type }}</span>
          <span class="summary-count">{{ group.count }} 项</span>
        </div>
        <h2>{{ group.title }}</h2>
        <p>{{ group.description }}</p>
      </article>
    </section>

    <section class="filters-card">
      <div class="section-head">
        <div>
          <h2>快速筛选</h2>
          <p>按资源类型、乐器、风格、场景和关键词快速定位可复用素材。</p>
        </div>
        <span class="result-count">共 {{ filteredResources.length }} 个结果</span>
      </div>

      <div class="filters-layout">
        <input
          v-model.trim="searchQuery"
          type="text"
          class="search-input"
          placeholder="搜索资源名称、说明、标签或和声内容"
        />

        <div class="filter-row">
          <div class="filter-group">
            <span class="filter-label">资源类型</span>
            <div class="chip-group">
              <button
                v-for="item in resourceTypes"
                :key="item"
                :class="['chip', { active: activeType === item }]"
                @click="activeType = item"
              >
                {{ item }}
              </button>
            </div>
          </div>

          <div class="filter-group">
            <span class="filter-label">乐器类型</span>
            <div class="chip-group">
              <button
                v-for="item in instruments"
                :key="item"
                :class="['chip chip-light', { active: activeInstrument === item }]"
                @click="activeInstrument = item"
              >
                {{ item }}
              </button>
            </div>
          </div>
        </div>

        <div class="filter-row">
          <div class="filter-group">
            <span class="filter-label">音乐风格</span>
            <div class="chip-group">
              <button
                v-for="item in styles"
                :key="item"
                :class="['chip chip-light', { active: activeStyle === item }]"
                @click="activeStyle = item"
              >
                {{ item }}
              </button>
            </div>
          </div>

          <div class="filter-group">
            <span class="filter-label">使用场景</span>
            <div class="chip-group">
              <button
                v-for="item in scenes"
                :key="item"
                :class="['chip chip-light', { active: activeScene === item }]"
                @click="activeScene = item"
              >
                {{ item }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="content-grid">
      <div class="library-card">
        <div class="section-head">
          <div>
            <h2>资源列表</h2>
            <p>每条资源都包含可复用的结构、和声、节奏或工程说明。</p>
          </div>
        </div>

        <div v-if="filteredResources.length === 0" class="empty-state">
          当前筛选条件下没有匹配资源，请调整后重试。
        </div>

        <div v-else class="resource-list">
          <article
            v-for="resource in filteredResources"
            :key="resource.id"
            class="resource-card"
          >
            <div class="resource-card-head">
              <div>
                <div class="resource-label-row">
                  <span class="type-pill">{{ resource.type }}</span>
                  <span class="scene-pill">{{ resource.scene }}</span>
                  <span class="instrument-pill">{{ resource.instrument }}</span>
                </div>
                <h3>{{ resource.title }}</h3>
              </div>
              <span class="style-pill">{{ resource.style }}</span>
            </div>

            <p class="resource-desc">{{ resource.description }}</p>

            <div class="resource-meta">
              <span>调号: {{ resource.keySignature }}</span>
              <span>拍号: {{ resource.timeSignature.numerator }}/{{ resource.timeSignature.denominator }}</span>
              <span>速度: {{ resource.tempo }} BPM</span>
            </div>

            <div class="resource-detail-grid">
              <div class="detail-box">
                <span class="detail-label">结构模板</span>
                <p>{{ resource.structure }}</p>
              </div>
              <div class="detail-box">
                <span class="detail-label">和声片段</span>
                <p>{{ resource.harmony }}</p>
              </div>
              <div class="detail-box">
                <span class="detail-label">节奏型</span>
                <p>{{ resource.rhythm }}</p>
              </div>
              <div class="detail-box">
                <span class="detail-label">示例工程</span>
                <p>{{ resource.projectHint }}</p>
              </div>
            </div>

            <div class="tag-group">
              <span v-for="tag in resource.tags" :key="tag" class="tag">{{ tag }}</span>
            </div>

            <div class="resource-footer">
              <p class="resource-purpose">{{ resource.useCase }}</p>
              <button class="btn btn-primary" @click="importResource(resource)">
                {{ currentScore ? '导入当前编辑器' : '新建草稿导入' }}
              </button>
            </div>
          </article>
        </div>
      </div>

      <aside class="guide-card">
        <div class="section-head">
          <div>
            <h2>导入说明</h2>
            <p>将资源作为当前创作的起点，而不是单独展示页。</p>
          </div>
        </div>

        <div class="guide-list">
          <div class="guide-item">
            <span class="guide-index">01</span>
            <div>
              <h3>降低创作门槛</h3>
              <p>先导入已有结构、和声或节奏型，再继续微调旋律与配器。</p>
            </div>
          </div>
          <div class="guide-item">
            <span class="guide-index">02</span>
            <div>
              <h3>筛选路径清晰</h3>
              <p>围绕乐器类型、音乐风格和使用场景构建筛选，定位更快。</p>
            </div>
          </div>
          <div class="guide-item">
            <span class="guide-index">03</span>
            <div>
              <h3>一键进入编辑</h3>
              <p>若已有当前乐谱则直接写入当前草稿，否则自动新建一份资源草稿。</p>
            </div>
          </div>
        </div>

        <div class="current-box">
          <span class="current-label">当前状态</span>
          <h3>{{ currentScore ? currentScore.title || '未命名乐谱' : '未打开编辑中的乐谱' }}</h3>
          <p>
            {{ currentScore
              ? '导入资源后会更新当前草稿的速度、调号、拍号、标签与说明，并跳转回编辑器。'
              : '选择任意资源后会创建一份新草稿并进入编辑器。' }}
          </p>
        </div>
      </aside>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useScoreStore } from '@/stores/scoreStore'
import type { Score, TimeSignature } from '@/types/score'

interface ResourceItem {
  id: string
  title: string
  type: string
  instrument: string
  style: string
  scene: string
  description: string
  useCase: string
  structure: string
  harmony: string
  rhythm: string
  projectHint: string
  tempo: number
  keySignature: string
  timeSignature: TimeSignature
  tags: string[]
}

const router = useRouter()
const scoreStore = useScoreStore()

const searchQuery = ref('')
const resourceTypes = ['全部', '常用乐谱模板', '和声片段', '节奏型素材', '示例工程']
const instruments = ['全部', '钢琴', '吉他', '弦乐', '管乐', '通用']
const styles = ['全部', '流行', '古典', '国风', '影视', '电子']
const scenes = ['全部', '教学', '练习', '演出', '比赛', '短视频']

const activeType = ref('全部')
const activeInstrument = ref('全部')
const activeStyle = ref('全部')
const activeScene = ref('全部')

const resources: ResourceItem[] = [
  {
    id: 'piano-pop-template',
    title: '钢琴流行伴奏起步模板',
    type: '常用乐谱模板',
    instrument: '钢琴',
    style: '流行',
    scene: '教学',
    description: '提供主歌、副歌、桥段三段式起稿框架，适合快速搭建钢琴流行作品骨架。',
    useCase: '适合初学者在课堂、练习或投稿前快速起稿。',
    structure: 'Intro 4 小节 / Verse 8 小节 / Chorus 8 小节 / Bridge 4 小节',
    harmony: 'C - G/B - Am7 - F',
    rhythm: '右手八分分解和弦，左手根音 + 八度支撑',
    projectHint: '示例工程预留旋律轨与伴奏轨，便于继续添加音符。',
    tempo: 96,
    keySignature: 'C',
    timeSignature: { numerator: 4, denominator: 4 },
    tags: ['钢琴', '流行', '教学']
  },
  {
    id: 'guitar-folk-harmony',
    title: '民谣吉他和声走向片段',
    type: '和声片段',
    instrument: '吉他',
    style: '流行',
    scene: '练习',
    description: '聚焦常用民谣和声进行，适合写歌时快速确定副歌和声方向。',
    useCase: '适合写作练习、弹唱编配和副歌灵感整理。',
    structure: '4 小节循环，可直接扩展为 8 或 16 小节段落',
    harmony: 'G - D/F# - Em - Cadd9',
    rhythm: '下 下上 上下上 的常见民谣扫弦律动',
    projectHint: '示例工程包含和弦标记区，可继续录入旋律与歌词。',
    tempo: 92,
    keySignature: 'G',
    timeSignature: { numerator: 4, denominator: 4 },
    tags: ['吉他', '民谣', '弹唱']
  },
  {
    id: 'string-cinematic-rhythm',
    title: '弦乐影视推进节奏型',
    type: '节奏型素材',
    instrument: '弦乐',
    style: '影视',
    scene: '演出',
    description: '面向情绪递进的影视段落，适合用作铺底节奏和张力推进。',
    useCase: '适合配乐起稿、舞台情绪铺垫和镜头推进段落。',
    structure: '8 小节持续推进，可接高潮段或转场段',
    harmony: 'Dm - Bb - F - C',
    rhythm: '持续八分音型叠加切分重音，适合弦乐组推进',
    projectHint: '示例工程建议先写低声部 ostinato，再叠高声部长音。',
    tempo: 118,
    keySignature: 'Dm',
    timeSignature: { numerator: 4, denominator: 4 },
    tags: ['弦乐', '影视', '推进']
  },
  {
    id: 'classic-piano-project',
    title: '古典钢琴示例工程',
    type: '示例工程',
    instrument: '钢琴',
    style: '古典',
    scene: '比赛',
    description: '提供较完整的段落组织与速度设定，适合作为比赛作品或练习曲的起点。',
    useCase: '适合比赛创作、考核作业和完整乐谱排版练习。',
    structure: 'A 段 8 小节 / A1 段 8 小节 / B 段 8 小节 / 尾声 4 小节',
    harmony: 'Am - E - F - C - Dm - E - Am',
    rhythm: '左手分解伴奏，右手主题句与装饰音留白',
    projectHint: '示例工程保留主题区、发展区和尾声注释，便于扩写。',
    tempo: 84,
    keySignature: 'Am',
    timeSignature: { numerator: 3, denominator: 4 },
    tags: ['古典', '钢琴', '比赛']
  },
  {
    id: 'guofeng-flute-template',
    title: '国风笛箫主题模板',
    type: '常用乐谱模板',
    instrument: '管乐',
    style: '国风',
    scene: '演出',
    description: '适合国风旋律起稿，强调留白、长音与过门衔接。',
    useCase: '适合舞台国风节目、主题旋律设计和古风短片配乐。',
    structure: '引子 2 小节 / 主段 8 小节 / 回环 8 小节 / 收束 2 小节',
    harmony: 'D - A - Bm - G',
    rhythm: '长音 + 倚音装饰，句尾保留自由延长空间',
    projectHint: '示例工程建议先写主旋律，再补充底部和声支撑。',
    tempo: 72,
    keySignature: 'D',
    timeSignature: { numerator: 4, denominator: 4 },
    tags: ['国风', '笛箫', '舞台']
  },
  {
    id: 'electro-short-video-loop',
    title: '电子短视频循环素材',
    type: '节奏型素材',
    instrument: '通用',
    style: '电子',
    scene: '短视频',
    description: '适合 15 到 30 秒短内容，强调循环记忆点和稳定律动。',
    useCase: '适合短视频、宣传片头和轻量氛围配乐。',
    structure: '4 小节循环，可无缝复制扩展',
    harmony: 'Fm - Db - Ab - Eb',
    rhythm: '四拍地鼓 + 反拍 clap + 高频切分 synth',
    projectHint: '示例工程保留鼓组、pad 与 hook 三条主轨建议。',
    tempo: 128,
    keySignature: 'Fm',
    timeSignature: { numerator: 4, denominator: 4 },
    tags: ['电子', '循环', '短视频']
  },
  {
    id: 'strings-teaching-progression',
    title: '弦乐教学和声示例',
    type: '和声片段',
    instrument: '弦乐',
    style: '古典',
    scene: '教学',
    description: '用最常见的弦乐写作和声走向帮助学生理解层次与声部支撑。',
    useCase: '适合和声教学、配器讲解和课堂示范。',
    structure: '2 小节一个和声单元，适合循环讲解与扩写',
    harmony: 'I - V6 - vi - IV - ii6 - V - I',
    rhythm: '中低声部二分支撑，高声部四分分句',
    projectHint: '示例工程建议从高声部主题开始，再补中低声部。',
    tempo: 76,
    keySignature: 'C',
    timeSignature: { numerator: 4, denominator: 4 },
    tags: ['和声', '教学', '弦乐']
  },
  {
    id: 'guitar-contest-project',
    title: '吉他原创比赛示例工程',
    type: '示例工程',
    instrument: '吉他',
    style: '流行',
    scene: '比赛',
    description: '为原创作品比赛准备的简洁工程骨架，适合从主题动机快速扩展成完整段落。',
    useCase: '适合比赛投稿、原创 demo 整理和弹唱作品成稿。',
    structure: '动机 2 小节 / 主歌 8 小节 / 副歌 8 小节 / 间奏 4 小节',
    harmony: 'A - E/G# - F#m - D',
    rhythm: '弹唱型切分分解 + 副歌扫弦强化',
    projectHint: '示例工程内建议保留人声旋律区与吉他节奏区。',
    tempo: 102,
    keySignature: 'A',
    timeSignature: { numerator: 4, denominator: 4 },
    tags: ['吉他', '原创', '比赛']
  }
]

const currentScore = computed(() => scoreStore.currentScore)
const recommendedResource = resources[0]
const importActionText = computed(() => currentScore.value ? '导入推荐资源到当前编辑器' : '用推荐资源新建草稿')

const resourceGroups = computed(() => {
  return resourceTypes
    .filter((item) => item !== '全部')
    .map((type) => {
      const count = resources.filter((resource) => resource.type === type).length
      const descriptionMap: Record<string, string> = {
        常用乐谱模板: '直接提供乐谱骨架，适合作为新作品起点。',
        和声片段: '优先解决和声组织问题，帮助快速搭建段落。',
        节奏型素材: '强化律动基础，适合先定 groove 再写旋律。',
        示例工程: '提供更完整的项目参考，便于整体推进创作。'
      }

      return {
        type,
        title: type,
        count,
        description: descriptionMap[type] ?? ''
      }
    })
})

const filteredResources = computed(() => {
  const query = searchQuery.value.toLowerCase()

  return resources.filter((resource) => {
    const matchesType = activeType.value === '全部' || resource.type === activeType.value
    const matchesInstrument = activeInstrument.value === '全部' || resource.instrument === activeInstrument.value
    const matchesStyle = activeStyle.value === '全部' || resource.style === activeStyle.value
    const matchesScene = activeScene.value === '全部' || resource.scene === activeScene.value
    const haystack = [
      resource.title,
      resource.description,
      resource.useCase,
      resource.harmony,
      resource.rhythm,
      resource.tags.join(' ')
    ].join(' ').toLowerCase()
    const matchesQuery = !query || haystack.includes(query)

    return matchesType && matchesInstrument && matchesStyle && matchesScene && matchesQuery
  })
})

const applyResourceToScore = (target: Score, resource: ResourceItem) => {
  target.tempo = resource.tempo
  target.keySignature = resource.keySignature
  target.timeSignature = resource.timeSignature
  target.tags = Array.from(new Set([...(target.tags ?? []), ...resource.tags]))
  target.description = [
    `资源类型：${resource.type}`,
    `适用场景：${resource.scene}`,
    `结构模板：${resource.structure}`,
    `和声片段：${resource.harmony}`,
    `节奏型：${resource.rhythm}`,
    `示例工程：${resource.projectHint}`
  ].join('\n')

  if (!target.title || target.title === '未命名乐谱') {
    target.title = resource.title
  }

  target.updatedAt = new Date()
}

const createResourceDraft = (resource: ResourceItem) => {
  const now = new Date()
  const draft: Score = {
    id: `${resource.id}-${Date.now()}`,
    title: resource.title,
    composer: '',
    tempo: resource.tempo,
    timeSignature: resource.timeSignature,
    keySignature: resource.keySignature,
    notes: [],
    measures: [],
    createdAt: now,
    updatedAt: now,
    isDraft: true,
    visibility: 'private',
    tags: resource.tags,
    description: [
      `资源类型：${resource.type}`,
      `适用场景：${resource.scene}`,
      `结构模板：${resource.structure}`,
      `和声片段：${resource.harmony}`,
      `节奏型：${resource.rhythm}`,
      `示例工程：${resource.projectHint}`
    ].join('\n')
  }

  return scoreStore.createScore(draft)
}

const importResource = (resource: ResourceItem) => {
  if (currentScore.value) {
    applyResourceToScore(currentScore.value, resource)
    const scoreIndex = scoreStore.scores.findIndex((item) => item.id === currentScore.value?.id)
    if (scoreIndex !== -1) {
      scoreStore.scores[scoreIndex] = { ...currentScore.value }
    }
    router.push(`/editor/${currentScore.value.id}`)
    return
  }

  const created = createResourceDraft(resource)
  router.push(`/editor/${created.id}`)
}
</script>

<style scoped>
.resource-library-page {
  @apply px-4 md:px-6 py-6 md:py-8 space-y-6;
}

.hero {
  @apply rounded-2xl border border-cyan-100 p-6 md:p-8 grid grid-cols-1 xl:grid-cols-[1.5fr_1fr] gap-5;
  background: linear-gradient(135deg, #ecfeff 0%, #eff6ff 50%, #f8fafc 100%);
}

.hero-kicker {
  @apply text-xs uppercase tracking-[0.24em] text-cyan-700 font-semibold;
}

.hero-title {
  @apply mt-3 text-3xl md:text-4xl font-bold text-slate-900;
}

.hero-desc {
  @apply mt-3 max-w-3xl text-sm md:text-base leading-7 text-slate-600;
}

.hero-actions {
  @apply mt-5 flex flex-wrap gap-3;
}

.hero-stats {
  @apply grid grid-cols-2 xl:grid-cols-1 gap-3;
}

.stat-card {
  @apply rounded-xl border border-white/80 bg-white/85 p-4 flex flex-col gap-1;
}

.stat-card-highlight {
  @apply border-cyan-200 bg-cyan-50;
}

.stat-value {
  @apply text-2xl font-bold text-slate-900;
}

.stat-label {
  @apply text-sm text-slate-500;
}

.summary-grid {
  @apply grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4;
}

.summary-card,
.filters-card,
.library-card,
.guide-card {
  @apply rounded-2xl border border-slate-200 bg-white p-4 md:p-6;
}

.summary-top,
.section-head {
  @apply flex items-start justify-between gap-3;
}

.summary-badge,
.type-pill {
  @apply inline-flex rounded-full bg-cyan-100 px-2.5 py-1 text-xs font-medium text-cyan-800;
}

.summary-count {
  @apply text-xs text-slate-500;
}

.summary-card h2,
.section-head h2 {
  @apply mt-4 text-lg font-semibold text-slate-900;
}

.summary-card p,
.section-head p {
  @apply mt-2 text-sm leading-6 text-slate-500;
}

.result-count {
  @apply inline-flex items-center rounded-full bg-slate-100 px-3 py-1 text-sm text-slate-600;
}

.filters-layout {
  @apply mt-5 space-y-5;
}

.search-input {
  @apply w-full rounded-xl border border-slate-300 bg-slate-50 px-4 py-3 text-sm text-slate-700 outline-none transition focus:border-cyan-500 focus:bg-white focus:ring-2 focus:ring-cyan-100;
}

.filter-row {
  @apply grid grid-cols-1 xl:grid-cols-2 gap-5;
}

.filter-group {
  @apply space-y-3;
}

.filter-label {
  @apply block text-sm font-medium text-slate-700;
}

.chip-group {
  @apply flex flex-wrap gap-2;
}

.chip {
  @apply rounded-full border border-cyan-200 bg-cyan-50 px-4 py-2 text-sm text-cyan-700 transition-colors;
}

.chip-light {
  @apply border-slate-200 bg-slate-50 text-slate-600;
}

.chip.active {
  @apply border-cyan-600 bg-cyan-600 text-white;
}

.content-grid {
  @apply grid grid-cols-1 xl:grid-cols-[1.7fr_0.9fr] gap-6;
}

.resource-list {
  @apply mt-5 grid grid-cols-1 2xl:grid-cols-2 gap-4;
}

.resource-card {
  @apply rounded-xl border border-slate-200 bg-slate-50/70 p-4 transition-all hover:-translate-y-0.5 hover:bg-white hover:shadow-md;
}

.resource-card-head {
  @apply flex items-start justify-between gap-3;
}

.resource-label-row {
  @apply flex flex-wrap gap-2;
}

.scene-pill,
.instrument-pill,
.style-pill {
  @apply inline-flex rounded-full px-2.5 py-1 text-xs;
}

.scene-pill {
  @apply bg-slate-200 text-slate-700;
}

.instrument-pill {
  @apply bg-emerald-100 text-emerald-700;
}

.style-pill {
  @apply bg-amber-100 text-amber-700 whitespace-nowrap;
}

.resource-card h3 {
  @apply mt-3 text-lg font-semibold text-slate-900;
}

.resource-desc {
  @apply mt-2 text-sm leading-6 text-slate-600;
}

.resource-meta {
  @apply mt-4 flex flex-wrap gap-2 text-xs text-slate-600;
}

.resource-detail-grid {
  @apply mt-4 grid grid-cols-1 md:grid-cols-2 gap-3;
}

.detail-box {
  @apply rounded-lg border border-slate-200 bg-white p-3;
}

.detail-label {
  @apply text-xs font-medium text-slate-500;
}

.detail-box p {
  @apply mt-2 text-sm leading-6 text-slate-700;
}

.tag-group {
  @apply mt-4 flex flex-wrap gap-2;
}

.tag {
  @apply rounded-md bg-slate-200 px-2 py-1 text-[11px] text-slate-700;
}

.resource-footer {
  @apply mt-5 flex items-center justify-between gap-3;
}

.resource-purpose {
  @apply text-sm leading-6 text-slate-500;
}

.guide-list {
  @apply mt-5 space-y-4;
}

.guide-item {
  @apply flex gap-3 rounded-xl border border-slate-200 bg-slate-50 p-4;
}

.guide-index {
  @apply text-sm font-semibold text-cyan-700;
}

.guide-item h3 {
  @apply text-base font-semibold text-slate-900;
}

.guide-item p,
.current-box p {
  @apply mt-2 text-sm leading-6 text-slate-600;
}

.current-box {
  @apply mt-5 rounded-xl border border-cyan-200 bg-cyan-50 p-4;
}

.current-label {
  @apply text-xs font-medium uppercase tracking-[0.18em] text-cyan-700;
}

.current-box h3 {
  @apply mt-3 text-lg font-semibold text-slate-900;
}

.empty-state {
  @apply mt-5 rounded-xl border border-dashed border-slate-300 bg-slate-50 px-4 py-10 text-center text-sm text-slate-500;
}

.btn {
  @apply rounded-xl px-4 py-2.5 text-sm font-medium transition-colors;
}

.btn-primary {
  @apply bg-cyan-700 text-white hover:bg-cyan-800;
}

.btn-secondary {
  @apply bg-slate-100 text-slate-700 hover:bg-slate-200;
}
</style>
