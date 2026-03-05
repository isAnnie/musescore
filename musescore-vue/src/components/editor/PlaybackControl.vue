<template>
  <div class="playback-control">
    <!-- 时间显示 -->
    <div class="time-display">
      <span class="current-time">{{ formatTime(currentTime) }}</span>
      <span class="separator">/</span>
      <span class="total-time">{{ formatTime(totalTime) }}</span>
    </div>

    <!-- 进度条 -->
    <div class="progress-container">
      <input
        type="range"
        v-model="localCurrentTime"
        :max="safeTotalTime"
        step="0.1"
        @input="handleSeek"
        class="progress-slider"
      />
      <div class="progress-bar" :style="{ width: `${progressPercentage}%` }"></div>
    </div>

    <!-- 控制按钮 -->
    <div class="control-buttons">
      <button @click="rewind" class="control-btn" title="快退">
        <Rewind class="w-5 h-5" />
      </button>
      
      <button v-if="!isPlaying" @click="play" class="control-btn play-btn" title="播放">
        <Play class="w-5 h-5" />
      </button>
      <button v-else @click="pause" class="control-btn pause-btn" title="暂停">
        <Pause class="w-5 h-5" />
      </button>
      
      <button @click="stop" class="control-btn" title="停止">
        <Square class="w-5 h-5" />
      </button>
      
      <button @click="forward" class="control-btn" title="快进">
        <FastForward class="w-5 h-5" />
      </button>
      
      <!-- 循环按钮 -->
      <button 
        @click="toggleLoop" 
        :class="['control-btn loop-btn', { 'active': isLooping }]"
        title="循环播放"
      >
        <Repeat class="w-5 h-5" />
      </button>
    </div>

    <!-- 速度控制 -->
    <div class="tempo-control">
      <button @click="decreaseTempo" class="tempo-btn" title="减慢">
        <Minus class="w-4 h-4" />
      </button>
      
      <div class="tempo-display">
        <span class="tempo-value">{{ tempo }} BPM</span>
      </div>
      
      <button @click="increaseTempo" class="tempo-btn" title="加快">
        <Plus class="w-4 h-4" />
      </button>
      
      <!-- 速度滑块 -->
      <input
        type="range"
        v-model="localTempo"
        min="40"
        max="200"
        @change="updateTempo"
        class="tempo-slider"
      />
    </div>

    <!-- 音量控制 -->
    <div class="volume-control">
      <button @click="toggleMute" class="volume-btn" title="静音">
        <Volume2 v-if="!isMuted" class="w-5 h-5" />
        <VolumeX v-else class="w-5 h-5" />
      </button>
      
      <input
        type="range"
        v-model="volume"
        min="0"
        max="100"
        @input="updateVolume"
        class="volume-slider"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import {
  Play,
  Pause,
  Square,
  Rewind,
  FastForward,
  Repeat,
  Volume2,
  VolumeX,
  Plus,
  Minus
} from 'lucide-vue-next'

const props = defineProps<{
  currentTime: number
  totalTime: number
  tempo: number
  isPlaying: boolean
}>()

const emit = defineEmits([
  'play',
  'pause',
  'stop',
  'seek',
  'tempo-change',
  'volume-change'
])

// 本地状态
const localCurrentTime = ref(props.currentTime)
const localTempo = ref(props.tempo)
const volume = ref(80)
const isMuted = ref(false)
const isLooping = ref(false)

const safeTotalTime = computed(() => {
  if (!Number.isFinite(props.totalTime) || props.totalTime <= 0) return 0.1
  return props.totalTime
})

// 计算进度百分比
const progressPercentage = computed(() => {
  if (!Number.isFinite(props.totalTime) || props.totalTime <= 0) return 0
  if (!Number.isFinite(props.currentTime) || props.currentTime <= 0) return 0
  const ratio = props.currentTime / props.totalTime
  return Math.min(100, Math.max(0, ratio * 100))
})

// 监听props变化
watch(() => props.currentTime, (newTime) => {
  if (!Number.isFinite(newTime) || newTime < 0) {
    localCurrentTime.value = 0
    return
  }
  localCurrentTime.value = Math.min(newTime, safeTotalTime.value)
})

watch(() => props.totalTime, (newTotalTime) => {
  if (!Number.isFinite(newTotalTime) || newTotalTime <= 0) {
    localCurrentTime.value = 0
    return
  }
  localCurrentTime.value = Math.min(localCurrentTime.value, newTotalTime)
})

watch(() => props.tempo, (newTempo) => {
  localTempo.value = newTempo
})

// 播放控制方法
const play = () => {
  emit('play')
}

const pause = () => {
  emit('pause')
}

const stop = () => {
  emit('stop')
}

const rewind = () => {
  const newTime = Math.max(0, props.currentTime - 5)
  emit('seek', newTime)
}

const forward = () => {
  const newTime = Math.min(safeTotalTime.value, props.currentTime + 5)
  emit('seek', newTime)
}

// 跳转处理
const handleSeek = () => {
  const target = Math.min(safeTotalTime.value, Math.max(0, Number(localCurrentTime.value) || 0))
  emit('seek', target)
}

// 更新速度
const updateTempo = () => {
  emit('tempo-change', localTempo.value)
}

// 速度调整
const increaseTempo = () => {
  localTempo.value = Math.min(200, localTempo.value + 5)
  updateTempo()
}

const decreaseTempo = () => {
  localTempo.value = Math.max(40, localTempo.value - 5)
  updateTempo()
}

// 音量控制
const updateVolume = () => {
  emit('volume-change', volume.value)
}

const toggleMute = () => {
  isMuted.value = !isMuted.value
  emit('volume-change', isMuted.value ? 0 : volume.value)
}

// 循环控制
const toggleLoop = () => {
  isLooping.value = !isLooping.value
  // 这里可以集成循环播放逻辑
}

// 时间格式化
const formatTime = (seconds: number): string => {
  if (!Number.isFinite(seconds) || seconds <= 0) return '0:00'
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins}:${secs.toString().padStart(2, '0')}`
}
</script>

<style scoped>
.playback-control {
  @apply bg-white border rounded-xl p-4 shadow-lg min-w-80;
}

/* 时间显示 */
.time-display {
  @apply flex items-center justify-center space-x-2 mb-4;
}

.current-time {
  @apply font-mono text-gray-800;
}

.separator {
  @apply text-gray-400;
}

.total-time {
  @apply font-mono text-gray-500;
}

/* 进度条 */
.progress-container {
  @apply relative h-2 bg-gray-200 rounded-full mb-4 cursor-pointer;
}

.progress-slider {
  @apply absolute inset-0 w-full h-full opacity-0 cursor-pointer z-10;
}

.progress-bar {
  @apply absolute left-0 top-0 h-full bg-blue-500 rounded-full;
}

/* 控制按钮 */
.control-buttons {
  @apply flex items-center justify-center space-x-4 mb-4;
}

.control-btn {
  @apply p-3 rounded-full hover:bg-gray-100 transition-colors;
}

.play-btn {
  @apply bg-green-500 text-white hover:bg-green-600;
}

.pause-btn {
  @apply bg-yellow-500 text-white hover:bg-yellow-600;
}

.loop-btn.active {
  @apply bg-purple-500 text-white hover:bg-purple-600;
}

/* 速度控制 */
.tempo-control {
  @apply flex items-center space-x-3 mb-4;
}

.tempo-btn {
  @apply p-2 rounded-full hover:bg-gray-100;
}

.tempo-display {
  @apply flex-1 text-center;
}

.tempo-value {
  @apply font-mono font-medium text-gray-800;
}

.tempo-slider {
  @apply flex-1 accent-blue-500;
}

/* 音量控制 */
.volume-control {
  @apply flex items-center space-x-3;
}

.volume-btn {
  @apply p-2 rounded-full hover:bg-gray-100;
}

.volume-slider {
  @apply flex-1 accent-green-500;
}
</style>
