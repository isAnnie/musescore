<template>
  <div class="music-toolbar">
    <div class="toolbar-section">
      <h4 class="section-title">音符</h4>
      <div class="note-buttons">
        <button
          v-for="note in noteTypes"
          :key="note.type"
          :class="['note-btn', { active: selectedNote === note.type }]"
          @click="selectNote(note.type)"
          :title="note.label"
        >
          <span class="note-icon">{{ note.icon }}</span>
        </button>
      </div>
    </div>

    <div class="toolbar-section">
      <h4 class="section-title">音高</h4>
      <div class="piano-keys">
        <button
          v-for="key in pianoKeys"
          :key="key.note"
          :class="['piano-key', key.class]"
          @click="selectPitch(key.note)"
          :title="key.note"
        >
          {{ key.label }}
        </button>
      </div>
    </div>

    <div class="toolbar-section">
      <h4 class="section-title">时长</h4>
      <div class="duration-controls">
        <div class="duration-btn" @click="adjustDuration(-0.5)">-</div>
        <span class="duration-value">{{ currentDuration }}拍</span>
        <div class="duration-btn" @click="adjustDuration(0.5)">+</div>
      </div>
    </div>

    <div class="toolbar-section">
      <h4 class="section-title">演奏</h4>
      <div class="playback-controls">
        <button class="play-btn" @click="playNote">
          <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM9.555 7.168A1 1 0 008 8v4a1 1 0 001.555.832l3-2a1 1 0 000-1.664l-3-2z" clip-rule="evenodd"/>
          </svg>
        </button>
        <button class="stop-btn" @click="stopNote">
          <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8 7a1 1 0 00-1 1v4a1 1 0 001 1h4a1 1 0 001-1V8a1 1 0 00-1-1H8z" clip-rule="evenodd"/>
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import * as Tone from 'tone'

const emit = defineEmits(['note-selected', 'pitch-selected'])

// 音符类型配置
const noteTypes = [
  { type: 'whole', label: '全音符', icon: '𝅝' },
  { type: 'half', label: '二分音符', icon: '𝅗𝅥' },
  { type: 'quarter', label: '四分音符', icon: '𝅘𝅥' },
  { type: 'eighth', label: '八分音符', icon: '𝅘𝅥𝅮' },
  { type: 'sixteenth', label: '十六分音符', icon: '𝅘𝅥𝅯' }
]

// 钢琴键盘配置
const pianoKeys = [
  { note: 'C4', label: 'C', class: 'white-key' },
  { note: 'C#4', label: 'C#', class: 'black-key' },
  { note: 'D4', label: 'D', class: 'white-key' },
  { note: 'D#4', label: 'D#', class: 'black-key' },
  { note: 'E4', label: 'E', class: 'white-key' },
  { note: 'F4', label: 'F', class: 'white-key' },
  { note: 'F#4', label: 'F#', class: 'black-key' },
  { note: 'G4', label: 'G', class: 'white-key' },
  { note: 'G#4', label: 'G#', class: 'black-key' },
  { note: 'A4', label: 'A', class: 'white-key' },
  { note: 'A#4', label: 'A#', class: 'black-key' },
  { note: 'B4', label: 'B', class: 'white-key' }
]

// 状态
const selectedNote = ref('quarter')
const currentPitch = ref('C4')
const currentDuration = ref(1)
const synth = ref<any>(null)

// 初始化音频
onMounted(() => {
  // 等待用户交互后初始化音频上下文
  document.addEventListener('click', () => {
    if (!synth.value) {
      synth.value = new Tone.Synth().toDestination()
    }
  }, { once: true })
})

// 选择音符
const selectNote = (type: string) => {
  selectedNote.value = type
  emit('note-selected', type)
}

// 选择音高
const selectPitch = (pitch: string) => {
  currentPitch.value = pitch
  emit('pitch-selected', pitch)
}

// 调整时长
const adjustDuration = (amount: number) => {
  const newDuration = currentDuration.value + amount
  if (newDuration >= 0.25 && newDuration <= 4) {
    currentDuration.value = Math.round(newDuration * 4) / 4 // 保持0.25的倍数
  }
}

// 播放音符
const playNote = () => {
  if (synth.value) {
    synth.value.triggerAttackRelease(currentPitch.value, currentDuration.value)
  }
}

// 停止音符
const stopNote = () => {
  if (synth.value) {
    synth.value.triggerRelease()
  }
}
</script>

<style scoped>
.music-toolbar {
  @apply bg-white border rounded-lg p-4 space-y-4;
}

.section-title {
  @apply text-sm font-medium text-gray-700 mb-2;
}

.note-buttons {
  @apply flex space-x-2;
}

.note-btn {
  @apply w-10 h-10 flex items-center justify-center border rounded hover:bg-gray-50 transition;
}

.note-btn.active {
  @apply bg-blue-100 border-blue-500 text-blue-700;
}

.note-icon {
  @apply text-lg;
}

.piano-keys {
  @apply flex relative h-20;
}

.white-key {
  @apply flex-1 bg-white border rounded-sm hover:bg-gray-100 transition text-xs;
  height: 100%;
  z-index: 1;
}

.black-key {
  @apply absolute bg-gray-800 text-white rounded-sm hover:bg-gray-900 transition text-xs;
  width: 12%;
  height: 60%;
  z-index: 2;
}

/* 黑色键定位 */
.piano-keys button:nth-child(2) { left: 8%; }  /* C# */
.piano-keys button:nth-child(4) { left: 24%; } /* D# */
.piano-keys button:nth-child(7) { left: 56%; } /* F# */
.piano-keys button:nth-child(9) { left: 72%; } /* G# */
.piano-keys button:nth-child(11) { left: 88%; } /* A# */

.duration-controls {
  @apply flex items-center justify-between space-x-4;
}

.duration-value {
  @apply text-lg font-medium;
}

.duration-btn {
  @apply w-8 h-8 flex items-center justify-center bg-gray-100 rounded-full cursor-pointer hover:bg-gray-200;
}

.playback-controls {
  @apply flex space-x-2;
}

.play-btn, .stop-btn {
  @apply p-2 rounded-full bg-blue-500 text-white hover:bg-blue-600 transition;
}

.stop-btn {
  @apply bg-red-500 hover:bg-red-600;
}
</style>