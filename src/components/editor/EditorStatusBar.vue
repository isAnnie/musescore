<template>
  <div class="editor-status-bar">
    <!-- 左侧：状态信息 -->
    <div class="status-left">
      <!-- 工具提示 -->
      <div class="tool-info">
        <span class="tool-icon">{{ getToolIcon }}</span>
        <span class="tool-name">{{ toolName }}</span>
      </div>

      <!-- 音符计数 -->
      <div class="stat-item" v-if="noteCount > 0">
        <span class="stat-icon">♪</span>
        <span class="stat-value">{{ noteCount }}</span>
        <span class="stat-label">音符</span>
      </div>

      <!-- 总时长 -->
      <div class="stat-item">
        <span class="stat-icon">⏱</span>
        <span class="stat-value">{{ formatDuration(totalDuration) }}</span>
        <span class="stat-label">总时长</span>
      </div>

      <!-- 当前位置 -->
      <div class="position-info">
        <span class="position-label">位置:</span>
        <span class="position-value">X:{{ currentPosition.x }}, Y:{{ currentPosition.y }}</span>
      </div>
    </div>

    <!-- 中间：缩放控制 -->
    <div class="status-center">
      <button
        class="zoom-btn"
        @click="$emit('zoom-out')"
        title="缩小 (Ctrl + -)"
        :disabled="zoomLevel <= 50"
      >
        <span class="zoom-icon">➖</span>
      </button>

      <div class="zoom-display">
        <span class="zoom-value">{{ zoomLevel }}%</span>
      </div>

      <button
        class="zoom-btn"
        @click="$emit('zoom-in')"
        title="放大 (Ctrl + +)"
        :disabled="zoomLevel >= 200"
      >
        <span class="zoom-icon">➕</span>
      </button>

      <button
        class="zoom-btn"
        @click="$emit('zoom-reset')"
        title="重置缩放 (Ctrl + 0)"
      >
        <span class="zoom-icon">↺</span>
      </button>
    </div>

    <!-- 右侧：系统状态 -->
    <div class="status-right">
      <!-- 自动保存状态 -->
      <div class="auto-save-status" v-if="autoSaveEnabled">
        <span class="save-icon">💾</span>
        <span class="save-text">自动保存已开启</span>
        <span class="save-time">{{ lastSaveTime }}</span>
      </div>

      <!-- 网格吸附 -->
      <div class="grid-snap">
        <label class="snap-label">
          <input
            type="checkbox"
            v-model="snapToGrid"
            @change="toggleSnap"
            class="snap-checkbox"
          />
          <span class="snap-text">网格吸附</span>
        </label>
      </div>

      <!-- 帮助按钮 -->
      <button
        class="help-btn"
        @click="showHelp"
        title="帮助 (F1)"
      >
        <span class="help-icon">❓</span>
        <span class="help-text">帮助</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

const props = defineProps<{
  zoomLevel: number
  currentPosition: { x: number; y: number }
  noteCount: number
  totalDuration: number
}>()

const emit = defineEmits(['zoom-in', 'zoom-out', 'zoom-reset'])

// 本地状态
const snapToGrid = ref(true)
const autoSaveEnabled = ref(true)
const lastSaveTime = ref('刚刚')

// 工具映射
const toolIcons = {
  select: '↖️',
  note: '♪',
  rest: '𝄽',
  chord: '♫',
  eraser: '⌫',
  text: 'T',
  measure: '|',
  clef: '𝄞'
}

const toolNames = {
  select: '选择工具',
  note: '音符工具',
  rest: '休止符工具',
  chord: '和弦工具',
  eraser: '橡皮擦工具',
  text: '文本工具',
  measure: '小节工具',
  clef: '谱号工具'
}

// 获取当前工具图标和名称
const getToolIcon = computed(() => {
  return '♪' // 这里应该从编辑器状态获取
})

const toolName = computed(() => {
  return '音符工具' // 这里应该从编辑器状态获取
})

// 格式化时长
const formatDuration = (duration: number) => {
  const minutes = Math.floor(duration / 60)
  const seconds = Math.floor(duration % 60)
  return `${minutes}:${seconds.toString().padStart(2, '0')}`
}

// 切换网格吸附
const toggleSnap = () => {
  console.log('网格吸附:', snapToGrid.value ? '开启' : '关闭')
  // 这里可以触发编辑器状态更新
}

// 显示帮助
const showHelp = () => {
  window.open('/help/editor', '_blank')
}
</script>

<style scoped>
.editor-status-bar {
  @apply flex items-center justify-between px-4 py-2 bg-gray-800 
         text-white text-sm border-t;
}

/* 左侧状态 */
.status-left {
  @apply flex items-center space-x-6;
}

.tool-info {
  @apply flex items-center space-x-2;
}

.tool-icon {
  @apply text-lg;
}

.tool-name {
  @apply text-gray-300;
}

.stat-item {
  @apply flex items-center space-x-1;
}

.stat-icon {
  @apply text-sm;
}

.stat-value {
  @apply font-medium;
}

.stat-label {
  @apply text-gray-400;
}

.position-info {
  @apply flex items-center space-x-1;
}

.position-label {
  @apply text-gray-400;
}

.position-value {
  @apply font-mono text-xs;
}

/* 中间缩放控制 */
.status-center {
  @apply flex items-center space-x-2;
}

.zoom-btn {
  @apply p-1 rounded hover:bg-gray-700 transition-colors 
         disabled:opacity-30 disabled:cursor-not-allowed;
}

.zoom-icon {
  @apply text-lg;
}

.zoom-display {
  @apply px-3;
}

.zoom-value {
  @apply font-medium;
}

/* 右侧系统状态 */
.status-right {
  @apply flex items-center space-x-4;
}

.auto-save-status {
  @apply flex items-center space-x-2;
}

.save-icon {
  @apply text-sm;
}

.save-text {
  @apply text-gray-300;
}

.save-time {
  @apply text-xs text-gray-400;
}

.grid-snap {
  @apply flex items-center;
}

.snap-label {
  @apply flex items-center space-x-1 cursor-pointer;
}

.snap-checkbox {
  @apply rounded;
}

.snap-text {
  @apply text-gray-300;
}

.help-btn {
  @apply flex items-center space-x-1 px-2 py-1 rounded hover:bg-gray-700 
         transition-colors;
}

.help-icon {
  @apply text-sm;
}

.help-text {
  @apply text-gray-300;
}
</style>
