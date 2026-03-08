<template>
  <div class="editor-properties">
    <div class="properties-header">
      <h3 class="properties-title">属性</h3>
      <button class="properties-close" @click="$emit('close')">✕</button>
    </div>

    <!-- 音符属性 -->
    <div v-if="note" class="properties-content">
      <!-- 音高控制 -->
      <div class="property-section">
        <h4 class="section-title">音高</h4>
        <div class="piano-widget">
          <div class="piano-keys">
            <button
              v-for="key in pianoKeys"
              :key="key.note"
              :class="['piano-key', key.type, { active: isKeyActive(key.note) }]"
              @click="updateNotePitch(key.note)"
              :title="key.note"
            >
              {{ key.label }}
            </button>
          </div>
        </div>
        
        <!-- 音高微调 -->
        <div class="pitch-control">
          <label class="control-label">音高微调</label>
          <div class="slider-container">
            <input
              type="range"
              v-model="localOctave"
              min="2"
              max="6"
              @change="updateOctave"
              class="slider"
            />
            <span class="slider-value">{{ localOctave }} 八度</span>
          </div>
        </div>
      </div>

      <!-- 音符属性 -->
      <div class="property-section">
        <h4 class="section-title">音符属性</h4>
        
        <!-- 音符类型 -->
        <div class="property-group">
          <label class="property-label">音符类型</label>
          <div class="note-type-buttons">
            <button
              v-for="type in noteTypes"
              :key="type.value"
              @click="updateNoteType(type.value)"
              :class="['note-type-btn', { active: note.type === type.value }]"
            >
              <span class="note-type-icon">{{ type.icon }}</span>
              <span class="note-type-label">{{ type.label }}</span>
            </button>
          </div>
        </div>

        <!-- 时长 -->
        <div class="property-group">
          <label class="property-label">时长</label>
          <div class="duration-control">
            <input
              type="range"
              v-model="localDuration"
              min="0.25"
              max="4"
              step="0.25"
              @change="updateDuration"
              class="slider"
            />
            <span class="slider-value">{{ localDuration }} 拍</span>
          </div>
        </div>

        <!-- 升降号 -->
        <div class="property-group">
          <label class="property-label">升降号</label>
          <div class="accidental-buttons">
            <button
              v-for="acc in accidentals"
              :key="acc.value"
              @click="updateAccidental(acc.value)"
              :class="['accidental-btn', { active: note.accidental === acc.value }]"
            >
              <span class="accidental-icon">{{ acc.symbol }}</span>
              <span class="accidental-label">{{ acc.label }}</span>
            </button>
          </div>
        </div>

        <!-- 演奏记号 -->
        <div class="property-group">
          <label class="property-label">演奏记号</label>
          <div class="articulation-buttons">
            <button
              v-for="art in articulations"
              :key="art.value"
              @click="updateArticulation(art.value)"
              :class="['articulation-btn', { active: note.articulation === art.value }]"
            >
              <span class="articulation-icon">{{ art.symbol }}</span>
              <span class="articulation-label">{{ art.label }}</span>
            </button>
          </div>
        </div>

        <!-- 延音踏板 -->
        <div class="property-group">
          <label class="property-label">延音踏板</label>
          <div class="pedal-buttons">
            <button
              @click="togglePedalStart"
              :class="['pedal-btn', { active: note.pedalStart }]"
              title="踏板开始"
            >
              Ped.
            </button>
            <button
              @click="togglePedalEnd"
              :class="['pedal-btn', { active: note.pedalEnd }]"
              title="踏板结束"
            >
              *
            </button>
          </div>
        </div>
      </div>

      <!-- MIDI属性 -->
      <div class="property-section">
        <h4 class="section-title">MIDI属性</h4>
        
        <!-- 力度 -->
        <div class="property-group">
          <label class="property-label">力度</label>
          <div class="velocity-control">
            <input
              type="range"
              v-model="localVelocity"
              min="1"
              max="127"
              @change="updateVelocity"
              class="slider"
            />
            <div class="slider-info">
              <span class="slider-value">{{ localVelocity }}</span>
              <span class="slider-hint">(0-127)</span>
            </div>
          </div>
        </div>

        <!-- 通道 -->
        <div class="property-group">
          <label class="property-label">MIDI通道</label>
          <div class="channel-control">
            <input
              type="range"
              v-model="localChannel"
              min="0"
              max="15"
              @change="updateChannel"
              class="slider"
            />
            <span class="slider-value">{{ localChannel }}</span>
          </div>
        </div>
      </div>

      <!-- 位置信息 -->
      <div class="property-section">
        <h4 class="section-title">位置</h4>
        <div class="position-grid">
          <div class="position-input">
            <label>X</label>
            <input
              type="number"
              v-model="localPosition.x"
              @change="updatePosition"
              class="input"
            />
          </div>
          <div class="position-input">
            <label>Y</label>
            <input
              type="number"
              v-model="localPosition.y"
              @change="updatePosition"
              class="input"
            />
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="property-actions">
        <button class="btn-delete" @click="$emit('delete-note')">
          删除音符
        </button>
        <button class="btn-play" @click="$emit('play-note')">
          试听
        </button>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="properties-empty">
      <div class="empty-icon">🎵</div>
      <p class="empty-text">未选中音符</p>
      <p class="empty-hint">点击音符以编辑其属性</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import type { Note } from '@/types/score'

const props = defineProps<{
  note: Note | null
}>()

const emit = defineEmits(['update-note', 'delete-note', 'play-note', 'close'])

// 本地状态
const localOctave = ref(4)
const localDuration = ref(1)
const localVelocity = ref(64)
const localChannel = ref(0)
const localPosition = ref({ x: 0, y: 0 })

// 钢琴键盘配置
const pianoKeys = computed(() => {
  const octave = localOctave.value
  return [
    { note: `C${octave}`, label: 'C', type: 'white' },
    { note: `C#${octave}`, label: 'C#', type: 'black' },
    { note: `D${octave}`, label: 'D', type: 'white' },
    { note: `D#${octave}`, label: 'D#', type: 'black' },
    { note: `E${octave}`, label: 'E', type: 'white' },
    { note: `F${octave}`, label: 'F', type: 'white' },
    { note: `F#${octave}`, label: 'F#', type: 'black' },
    { note: `G${octave}`, label: 'G', type: 'white' },
    { note: `G#${octave}`, label: 'G#', type: 'black' },
    { note: `A${octave}`, label: 'A', type: 'white' },
    { note: `A#${octave}`, label: 'A#', type: 'black' },
    { note: `B${octave}`, label: 'B', type: 'white' }
  ]
})

// 音符类型配置
const noteTypes = [
  { value: 'whole', label: '全音符', icon: '𝅝' },
  { value: 'half', label: '二分音符', icon: '𝅗𝅥' },
  { value: 'quarter', label: '四分音符', icon: '𝅘𝅥' },
  { value: 'eighth', label: '八分音符', icon: '𝅘𝅥𝅮' },
  { value: 'sixteenth', label: '十六分音符', icon: '𝅘𝅥𝅯' }
]

// 升降号配置
const accidentals = [
  { value: 'sharp', label: '升号', symbol: '♯' },
  { value: 'flat', label: '降号', symbol: '♭' },
  { value: 'natural', label: '还原', symbol: '♮' }
]

// 演奏记号配置
const articulations = [
  { value: 'staccato', label: '断奏', symbol: '•' },
  { value: 'tenuto', label: '保持音', symbol: '–' },
  { value: 'accent', label: '重音', symbol: '>' }
]

// 检查键位是否激活
const isKeyActive = (noteName: string) => {
  return props.note?.pitch === noteName
}

// 监听音符变化
watch(() => props.note, (newNote) => {
  if (newNote) {
    // 更新本地状态
    const octave = parseInt(newNote.pitch.slice(-1))
    localOctave.value = isNaN(octave) ? 4 : octave
    localDuration.value = newNote.duration
    localVelocity.value = newNote.velocity || 64
    localChannel.value = newNote.channel || 0
    localPosition.value = { ...newNote.position }
  }
}, { immediate: true })

// 更新音高
const updateNotePitch = (pitch: string) => {
  if (props.note) {
    emit('update-note', { pitch })
  }
}

// 更新八度
const updateOctave = () => {
  if (props.note) {
    const pitch = props.note.pitch.slice(0, -1) + localOctave.value
    emit('update-note', { pitch })
  }
}

// 更新音符类型
const updateNoteType = (type: string) => {
  if (props.note) {
    emit('update-note', { type })
  }
}

// 更新时长
const updateDuration = () => {
  if (props.note) {
    emit('update-note', { duration: localDuration.value })
  }
}

// 更新升降号
const updateAccidental = (accidental: string | undefined) => {
  if (props.note) {
    const newAccidental = props.note.accidental === accidental ? undefined : accidental
    emit('update-note', { accidental: newAccidental })
  }
}

// 更新演奏记号
const updateArticulation = (articulation: string | undefined) => {
  if (props.note) {
    const newArticulation = props.note.articulation === articulation ? undefined : articulation
    emit('update-note', { articulation: newArticulation })
  }
}

const togglePedalStart = () => {
  if (!props.note) return
  emit('update-note', { pedalStart: !props.note.pedalStart })
}

const togglePedalEnd = () => {
  if (!props.note) return
  emit('update-note', { pedalEnd: !props.note.pedalEnd })
}

// 更新力度
const updateVelocity = () => {
  if (props.note) {
    emit('update-note', { velocity: localVelocity.value })
  }
}

// 更新通道
const updateChannel = () => {
  if (props.note) {
    emit('update-note', { channel: localChannel.value })
  }
}

// 更新位置
const updatePosition = () => {
  if (props.note) {
    emit('update-note', { position: { ...localPosition.value } })
  }
}
</script>

<style scoped>
.editor-properties {
  @apply w-full h-full flex flex-col border-l border-slate-200;
  background: linear-gradient(180deg, #f8fafc 0%, #ffffff 16%);
}

.properties-header {
  @apply sticky top-0 z-10 flex items-center justify-between px-4 py-3 border-b border-slate-200;
  background: rgba(248, 250, 252, 0.92);
  backdrop-filter: blur(8px);
}

.properties-title {
  @apply text-base font-semibold text-slate-800 tracking-wide;
}

.properties-close {
  @apply w-8 h-8 rounded-md text-base text-slate-500 hover:text-slate-700
         hover:bg-slate-100 transition-all;
}

.properties-content {
  @apply flex-1 px-3 py-3 overflow-y-auto space-y-3;
  scrollbar-width: thin;
}

.properties-empty {
  @apply flex-1 flex flex-col items-center justify-center p-8;
}

.empty-icon {
  @apply text-6xl mb-4;
}

.empty-text {
  @apply text-lg text-gray-600 mb-2;
}

.empty-hint {
  @apply text-sm text-gray-500;
}

/* 属性区块 */
.property-section {
  @apply rounded-xl border border-slate-200 bg-white p-3 shadow-sm;
}

.section-title {
  @apply text-xs font-semibold text-slate-600 uppercase tracking-wider mb-2;
}

/* 钢琴小部件 */
.piano-widget {
  @apply mb-3;
}

.piano-keys {
  @apply flex relative h-16 rounded-md overflow-hidden border border-slate-300 bg-slate-100;
}

.piano-key {
  @apply flex-1 flex items-end justify-center pb-1 text-[10px] font-medium relative transition-all;
}

.piano-key.white {
  @apply bg-white hover:bg-slate-100;
  height: 100%;
  z-index: 1;
}

.piano-key.black {
  @apply absolute bg-slate-800 text-slate-100 hover:bg-slate-900 rounded-b-sm;
  width: 16%;
  height: 60%;
  z-index: 2;
}

.piano-key.active {
  @apply bg-blue-500 text-white;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.4);
}

.piano-key.active.black {
  @apply bg-blue-700;
}

/* 键位定位 */
.piano-keys .piano-key:nth-child(2) { left: 8%; }   /* C# */
.piano-keys .piano-key:nth-child(4) { left: 24%; }  /* D# */
.piano-keys .piano-key:nth-child(7) { left: 56%; }  /* F# */
.piano-keys .piano-key:nth-child(9) { left: 72%; }  /* G# */
.piano-keys .piano-key:nth-child(11) { left: 88%; } /* A# */

/* 控制组件 */
.property-group {
  @apply mb-3;
}

.property-label {
  @apply block text-xs font-medium text-slate-600 mb-2;
}

/* 音符类型按钮 */
.note-type-buttons {
  @apply grid grid-cols-3 gap-2;
}

.note-type-btn {
  @apply p-2 border border-slate-200 rounded-lg flex flex-col items-center justify-center
         hover:bg-slate-50 hover:border-slate-300 transition-all;
}

.note-type-btn.active {
  @apply bg-blue-50 border-blue-300 text-blue-700;
}

.note-type-icon {
  @apply text-lg;
}

.note-type-label {
  @apply text-[11px] mt-1;
}

/* 升降号按钮 */
.accidental-buttons {
  @apply grid grid-cols-3 gap-2;
}

.accidental-btn {
  @apply p-2 border border-slate-200 rounded-lg flex flex-col items-center justify-center
         hover:bg-slate-50 hover:border-slate-300 transition-all;
}

.accidental-btn.active {
  @apply bg-amber-50 border-amber-300 text-amber-700;
}

.accidental-icon {
  @apply text-lg;
}

.accidental-label {
  @apply text-[11px] mt-1;
}

/* 演奏记号按钮 */
.articulation-buttons {
  @apply grid grid-cols-3 gap-2;
}

.articulation-btn {
  @apply p-2 border border-slate-200 rounded-lg flex flex-col items-center justify-center
         hover:bg-slate-50 hover:border-slate-300 transition-all;
}

.articulation-btn.active {
  @apply bg-emerald-50 border-emerald-300 text-emerald-700;
}

.articulation-icon {
  @apply text-lg font-bold;
}

.articulation-label {
  @apply text-[11px] mt-1;
}

.pedal-buttons {
  @apply grid grid-cols-2 gap-2;
}

.pedal-btn {
  @apply p-2 border border-slate-200 rounded-lg text-sm font-medium text-slate-700
         hover:bg-slate-50 hover:border-slate-300 transition-all;
}

.pedal-btn.active {
  @apply bg-indigo-50 border-indigo-300 text-indigo-700;
}

/* 滑块控制 */
.slider-container {
  @apply flex items-center gap-2;
}

.control-label {
  @apply block text-xs font-medium text-slate-600 mb-2;
}

.slider {
  @apply flex-1 accent-blue-500 h-1.5 rounded-lg;
}

.slider-value {
  @apply text-xs text-slate-600 min-w-[56px] text-right tabular-nums;
}

.slider-info {
  @apply flex items-center gap-1;
}

.slider-hint {
  @apply text-[11px] text-slate-500;
}

/* 力度控制 */
.velocity-control {
  @apply space-y-1;
}

/* 位置输入 */
.position-grid {
  @apply grid grid-cols-2 gap-3;
}

.position-input {
  @apply space-y-1;
}

.position-input label {
  @apply block text-xs text-slate-600;
}

.input {
  @apply w-full px-2.5 py-1.5 text-sm border border-slate-300 rounded-md bg-white
         focus:outline-none focus:ring-2 focus:ring-blue-200 focus:border-blue-400;
}

/* 操作按钮 */
.property-actions {
  @apply sticky bottom-0 bg-white/90 backdrop-blur px-1 pt-3 pb-1 flex gap-2;
  border-top: 1px solid #e2e8f0;
}

.btn-delete {
  @apply flex-1 px-3 py-2 text-sm font-medium bg-red-500 text-white rounded-md
         hover:bg-red-600 transition-colors;
}

.btn-play {
  @apply flex-1 px-3 py-2 text-sm font-medium bg-emerald-500 text-white rounded-md
         hover:bg-emerald-600 transition-colors;
}
</style>
