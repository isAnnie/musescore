<template>
  <div class="featured-page">
    <section class="hero">
      <div class="hero-content">
        <p class="hero-kicker">FEATURED SCORES</p>
        <h1 class="hero-title">精选乐谱</h1>
        <p class="hero-desc">收录简单、知名度高、上手快的经典旋律，适合练习与教学。</p>
      </div>
      <div class="hero-stats">
        <div class="stat-card">
          <div class="stat-value">{{ featuredScores.length }}</div>
          <div class="stat-label">入门名曲</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ easyCount }}</div>
          <div class="stat-label">简单难度</div>
        </div>
      </div>
    </section>

    <section class="score-section">
      <div class="section-header">
        <h2>推荐曲目</h2>
        <p>点击“打开编辑器”可快速创建一份同名乐谱草稿。</p>
      </div>

      <div class="score-grid">
        <article
          v-for="score in featuredScores"
          :key="score.id"
          class="score-card"
        >
          <div class="score-head">
            <h3 class="score-title">{{ score.title }}</h3>
            <span class="score-level">{{ score.difficulty }}</span>
          </div>

          <p class="score-composer">{{ score.composer }}</p>
          <p class="score-desc">{{ score.description }}</p>

          <div class="score-meta">
            <span>🎼 {{ score.keySignature }} 调</span>
            <span>🕒 {{ score.timeSignature.numerator }}/{{ score.timeSignature.denominator }}</span>
            <span>♩={{ score.tempo }}</span>
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

          <button class="open-btn" @click="openInEditor(score)">
            打开编辑器
          </button>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useScoreStore } from '@/stores/scoreStore'
import type { Note, Score, TimeSignature } from '@/types/score'

interface FeaturedScore {
  id: string
  title: string
  composer: string
  description: string
  difficulty: '简单' | '中等'
  tempo: number
  keySignature: string
  timeSignature: TimeSignature
  tags: string[]
  notes: Array<{
    pitch: string
    duration: number
    measureIndex: number
    beatInMeasure: number
    clef?: 'treble' | 'bass'
    accidental?: 'sharp' | 'flat' | 'natural'
  }>
}

const router = useRouter()
const scoreStore = useScoreStore()

const generateTorrentEtudeNotes = (): FeaturedScore['notes'] => {
  const notes: FeaturedScore['notes'] = []
  const treblePatterns: Record<string, string[]> = {
    Csm: ['C#4', 'E4', 'G#4', 'C#5', 'G#4', 'E4', 'C#4', 'G#3'],
    Gsm: ['G#3', 'B3', 'D#4', 'G#4', 'D#4', 'B3', 'G#3', 'D#3'],
    A: ['A3', 'C#4', 'E4', 'A4', 'E4', 'C#4', 'A3', 'E3'],
    E: ['E3', 'G#3', 'B3', 'E4', 'B3', 'G#3', 'E3', 'B2'],
    Fsm: ['F#3', 'A3', 'C#4', 'F#4', 'C#4', 'A3', 'F#3', 'C#3'],
    B: ['B3', 'D#4', 'F#4', 'B4', 'F#4', 'D#4', 'B3', 'F#3']
  }
  const bassPatterns: Record<string, string[]> = {
    Csm: ['C#2', 'G#2', 'C#3', 'G#2'],
    Gsm: ['G#1', 'D#2', 'G#2', 'D#2'],
    A: ['A1', 'E2', 'A2', 'E2'],
    E: ['E1', 'B1', 'E2', 'B1'],
    Fsm: ['F#1', 'C#2', 'F#2', 'C#2'],
    B: ['B1', 'F#2', 'B2', 'F#2']
  }
  const progression = ['Csm', 'Gsm', 'A', 'E', 'Fsm', 'Csm', 'B', 'Gsm']

  for (let measure = 0; measure < 30; measure += 1) {
    const chord = progression[measure % progression.length]
    const treble = treblePatterns[chord]
    const bass = bassPatterns[chord]

    for (let i = 0; i < treble.length; i += 1) {
      notes.push({
        pitch: treble[i],
        duration: 0.5,
        measureIndex: measure,
        beatInMeasure: i * 0.5,
        clef: 'treble'
      })
    }

    for (let i = 0; i < bass.length; i += 1) {
      notes.push({
        pitch: bass[i],
        duration: 1,
        measureIndex: measure,
        beatInMeasure: i,
        clef: 'bass'
      })
    }
  }

  return notes
}

const featuredScores: FeaturedScore[] = [
  {
    id: 'featured-torrent-etude',
    title: '肖邦：激流练习曲（30小节节选）',
    composer: 'Frédéric Chopin',
    description: '基于肖邦《练习曲 Op.10 No.4（激流）》风格整理的30小节双谱练习节选，突出右手快速流动与左手支撑。',
    difficulty: '中等',
    tempo: 172,
    keySignature: 'C#m',
    timeSignature: { numerator: 4, denominator: 4 },
    tags: ['肖邦', 'Op.10 No.4', '30小节', '高低音双谱'],
    notes: generateTorrentEtudeNotes()
  },
  {
    id: 'featured-ode-to-joy',
    title: '欢乐颂',
    composer: '贝多芬',
    description: '旋律线条清晰，节奏规律，非常适合初学者识谱。',
    difficulty: '简单',
    tempo: 92,
    keySignature: 'C',
    timeSignature: { numerator: 4, denominator: 4 },
    tags: ['入门', '古典', '知名旋律'],
    notes: [
      { pitch: 'E4', duration: 1, measureIndex: 0, beatInMeasure: 0 },
      { pitch: 'E4', duration: 1, measureIndex: 0, beatInMeasure: 1 },
      { pitch: 'F4', duration: 1, measureIndex: 0, beatInMeasure: 2 },
      { pitch: 'G4', duration: 1, measureIndex: 0, beatInMeasure: 3 },
      { pitch: 'G4', duration: 1, measureIndex: 1, beatInMeasure: 0 },
      { pitch: 'F4', duration: 1, measureIndex: 1, beatInMeasure: 1 },
      { pitch: 'E4', duration: 1, measureIndex: 1, beatInMeasure: 2 },
      { pitch: 'D4', duration: 1, measureIndex: 1, beatInMeasure: 3 },
      { pitch: 'C4', duration: 1, measureIndex: 2, beatInMeasure: 0 },
      { pitch: 'C4', duration: 1, measureIndex: 2, beatInMeasure: 1 },
      { pitch: 'D4', duration: 1, measureIndex: 2, beatInMeasure: 2 },
      { pitch: 'E4', duration: 1, measureIndex: 2, beatInMeasure: 3 },
      { pitch: 'E4', duration: 1.5, measureIndex: 3, beatInMeasure: 0 },
      { pitch: 'D4', duration: 0.5, measureIndex: 3, beatInMeasure: 1.5 },
      { pitch: 'D4', duration: 2, measureIndex: 3, beatInMeasure: 2 }
    ]
  },
  {
    id: 'featured-twinkle',
    title: '小星星',
    composer: '法国民谣',
    description: '最经典的入门练习曲，适合练习音程与节奏稳定性。',
    difficulty: '简单',
    tempo: 88,
    keySignature: 'C',
    timeSignature: { numerator: 4, denominator: 4 },
    tags: ['儿童', '入门', '启蒙'],
    notes: [
      { pitch: 'C4', duration: 1, measureIndex: 0, beatInMeasure: 0 },
      { pitch: 'C4', duration: 1, measureIndex: 0, beatInMeasure: 1 },
      { pitch: 'G4', duration: 1, measureIndex: 0, beatInMeasure: 2 },
      { pitch: 'G4', duration: 1, measureIndex: 0, beatInMeasure: 3 },
      { pitch: 'A4', duration: 1, measureIndex: 1, beatInMeasure: 0 },
      { pitch: 'A4', duration: 1, measureIndex: 1, beatInMeasure: 1 },
      { pitch: 'G4', duration: 2, measureIndex: 1, beatInMeasure: 2 },
      { pitch: 'F4', duration: 1, measureIndex: 2, beatInMeasure: 0 },
      { pitch: 'F4', duration: 1, measureIndex: 2, beatInMeasure: 1 },
      { pitch: 'E4', duration: 1, measureIndex: 2, beatInMeasure: 2 },
      { pitch: 'E4', duration: 1, measureIndex: 2, beatInMeasure: 3 },
      { pitch: 'D4', duration: 1, measureIndex: 3, beatInMeasure: 0 },
      { pitch: 'D4', duration: 1, measureIndex: 3, beatInMeasure: 1 },
      { pitch: 'C4', duration: 2, measureIndex: 3, beatInMeasure: 2 }
    ]
  },
  {
    id: 'featured-happy-birthday',
    title: '生日快乐',
    composer: 'Patty Hill / Mildred Hill',
    description: '短小精悍，常见场景曲，便于练习移调与伴奏。',
    difficulty: '简单',
    tempo: 96,
    keySignature: 'F',
    timeSignature: { numerator: 3, denominator: 4 },
    tags: ['流行', '常用', '短曲'],
    notes: [
      { pitch: 'C4', duration: 0.5, measureIndex: 0, beatInMeasure: 0 },
      { pitch: 'C4', duration: 0.5, measureIndex: 0, beatInMeasure: 0.5 },
      { pitch: 'D4', duration: 1, measureIndex: 0, beatInMeasure: 1 },
      { pitch: 'C4', duration: 1, measureIndex: 0, beatInMeasure: 2 },
      { pitch: 'F4', duration: 1, measureIndex: 1, beatInMeasure: 0 },
      { pitch: 'E4', duration: 2, measureIndex: 1, beatInMeasure: 1 },
      { pitch: 'C4', duration: 0.5, measureIndex: 2, beatInMeasure: 0 },
      { pitch: 'C4', duration: 0.5, measureIndex: 2, beatInMeasure: 0.5 },
      { pitch: 'D4', duration: 1, measureIndex: 2, beatInMeasure: 1 },
      { pitch: 'C4', duration: 1, measureIndex: 2, beatInMeasure: 2 },
      { pitch: 'G4', duration: 1, measureIndex: 3, beatInMeasure: 0 },
      { pitch: 'F4', duration: 2, measureIndex: 3, beatInMeasure: 1 }
    ]
  },
  {
    id: 'featured-fur-elise-lite',
    title: '献给爱丽丝（简化版）',
    composer: '贝多芬',
    description: '保留标志性主题，降低演奏门槛，适合进阶入门。',
    difficulty: '中等',
    tempo: 84,
    keySignature: 'Am',
    timeSignature: { numerator: 3, denominator: 8 },
    tags: ['古典', '主题旋律', '简化版'],
    notes: [
      { pitch: 'E5', duration: 0.5, measureIndex: 0, beatInMeasure: 0 },
      { pitch: 'D#5', duration: 0.5, measureIndex: 0, beatInMeasure: 0.5, accidental: 'sharp' },
      { pitch: 'E5', duration: 0.5, measureIndex: 0, beatInMeasure: 1 },
      { pitch: 'D#5', duration: 0.5, measureIndex: 0, beatInMeasure: 1.5, accidental: 'sharp' },
      { pitch: 'E5', duration: 0.5, measureIndex: 0, beatInMeasure: 2 },
      { pitch: 'B4', duration: 0.5, measureIndex: 0, beatInMeasure: 2.5 },
      { pitch: 'D5', duration: 0.5, measureIndex: 1, beatInMeasure: 0 },
      { pitch: 'C5', duration: 0.5, measureIndex: 1, beatInMeasure: 0.5 },
      { pitch: 'A4', duration: 1, measureIndex: 1, beatInMeasure: 1 },
      { pitch: 'C4', duration: 0.5, measureIndex: 2, beatInMeasure: 0 },
      { pitch: 'E4', duration: 0.5, measureIndex: 2, beatInMeasure: 0.5 },
      { pitch: 'A4', duration: 1, measureIndex: 2, beatInMeasure: 1 }
    ]
  },
  {
    id: 'featured-canon-lite',
    title: '卡农（简化版）',
    composer: '帕赫贝尔',
    description: '和声进行经典，旋律温和，适合练习分句与连音。',
    difficulty: '中等',
    tempo: 72,
    keySignature: 'D',
    timeSignature: { numerator: 4, denominator: 4 },
    tags: ['经典', '婚礼', '简化版'],
    notes: [
      { pitch: 'A4', duration: 1, measureIndex: 0, beatInMeasure: 0 },
      { pitch: 'F#4', duration: 1, measureIndex: 0, beatInMeasure: 1, accidental: 'sharp' },
      { pitch: 'G4', duration: 1, measureIndex: 0, beatInMeasure: 2 },
      { pitch: 'D4', duration: 1, measureIndex: 0, beatInMeasure: 3 },
      { pitch: 'E4', duration: 1, measureIndex: 1, beatInMeasure: 0 },
      { pitch: 'A3', duration: 1, measureIndex: 1, beatInMeasure: 1 },
      { pitch: 'B3', duration: 1, measureIndex: 1, beatInMeasure: 2 },
      { pitch: 'F#3', duration: 1, measureIndex: 1, beatInMeasure: 3, accidental: 'sharp' },
      { pitch: 'G3', duration: 1, measureIndex: 2, beatInMeasure: 0 },
      { pitch: 'D3', duration: 1, measureIndex: 2, beatInMeasure: 1 },
      { pitch: 'G3', duration: 1, measureIndex: 2, beatInMeasure: 2 },
      { pitch: 'A3', duration: 1, measureIndex: 2, beatInMeasure: 3 }
    ]
  },
  {
    id: 'featured-jingle-bells',
    title: '铃儿响叮当',
    composer: 'James Lord Pierpont',
    description: '节奏鲜明、重复度高，适合训练稳定节拍。',
    difficulty: '简单',
    tempo: 108,
    keySignature: 'C',
    timeSignature: { numerator: 4, denominator: 4 },
    tags: ['节日', '练习', '高知名度'],
    notes: [
      { pitch: 'E4', duration: 1, measureIndex: 0, beatInMeasure: 0 },
      { pitch: 'E4', duration: 1, measureIndex: 0, beatInMeasure: 1 },
      { pitch: 'E4', duration: 2, measureIndex: 0, beatInMeasure: 2 },
      { pitch: 'E4', duration: 1, measureIndex: 1, beatInMeasure: 0 },
      { pitch: 'E4', duration: 1, measureIndex: 1, beatInMeasure: 1 },
      { pitch: 'E4', duration: 2, measureIndex: 1, beatInMeasure: 2 },
      { pitch: 'E4', duration: 1, measureIndex: 2, beatInMeasure: 0 },
      { pitch: 'G4', duration: 1, measureIndex: 2, beatInMeasure: 1 },
      { pitch: 'C4', duration: 1, measureIndex: 2, beatInMeasure: 2 },
      { pitch: 'D4', duration: 1, measureIndex: 2, beatInMeasure: 3 },
      { pitch: 'E4', duration: 4, measureIndex: 3, beatInMeasure: 0 }
    ]
  }
]

const easyCount = computed(() => featuredScores.filter((item) => item.difficulty === '简单').length)

const durationToType = (duration: number): Note['type'] => {
  if (duration >= 3.5) return 'whole'
  if (duration >= 1.75) return 'half'
  if (duration >= 0.75) return 'quarter'
  if (duration >= 0.375) return 'eighth'
  return 'sixteenth'
}

const pitchToStepFromMiddleC = (pitch: string): number => {
  const match = pitch.match(/^([A-Ga-g])([#b]?)(\d)$/)
  if (!match) return 0
  const [, stepRaw, , octaveRaw] = match
  const step = stepRaw.toUpperCase()
  const octave = Number(octaveRaw)
  const stepIndex: Record<string, number> = { C: 0, D: 1, E: 2, F: 3, G: 4, A: 5, B: 6 }
  return (octave - 4) * 7 + (stepIndex[step] ?? 0)
}

const toEditorPosition = (
  measureIndex: number,
  beatInMeasure: number,
  beatsPerMeasure: number,
  clef: 'treble' | 'bass'
) => {
  const measuresPerRow = 4
  const staffPaddingLeft = 40
  const staffTop = 40
  const rowHeight = 260
  const grandStaffGap = 88
  const pitchStepY = 5
  const staveWidth = 240
  const noteStartOffset = 20
  const noteAreaWidth = 200

  const rowIndex = Math.floor(measureIndex / measuresPerRow)
  const colIndex = measureIndex % measuresPerRow
  const baseX = staffPaddingLeft + colIndex * staveWidth
  const x = baseX + noteStartOffset + (beatInMeasure / Math.max(1, beatsPerMeasure)) * noteAreaWidth
  const yBase = staffTop + rowIndex * rowHeight + (clef === 'bass' ? grandStaffGap : 0)
  const middleCForClef = clef === 'bass' ? 78 : 50
  return { x, yBase, middleCForClef, pitchStepY }
}

const openInEditor = (item: FeaturedScore) => {
  const beatsPerMeasure = (item.timeSignature.numerator * 4) / item.timeSignature.denominator
  const notes: Note[] = item.notes.map((entry, index) => {
    const clef = entry.clef ?? 'treble'
    const pos = toEditorPosition(entry.measureIndex, entry.beatInMeasure, beatsPerMeasure, clef)
    const stepFromMiddleC = pitchToStepFromMiddleC(entry.pitch)
    return {
      id: `${item.id}-note-${index}`,
      type: durationToType(entry.duration),
      pitch: entry.pitch,
      duration: entry.duration,
      position: {
        x: pos.x,
        y: pos.yBase + pos.middleCForClef - stepFromMiddleC * pos.pitchStepY
      },
      clef,
      measureIndex: entry.measureIndex,
      beatInMeasure: entry.beatInMeasure,
      accidental: entry.accidental
    }
  })

  const draft: Score = {
    id: Date.now().toString(),
    title: item.title,
    composer: item.composer,
    tempo: item.tempo,
    timeSignature: item.timeSignature,
    keySignature: item.keySignature,
    notes,
    measures: [],
    createdAt: new Date(),
    updatedAt: new Date(),
    isDraft: true,
    description: item.description,
    tags: item.tags
  }

  const created = scoreStore.createScore(draft)
  router.push(`/editor/${created.id}`)
}
</script>

<style scoped>
.featured-page {
  @apply px-4 md:px-6 py-6 md:py-8;
}

.hero {
  @apply rounded-2xl p-6 md:p-8 mb-6 grid grid-cols-1 md:grid-cols-3 gap-4;
  background: linear-gradient(135deg, #0f766e 0%, #115e59 35%, #164e63 100%);
}

.hero-content {
  @apply md:col-span-2 text-white;
}

.hero-kicker {
  @apply text-xs uppercase tracking-[0.2em] text-teal-200 mb-2;
}

.hero-title {
  @apply text-3xl md:text-4xl font-bold mb-2;
}

.hero-desc {
  @apply text-sm md:text-base text-teal-100;
}

.hero-stats {
  @apply grid grid-cols-2 md:grid-cols-1 gap-3;
}

.stat-card {
  @apply rounded-xl p-3 border border-white/20 bg-white/10 text-white;
}

.stat-value {
  @apply text-2xl font-bold;
}

.stat-label {
  @apply text-xs text-teal-100 mt-1;
}

.score-section {
  @apply rounded-2xl border border-slate-200 bg-white p-4 md:p-6;
}

.section-header {
  @apply mb-4;
}

.section-header h2 {
  @apply text-xl font-semibold text-slate-800;
}

.section-header p {
  @apply text-sm text-slate-500 mt-1;
}

.score-grid {
  @apply grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4;
}

.score-card {
  @apply rounded-xl border border-slate-200 p-4 bg-slate-50/50 hover:bg-white hover:shadow-md transition-all;
}

.score-head {
  @apply flex items-start justify-between gap-3;
}

.score-title {
  @apply text-lg font-semibold text-slate-800;
}

.score-level {
  @apply text-xs px-2 py-1 rounded-full bg-amber-100 text-amber-700 whitespace-nowrap;
}

.score-composer {
  @apply text-sm text-slate-600 mt-1;
}

.score-desc {
  @apply text-sm text-slate-500 mt-3 leading-relaxed min-h-[44px];
}

.score-meta {
  @apply mt-3 flex flex-wrap gap-2 text-xs text-slate-600;
}

.score-tags {
  @apply mt-3 flex flex-wrap gap-2;
}

.tag {
  @apply text-[11px] px-2 py-1 rounded bg-slate-200 text-slate-700;
}

.open-btn {
  @apply mt-4 w-full rounded-lg py-2 text-sm font-medium text-white bg-teal-700 hover:bg-teal-800 transition-colors;
}
</style>
