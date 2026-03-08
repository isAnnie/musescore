<template>
  <div class="full-score-editor">
    <!-- 缂栬緫鍣ㄥご閮?-->
    <EditorHeader 
      :score="currentScore"
      @save="saveScore"
      @publish="publishScore"
      @export="exportScore"
      @play="playScore"
      @stop="stopPlayback"
      @toggle-metronome="toggleMetronome"
      @tempo-change="updateTempo"
      :metronome-active="metronomeActive"
      :is-playing="isPlaying"
    />

    <!-- 涓荤紪杈戝尯鍩?-->
    <div class="editor-main">
      <!-- 宸︿晶宸ュ叿闈㈡澘 -->
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

      <!-- 涓ぎ涔愯氨鏄剧ず鍖哄煙 -->
      <div class="score-display-area">
        <ScoreCanvas 
          ref="scoreCanvas"
          :notes="notes"
          :selected-note-id="selectedNoteId"
          :current-tool="currentTool"
          :time-signature="currentScore?.timeSignature"
          :zoom-level="zoomLevel"
          :beat-snap="beatSnap"
          :current-measure-index="currentPlaybackMeasureIndex"
          @note-click="handleNoteClick"
          @canvas-click="handleCanvasClick"
          @note-move="handleNoteMove"
          @note-drag-start="handleNoteDragStart"
          @note-drag-end="handleNoteDragEnd"
        />
      </div>

      <!-- 鍙充晶灞炴€ч潰鏉匡紙鎮诞锛屼笉鍙備笌涓诲竷灞€锛?-->
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

    <!-- 搴曢儴鐘舵€佹爮 -->
    <EditorStatusBar 
      :zoom-level="zoomLevel"
      :current-position="currentPosition"
      :note-count="notes.length"
      :total-duration="totalDuration"
      @zoom-in="zoomIn"
      @zoom-out="zoomOut"
      @zoom-reset="zoomReset"
    />

    <!-- 鎾斁鎺у埗娴姩闈㈡澘 -->
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
import { useScoreStore } from '@/stores/scoreStore'
import * as Tone from 'tone'
import { v4 as uuidv4 } from 'uuid'
import type { Note, Score, TimeSignature } from '@/types/score'
import { ensureSharedPianoReady, getSharedPianoSampler, prewarmSharedPianoSampler } from '@/services/audioSampler'

// 缁勪欢瀵煎叆
import EditorHeader from './EditorHeader.vue'
import EditorToolbar from './EditorToolbar.vue'
import ScoreCanvas from './ScoreCanvas.vue'
import EditorProperties from './EditorProperties.vue'
import EditorStatusBar from './EditorStatusBar.vue'
import PlaybackControl from './PlaybackControl.vue'

type PublishPayload = {
  visibility: 'public' | 'private' | 'unlisted'
  description: string
  tags: string[]
}

const emit = defineEmits<{
  save: []
  publish: [payload: PublishPayload]
  export: [format: 'midi' | 'pdf' | 'musicxml']
}>()

const scoreStore = useScoreStore()

// 缂栬緫鍣ㄧ姸鎬?
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

// 鎾斁鐘舵€?
const isPlaying = ref(false)
const metronomeActive = ref(false)
const currentTime = ref(0)
const totalTime = ref(0)
const tempo = ref(120)
const currentPlaybackMeasureIndex = ref<number | null>(null)

// 闊抽缁勪欢
let piano: Tone.Sampler | null = null
let metronomeSynth: Tone.MembraneSynth | null = null
let metronomeLoop: Tone.Loop | null = null
let transport = Tone.Transport
let playbackEndEventId: number | null = null
let progressAnimationFrame: number | null = null
let totalPlaybackTicks = 0

const normalizeTempo = (value: unknown) => {
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) return 120
  return Math.min(200, Math.max(40, parsed))
}

// 璁＄畻灞炴€?
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

const getBeatsPerMeasureForPlayback = () => {
  return getBeatsPerMeasure(currentScore.value?.timeSignature)
}

const getAbsoluteBeatFromMeasure = (note: Note, beatsPerMeasure: number) => {
  if (typeof note.measureIndex !== 'number' || typeof note.beatInMeasure !== 'number') return null
  const measure = Math.max(0, note.measureIndex)
  const beatInMeasure = Math.max(0, note.beatInMeasure)
  return measure * beatsPerMeasure + beatInMeasure
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
  return { x: 0, y: 0 } // 鏍规嵁瀹為檯鍏夋爣浣嶇疆鏇存柊
})

// 宸ュ叿鍒囨崲
const handleToolChange = (tool: any) => {
  currentTool.value = tool
  selectedNoteId.value = null // 鍒囨崲宸ュ叿鏃跺彇娑堥€変腑
}

// 闊崇鐐瑰嚮澶勭悊
const handleNoteClick = (noteId: string) => {
  if (currentTool.value === 'select') {
    selectedNoteId.value = noteId
  } else if (currentTool.value === 'chord') {
    selectedNoteId.value = noteId
  } else if (currentTool.value === 'eraser') {
    deleteNote(noteId)
  }
}

// 鐢诲竷鐐瑰嚮澶勭悊锛堟坊鍔犳柊闊崇锛?
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

// 娣诲姞闊崇鍒版寚瀹氫綅缃?
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

  // 濡傛灉鏄拰寮︽ā寮忥紝娣诲姞鍒板綋鍓嶉€変腑闊崇鐨勫拰寮︿腑
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

// 闊崇绉诲姩澶勭悊
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

// 闊崇鎷栨嫿寮€濮?
const handleNoteDragStart = (noteId: string) => {
  console.log('鎷栨嫿寮€濮?', noteId)
}

// 闊崇鎷栨嫿缁撴潫
const handleNoteDragEnd = (noteId: string, finalPosition: { x: number; y: number }) => {
  console.log('鎷栨嫿缁撴潫:', noteId, finalPosition)
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

// 鏇存柊閫変腑鐨勯煶绗?
const updateSelectedNote = (updates: Partial<Note>) => {
  if (!selectedNote.value) return
  
  const updatedNote = { ...selectedNote.value, ...updates }
  scoreStore.updateNote(selectedNote.value.id, updatedNote)
}

// 鍒犻櫎閫変腑鐨勯煶绗?
const deleteSelectedNote = () => {
  if (selectedNote.value) {
    deleteNote(selectedNote.value.id)
  }
}

// 鍒犻櫎闊崇
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

// 淇濆瓨涔愯氨
const saveScore = async () => {
  if (!currentScore.value) return

  try {
    emit('save')
  } catch (error) {
    console.error('保存失败:', error)
  }
}

const publishScore = (payload: PublishPayload) => {
  emit('publish', payload)
}

// 瀵煎嚭涔愯氨
const exportScore = (format: 'midi' | 'pdf' | 'musicxml') => {
  if (!currentScore.value) return
  emit('export', format)
  
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
  // 浣跨敤绗笁鏂瑰簱濡?MIDI.js 鎴栬嚜宸辩殑瀹炵幇
  console.log('导出为 MIDI')
}

const exportAsPDF = () => {
  // 浣跨敤 html2pdf 鎴栫被浼煎簱
  console.log('导出为 PDF')
}

const exportAsMusicXML = () => {
  // 鐢熸垚 MusicXML 鏍煎紡
  console.log('导出为 MusicXML')
}

// 鎾斁涔愯氨
const playScore = async () => {
  if (isPlaying.value) {
    pausePlayback()
    return
  }

  try {
    await Tone.start()
    await initAudioComponents()
    await prewarmSamplerForFirstPlayback()
    
    // 閲嶇疆鎾斁浣嶇疆
    transport.stop()
    transport.position = 0
    currentTime.value = 0
    currentPlaybackMeasureIndex.value = 0
    
    // 瀹夋帓闊崇鎾斁
    scheduleNotesForPlayback()
    if (totalTime.value <= 0) return
    
    // 鍚姩鑺傛媿鍣?
    if (metronomeActive.value) {
      startMetronome()
    }
    
    // 开始播放
    transport.start('+0.05')
    isPlaying.value = true
    startProgressTimer()
  } catch (error) {
    console.error('播放失败:', error)
  }
}

// 鏆傚仠鎾斁
const pausePlayback = () => {
  transport.pause()
  isPlaying.value = false
  stopProgressTimer()
  ;(piano as any)?.releaseAll?.()
  
  if (metronomeLoop) {
    metronomeLoop.stop()
  }
}

// 鍋滄鎾斁
const stopPlayback = () => {
  transport.stop()
  transport.position = 0
  clearPlaybackEndEvent()
  isPlaying.value = false
  stopProgressTimer()
  currentTime.value = 0
  currentPlaybackMeasureIndex.value = null
  ;(piano as any)?.releaseAll?.()
  
  if (metronomeLoop) {
    metronomeLoop.stop()
  }
}

// 璺宠浆鍒版寚瀹氭椂闂?
const seekPlayback = (time: number) => {
  const maxTime = totalTime.value > 0 ? totalTime.value : 0
  const safeTime = Math.min(maxTime, Math.max(0, Number(time) || 0))
  transport.seconds = safeTime
  currentTime.value = safeTime
  currentPlaybackMeasureIndex.value = getCurrentMeasureIndexFromTransport()
}

// 鏇存柊閫熷害
const updateTempo = (newTempo: number) => {
  const nextTempo = normalizeTempo(newTempo)
  tempo.value = nextTempo
  transport.bpm.value = nextTempo
  if (totalPlaybackTicks > 0) {
    totalTime.value = Tone.Ticks(totalPlaybackTicks).toSeconds()
  }
  
  if (currentScore.value) {
    currentScore.value.tempo = nextTempo
  }
}

// 鍒囨崲鑺傛媿鍣?
const toggleMetronome = () => {
  metronomeActive.value = !metronomeActive.value
  
  if (metronomeActive.value && isPlaying.value) {
    startMetronome()
  } else if (metronomeLoop) {
    metronomeLoop.stop()
  }
}

// 鍒濆鍖栭煶棰戠粍浠?
const initAudioComponents = async () => {
  if (!piano) {
    piano = getSharedPianoSampler()
  }
  await ensureSharedPianoReady()
  
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

const prewarmSamplerForFirstPlayback = async () => {
  try {
    await prewarmSharedPianoSampler()
  } catch (error) {
    console.warn('采样预热失败，继续播放。', error)
  }
}

const clearPlaybackEndEvent = () => {
  if (playbackEndEventId === null) return
  transport.clear(playbackEndEventId)
  playbackEndEventId = null
}

const getTicksForBeats = (beats: number) => {
  const ppq = transport.PPQ || 192
  return Math.max(0, Math.round(beats * ppq))
}

const getCurrentMeasureIndexFromTransport = () => {
  const beatsPerMeasure = getBeatsPerMeasure(currentScore.value?.timeSignature)
  if (beatsPerMeasure <= 0) return null
  const currentTicks = Math.max(0, Number.isFinite(transport.ticks) ? Number(transport.ticks) : 0)
  const currentBeat = currentTicks / (transport.PPQ || 192)
  return Math.max(0, Math.floor(currentBeat / beatsPerMeasure))
}

const stopProgressTimer = () => {
  if (progressAnimationFrame === null) return
  cancelAnimationFrame(progressAnimationFrame)
  progressAnimationFrame = null
}

const startProgressTimer = () => {
  stopProgressTimer()
  let lastFrameTime = 0
  const frameIntervalMs = 33

  const updateProgress = (timestamp: number) => {
    if (!isPlaying.value) return
    if (timestamp - lastFrameTime >= frameIntervalMs) {
      lastFrameTime = timestamp
      const elapsed = Number.isFinite(transport.seconds) ? transport.seconds : 0
      const duration = totalTime.value > 0 ? totalTime.value : 0
      const currentTicks = Math.max(0, Number.isFinite(transport.ticks) ? Number(transport.ticks) : 0)
      const nextCurrentTime = duration > 0 ? Math.min(Math.max(0, elapsed), duration) : 0
      currentTime.value = nextCurrentTime
      currentPlaybackMeasureIndex.value = getCurrentMeasureIndexFromTransport()
    }
    progressAnimationFrame = requestAnimationFrame(updateProgress)
  }

  progressAnimationFrame = requestAnimationFrame(updateProgress)
}

const schedulePlaybackEnd = (endTicks: number) => {
  clearPlaybackEndEvent()

  if (endTicks <= 0) return

  playbackEndEventId = transport.scheduleOnce(() => {
    stopPlayback()
  }, `${endTicks}i`)
}

// 瀹夋帓闊崇鎾斁
const scheduleNotesForPlayback = () => {
  if (!piano) return
  
  // 娓呴櫎涔嬪墠鐨勫畨鎺?
  transport.cancel(0)
  clearPlaybackEndEvent()
  ;(piano as any).releaseAll?.()

  type NoteEvent = {
    note: Note
    startBeat: number
    endBeat: number
  }
  type PedalSegment = { startBeat: number; endBeat: number }

  const beatsPerMeasure = getBeatsPerMeasureForPlayback()
  const timelineColumns = getTimelineColumns(notes.value)
  const fallbackStartByColumn = new Map<string, number>()

  // For notes without measure/beat metadata, preserve old sequential behavior as fallback.
  let fallbackCurrentBeat = 0
  timelineColumns.forEach((column) => {
    if (!column.length) return
    const key = getTimelineColumnKey(column[0])
    fallbackStartByColumn.set(key, fallbackCurrentBeat)
    const columnDuration = column.reduce((maxDuration, note) => Math.max(maxDuration, note.duration || 0), 0)
    fallbackCurrentBeat += Math.max(0, columnDuration)
  })

  const noteEvents: NoteEvent[] = notes.value
    .filter((note) => !note.isRest)
    .map((note) => {
      const absoluteFromMeasure = getAbsoluteBeatFromMeasure(note, beatsPerMeasure)
      const fallbackStart = fallbackStartByColumn.get(getTimelineColumnKey(note)) ?? 0
      const startBeat = absoluteFromMeasure ?? fallbackStart
      const duration = Math.max(0, note.duration || 0)
      return {
        note,
        startBeat,
        endBeat: startBeat + duration
      }
    })
    .sort((a, b) => {
      if (Math.abs(a.startBeat - b.startBeat) > 0.001) return a.startBeat - b.startBeat
      return sortNotesByTimeline(a.note, b.note)
    })

  const pedalSegments: PedalSegment[] = []
  let activePedalStart: number | null = null

  noteEvents.forEach(({ note, startBeat }) => {
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
    const lastEndBeat = noteEvents.reduce((max, event) => Math.max(max, event.endBeat), activePedalStart)
    pedalSegments.push({ startBeat: activePedalStart, endBeat: lastEndBeat })
  }

  const sortedPedalSegments = pedalSegments
    .filter((segment) => segment.endBeat > segment.startBeat)
    .sort((a, b) => a.startBeat - b.startBeat)

  type PlaybackItem = {
    pitch: string
    durationTicks: number
    velocity: number
  }

  const playbackItemsByStartTicks = new Map<number, PlaybackItem[]>()
  let maxEndBeat = 0
  noteEvents.forEach(({ note, startBeat, endBeat }) => {
    let effectiveEndBeat = endBeat

    for (let i = 0; i < sortedPedalSegments.length; i += 1) {
      const segment = sortedPedalSegments[i]
      if (effectiveEndBeat < segment.startBeat - 0.001) {
        break
      }
      if (effectiveEndBeat >= segment.startBeat - 0.001 && effectiveEndBeat < segment.endBeat) {
        effectiveEndBeat = Math.max(effectiveEndBeat, segment.endBeat)
      }
    }

    maxEndBeat = Math.max(maxEndBeat, effectiveEndBeat)
    const noteStartTicks = getTicksForBeats(startBeat)
    const durationTicks = Math.max(1, getTicksForBeats(effectiveEndBeat - startBeat))
    const velocity = (note.velocity ?? 82) / 127
    const bucket = playbackItemsByStartTicks.get(noteStartTicks) ?? []
    bucket.push({
      pitch: note.pitch,
      durationTicks,
      velocity
    })
    playbackItemsByStartTicks.set(noteStartTicks, bucket)
  })

  playbackItemsByStartTicks.forEach((items, startTicks) => {
    transport.schedule((scheduledTime) => {
      if (!piano) return
      items.forEach((item) => {
        piano!.triggerAttackRelease(item.pitch, `${item.durationTicks}i`, scheduledTime, item.velocity)
      })
    }, `${startTicks}i`)
  })
  
  totalPlaybackTicks = getTicksForBeats(maxEndBeat)
  totalTime.value = Tone.Ticks(totalPlaybackTicks).toSeconds()
  schedulePlaybackEnd(totalPlaybackTicks + getTicksForBeats(0.1))
}

// 鍚姩鑺傛媿鍣?
const startMetronome = () => {
  if (!metronomeSynth) return
  
  if (metronomeLoop) {
    metronomeLoop.stop()
  }
  
  metronomeLoop = new Tone.Loop((time) => {
    const beat = Math.floor(transport.seconds / (60 / tempo.value))
    
    // 寮烘媿
    if (beat % 4 === 0) {
      metronomeSynth?.triggerAttackRelease('C5', '32n', time, 0.8)
    } 
    // 寮辨媿
    else {
      metronomeSynth?.triggerAttackRelease('C4', '32n', time, 0.4)
    }
  }, '4n')
  
  metronomeLoop.start(0)
}

// 闊抽珮璁＄畻
const calculatePitchFromPosition = (y: number): string => {
  const staffTop = 40
  const rowHeight = 150
  const pitchStepY = 5
  const rowIndex = Math.max(0, Math.floor((y - staffTop) / rowHeight))
  const middleCPosition = staffTop + rowIndex * rowHeight + 50
  const positionFromMiddleC = Math.round((middleCPosition - y) / pitchStepY)
  
  // 闊抽珮鏁扮粍
  const pitches = ['C', 'D', 'E', 'F', 'G', 'A', 'B']
  const octave = 4 + Math.floor(positionFromMiddleC / 7)
  const pitchIndex = (positionFromMiddleC % 7 + 7) % 7
  
  return `${pitches[pitchIndex]}${octave}`
}

// 鏍规嵁闊崇绫诲瀷鑾峰彇鏃堕暱
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

// 缂╂斁鎺у埗
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

// 鐢熷懡鍛ㄦ湡
onMounted(() => {
  if (currentScore.value) {
    tempo.value = normalizeTempo(currentScore.value.tempo)
    transport.bpm.value = tempo.value
  }
  void initAudioComponents()
  
  // 鐩戝惉閿洏蹇嵎閿?
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

  metronomeSynth?.dispose()
  metronomeLoop?.dispose()
  piano = null
  metronomeSynth = null
  metronomeLoop = null
})

// 閿洏蹇嵎閿?
const handleKeyDown = (event: KeyboardEvent) => {
  // 闃绘娴忚鍣ㄩ粯璁よ涓?
  if (event.ctrlKey || event.metaKey) {
    switch (event.key.toLowerCase()) {
      case 's':
        event.preventDefault()
        saveScore()
        break
      case 'z':
        if (event.shiftKey) {
          // Ctrl+Shift+Z 閲嶅仛
          console.log('閲嶅仛')
        } else {
          // Ctrl+Z 鎾ら攢
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
  
  // 宸ュ叿蹇嵎閿?
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

watch(
  () => currentScore.value?.tempo,
  (nextTempo) => {
    if (typeof nextTempo !== 'number') return
    const normalized = normalizeTempo(nextTempo)
    tempo.value = normalized
    transport.bpm.value = normalized
    if (totalPlaybackTicks > 0) {
      totalTime.value = Tone.Ticks(totalPlaybackTicks).toSeconds()
    }
  }
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

.playback-control-floating {
  @apply fixed right-6 bottom-6 z-50;
}
</style>





