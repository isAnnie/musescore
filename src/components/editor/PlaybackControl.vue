<template>
  <div class="playback-control">
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

      <button @click="stop" class="control-btn" title="ֹͣ">
        <Square class="w-5 h-5" />
      </button>

      <button @click="forward" class="control-btn" title="快进">
        <FastForward class="w-5 h-5" />
      </button>

      <button
        @click="toggleLoop"
        :class="['control-btn loop-btn', { active: isLooping }]"
        title="循环播放"
      >
        <Repeat class="w-5 h-5" />
      </button>
    </div>

    <div class="tempo-control">
      <button @click="decreaseTempo" class="tempo-btn" title="减慢" :disabled="isPlaying">
        <Minus class="w-4 h-4" />
      </button>

      <div class="tempo-display">
        <span class="tempo-value">{{ tempo }} BPM</span>
      </div>

      <button @click="increaseTempo" class="tempo-btn" title="加快" :disabled="isPlaying">
        <Plus class="w-4 h-4" />
      </button>

      <input
        type="range"
        v-model.number="localTempo"
        min="40"
        max="200"
        @change="updateTempo"
        class="tempo-slider"
        :disabled="isPlaying"
      />
    </div>

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

const localTempo = ref(props.tempo)
const volume = ref(80)
const isMuted = ref(false)
const isLooping = ref(false)

const safeTotalTime = computed(() => {
  if (!Number.isFinite(props.totalTime) || props.totalTime <= 0) return 0.1
  return props.totalTime
})

watch(() => props.tempo, (newTempo) => {
  localTempo.value = Number(newTempo) || 120
})

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

const updateTempo = () => {
  if (props.isPlaying) return
  const nextTempo = Math.min(200, Math.max(40, Number(localTempo.value) || 120))
  localTempo.value = nextTempo
  emit('tempo-change', nextTempo)
}

const increaseTempo = () => {
  localTempo.value = Math.min(200, localTempo.value + 5)
  updateTempo()
}

const decreaseTempo = () => {
  localTempo.value = Math.max(40, localTempo.value - 5)
  updateTempo()
}

const updateVolume = () => {
  emit('volume-change', volume.value)
}

const toggleMute = () => {
  isMuted.value = !isMuted.value
  emit('volume-change', isMuted.value ? 0 : volume.value)
}

const toggleLoop = () => {
  isLooping.value = !isLooping.value
}
</script>

<style scoped>
.playback-control {
  @apply bg-white border rounded-xl p-4 shadow-lg min-w-80;
}

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
