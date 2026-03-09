<template>
  <div class="editor-toolbar">
    <fieldset class="toolbar-fieldset" :disabled="isPlaying">
    <!-- 工具选择 -->
    <div class="tool-section">
      <h4 class="section-title">工具</h4>
      <div class="tool-grid">
        <button
          @click="selectTool('select')"
          :class="['tool-btn', { 'active': currentTool === 'select' }]"
          title="选择工具 (1)"
        >
          <MousePointer class="w-5 h-5" />
        </button>
        
        <button
          @click="selectTool('note')"
          :class="['tool-btn', { 'active': currentTool === 'note' }]"
          title="音符工具 (2)"
        >
          <Music class="w-5 h-5" />
        </button>
        
        <button
          @click="selectTool('rest')"
          :class="['tool-btn', { 'active': currentTool === 'rest' }]"
          title="休止符工具 (3)"
        >
          <Pause class="w-5 h-5" />
        </button>
        
        <button
          @click="selectTool('chord')"
          :class="['tool-btn', { 'active': currentTool === 'chord' }]"
          title="和弦工具 (4)"
        >
          <Layers class="w-5 h-5" />
        </button>
        
        <button
          @click="selectTool('eraser')"
          :class="['tool-btn', { 'active': currentTool === 'eraser' }]"
          title="橡皮擦工具 (5)"
        >
          <Eraser class="w-5 h-5" />
        </button>
        
        <button
          @click="selectTool('text')"
          :class="['tool-btn', { 'active': currentTool === 'text' }]"
          title="文本工具 (6)"
        >
          <Type class="w-5 h-5" />
        </button>
      </div>
    </div>

    <!-- 音符类型选择 -->
    <div class="tool-section" v-if="currentTool === 'note' || currentTool === 'chord' || currentTool === 'rest'">
      <h4 class="section-title">音符类型</h4>
      <div class="note-type-grid">
        <button
          v-for="noteType in noteTypes"
          :key="noteType.type"
          @click="selectNoteType(noteType.type)"
          :class="['note-type-btn', { 'active': selectedNoteType === noteType.type }]"
          :title="noteType.label"
        >
          <span class="note-icon">{{ noteType.icon }}</span>
          <span class="note-duration">{{ noteType.duration }}拍</span>
        </button>
      </div>
    </div>

    <!-- 升降号选择 -->
    <div class="tool-section" v-if="currentTool === 'note' || currentTool === 'chord'">
      <h4 class="section-title">升降号</h4>
      <div class="accidental-grid">
        <button
          v-for="acc in accidentals"
          :key="acc.type"
          @click="selectAccidental(acc.type)"
          :class="['accidental-btn', { 'active': selectedAccidental === acc.type }]"
          :title="acc.label"
        >
          <span class="acc-icon">{{ acc.symbol }}</span>
        </button>
      </div>
    </div>

    <!-- 演奏记号 -->
    <div class="tool-section" v-if="currentTool === 'note' || currentTool === 'chord'">
      <h4 class="section-title">演奏记号</h4>
      <div class="articulation-grid">
        <button
          v-for="art in articulations"
          :key="art.type"
          @click="selectArticulation(art.type)"
          :class="['articulation-btn', { 'active': selectedArticulation === art.type }]"
          :title="art.label"
        >
          <span class="art-icon">{{ art.symbol }}</span>
        </button>
      </div>
    </div>

    <div class="tool-section" v-if="currentTool === 'note' || currentTool === 'chord' || currentTool === 'rest'">
      <h4 class="section-title">节奏规则</h4>
      <div class="rhythm-actions">
        <label class="snap-label">
          <span>吸附精度</span>
          <select
            class="snap-select"
            :value="beatSnap"
            @change="updateBeatSnap"
          >
            <option v-for="option in snapOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </label>
        <button
          class="rule-btn"
          :class="{ active: dottedNote }"
          @click="$emit('dotted-change', !dottedNote)"
          title="附点音符"
        >
          附点
        </button>
        <button
          class="rule-btn"
          :disabled="!canTieNext"
          @click="$emit('tie-next')"
          title="与下一音符连音"
        >
          连音到下一音
        </button>
      </div>
    </div>

    <!-- 撤销/重做 -->
    <div class="tool-section">
      <h4 class="section-title">操作</h4>
      <div class="action-buttons">
        <button
          @click="undo"
          class="action-btn"
          :disabled="!canUndo"
          title="撤销 (Ctrl+Z)"
        >
          <Undo class="w-4 h-4" />
        </button>
        <button
          @click="redo"
          class="action-btn"
          :disabled="!canRedo"
          title="重做 (Ctrl+Shift+Z)"
        >
          <Redo class="w-4 h-4" />
        </button>
      </div>
    </div>

    <!-- 快速帮助 -->
    <div class="tool-section">
      <h4 class="section-title">快捷键</h4>
      <div class="shortcut-list">
        <div class="shortcut-item">
          <kbd class="shortcut-key">1</kbd>
          <span class="shortcut-label">选择工具</span>
        </div>
        <div class="shortcut-item">
          <kbd class="shortcut-key">2</kbd>
          <span class="shortcut-label">音符工具</span>
        </div>
        <div class="shortcut-item">
          <kbd class="shortcut-key">Delete</kbd>
          <span class="shortcut-label">删除音符</span>
        </div>
        <div class="shortcut-item">
          <kbd class="shortcut-key">Space</kbd>
          <span class="shortcut-label">播放/暂停</span>
        </div>
      </div>
    </div>
    </fieldset>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import {
  MousePointer,
  Music,
  Pause,
  Layers,
  Eraser,
  Type,
  Undo,
  Redo
} from 'lucide-vue-next'

const emit = defineEmits([
  'tool-change',
  'note-type-change',
  'accidental-change',
  'articulation-change',
  'dotted-change',
  'tie-next',
  'beat-snap-change'
])

const props = defineProps<{
  currentTool: string
  selectedNoteType: string
  selectedAccidental: string | null
  selectedArticulation: string | null
  dottedNote: boolean
  canTieNext: boolean
  beatSnap: number
  isPlaying: boolean
}>()

// 工具状态
const canUndo = ref(true)
const canRedo = ref(false)

// 音符类型配置
const noteTypes = [
  { type: 'whole', label: '全音符', icon: '𝅝', duration: 4 },
  { type: 'half', label: '二分音符', icon: '𝅗𝅥', duration: 2 },
  { type: 'quarter', label: '四分音符', icon: '𝅘𝅥', duration: 1 },
  { type: 'eighth', label: '八分音符', icon: '𝅘𝅥𝅮', duration: 0.5 },
  { type: 'sixteenth', label: '十六分音符', icon: '𝅘𝅥𝅯', duration: 0.25 }
]

// 升降号配置
const accidentals = [
  { type: 'sharp', label: '升号', symbol: '♯' },
  { type: 'flat', label: '降号', symbol: '♭' },
  { type: 'natural', label: '还原号', symbol: '♮' }
]

// 演奏记号配置
const articulations = [
  { type: 'staccato', label: '断奏', symbol: '.' },
  { type: 'tenuto', label: '保持音', symbol: '–' },
  { type: 'accent', label: '重音', symbol: '>' },
  { type: 'tremolo', label: '颤音', symbol: 'tr' }
]

const snapOptions = [
  { value: 1, label: '1/4 拍' },
  { value: 0.5, label: '1/8 拍' },
  { value: 0.25, label: '1/16 拍' }
]

// 选择工具
const selectTool = (tool: string) => {
  emit('tool-change', tool)
}

// 选择音符类型
const selectNoteType = (type: string) => {
  emit('note-type-change', type)
}

// 选择升降号
const selectAccidental = (type: string | null) => {
  const newType = type === props.selectedAccidental ? null : type
  emit('accidental-change', newType)
}

// 选择演奏记号
const selectArticulation = (type: string | null) => {
  const newType = type === props.selectedArticulation ? null : type
  emit('articulation-change', newType)
}

const updateBeatSnap = (event: Event) => {
  const value = Number((event.target as HTMLSelectElement).value)
  emit('beat-snap-change', value)
}

// 撤销操作
const undo = () => {
  console.log('撤销操作')
  // 实际实现中会调用store的undo方法
}

// 重做操作
const redo = () => {
  console.log('重做操作')
  // 实际实现中会调用store的redo方法
}
</script>

<style scoped>
.editor-toolbar {
  @apply w-64 bg-white border-r p-4 space-y-6 overflow-y-auto;
  height: calc(100vh - 64px);
}

.toolbar-fieldset {
  @apply space-y-6;
  border: 0;
  margin: 0;
  padding: 0;
  min-width: 0;
}

.toolbar-fieldset:disabled {
  opacity: 0.55;
}

.tool-section {
  @apply space-y-2;
}

.section-title {
  @apply text-xs font-semibold text-gray-500 uppercase tracking-wider;
}

/* 工具按钮网格 */
.tool-grid {
  @apply grid grid-cols-3 gap-2;
}

.tool-btn {
  @apply p-3 rounded-lg border flex items-center justify-center 
         hover:bg-gray-50 transition-colors;
}

.tool-btn.active {
  @apply bg-blue-50 border-blue-300 text-blue-600;
}

/* 音符类型网格 */
.note-type-grid {
  @apply grid grid-cols-2 gap-2;
}

.note-type-btn {
  @apply p-2 rounded border flex flex-col items-center justify-center 
         hover:bg-gray-50 transition-colors;
}

.note-type-btn.active {
  @apply bg-blue-50 border-blue-300;
}

.note-icon {
  @apply text-xl;
}

.note-duration {
  @apply text-xs text-gray-500 mt-1;
}

/* 升降号网格 */
.accidental-grid {
  @apply grid grid-cols-3 gap-2;
}

.accidental-btn {
  @apply p-3 rounded border flex items-center justify-center 
         hover:bg-gray-50 transition-colors text-xl;
}

.accidental-btn.active {
  @apply bg-purple-50 border-purple-300 text-purple-600;
}

/* 演奏记号网格 */
.articulation-grid {
  @apply grid grid-cols-3 gap-2;
}

.articulation-btn {
  @apply p-3 rounded border flex items-center justify-center 
         hover:bg-gray-50 transition-colors text-xl font-bold;
}

.articulation-btn.active {
  @apply bg-green-50 border-green-300 text-green-600;
}

.rhythm-actions {
  @apply flex flex-col gap-2;
}

.snap-label {
  @apply flex items-center justify-between gap-2 text-sm text-gray-600;
}

.snap-select {
  @apply border rounded px-2 py-1 text-sm bg-white;
}

.rule-btn {
  @apply px-3 py-2 rounded border text-sm hover:bg-gray-50 transition-colors disabled:opacity-40 disabled:cursor-not-allowed;
}

.rule-btn.active {
  @apply bg-orange-50 border-orange-300 text-orange-700;
}

/* 操作按钮 */
.action-buttons {
  @apply flex space-x-2;
}

.action-btn {
  @apply flex-1 p-2 rounded border flex items-center justify-center 
         hover:bg-gray-50 transition-colors;
}

.action-btn:disabled {
  @apply opacity-50 cursor-not-allowed;
}

/* 快捷键列表 */
.shortcut-list {
  @apply space-y-1;
}

.shortcut-item {
  @apply flex items-center space-x-2;
}

.shortcut-key {
  @apply px-2 py-1 bg-gray-100 rounded text-xs font-mono;
}

.shortcut-label {
  @apply text-xs text-gray-600;
}
</style>
