<template>
  <div class="editor-header">
    <div class="score-info">
      <input
        v-model="localTitle"
        @blur="updateTitle"
        @keyup.enter="updateTitle"
        class="score-title-input"
        placeholder="输入乐谱标题"
      />
      <div class="score-meta">
        <input
          v-model="localComposer"
          @blur="updateComposer"
          class="meta-input"
          placeholder="作曲"
        />
        <select v-model="localTimeSignature.numerator" @change="updateTimeSignature" class="meta-select">
          <option v-for="n in timeNumerators" :key="n" :value="n">{{ n }}</option>
        </select>
        <span>/</span>
        <select v-model="localTimeSignature.denominator" @change="updateTimeSignature" class="meta-select">
          <option v-for="d in timeDenominators" :key="d" :value="d">{{ d }}</option>
        </select>
        <select v-model="localKeySignature" @change="updateKeySignature" class="meta-select">
          <option v-for="key in keySignatures" :key="key" :value="key">{{ key }}</option>
        </select>
      </div>
    </div>

    <div class="playback-controls">
      <button @click="$emit('stop')" class="control-btn">Stop</button>
      <button @click="$emit('play')" class="control-btn play-btn">{{ isPlaying ? 'Pause' : 'Play' }}</button>
      <input type="range" v-model.number="localTempo" min="40" max="200" @change="updateTempo" />
      <span class="tempo-value">{{ localTempo }} BPM</span>
      <button @click="$emit('toggle-metronome')" :class="['control-btn', { active: metronomeActive }]">Metro</button>
    </div>

    <div class="action-buttons">
      <button @click="$emit('save')" class="action-btn save-btn">保存</button>

      <div class="publish-wrapper">
        <button @click="togglePublishPanel" class="action-btn publish-btn">发布</button>
        <div v-if="showPublishPanel" class="publish-panel">
          <label class="field-label">可见性</label>
          <select v-model="publishVisibility" class="field-control">
            <option value="public">公开</option>
            <option value="unlisted">不公开链接可见</option>
            <option value="private">仅自己可见</option>
          </select>

          <label class="field-label">标签（逗号分隔）</label>
          <input v-model="publishTagsInput" class="field-control" placeholder="piano, original" />

          <label class="field-label">简介</label>
          <textarea v-model="publishDescription" class="field-control" rows="3" placeholder="介绍这首作品"></textarea>

          <div class="panel-actions">
            <button class="action-btn" @click="showPublishPanel = false">取消</button>
            <button class="action-btn publish-btn" @click="confirmPublish">确认发布</button>
          </div>
        </div>
      </div>

      <div class="dropdown export-dropdown">
        <button class="action-btn export-btn">导出</button>
        <div class="dropdown-menu">
          <button @click="$emit('export', 'midi')" class="dropdown-item">MIDI</button>
          <button @click="$emit('export', 'pdf')" class="dropdown-item">PDF</button>
          <button @click="$emit('export', 'musicxml')" class="dropdown-item">MusicXML</button>
        </div>
      </div>

      <button @click="shareScore" class="action-btn">分享</button>
      <button @click="printScore" class="action-btn">打印</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { Score, TimeSignature } from '@/types/score'

interface Props {
  score: Score | null
  metronomeActive: boolean
  isPlaying: boolean
}

const props = defineProps<Props>()
const emit = defineEmits(['save', 'export', 'play', 'stop', 'toggle-metronome', 'publish', 'tempo-change'])

const localTitle = ref('')
const localComposer = ref('')
const localTimeSignature = ref<TimeSignature>({ numerator: 4, denominator: 4 })
const localKeySignature = ref('C')
const localTempo = ref(120)

const showPublishPanel = ref(false)
const publishVisibility = ref<'public' | 'private' | 'unlisted'>('public')
const publishTagsInput = ref('')
const publishDescription = ref('')

const timeNumerators = [2, 3, 4, 6, 9, 12]
const timeDenominators = [2, 4, 8, 16]
const keySignatures = ['C', 'G', 'D', 'A', 'E', 'B', 'F#', 'C#', 'F', 'Bb', 'Eb', 'Ab', 'Db', 'Gb', 'Cb']

const toTempoNumber = (value: unknown) => {
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) return 120
  return Math.min(200, Math.max(40, parsed))
}

watch(() => props.score, (newScore) => {
  if (!newScore) return
  localTitle.value = newScore.title
  localComposer.value = newScore.composer
  localTimeSignature.value = { ...newScore.timeSignature }
  localKeySignature.value = newScore.keySignature
  localTempo.value = toTempoNumber(newScore.tempo)
}, { immediate: true })

const updateTitle = () => {
  if (!props.score) return
  props.score.title = localTitle.value
  emit('save')
}

const updateComposer = () => {
  if (!props.score) return
  props.score.composer = localComposer.value
  emit('save')
}

const updateTimeSignature = () => {
  if (!props.score) return
  props.score.timeSignature = { ...localTimeSignature.value }
  emit('save')
}

const updateKeySignature = () => {
  if (!props.score) return
  props.score.keySignature = localKeySignature.value
  emit('save')
}

const updateTempo = () => {
  const nextTempo = toTempoNumber(localTempo.value)
  localTempo.value = nextTempo
  emit('tempo-change', nextTempo)
}

const parsePublishTags = (input: string) => input
  .split(',')
  .map((item) => item.trim())
  .filter(Boolean)

const togglePublishPanel = () => {
  showPublishPanel.value = !showPublishPanel.value
}

const confirmPublish = () => {
  emit('publish', {
    visibility: publishVisibility.value,
    tags: parsePublishTags(publishTagsInput.value),
    description: publishDescription.value.trim()
  })
  showPublishPanel.value = false
}

const shareScore = async () => {
  if (!props.score) return

  try {
    const shareData = {
      title: props.score.title,
      text: `查看我的乐谱: ${props.score.title}`,
      url: `${window.location.origin}/score/${props.score.id}`
    }

    if (navigator.share) {
      await navigator.share(shareData)
      return
    }

    await navigator.clipboard.writeText(shareData.url)
    alert('链接已复制到剪贴板')
  } catch (error) {
    console.error('分享失败:', error)
  }
}

const printScore = () => {
  window.print()
}
</script>

<style scoped>
.editor-header {
  @apply flex items-center justify-between p-4 bg-white border-b shadow-sm gap-4;
}

.score-info {
  @apply min-w-64;
}

.score-title-input {
  @apply text-xl font-bold bg-transparent border-none outline-none px-2 py-1 rounded w-full;
}

.score-meta {
  @apply flex items-center gap-2 mt-1;
}

.meta-input,
.meta-select {
  @apply text-sm border rounded px-2 py-1;
}

.playback-controls {
  @apply flex items-center gap-2;
}

.control-btn {
  @apply px-3 py-1 rounded bg-gray-100 hover:bg-gray-200;
}

.control-btn.active,
.play-btn {
  @apply bg-blue-500 text-white;
}

.tempo-value {
  @apply text-sm text-gray-700;
}

.action-buttons {
  @apply flex items-center gap-2;
}

.action-btn {
  @apply px-3 py-2 rounded text-sm bg-gray-100 hover:bg-gray-200;
}

.save-btn {
  @apply bg-blue-500 text-white hover:bg-blue-600;
}

.publish-wrapper {
  @apply relative;
}

.publish-btn {
  @apply bg-emerald-500 text-white hover:bg-emerald-600;
}

.publish-panel {
  @apply absolute right-0 top-full mt-2 w-80 bg-white border rounded-lg shadow-lg p-3 z-50;
}

.field-label {
  @apply block text-xs text-gray-600 mb-1 mt-1;
}

.field-control {
  @apply w-full border rounded px-2 py-1 text-sm;
}

.panel-actions {
  @apply flex justify-end gap-2 mt-3;
}

.export-dropdown {
  @apply relative;
}

.export-btn {
  @apply bg-purple-500 text-white hover:bg-purple-600;
}

.dropdown-menu {
  @apply absolute top-full right-0 mt-1 bg-white border rounded shadow-lg hidden z-50;
}

.export-dropdown:hover .dropdown-menu {
  @apply block;
}

.dropdown-item {
  @apply block w-full text-left px-4 py-2 hover:bg-gray-100;
}
</style>
