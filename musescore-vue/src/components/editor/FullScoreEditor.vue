<template>
  <div class="full-score-editor">
    <!-- 编辑器头部 -->
    <EditorHeader 
      :score="currentScore"
      @save="saveScore"
      @export="exportScore"
      @play="playScore"
      @stop="stopPlayback"
      @toggle-metronome="toggleMetronome"
      :metronome-active="metronomeActive"
      :is-playing="isPlaying"
    />

    <!-- 主编辑区域 -->
    <div class="editor-main">
      <!-- 左侧工具面板 -->
      <EditorToolbar 
        class="editor-toolbar-panel"
        :current-tool="currentTool"
        :selected-note-type="selectedNoteType"
        :selected-accidental="selectedAccidental"
        :selected-articulation="selectedArticulation"
        :dotted-note="dottedNote"
        :can-tie-next="canTieNext"
        :beat-snap="beatSnap"
        @tool-change="handleToolChange"
        @note-type-change="selectedNoteType = $event"
        @accidental-change="selectedAccidental = $event"
        @articulation-change="selectedArticulation = $event"
        @dotted-change="dottedNote = $event"
        @beat-snap-change="beatSnap = $event"
        @tie-next="tieSelectedToNext"
      />

      <!-- 中央乐谱显示区域 -->
      <div class="score-display-area">
        <ScoreCanvas 
          ref="scoreCanvas"
          :notes="notes"
          :selected-note-id="selectedNoteId"
          :current-tool="currentTool"
          :time-signature="currentScore?.timeSignature"
          :zoom-level="zoomLevel"
          :beat-snap="beatSnap"
          @note-click="handleNoteClick"
          @canvas-click="handleCanvasClick"
          @note-move="handleNoteMove"
          @note-drag-start="handleNoteDragStart"
          @note-drag-end="handleNoteDragEnd"
        />
        
        <!-- 播放进度条 -->
        <div v-if="isPlaying" class="playback-progress">
          <div 
            class="progress-bar" 
            :style="{ width: `${playbackProgress}%` }"
          ></div>
        </div>
      </div>

      <!-- 右侧属性面板（悬浮，不参与主布局） -->
      <Transition name="properties-slide">
        <div v-if="selectedNote" class="properties-drawer">
          <EditorProperties 
            :note="selectedNote"
            @update-note="updateSelectedNote"
            @delete-note="deleteSelectedNote"
            @close="selectedNoteId = null"
          />
        </div>
      </Transition>
    </div>

    <!-- 底部状态栏 -->
    <EditorStatusBar 
      :zoom-level="zoomLevel"
      :current-position="currentPosition"
      :note-count="notes.length"
      :total-duration="totalDuration"
      @zoom-in="zoomIn"
      @zoom-out="zoomOut"
      @zoom-reset="zoomReset"
    />

    <!-- 播放控制浮动面板 -->
    <PlaybackControl 
      v-if="isPlaying"
      :current-time="currentTime"
      :total-time="totalTime"
      :tempo="tempo"
      :is-playing="isPlaying"
      @pause="pausePlayback"
      @stop="stopPlayback"
      @seek="seekPlayback"
      @tempo-change="updateTempo"
      class="playback-control-floating"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useScoreStore } from '@/stores/scoreStore'
import * as Tone from 'tone'
import { v4 as uuidv4 } from 'uuid'
import type { Note, Score, TimeSignature } from '@/types/score'

// 组件导入
import EditorHeader from './EditorHeader.vue'
import EditorToolbar from './EditorToolbar.vue'
import ScoreCanvas from './ScoreCanvas.vue'
import EditorProperties from './EditorProperties.vue'
import EditorStatusBar from './EditorStatusBar.vue'
import PlaybackControl from './PlaybackControl.vue'

const route = useRoute()
const scoreStore = useScoreStore()

// 编辑器状态
const currentTool = ref<'select' | 'note' | 'rest' | 'chord' | 'eraser' | 'text'>('select')
const selectedNoteType = ref<'whole' | 'half' | 'quarter' | 'eighth' | 'sixteenth'>('quarter')
const selectedAccidental = ref<'sharp' | 'flat' | 'natural' | null>(null)
const selectedArticulation = ref<'staccato' | 'tenuto' | 'accent' | null>(null)
const dottedNote = ref(false)
const beatSnap = ref(0.25)
const selectedNoteId = ref<string | null>(null)
const zoomLevel = ref(100)

const staffTop = 40
const timelineRowHeight = 150

// 播放状态
const isPlaying = ref(false)
const metronomeActive = ref(false)
const playbackProgress = ref(0)
const currentTime = ref(0)
const totalTime = ref(0)
const tempo = ref(120)
const pianoReady = ref(false)

// 音频组件
let piano: Tone.Sampler | null = null
let metronomeSynth: Tone.MembraneSynth | null = null
let metronomeLoop: Tone.Loop | null = null
let transport = Tone.Transport
let playbackEndEventId: number | null = null
let progressTimer: ReturnType<typeof setInterval> | null = null
let hasPlaybackWarmedUp = false

const normalizeTempo = (value: unknown) => {
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) return 120
  return Math.min(200, Math.max(40, parsed))
}

// 计算属性
const currentScore = computed(() => scoreStore.currentScore)
const notes = computed(() => currentScore.value?.notes || [])

const getTimelineRowIndex = (note: Pick<Note, 'position'>) => {
  return Math.max(0, Math.floor((note.position.y - staffTop) / timelineRowHeight))
}

const sortNotesByTimeline = (a: Note, b: Note) => {
  const aMeasure = typeof a.measureIndex === 'number' ? a.measureIndex : -1
  const bMeasure = typeof b.measureIndex === 'number' ? b.measureIndex : -1
  if (aMeasure !== bMeasure) return aMeasure - bMeasure

  const aBeat = typeof a.beatInMeasure === 'number' ? a.beatInMeasure : -1
  const bBeat = typeof b.beatInMeasure === 'number' ? b.beatInMeasure : -1
  if (Math.abs(aBeat - bBeat) > 0.001) return aBeat - bBeat

  const aClef = a.clef === 'bass' ? 1 : 0
  const bClef = b.clef === 'bass' ? 1 : 0
  if (aClef !== bClef) return aClef - bClef

  const rowDiff = getTimelineRowIndex(a) - getTimelineRowIndex(b)
  if (rowDiff !== 0) return rowDiff

  const xDiff = a.position.x - b.position.x
  if (xDiff !== 0) return xDiff

  return a.position.y - b.position.y
}

const getTimelineColumnKey = (note: Pick<Note, 'position'>) => {
  const full = note as Note
  if (typeof full.measureIndex === 'number' && typeof full.beatInMeasure === 'number') {
    return `m${full.measureIndex}:b${Number(full.beatInMeasure.toFixed(3))}`
  }

  const rowIndex = getTimelineRowIndex(note)
  const snappedX = Math.round(note.position.x * 1000) / 1000
  return `${rowIndex}:${snappedX}`
}

const getTimelineColumns = (source: Note[]) => {
  const columns = new Map<string, Note[]>()
  const orderedKeys: string[] = []

  const sorted = [...source].sort(sortNotesByTimeline)
  sorted.forEach((note) => {
    const key = getTimelineColumnKey(note)
    if (!columns.has(key)) {
      columns.set(key, [])
      orderedKeys.push(key)
    }
    columns.get(key)!.push(note)
  })

  return orderedKeys.map((key) => (columns.get(key) ?? []).sort((a, b) => a.position.y - b.position.y))
}

const getBeatsPerMeasure = (signature?: TimeSignature) => {
  const numerator = signature?.numerator ?? 4
  const denominator = signature?.denominator ?? 4
  if (denominator <= 0) return 4
  return (numerator * 4) / denominator
}

const roundToBeatSnap = (value: number) => {
  const snap = beatSnap.value || 0.25
  return Math.round(value / snap) * snap
}

const canInsertNoteInMeasure = (
  noteDraft: Pick<Note, 'duration' | 'measureIndex' | 'beatInMeasure' | 'clef'>,
  excludeNoteId?: string
) => {
  if (typeof noteDraft.measureIndex !== 'number' || typeof noteDraft.beatInMeasure !== 'number') {
    return true
  }

  const beatsPerMeasure = getBeatsPerMeasure(currentScore.value?.timeSignature)
  const targetClef = noteDraft.clef ?? 'treble'
  const measureNotes = notes.value.filter((note) => {
    if (note.measureIndex !== noteDraft.measureIndex) return false
    if ((note.clef ?? 'treble') !== targetClef) return false
    if (excludeNoteId && note.id === excludeNoteId) return false
    return true
  })
  const columnDurations = new Map<number, number>()

  measureNotes.forEach((note) => {
    if (typeof note.beatInMeasure !== 'number') return
    const beat = Number(note.beatInMeasure.toFixed(3))
    columnDurations.set(beat, Math.max(columnDurations.get(beat) ?? 0, note.duration))
  })

  const targetBeat = Number(noteDraft.beatInMeasure.toFixed(3))
  const nextDuration = Math.max(columnDurations.get(targetBeat) ?? 0, noteDraft.duration)
  columnDurations.set(targetBeat, nextDuration)

  const usedBeats = Array.from(columnDurations.values()).reduce((sum, duration) => sum + duration, 0)
  return usedBeats <= beatsPerMeasure + 0.001
}

let isRecalculatingRests = false
let restRecalcTimer: ReturnType<typeof setTimeout> | null = null
const REST_RECALC_DEBOUNCE_MS = 180

const recalculateExistingRests = () => {
  if (!currentScore.value || isRecalculatingRests) return

  const beatsPerMeasure = getBeatsPerMeasure(currentScore.value.timeSignature)
  const updates: Array<{ id: string; duration: number }> = []
  const removals: string[] = []
  const measureMap = new Map<string, Note[]>()

  notes.value.forEach((note) => {
    if (typeof note.measureIndex !== 'number' || typeof note.beatInMeasure !== 'number') return
    const clef = note.clef ?? 'treble'
    const key = `${note.measureIndex}:${clef}`
    const list = measureMap.get(key) ?? []
    list.push(note)
    measureMap.set(key, list)
  })

  measureMap.forEach((measureNotes) => {
    const beatPoints = Array.from(new Set(
      measureNotes
        .map((note) => note.beatInMeasure)
        .filter((beat): beat is number => typeof beat === 'number')
        .map((beat) => Number(beat.toFixed(3)))
    )).sort((a, b) => a - b)

    measureNotes
      .filter((note) => note.isRest && typeof note.beatInMeasure === 'number')
      .forEach((restNote) => {
        const startBeat = Number(restNote.beatInMeasure!.toFixed(3))
        const hasPlayableNoteAtSameBeat = measureNotes.some((note) => {
          if (note.id === restNote.id) return false
          if (note.isRest) return false
          if (typeof note.beatInMeasure !== 'number') return false
          return Math.abs(Number(note.beatInMeasure.toFixed(3)) - startBeat) <= 0.001
        })

        if (hasPlayableNoteAtSameBeat) {
          removals.push(restNote.id)
          return
        }

        const nextBeat = beatPoints.find((beat) => beat > startBeat + 0.001)
        const endBeat = typeof nextBeat === 'number' ? nextBeat : beatsPerMeasure
        const rawDuration = Math.max(0, endBeat - startBeat)
        if (rawDuration <= 0.001) {
          removals.push(restNote.id)
          return
        }

        const minDuration = beatSnap.value || 0.25
        const snappedDuration = Math.max(minDuration, roundToBeatSnap(rawDuration))

        if (Math.abs((restNote.duration || 0) - snappedDuration) > 0.001) {
          updates.push({ id: restNote.id, duration: snappedDuration })
        }
      })
  })

  if (updates.length === 0 && removals.length === 0) return

  isRecalculatingRests = true
  removals.forEach((id) => {
    scoreStore.removeNote(id)
  })
  updates.forEach((item) => {
    scoreStore.updateNote(item.id, { duration: item.duration })
  })
  isRecalculatingRests = false
}

const scheduleRestRecalculation = () => {
  if (restRecalcTimer) {
    clearTimeout(restRecalcTimer)
  }

  restRecalcTimer = setTimeout(() => {
    restRecalcTimer = null
    recalculateExistingRests()
  }, REST_RECALC_DEBOUNCE_MS)
}

const totalDuration = computed(() => {
  return getTimelineColumns(notes.value).reduce((total, column) => {
    const columnDuration = column.reduce((maxDuration, note) => Math.max(maxDuration, note.duration), 0)
    return total + columnDuration
  }, 0)
})

const selectedNote = computed(() => {
  if (!selectedNoteId.value) return null
  return notes.value.find(note => note.id === selectedNoteId.value)
})

const canTieNext = computed(() => {
  return Boolean(selectedNote.value && !selectedNote.value.isRest)
})

const currentPosition = computed(() => {
  return { x: 0, y: 0 } // 根据实际光标位置更新
})

// 工具切换
const handleToolChange = (tool: any) => {
  currentTool.value = tool
  selectedNoteId.value = null // 切换工具时取消选中
}

// 音符点击处理
const handleNoteClick = (noteId: string) => {
  if (currentTool.value === 'select') {
    selectedNoteId.value = noteId
  } else if (currentTool.value === 'chord') {
    selectedNoteId.value = noteId
  } else if (currentTool.value === 'eraser') {
    deleteNote(noteId)
  }
}

// 画布点击处理（添加新音符）
const handleCanvasClick = (position: {
  x: number
  y: number
  pitch?: string
  clef?: 'treble' | 'bass'
  measureIndex?: number
  beatInMeasure?: number
}) => {
  if (currentTool.value === 'note' || currentTool.value === 'chord' || currentTool.value === 'rest') {
    addNoteAtPosition(position)
  }
}

// 添加音符到指定位置
const addNoteAtPosition = (position: {
  x: number
  y: number
  pitch?: string
  clef?: 'treble' | 'bass'
  measureIndex?: number
  beatInMeasure?: number
}) => {
  if (!currentScore.value) return

  const noteDuration = getDurationFromNoteType(selectedNoteType.value, dottedNote.value)
  const note: Note = {
    id: uuidv4(),
    type: selectedNoteType.value,
    pitch: currentTool.value === 'rest' ? 'B4' : (position.pitch ?? calculatePitchFromPosition(position.y)),
    duration: noteDuration,
    position: { x: position.x, y: position.y },
    clef: position.clef ?? 'treble',
    measureIndex: position.measureIndex,
    beatInMeasure: position.beatInMeasure,
    isRest: currentTool.value === 'rest',
    dots: dottedNote.value ? 1 : 0,
    selected: true
  }

  if (!note.isRest && selectedAccidental.value) {
    note.accidental = selectedAccidental.value
  }

  if (!note.isRest && selectedArticulation.value) {
    note.articulation = selectedArticulation.value
  }

  // 如果是和弦模式，添加到当前选中音符的和弦中
  if (currentTool.value === 'chord' && selectedNote.value && !selectedNote.value.isRest) {
    const chordNote = { ...note }
    chordNote.position.x = selectedNote.value.position.x
    chordNote.clef = selectedNote.value.clef ?? note.clef
    chordNote.measureIndex = selectedNote.value.measureIndex
    chordNote.beatInMeasure = selectedNote.value.beatInMeasure
    if (!canInsertNoteInMeasure(chordNote)) {
      console.warn('该小节音符时值已满，无法继续添加。')
      return
    }
    scoreStore.addNote(chordNote)
    selectedNoteId.value = chordNote.id
  } else {
    if (!canInsertNoteInMeasure(note)) {
      console.warn('该小节音符时值已满，无法继续添加。')
      return
    }
    scoreStore.addNote(note)
    selectedNoteId.value = note.id
  }
}

// 音符移动处理
const handleNoteMove = (
  noteId: string,
  newPosition: {
    x: number
    y: number
    pitch?: string
    clef?: 'treble' | 'bass'
    measureIndex?: number
    beatInMeasure?: number
  }
) => {
  const note = notes.value.find(n => n.id === noteId)
  if (note) {
    if (
      typeof newPosition.measureIndex === 'number'
      && typeof newPosition.beatInMeasure === 'number'
      && (newPosition.measureIndex !== note.measureIndex || newPosition.beatInMeasure !== note.beatInMeasure)
      && !canInsertNoteInMeasure({
        duration: note.duration,
        clef: newPosition.clef ?? note.clef ?? 'treble',
        measureIndex: newPosition.measureIndex,
        beatInMeasure: newPosition.beatInMeasure
      }, note.id)
    ) {
      return
    }

    scoreStore.updateNote(noteId, {
      position: newPosition,
      pitch: newPosition.pitch ?? calculatePitchFromPosition(newPosition.y),
      clef: newPosition.clef ?? note.clef,
      measureIndex: newPosition.measureIndex,
      beatInMeasure: newPosition.beatInMeasure
    })
  }
}

// 音符拖拽开始
const handleNoteDragStart = (noteId: string) => {
  console.log('拖拽开始:', noteId)
}

// 音符拖拽结束
const handleNoteDragEnd = (noteId: string, finalPosition: { x: number; y: number }) => {
  console.log('拖拽结束:', noteId, finalPosition)
}

const tieSelectedToNext = () => {
  if (!selectedNote.value || selectedNote.value.isRest) return

  const sortedNotes = [...notes.value].sort(sortNotesByTimeline)
  const currentIndex = sortedNotes.findIndex((note) => note.id === selectedNote.value?.id)
  if (currentIndex === -1) return

  const nextNote = sortedNotes
    .slice(currentIndex + 1)
    .find((note) => !note.isRest && note.pitch === selectedNote.value?.pitch)

  if (!nextNote) return

  scoreStore.updateNote(selectedNote.value.id, { tieToNext: true })
  scoreStore.updateNote(nextNote.id, { tieFromPrev: true })
}

// 更新选中的音符
const updateSelectedNote = (updates: Partial<Note>) => {
  if (!selectedNote.value) return
  
  const updatedNote = { ...selectedNote.value, ...updates }
  scoreStore.updateNote(selectedNote.value.id, updatedNote)
}

// 删除选中的音符
const deleteSelectedNote = () => {
  if (selectedNote.value) {
    deleteNote(selectedNote.value.id)
  }
}

// 删除音符
const deleteNote = (noteId: string) => {
  const target = notes.value.find((note) => note.id === noteId)
  if (target && !target.isRest) {
    const sortedNotes = [...notes.value].sort(sortNotesByTimeline)
    const currentIndex = sortedNotes.findIndex((note) => note.id === noteId)

    if (currentIndex !== -1) {
      if (target.tieToNext) {
        const nextNote = sortedNotes
          .slice(currentIndex + 1)
          .find((note) => !note.isRest && note.pitch === target.pitch)
        if (nextNote) {
          scoreStore.updateNote(nextNote.id, { tieFromPrev: false })
        }
      }

      if (target.tieFromPrev) {
        const prevNote = [...sortedNotes]
          .slice(0, currentIndex)
          .reverse()
          .find((note) => !note.isRest && note.pitch === target.pitch)
        if (prevNote) {
          scoreStore.updateNote(prevNote.id, { tieToNext: false })
        }
      }
    }
  }

  scoreStore.removeNote(noteId)
  if (selectedNoteId.value === noteId) {
    selectedNoteId.value = null
  }
}

// 保存乐谱
const saveScore = async () => {
  if (!currentScore.value) return
  
  try {
    // 这里可以调用API保存到服务器
    console.log('保存乐谱:', currentScore.value)
    // 实际项目中: await api.saveScore(currentScore.value)
  } catch (error) {
    console.error('保存失败:', error)
  }
}

// 导出乐谱
const exportScore = (format: 'midi' | 'pdf' | 'musicxml') => {
  if (!currentScore.value) return
  
  switch (format) {
    case 'midi':
      exportAsMIDI()
      break
    case 'pdf':
      exportAsPDF()
      break
    case 'musicxml':
      exportAsMusicXML()
      break
  }
}

const exportAsMIDI = () => {
  // 使用第三方库如 MIDI.js 或自己的实现
  console.log('导出为MIDI')
}

const exportAsPDF = () => {
  // 使用 html2pdf 或类似库
  console.log('导出为PDF')
}

const exportAsMusicXML = () => {
  // 生成 MusicXML 格式
  console.log('导出为MusicXML')
}

// 播放乐谱
const playScore = async () => {
  if (isPlaying.value) {
    pausePlayback()
    return
  }

  try {
    await Tone.start()
    initAudioComponents()
    await ensurePianoReady()
    await prewarmSamplerForFirstPlayback()
    
    // 重置播放位置
    transport.stop()
    transport.position = 0
    currentTime.value = 0
    
    // 安排音符播放
    scheduleNotesForPlayback()
    if (totalTime.value <= 0) return
    
    // 启动节拍器
    if (metronomeActive.value) {
      startMetronome()
    }
    
    // 开始播放
    transport.start()
    isPlaying.value = true
    startProgressTimer()
  } catch (error) {
    console.error('播放失败:', error)
  }
}

// 暂停播放
const pausePlayback = () => {
  transport.pause()
  isPlaying.value = false
  stopProgressTimer()
  ;(piano as any)?.releaseAll?.()
  
  if (metronomeLoop) {
    metronomeLoop.stop()
  }
}

// 停止播放
const stopPlayback = () => {
  transport.stop()
  transport.position = 0
  clearPlaybackEndEvent()
  isPlaying.value = false
  stopProgressTimer()
  playbackProgress.value = 0
  currentTime.value = 0
  ;(piano as any)?.releaseAll?.()
  
  if (metronomeLoop) {
    metronomeLoop.stop()
  }
}

// 跳转到指定时间
const seekPlayback = (time: number) => {
  const maxTime = totalTime.value > 0 ? totalTime.value : 0
  const safeTime = Math.min(maxTime, Math.max(0, Number(time) || 0))
  transport.seconds = safeTime
  currentTime.value = safeTime
  playbackProgress.value = maxTime > 0 ? (safeTime / maxTime) * 100 : 0
}

// 更新速度
const updateTempo = (newTempo: number) => {
  const nextTempo = normalizeTempo(newTempo)
  tempo.value = nextTempo
  transport.bpm.value = nextTempo
  
  if (currentScore.value) {
    currentScore.value.tempo = nextTempo
  }
}

// 切换节拍器
const toggleMetronome = () => {
  metronomeActive.value = !metronomeActive.value
  
  if (metronomeActive.value && isPlaying.value) {
    startMetronome()
  } else if (metronomeLoop) {
    metronomeLoop.stop()
  }
}

// 初始化音频组件
const initAudioComponents = () => {
  if (!piano) {
    piano = new Tone.Sampler({
      urls: {
        A0: 'A0.mp3',
        C1: 'C1.mp3',
        'D#1': 'Ds1.mp3',
        'F#1': 'Fs1.mp3',
        A1: 'A1.mp3',
        C2: 'C2.mp3',
        'D#2': 'Ds2.mp3',
        'F#2': 'Fs2.mp3',
        A2: 'A2.mp3',
        C3: 'C3.mp3',
        'D#3': 'Ds3.mp3',
        'F#3': 'Fs3.mp3',
        A3: 'A3.mp3',
        C4: 'C4.mp3',
        'D#4': 'Ds4.mp3',
        'F#4': 'Fs4.mp3',
        A4: 'A4.mp3',
        C5: 'C5.mp3',
        'D#5': 'Ds5.mp3',
        'F#5': 'Fs5.mp3',
        A5: 'A5.mp3',
        C6: 'C6.mp3',
        'D#6': 'Ds6.mp3',
        'F#6': 'Fs6.mp3',
        A6: 'A6.mp3',
        C7: 'C7.mp3',
        'D#7': 'Ds7.mp3',
        'F#7': 'Fs7.mp3',
        A7: 'A7.mp3',
        C8: 'C8.mp3'
      },
      release: 1,
      baseUrl: 'https://tonejs.github.io/audio/salamander/',
      onload: () => {
        pianoReady.value = true
      }
    }).toDestination()
  }
  
  if (!metronomeSynth) {
    metronomeSynth = new Tone.MembraneSynth({
      pitchDecay: 0.05,
      octaves: 4,
      oscillator: { type: 'sine' },
      envelope: {
        attack: 0.001,
        decay: 0.3,
        sustain: 0.01,
        release: 0.1
      }
    }).toDestination()
  }
}

const ensurePianoReady = async () => {
  if (!piano) return

  if (!piano.loaded) {
    await Tone.loaded()
  }

  pianoReady.value = true
}

const prewarmSamplerForFirstPlayback = async () => {
  if (hasPlaybackWarmedUp || !piano) return

  try {
    const warmupAt = Tone.now() + 0.02
    piano.triggerAttackRelease('C4', 0.02, warmupAt, 0.0001)
    piano.triggerAttackRelease('C5', 0.02, warmupAt + 0.01, 0.0001)
    await new Promise((resolve) => setTimeout(resolve, 60))
  } catch (error) {
    console.warn('预热采样失败，继续播放:', error)
  } finally {
    hasPlaybackWarmedUp = true
  }
}

const clearPlaybackEndEvent = () => {
  if (playbackEndEventId === null) return
  transport.clear(playbackEndEventId)
  playbackEndEventId = null
}

const stopProgressTimer = () => {
  if (!progressTimer) return
  clearInterval(progressTimer)
  progressTimer = null
}

const startProgressTimer = () => {
  stopProgressTimer()
  progressTimer = setInterval(() => {
    if (!isPlaying.value) return
    const elapsed = Number.isFinite(transport.seconds) ? transport.seconds : 0
    const duration = totalTime.value > 0 ? totalTime.value : 0
    const nextCurrentTime = duration > 0 ? Math.min(Math.max(0, elapsed), duration) : 0
    const nextProgress = duration > 0 ? (nextCurrentTime / duration) * 100 : 0
    currentTime.value = nextCurrentTime
    playbackProgress.value = nextProgress
  }, 50)
}

const schedulePlaybackEnd = (durationSeconds: number) => {
  clearPlaybackEndEvent()

  if (durationSeconds <= 0) return

  playbackEndEventId = transport.scheduleOnce(() => {
    stopPlayback()
  }, durationSeconds)
}

// 安排音符播放
const scheduleNotesForPlayback = () => {
  if (!piano) return
  
  // 清除之前的安排
  transport.cancel(0)
  clearPlaybackEndEvent()
  ;(piano as any).releaseAll?.()
  
  let currentBeat = 0
  const beatDuration = 60 / tempo.value
  
  const timelineColumns = getTimelineColumns(notes.value)
  const playbackColumns = timelineColumns.map((column) => {
    const startBeat = currentBeat
    const columnDuration = column.reduce((maxDuration, note) => Math.max(maxDuration, note.duration || 0), 0)
    currentBeat += columnDuration
    return { column, startBeat, columnDuration }
  })

  type PedalSegment = { startBeat: number; endBeat: number }
  const pedalSegments: PedalSegment[] = []
  let activePedalStart: number | null = null

  playbackColumns
    .flatMap((item) =>
      item.column
        .filter((note) => !note.isRest)
        .map((note) => ({ note, startBeat: item.startBeat }))
    )
    .sort((a, b) => {
      if (Math.abs(a.startBeat - b.startBeat) > 0.001) return a.startBeat - b.startBeat
      return sortNotesByTimeline(a.note, b.note)
    })
    .forEach(({ note, startBeat }) => {
      if (note.pedalStart) {
        if (activePedalStart !== null) {
          pedalSegments.push({ startBeat: activePedalStart, endBeat: startBeat })
        }
        activePedalStart = startBeat
      }
      if (note.pedalEnd && activePedalStart !== null) {
        pedalSegments.push({ startBeat: activePedalStart, endBeat: startBeat })
        activePedalStart = null
      }
    })

  if (activePedalStart !== null) {
    pedalSegments.push({ startBeat: activePedalStart, endBeat: currentBeat })
  }

  let maxEndBeat = 0
  playbackColumns.forEach(({ column, startBeat }) => {
    column.forEach((note) => {
      if (note.isRest) return

      const noteDuration = note.duration || 0
      const noteStartBeat = startBeat
      const noteEndBeat = noteStartBeat + noteDuration
      let effectiveEndBeat = noteEndBeat

      pedalSegments.forEach((segment) => {
        if (effectiveEndBeat >= segment.startBeat - 0.001 && effectiveEndBeat < segment.endBeat) {
          effectiveEndBeat = Math.max(effectiveEndBeat, segment.endBeat)
        }
      })

      maxEndBeat = Math.max(maxEndBeat, effectiveEndBeat)

      transport.schedule((scheduledTime) => {
        if (!piano) return
        const velocity = (note.velocity ?? 82) / 127
        const durationSeconds = Math.max(0.01, (effectiveEndBeat - noteStartBeat) * beatDuration)
        piano.triggerAttackRelease(note.pitch, durationSeconds, scheduledTime, velocity)
      }, noteStartBeat * beatDuration)
    })
  })
  
  totalTime.value = Math.max(0, maxEndBeat * beatDuration)
  schedulePlaybackEnd(totalTime.value + 0.05)
}

// 启动节拍器
const startMetronome = () => {
  if (!metronomeSynth) return
  
  if (metronomeLoop) {
    metronomeLoop.stop()
  }
  
  metronomeLoop = new Tone.Loop((time) => {
    const beat = Math.floor(transport.seconds / (60 / tempo.value))
    
    // 强拍
    if (beat % 4 === 0) {
      metronomeSynth?.triggerAttackRelease('C5', '32n', time, 0.8)
    } 
    // 弱拍
    else {
      metronomeSynth?.triggerAttackRelease('C4', '32n', time, 0.4)
    }
  }, '4n')
  
  metronomeLoop.start(0)
}

// 音高计算
const calculatePitchFromPosition = (y: number): string => {
  const staffTop = 40
  const rowHeight = 150
  const pitchStepY = 5
  const rowIndex = Math.max(0, Math.floor((y - staffTop) / rowHeight))
  const middleCPosition = staffTop + rowIndex * rowHeight + 50
  const positionFromMiddleC = Math.round((middleCPosition - y) / pitchStepY)
  
  // 音高数组
  const pitches = ['C', 'D', 'E', 'F', 'G', 'A', 'B']
  const octave = 4 + Math.floor(positionFromMiddleC / 7)
  const pitchIndex = (positionFromMiddleC % 7 + 7) % 7
  
  return `${pitches[pitchIndex]}${octave}`
}

// 根据音符类型获取时长
const getDurationFromNoteType = (type: string, dotted = false): number => {
  const durations: Record<string, number> = {
    'whole': 4,
    'half': 2,
    'quarter': 1,
    'eighth': 0.5,
    'sixteenth': 0.25
  }
  const base = durations[type] || 1
  return dotted ? base * 1.5 : base
}

// 缩放控制
const zoomIn = () => {
  if (zoomLevel.value < 200) {
    zoomLevel.value += 10
  }
}

const zoomOut = () => {
  if (zoomLevel.value > 50) {
    zoomLevel.value -= 10
  }
}

const zoomReset = () => {
  zoomLevel.value = 100
}

// 加载乐谱
const loadScore = () => {
  const scoreId = route.params.id as string
  if (scoreId) {
    const score = scoreStore.scores.find(s => s.id === scoreId)
    if (score) {
      scoreStore.currentScore = score
      tempo.value = normalizeTempo(score.tempo)
    }
  } else {
    // 创建新乐谱
    const newScore: Score = {
      id: uuidv4(),
      title: '未命名乐谱',
      composer: '',
      tempo: 120,
      timeSignature: { numerator: 4, denominator: 4 },
      keySignature: 'C',
      notes: [],
      measures: [],
      createdAt: new Date(),
      updatedAt: new Date(),
      isDraft: true
    }
    scoreStore.createScore(newScore)
  }
}

// 生命周期
onMounted(() => {
  loadScore()
  initAudioComponents()
  
  // 设置初始速度
  transport.bpm.value = tempo.value
  
  // 监听键盘快捷键
  document.addEventListener('keydown', handleKeyDown)
})

onUnmounted(() => {
  stopPlayback()
  document.removeEventListener('keydown', handleKeyDown)
  clearPlaybackEndEvent()
  if (restRecalcTimer) {
    clearTimeout(restRecalcTimer)
    restRecalcTimer = null
  }

  piano?.dispose()
  metronomeSynth?.dispose()
  metronomeLoop?.dispose()
})

// 键盘快捷键
const handleKeyDown = (event: KeyboardEvent) => {
  // 阻止浏览器默认行为
  if (event.ctrlKey || event.metaKey) {
    switch (event.key.toLowerCase()) {
      case 's':
        event.preventDefault()
        saveScore()
        break
      case 'z':
        if (event.shiftKey) {
          // Ctrl+Shift+Z 重做
          console.log('重做')
        } else {
          // Ctrl+Z 撤销
          console.log('撤销')
        }
        event.preventDefault()
        break
      case ' ':
        event.preventDefault()
        playScore()
        break
    }
  }
  
  // 工具快捷键
  switch (event.key) {
    case '1':
      currentTool.value = 'select'
      break
    case '2':
      currentTool.value = 'note'
      break
    case '3':
      currentTool.value = 'rest'
      break
    case '4':
      currentTool.value = 'eraser'
      break
    case '.':
      dottedNote.value = !dottedNote.value
      break
    case 't':
    case 'T':
      tieSelectedToNext()
      break
    case 'Delete':
    case 'Backspace':
      if (selectedNote.value) {
        deleteSelectedNote()
      }
      break
  }
}

watch(
  () => [notes.value, currentScore.value?.timeSignature],
  () => {
    scheduleRestRecalculation()
  },
  { deep: true }
)
</script>

<style scoped>
.full-score-editor {
  @apply h-screen flex flex-col bg-gray-50;
}

.editor-main {
  @apply flex flex-1 overflow-hidden relative;
}

.editor-toolbar-panel {
  @apply shrink-0;
}

.score-display-area {
  @apply flex-1 relative overflow-auto bg-white border-x;
}

.properties-drawer {
  @apply absolute right-3 top-3 bottom-3 z-40 w-[340px] max-w-[42vw] rounded-xl overflow-hidden shadow-2xl border border-slate-200;
}

.properties-slide-enter-active,
.properties-slide-leave-active {
  transition: all 0.2s ease;
}

.properties-slide-enter-from,
.properties-slide-leave-to {
  transform: translateX(16px);
  opacity: 0;
}

.playback-progress {
  @apply absolute bottom-0 left-0 right-0 h-1 bg-gray-200;
}

.progress-bar {
  @apply h-full bg-blue-500 transition-all duration-100;
}

.playback-control-floating {
  @apply fixed right-6 bottom-6 z-50;
}
</style>
