<template>
  <div class="editor-header">
    <!-- 左侧：乐谱信息 -->
    <div class="score-info">
      <input
        v-model="localTitle"
        @blur="updateTitle"
        @keyup.enter="updateTitle"
        class="score-title-input"
        placeholder="输入乐谱标题"
      />
      
      <div class="score-meta">
        <div class="composer-input">
          <span class="label">作曲:</span>
          <input
            v-model="localComposer"
            @blur="updateComposer"
            placeholder="作曲家"
          />
        </div>
        
        <div class="time-signature-selector">
          <select v-model="localTimeSignature.numerator" @change="updateTimeSignature">
            <option v-for="n in timeNumerators" :key="n" :value="n">{{ n }}</option>
          </select>
          <span class="separator">/</span>
          <select v-model="localTimeSignature.denominator" @change="updateTimeSignature">
            <option v-for="d in timeDenominators" :key="d" :value="d">{{ d }}</option>
          </select>
        </div>
        
        <div class="key-signature-selector">
          <select v-model="localKeySignature" @change="updateKeySignature">
            <option v-for="key in keySignatures" :key="key" :value="key">{{ key }}</option>
          </select>
        </div>
      </div>
    </div>

    <!-- 中间：播放控制 -->
    <div class="playback-controls">
      <button 
        @click="$emit('stop')"
        class="control-btn stop-btn"
        title="停止"
      >
        <span class="icon">⏹️</span>
      </button>
      
      <button 
        @click="$emit('play')"
        :class="['control-btn play-btn', { 'playing': isPlaying }]"
        :title="isPlaying ? '暂停' : '播放'"
      >
        <span class="icon">{{ isPlaying ? '⏸️' : '▶️' }}</span>
      </button>
      
      <div class="tempo-control">
        <label class="tempo-label">速度</label>
        <input
          type="range"
          v-model="localTempo"
          min="40"
          max="200"
          @change="updateTempo"
          class="tempo-slider"
        />
        <span class="tempo-value">{{ localTempo }} BPM</span>
      </div>
      
      <button
        @click="$emit('toggle-metronome')"
        :class="['control-btn metronome-btn', { 'active': metronomeActive }]"
        title="节拍器"
      >
        <span class="icon">🎵</span>
      </button>
    </div>

    <!-- 右侧：操作按钮 -->
    <div class="action-buttons">
      <button
        @click="$emit('save')"
        class="action-btn save-btn"
        title="保存乐谱"
      >
        <span class="icon">💾</span>
        <span class="text">保存</span>
      </button>
      
      <div class="dropdown export-dropdown">
        <button class="action-btn export-btn">
          <span class="icon">📤</span>
          <span class="text">导出</span>
          <span class="arrow">▼</span>
        </button>
        <div class="dropdown-menu">
          <button @click="$emit('export', 'midi')" class="dropdown-item">
            <span class="icon">🎹</span>
            MIDI
          </button>
          <button @click="$emit('export', 'pdf')" class="dropdown-item">
            <span class="icon">📄</span>
            PDF
          </button>
          <button @click="$emit('export', 'musicxml')" class="dropdown-item">
            <span class="icon">📝</span>
            MusicXML
          </button>
        </div>
      </div>
      
      <button
        @click="shareScore"
        class="action-btn share-btn"
        title="分享乐谱"
      >
        <span class="icon">🔗</span>
        <span class="text">分享</span>
      </button>
      
      <button
        @click="printScore"
        class="action-btn print-btn"
        title="打印乐谱"
      >
        <span class="icon">🖨️</span>
        <span class="text">打印</span>
      </button>
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
const emit = defineEmits(['save', 'export', 'play', 'stop', 'toggle-metronome'])

// 本地状态
const localTitle = ref('')
const localComposer = ref('')
const localTimeSignature = ref<TimeSignature>({ numerator: 4, denominator: 4 })
const localKeySignature = ref('C')
const localTempo = ref(120)

// 选项配置
const timeNumerators = [2, 3, 4, 6, 9, 12]
const timeDenominators = [2, 4, 8, 16]
const keySignatures = [
  'C', 'G', 'D', 'A', 'E', 'B', 'F#', 'C#',
  'F', 'Bb', 'Eb', 'Ab', 'Db', 'Gb', 'Cb'
]

// 监听乐谱变化
watch(() => props.score, (newScore) => {
  if (newScore) {
    localTitle.value = newScore.title
    localComposer.value = newScore.composer
    localTimeSignature.value = newScore.timeSignature
    localKeySignature.value = newScore.keySignature
    localTempo.value = newScore.tempo
  }
}, { immediate: true })

// 更新标题
const updateTitle = () => {
  if (props.score) {
    props.score.title = localTitle.value
    emit('save')
  }
}

// 更新作曲家
const updateComposer = () => {
  if (props.score) {
    props.score.composer = localComposer.value
    emit('save')
  }
}

// 更新拍号
const updateTimeSignature = () => {
  if (props.score) {
    props.score.timeSignature = { ...localTimeSignature.value }
    emit('save')
  }
}

// 更新调号
const updateKeySignature = () => {
  if (props.score) {
    props.score.keySignature = localKeySignature.value
    emit('save')
  }
}

// 更新速度
const updateTempo = () => {
  if (props.score) {
    props.score.tempo = localTempo.value
    emit('save')
  }
}

// 分享乐谱
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
    } else {
      // 复制链接到剪贴板
      await navigator.clipboard.writeText(shareData.url)
      alert('链接已复制到剪贴板！')
    }
  } catch (error) {
    console.error('分享失败:', error)
  }
}

// 打印乐谱
const printScore = () => {
  window.print()
}
</script>

<style scoped>
.editor-header {
  @apply flex items-center justify-between p-4 bg-white border-b shadow-sm;
}

/* 乐谱信息样式 */
.score-info {
  @apply flex flex-col space-y-2 min-w-64;
}

.score-title-input {
  @apply text-xl font-bold bg-transparent border-none outline-none 
         hover:bg-gray-100 px-2 py-1 rounded;
}

.score-meta {
  @apply flex items-center space-x-4;
}

.composer-input {
  @apply flex items-center space-x-1;
}

.composer-input .label {
  @apply text-sm text-gray-500;
}

.composer-input input {
  @apply text-sm bg-transparent border-none outline-none px-1 
         hover:bg-gray-100 rounded min-w-20;
}

.time-signature-selector {
  @apply flex items-center space-x-1;
}

.time-signature-selector select {
  @apply text-sm border rounded px-1 py-0.5 outline-none 
         bg-white hover:bg-gray-50;
}

.separator {
  @apply text-gray-400;
}

.key-signature-selector select {
  @apply text-sm border rounded px-1 py-0.5 outline-none 
         bg-white hover:bg-gray-50;
}

/* 播放控制样式 */
.playback-controls {
  @apply flex items-center space-x-4;
}

.control-btn {
  @apply p-2 rounded-full flex items-center justify-center 
         transition-all duration-200;
}

.control-btn:hover {
  @apply transform scale-110;
}

.play-btn {
  @apply w-12 h-12 bg-green-500 hover:bg-green-600 text-white;
}

.play-btn.playing {
  @apply bg-yellow-500 hover:bg-yellow-600;
}

.stop-btn {
  @apply w-10 h-10 bg-red-500 hover:bg-red-600 text-white;
}

.tempo-control {
  @apply flex items-center space-x-2;
}

.tempo-label {
  @apply text-sm text-gray-600;
}

.tempo-slider {
  @apply w-32 accent-blue-500;
}

.tempo-value {
  @apply text-sm font-medium w-16;
}

.metronome-btn {
  @apply w-10 h-10 bg-gray-200 hover:bg-gray-300;
}

.metronome-btn.active {
  @apply bg-blue-500 text-white hover:bg-blue-600;
}

/* 操作按钮样式 */
.action-buttons {
  @apply flex items-center space-x-2;
}

.action-btn {
  @apply flex items-center space-x-1 px-3 py-2 rounded-lg 
         transition-colors;
}

.save-btn {
  @apply bg-blue-500 text-white hover:bg-blue-600;
}

.export-dropdown {
  @apply relative;
}

.export-btn {
  @apply bg-purple-500 text-white hover:bg-purple-600;
}

.share-btn {
  @apply bg-green-500 text-white hover:bg-green-600;
}

.print-btn {
  @apply bg-gray-200 hover:bg-gray-300;
}

.dropdown-menu {
  @apply absolute top-full right-0 mt-1 bg-white border rounded-lg 
         shadow-lg py-1 z-50 min-w-32 hidden group-hover:block;
}

.export-dropdown:hover .dropdown-menu {
  @apply block;
}

.dropdown-item {
  @apply flex items-center space-x-2 px-4 py-2 hover:bg-gray-100 
         w-full text-left;
}
</style>