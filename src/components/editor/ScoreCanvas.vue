<template>
  <div
    ref="container"
    class="score-canvas-container"
    :style="{ transform: `scale(${zoomLevel / 100})`, transformOrigin: 'top left' }"
  >
    <button class="debug-toggle" @click.stop="showDebugLayer = !showDebugLayer">
      {{ showDebugLayer ? '隐藏调试' : '显示调试' }}
    </button>

    <div
      v-if="highlightedMeasureStyle"
      class="measure-highlight"
      :style="highlightedMeasureStyle"
    ></div>

    <div ref="vfHost" class="vf-host"></div>

    <div
      v-if="previewNote"
      class="note-preview"
      :style="{
        left: `${previewNote.position.x}px`,
        top: `${previewNote.position.y}px`
      }"
    >
      {{ getPreviewIcon(previewNote) }}
    </div>

    <div v-if="showDebugLayer && debugState" class="debug-panel">
      <div>鼠标: x={{ debugState.pointer.x.toFixed(1) }}, y={{ debugState.pointer.y.toFixed(1) }}</div>
      <div>吸附: 小节 {{ debugState.snapped.measureIndex }}, 拍 {{ debugState.snapped.beat.toFixed(2) }}, {{ debugState.snapped.clef }}</div>
      <div v-if="debugState.hit">命中: {{ debugState.hit.pitch }} ({{ debugState.hit.clef }})</div>
      <div v-else>命中: 无</div>
    </div>

    <div
      v-if="showDebugLayer && debugState"
      class="debug-marker"
      :style="{ left: `${debugState.snapped.x}px`, top: `${debugState.snapped.y}px` }"
    ></div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { Accidental, Annotation, Beam, Dot, Formatter, Renderer, Stave, StaveConnector, StaveNote, StaveTie, Voice } from 'vexflow'
import type { Note, TimeSignature } from '@/types/score'

const props = withDefaults(defineProps<{
  notes: Note[]
  selectedNoteId: string | null
  currentTool: string
  timeSignature?: TimeSignature
  zoomLevel?: number
  beatSnap?: number
  currentMeasureIndex?: number | null
}>(), {
  timeSignature: () => ({ numerator: 4, denominator: 4 }),
  zoomLevel: 100,
  beatSnap: 0.25,
  currentMeasureIndex: null
})

const emit = defineEmits([
  'note-click',
  'canvas-click',
  'note-move',
  'note-drag-start',
  'note-drag-end'
])

const container = ref<HTMLElement>()
const vfHost = ref<HTMLDivElement>()
let resizeObserver: ResizeObserver | null = null

const previewNote = ref<Note | null>(null)
const isDragging = ref(false)
const dragNoteId = ref<string | null>(null)
const showDebugLayer = ref(false)
const renderedNotePoints = ref(new Map<string, { x: number; y: number; clef: 'treble' | 'bass' }>())

type SnapPoint = {
  x: number
  y: number
  clef: 'treble' | 'bass'
  rowIndex: number
  colIndex: number
  measureIndex: number
  beat: number
}

type DebugState = {
  pointer: { x: number; y: number }
  snapped: SnapPoint
  hit: { id: string; pitch: string; clef: 'treble' | 'bass' } | null
}
const debugState = ref<DebugState | null>(null)

const staffPadding = {
  left: 40,
  top: 40,
  right: 40,
  bottom: 40
}

const measuresPerRow = 4
const rowHeight = 260
const grandStaffGap = 88
const pitchStepY = 5

const durationByType: Record<string, number> = {
  whole: 4,
  half: 2,
  quarter: 1,
  eighth: 0.5,
  sixteenth: 0.25
}

const typeByDuration: Array<{ duration: number; type: Note['type'] }> = [
  { duration: 4, type: 'whole' },
  { duration: 2, type: 'half' },
  { duration: 1, type: 'quarter' },
  { duration: 0.5, type: 'eighth' },
  { duration: 0.25, type: 'sixteenth' }
]

const getDurationFromType = (noteType: string): number => {
  return durationByType[noteType] ?? 1
}

const getBeatSnapValue = () => {
  if (props.beatSnap === 1 || props.beatSnap === 0.5 || props.beatSnap === 0.25) {
    return props.beatSnap
  }

  return 0.25
}

const getBeatsPerMeasure = () => {
  const numerator = props.timeSignature?.numerator ?? 4
  const denominator = props.timeSignature?.denominator ?? 4
  if (denominator <= 0) return 4
  return (numerator * 4) / denominator
}

const getVexDuration = (note: Pick<Note, 'type' | 'isRest'>): string => {
  const mapping: Record<string, string> = {
    whole: 'w',
    half: 'h',
    quarter: 'q',
    eighth: '8',
    sixteenth: '16'
  }

  const base = mapping[note.type] ?? 'q'
  return note.isRest ? `${base}r` : base
}

const normalizePitch = (pitch: string): string => {
  const match = pitch.match(/^([A-Ga-g])([#b]?)(\d)$/)

  if (!match) {
    return 'c/4'
  }

  const [, step, accidental, octave] = match
  return `${step.toLowerCase()}${accidental}/${octave}`
}

const accidentalToSymbol = (accidental: Note['accidental']): string | null => {
  if (accidental === 'sharp') return '#'
  if (accidental === 'flat') return 'b'
  if (accidental === 'natural') return 'n'

  return null
}

const getPreviewIcon = (note: Note): string => {
  if (note.isRest) {
    return '饾劷'
  }

  const icons: Record<string, string> = {
    whole: '饾厺',
    half: '饾厼',
    quarter: '饾厽',
    eighth: '饾厾',
    sixteenth: '饾叀'
  }

  return icons[note.type] ?? '饾厽'
}

const getContainerPoint = (event: MouseEvent) => {
  if (!container.value) {
    return { x: 0, y: 0 }
  }

  const rect = container.value.getBoundingClientRect()
  const scale = props.zoomLevel / 100

  return {
    x: (event.clientX - rect.left) / scale,
    y: (event.clientY - rect.top) / scale
  }
}

const highlightedMeasureStyle = computed(() => {
  if (!container.value) return null
  if (typeof props.currentMeasureIndex !== 'number' || props.currentMeasureIndex < 0) return null

  const width = Math.max(container.value.clientWidth, 900)
  const { staveWidth } = measureLayoutFromWidth(width)
  const rowIndex = Math.floor(props.currentMeasureIndex / measuresPerRow)
  const colIndex = props.currentMeasureIndex % measuresPerRow
  const x = staffPadding.left + colIndex * staveWidth
  const y = staffPadding.top + rowIndex * rowHeight - 10

  return {
    left: `${x}px`,
    top: `${y}px`,
    width: `${Math.max(10, staveWidth - 8)}px`,
    height: `${grandStaffGap + 84}px`
  }
})

const clamp = (value: number, min: number, max: number) => Math.min(max, Math.max(min, value))

const measureLayoutFromWidth = (width: number) => {
  const staveWidth = Math.floor((width - staffPadding.left - staffPadding.right) / measuresPerRow)
  const drawableWidth = Math.max(50, staveWidth - 8)

  return { staveWidth, drawableWidth }
}

const getStaffLayout = (rowIndex: number) => {
  const trebleY = staffPadding.top + rowIndex * rowHeight
  const bassY = trebleY + grandStaffGap
  return { trebleY, bassY }
}

const getMiddleCForClef = (rowIndex: number, clef: 'treble' | 'bass') => {
  const { trebleY, bassY } = getStaffLayout(rowIndex)
  if (clef === 'bass') {
    const bassStave = new Stave(0, bassY, 120)
    return bassStave.getYForLine(-1)
  }
  const trebleStave = new Stave(0, trebleY, 120)
  return trebleStave.getYForLine(5)
}

const getClefBoundaryY = (rowIndex: number) => {
  const { trebleY, bassY } = getStaffLayout(rowIndex)
  const trebleStave = new Stave(0, trebleY, 120)
  const bassStave = new Stave(0, bassY, 120)

  const trebleBottomWithLedger = trebleStave.getYForLine(4) + pitchStepY * 3
  const bassTopWithLedger = bassStave.getYForLine(0) - pitchStepY * 3

  return (trebleBottomWithLedger + bassTopWithLedger) / 2
}

const getPitchFromSnappedPoint = (
  snapped: { y: number; rowIndex: number; clef: 'treble' | 'bass' }
) => {
  const middleCY = getMiddleCForClef(snapped.rowIndex, snapped.clef)
  const positionFromMiddleC = Math.round((middleCY - snapped.y) / pitchStepY)

  const pitches = ['C', 'D', 'E', 'F', 'G', 'A', 'B']
  const octave = 4 + Math.floor(positionFromMiddleC / 7)
  const pitchIndex = (positionFromMiddleC % 7 + 7) % 7

  return `${pitches[pitchIndex]}${octave}`
}

const getRenderedYFromPitch = (rowIndex: number, clef: 'treble' | 'bass', pitch: string) => {
  const match = pitch.match(/^([A-Ga-g])([#b]?)(\d)$/)
  if (!match) {
    return getMiddleCForClef(rowIndex, clef)
  }

  const [, stepRaw, , octaveRaw] = match
  const step = stepRaw.toUpperCase()
  const octave = Number(octaveRaw)
  const stepToIndex: Record<string, number> = { C: 0, D: 1, E: 2, F: 3, G: 4, A: 5, B: 6 }
  const positionFromMiddleC = (octave - 4) * 7 + (stepToIndex[step] ?? 0)
  const middleC = getMiddleCForClef(rowIndex, clef)

  return middleC - positionFromMiddleC * pitchStepY
}

const snapPointToGrid = (point: { x: number; y: number }, width: number) => {
  const beatSnap = getBeatSnapValue()
  const beatsPerMeasure = getBeatsPerMeasure()
  const { staveWidth } = measureLayoutFromWidth(width)
  const rowIndex = Math.max(0, Math.floor((point.y - staffPadding.top) / rowHeight))
  const { trebleY, bassY } = getStaffLayout(rowIndex)
  const clefBoundary = getClefBoundaryY(rowIndex)
  const clef: 'treble' | 'bass' = point.y > clefBoundary ? 'bass' : 'treble'
  const colRaw = Math.floor((point.x - staffPadding.left) / staveWidth)
  const colIndex = clamp(colRaw, 0, measuresPerRow - 1)
  const measureIndex = rowIndex * measuresPerRow + colIndex

  const baseX = staffPadding.left + colIndex * staveWidth
  const staveY = clef === 'bass' ? bassY : trebleY
  const snapStave = new Stave(baseX, staveY, staveWidth - 8)
  if (measureIndex === 0) {
    const tsText = `${props.timeSignature?.numerator ?? 4}/${props.timeSignature?.denominator ?? 4}`
    snapStave.addClef(clef).addTimeSignature(tsText).addKeySignature('C')
  }

  const noteStartX = snapStave.getNoteStartX()
  const noteEndX = snapStave.getNoteEndX()
  const noteAreaWidth = Math.max(1, noteEndX - noteStartX)
  const localX = clamp(point.x - noteStartX, 0, noteAreaWidth)
  const beat = clamp(
    Math.round((localX / noteAreaWidth) * beatsPerMeasure / beatSnap) * beatSnap,
    0,
    beatsPerMeasure - beatSnap
  )
  const snappedX = noteStartX + (beat / beatsPerMeasure) * noteAreaWidth

  const middleC = getMiddleCForClef(rowIndex, clef)
  const snappedY = middleC - Math.round((middleC - point.y) / pitchStepY) * pitchStepY

  return {
    x: snappedX,
    y: snappedY,
    clef,
    rowIndex,
    colIndex,
    measureIndex,
    beat
  }
}

const getPlacement = (position: { x: number; y: number }, width: number) => {
  return snapPointToGrid(position, width)
}

const getPlacementForNote = (note: Note, width: number): SnapPoint => {
  const beatsPerMeasure = getBeatsPerMeasure()
  const hasMeasureData = typeof note.measureIndex === 'number' && typeof note.beatInMeasure === 'number'
  if (!hasMeasureData) {
    return getPlacement(note.position, width)
  }

  const measureIndex = Math.max(0, Math.floor(note.measureIndex!))
  const rowIndex = Math.max(0, Math.floor(measureIndex / measuresPerRow))
  const colIndex = measureIndex % measuresPerRow
  const clef: 'treble' | 'bass' = note.clef ?? 'treble'
  const beat = clamp(note.beatInMeasure!, 0, Math.max(0, beatsPerMeasure - 0.001))
  const { staveWidth } = measureLayoutFromWidth(width)
  const { trebleY, bassY } = getStaffLayout(rowIndex)
  const baseX = staffPadding.left + colIndex * staveWidth
  const staveY = clef === 'bass' ? bassY : trebleY
  const stave = new Stave(baseX, staveY, staveWidth - 8)
  if (measureIndex === 0) {
    const tsText = `${props.timeSignature?.numerator ?? 4}/${props.timeSignature?.denominator ?? 4}`
    stave.addClef(clef).addTimeSignature(tsText).addKeySignature('C')
  }

  const noteStartX = stave.getNoteStartX()
  const noteEndX = stave.getNoteEndX()
  const noteAreaWidth = Math.max(1, noteEndX - noteStartX)
  const x = noteStartX + (beat / Math.max(1, beatsPerMeasure)) * noteAreaWidth
  const y = getRenderedYFromPitch(rowIndex, clef, note.pitch)

  return {
    x,
    y,
    clef,
    rowIndex,
    colIndex,
    measureIndex,
    beat
  }
}

type TickableNote = Pick<Note, 'type' | 'isRest'> & { pitch: string; dots?: number; clef?: 'treble' | 'bass' }
type NoteRenderRef = { staveNote: StaveNote; keyIndex: number }

const createVexNote = (note: Note | TickableNote) => {
  const clef = (note as Note).clef ?? 'treble'
  const staveNote = new StaveNote({
    clef,
    keys: [note.isRest ? 'b/4' : normalizePitch(note.pitch)],
    duration: getVexDuration(note)
  })

  const fullNote = note as Note

  if (!note.isRest) {
    const symbol = accidentalToSymbol(fullNote.accidental)
    if (symbol) {
      staveNote.addModifier(new Accidental(symbol), 0)
    }
  }

  const dots = fullNote.dots ?? 0
  for (let i = 0; i < dots; i += 1) {
    Dot.buildAndAttach([staveNote], { all: true })
  }

  if (!note.isRest && fullNote.pedalStart) {
    const pedMark = new Annotation('Ped.')
      .setFont('Arial', 11)
      .setVerticalJustification(Annotation.VerticalJustify.BOTTOM)
    staveNote.addModifier(pedMark, 0)
  }

  if (!note.isRest && fullNote.pedalEnd) {
    const releaseMark = new Annotation('*')
      .setFont('Arial', 12, 'bold')
      .setVerticalJustification(Annotation.VerticalJustify.BOTTOM)
    staveNote.addModifier(releaseMark, 0)
  }

  if (fullNote.id && fullNote.id === props.selectedNoteId) {
    staveNote.setStyle({ fillStyle: '#2563eb', strokeStyle: '#2563eb' })
  }

  return staveNote
}

const createVexChordNote = (chordNotes: Note[]) => {
  const sorted = [...chordNotes].sort((a, b) => b.position.y - a.position.y)
  const headNote = sorted[0]
  const keys = sorted.map((note) => normalizePitch(note.pitch))
  const clef = headNote.clef ?? 'treble'

  const staveNote = new StaveNote({
    clef,
    keys,
    duration: getVexDuration(headNote)
  })

  sorted.forEach((note, index) => {
    const symbol = accidentalToSymbol(note.accidental)
    if (symbol) {
      staveNote.addModifier(new Accidental(symbol), index)
    }
  })

  const dots = Math.max(...sorted.map((note) => note.dots ?? 0))
  for (let i = 0; i < dots; i += 1) {
    Dot.buildAndAttach([staveNote], { all: true })
  }

  if (sorted.some((note) => note.pedalStart)) {
    const pedMark = new Annotation('Ped.')
      .setFont('Arial', 11)
      .setVerticalJustification(Annotation.VerticalJustify.BOTTOM)
    staveNote.addModifier(pedMark, 0)
  }

  if (sorted.some((note) => note.pedalEnd)) {
    const releaseMark = new Annotation('*')
      .setFont('Arial', 12, 'bold')
      .setVerticalJustification(Annotation.VerticalJustify.BOTTOM)
    staveNote.addModifier(releaseMark, 0)
  }

  if (sorted.some((note) => note.id === props.selectedNoteId)) {
    staveNote.setStyle({ fillStyle: '#2563eb', strokeStyle: '#2563eb' })
  }

  return { staveNote, sortedNotes: sorted }
}

const makeRestTickables = (gapBeats: number, clef: 'treble' | 'bass'): TickableNote[] => {
  const beatSnap = getBeatSnapValue()
  const tickables: TickableNote[] = []
  let remaining = Math.round(gapBeats / beatSnap) * beatSnap

  while (remaining > 0.001) {
    const segment = typeByDuration.find((item) => item.duration <= remaining + 0.001)
    if (!segment) break

    tickables.push({
      type: segment.type,
      isRest: true,
      pitch: 'B4',
      clef
    })

    remaining -= segment.duration
  }

  return tickables
}

const drawScore = () => {
  if (!vfHost.value || !container.value) return

  const width = Math.max(container.value.clientWidth, 900)
  const placements = props.notes.map((note) => ({ note, placement: getPlacementForNote(note, width) }))
  const maxMeasureIndex = placements.length ? Math.max(...placements.map((item) => item.placement.measureIndex)) : 0
  const measureCount = Math.max(1, maxMeasureIndex + 1)
  const rows = Math.max(1, Math.ceil(measureCount / measuresPerRow))
  const height = Math.max(container.value.clientHeight, staffPadding.top + rows * rowHeight + staffPadding.bottom)

  vfHost.value.innerHTML = ''

  const renderer = new Renderer(vfHost.value, Renderer.Backends.SVG)
  renderer.resize(width, height)
  const context = renderer.getContext()
  renderedNotePoints.value = new Map()

  const { staveWidth } = measureLayoutFromWidth(width)
  const beatsPerMeasure = getBeatsPerMeasure()
  const vexNoteById = new Map<string, NoteRenderRef>()
  const placementById = new Map<string, SnapPoint>()
  const measureMap = new Map<number, Array<{ note: Note; beat: number; clef: 'treble' | 'bass' }>>()

  placements.forEach(({ note, placement }) => {
    placementById.set(note.id, placement as SnapPoint)
    const list = measureMap.get(placement.measureIndex) ?? []
    const clef = note.clef ?? placement.clef
    list.push({ note: { ...note, clef }, beat: placement.beat, clef })
    measureMap.set(placement.measureIndex, list)
  })

  for (let measureIndex = 0; measureIndex < measureCount; measureIndex += 1) {
    const rowIndex = Math.floor(measureIndex / measuresPerRow)
    const colIndex = measureIndex % measuresPerRow
    const staveX = staffPadding.left + colIndex * staveWidth
    const { trebleY, bassY } = getStaffLayout(rowIndex)
    const trebleStave = new Stave(staveX, trebleY, staveWidth - 8)
    const bassStave = new Stave(staveX, bassY, staveWidth - 8)

    if (measureIndex === 0) {
      const tsText = `${props.timeSignature?.numerator ?? 4}/${props.timeSignature?.denominator ?? 4}`
      trebleStave.addClef('treble').addTimeSignature(tsText).addKeySignature('C')
      bassStave.addClef('bass').addTimeSignature(tsText).addKeySignature('C')
    }

    trebleStave.setContext(context).draw()
    bassStave.setContext(context).draw()

    new StaveConnector(trebleStave, bassStave)
      .setType(StaveConnector.type.BRACE)
      .setContext(context)
      .draw()
    new StaveConnector(trebleStave, bassStave)
      .setType(StaveConnector.type.SINGLE_LEFT)
      .setContext(context)
      .draw()

    const items = (measureMap.get(measureIndex) ?? []).sort((a, b) => {
      const beatDiff = a.beat - b.beat
      if (beatDiff !== 0) return beatDiff
      return a.note.position.y - b.note.position.y
    })

    const buildClefVoice = (clef: 'treble' | 'bass') => {
      const clefItems = items.filter((item) => item.clef === clef)
      const vexNotes: StaveNote[] = []
      let cursorBeat = 0
      let index = 0

      while (index < clefItems.length) {
        const currentItem = clefItems[index]
        const currentDuration = currentItem.note.duration || getDurationFromType(currentItem.note.type)

        if (currentItem.beat > cursorBeat + 0.001) {
          makeRestTickables(currentItem.beat - cursorBeat, clef).forEach((rest) => {
            vexNotes.push(createVexNote(rest))
          })
          cursorBeat = currentItem.beat
        }

        if (currentItem.note.isRest) {
          vexNotes.push(createVexNote({ ...currentItem.note, clef }))
          cursorBeat += currentDuration
          index += 1
          continue
        }

        const chordNotes: Note[] = [currentItem.note]
        let nextIndex = index + 1
        while (nextIndex < clefItems.length) {
          const nextItem = clefItems[nextIndex]
          const nextDuration = nextItem.note.duration || getDurationFromType(nextItem.note.type)
          const sameBeat = Math.abs(nextItem.beat - currentItem.beat) <= 0.001
          const sameDuration = Math.abs(nextDuration - currentDuration) <= 0.001
          if (!sameBeat || !sameDuration || nextItem.note.isRest) break
          chordNotes.push(nextItem.note)
          nextIndex += 1
        }

        if (chordNotes.length > 1) {
          const { staveNote, sortedNotes } = createVexChordNote(chordNotes)
          vexNotes.push(staveNote)
          sortedNotes.forEach((note, keyIndex) => {
            vexNoteById.set(note.id, { staveNote, keyIndex })
          })
        } else {
          const staveNote = createVexNote(chordNotes[0])
          vexNotes.push(staveNote)
          vexNoteById.set(chordNotes[0].id, { staveNote, keyIndex: 0 })
        }

        cursorBeat += currentDuration
        index = nextIndex
      }

      if (cursorBeat < beatsPerMeasure - 0.001) {
        makeRestTickables(beatsPerMeasure - cursorBeat, clef).forEach((rest) => {
          vexNotes.push(createVexNote(rest))
        })
      }

      const voice = new Voice({ num_beats: beatsPerMeasure, beat_value: 4 })
      voice.setStrict(false)
      voice.addTickables(vexNotes)

      return { voice, vexNotes }
    }

    const trebleVoiceData = buildClefVoice('treble')
    const bassVoiceData = buildClefVoice('bass')

    new Formatter()
      .joinVoices([trebleVoiceData.voice])
      .joinVoices([bassVoiceData.voice])
      .formatToStave([trebleVoiceData.voice], trebleStave)
      .formatToStave([bassVoiceData.voice], bassStave)

    trebleVoiceData.voice.draw(context, trebleStave)
    bassVoiceData.voice.draw(context, bassStave)

    Beam.generateBeams(trebleVoiceData.vexNotes.filter((note) => !note.isRest?.())).forEach((beam) => {
      beam.setContext(context).draw()
    })
    Beam.generateBeams(bassVoiceData.vexNotes.filter((note) => !note.isRest?.())).forEach((beam) => {
      beam.setContext(context).draw()
    })
  }

  const sortedNotes = placements
    .map(({ note, placement }) => ({ note, placement }))
    .sort((a, b) => {
      const measureDiff = a.placement.measureIndex - b.placement.measureIndex
      if (measureDiff !== 0) return measureDiff
      const beatDiff = a.placement.beat - b.placement.beat
      if (beatDiff !== 0) return beatDiff
      const clefDiff = (a.note.clef === 'bass' ? 1 : 0) - (b.note.clef === 'bass' ? 1 : 0)
      if (clefDiff !== 0) return clefDiff
      return a.note.position.y - b.note.position.y
    })
    .map((item) => item.note)

  sortedNotes.forEach((note, index) => {
    if (!note.tieToNext || note.isRest) return

    const next = sortedNotes
      .slice(index + 1)
      .find((item) => !item.isRest && item.pitch === note.pitch && (item.clef ?? 'treble') === (note.clef ?? 'treble'))
    if (!next) return

    const firstNote = vexNoteById.get(note.id)
    const lastNote = vexNoteById.get(next.id)
    if (!firstNote || !lastNote) return

    const tie = new StaveTie({
      first_note: firstNote.staveNote,
      last_note: lastNote.staveNote,
      first_indices: [firstNote.keyIndex],
      last_indices: [lastNote.keyIndex]
    })

    tie.setContext(context).draw()
  })

  const rendered = new Map<string, { x: number; y: number; clef: 'treble' | 'bass' }>()
  props.notes.forEach((note) => {
    const refInfo = vexNoteById.get(note.id)
    if (!refInfo) return

    const absoluteX = refInfo.staveNote.getAbsoluteX()
    const ys = refInfo.staveNote.getYs()
    const y = Array.isArray(ys) ? (ys[refInfo.keyIndex] ?? ys[0]) : note.position.y
    rendered.set(note.id, { x: absoluteX, y, clef: note.clef ?? 'treble' })
  })
  renderedNotePoints.value = rendered

  const noteOrder = [...props.notes]
    .filter((note) => !note.isRest)
    .sort((a, b) => {
      const pa = placementById.get(a.id)
      const pb = placementById.get(b.id)
      if (!pa || !pb) return 0
      if (pa.measureIndex !== pb.measureIndex) return pa.measureIndex - pb.measureIndex
      if (Math.abs(pa.beat - pb.beat) > 0.001) return pa.beat - pb.beat
      return (a.clef === 'bass' ? 1 : 0) - (b.clef === 'bass' ? 1 : 0)
    })

  const rowStartX = staffPadding.left + 8
  const rowEndX = staffPadding.left + measuresPerRow * staveWidth - 16

  const drawPedalLineSegment = (rowIndex: number, fromX: number, toX: number, isSegmentEnd: boolean) => {
    const y = getStaffLayout(rowIndex).bassY + 78
    const ctx: any = context
    ctx.save?.()
    ctx.setStrokeStyle?.('#475569')
    ctx.setLineWidth?.(1.2)
    ctx.beginPath?.()
    ctx.moveTo?.(fromX, y)
    ctx.lineTo?.(toX, y)
    ctx.stroke?.()

    if (isSegmentEnd) {
      ctx.beginPath?.()
      ctx.moveTo?.(toX, y)
      ctx.lineTo?.(toX, y - 8)
      ctx.stroke?.()
    }
    ctx.restore?.()
  }

  const drawPedalRange = (startNote: Note, endNote?: Note) => {
    const startPlacement = placementById.get(startNote.id)
    const startPoint = rendered.get(startNote.id)
    if (!startPlacement || !startPoint) return

    const startRow = startPlacement.rowIndex
    const startX = startPoint.x

    if (!endNote) {
      drawPedalLineSegment(startRow, startX, rowEndX, true)
      return
    }

    const endPlacement = placementById.get(endNote.id)
    const endPoint = rendered.get(endNote.id)
    if (!endPlacement || !endPoint) return

    const endRow = endPlacement.rowIndex
    const endX = endPoint.x

    if (startRow === endRow) {
      drawPedalLineSegment(startRow, startX, endX, true)
      return
    }

    drawPedalLineSegment(startRow, startX, rowEndX, false)
    for (let row = startRow + 1; row < endRow; row += 1) {
      drawPedalLineSegment(row, rowStartX, rowEndX, false)
    }
    drawPedalLineSegment(endRow, rowStartX, endX, true)
  }

  let activePedalStart: Note | null = null
  noteOrder.forEach((note) => {
    if (note.pedalStart) {
      if (activePedalStart) {
        drawPedalRange(activePedalStart, note)
      }
      activePedalStart = note
    }

    if (note.pedalEnd && activePedalStart) {
      drawPedalRange(activePedalStart, note)
      activePedalStart = null
    }
  })

  if (activePedalStart) {
    drawPedalRange(activePedalStart)
  }
}

const findNoteByPoint = (x: number, y: number, width: number) => {
  const renderedNearest = props.notes
    .map((note) => {
      const rendered = renderedNotePoints.value.get(note.id)
      if (!rendered) return null
      return {
        note,
        distance: Math.hypot(rendered.x - x, rendered.y - y)
      }
    })
    .filter((item): item is { note: Note; distance: number } => Boolean(item))
    .sort((a, b) => a.distance - b.distance)[0]

  if (renderedNearest && renderedNearest.distance <= 22) {
    return renderedNearest.note
  }

  const nearestVisual = props.notes
    .map((note) => {
      const placement = getPlacementForNote(note, width)
      const clef = note.clef ?? placement.clef
      return {
        note,
        distance: Math.hypot(
          placement.x - x,
          getRenderedYFromPitch(placement.rowIndex, clef, note.pitch) - y
        )
      }
    })
    .sort((a, b) => a.distance - b.distance)[0]

  if (nearestVisual && nearestVisual.distance <= 20) {
    return nearestVisual.note
  }

  const clickedPlacement = snapPointToGrid({ x, y }, width)
  const beatTolerance = 0.001

  const candidates = props.notes
    .map((note) => {
      const placement = getPlacementForNote(note, width)
      const clef = note.clef ?? placement.clef
      return {
        note,
        placement,
        clef,
        displayPoint: {
          x: placement.x,
          y: getRenderedYFromPitch(placement.rowIndex, clef, note.pitch)
        }
      }
    })
    .filter(({ placement, clef }) => {
      return placement.measureIndex === clickedPlacement.measureIndex
        && Math.abs(placement.beat - clickedPlacement.beat) <= beatTolerance
        && clickedPlacement.clef === clef
    })

  if (candidates.length > 0) {
    return candidates
      .map((item) => ({
        note: item.note,
        distance: Math.hypot(item.displayPoint.x - x, item.displayPoint.y - y)
      }))
      .sort((a, b) => a.distance - b.distance)[0]
      ?.note
  }
  return undefined
}

const updateDebugState = (point: { x: number; y: number }, snapped: SnapPoint, hit: Note | undefined) => {
  if (!showDebugLayer.value) return
  debugState.value = {
    pointer: point,
    snapped,
    hit: hit
      ? { id: hit.id, pitch: hit.pitch, clef: hit.clef ?? 'treble' }
      : null
  }
}

const handleCanvasClick = (event: MouseEvent) => {
  if (!container.value) return

  const width = Math.max(container.value.clientWidth, 900)
  const point = getContainerPoint(event)
  const clickedNote = findNoteByPoint(point.x, point.y, width)
  const snapped = snapPointToGrid(point, width)
  updateDebugState(point, snapped, clickedNote)

  if (clickedNote) {
    if (props.currentTool === 'chord' && props.selectedNoteId && clickedNote.id === props.selectedNoteId) {
      const pitch = getPitchFromSnappedPoint(snapped)
      emit('canvas-click', {
        x: snapped.x,
        y: snapped.y,
        pitch,
        clef: snapped.clef,
        measureIndex: snapped.measureIndex,
        beatInMeasure: snapped.beat
      })
      return
    }

    emit('note-click', clickedNote.id)
    return
  }

  const pitch = getPitchFromSnappedPoint(snapped)
  emit('canvas-click', {
    x: snapped.x,
    y: snapped.y,
    pitch,
    clef: snapped.clef,
    measureIndex: snapped.measureIndex,
    beatInMeasure: snapped.beat
  })
}

const handleMouseMove = (event: MouseEvent) => {
  if (!container.value) return

  const point = getContainerPoint(event)
  const width = Math.max(container.value.clientWidth, 900)
  const snapped = snapPointToGrid(point, width)
  const hoveredNote = findNoteByPoint(point.x, point.y, width)
  updateDebugState(point, snapped, hoveredNote)

  if (isDragging.value && dragNoteId.value) {
    const pitch = getPitchFromSnappedPoint(snapped)
    emit('note-move', dragNoteId.value, {
      x: snapped.x,
      y: snapped.y,
      pitch,
      clef: snapped.clef,
      measureIndex: snapped.measureIndex,
      beatInMeasure: snapped.beat
    })
    return
  }

  if (props.currentTool === 'note' || props.currentTool === 'chord' || props.currentTool === 'rest') {
    previewNote.value = {
      id: 'preview',
      type: 'quarter',
      pitch: getPitchFromSnappedPoint(snapped),
      duration: 1,
      position: { x: snapped.x, y: snapped.y },
      clef: snapped.clef,
      isRest: props.currentTool === 'rest'
    }
  }
}

const handleMouseDown = (event: MouseEvent) => {
  if (props.currentTool !== 'select') return
  if (!container.value) return

  const width = Math.max(container.value.clientWidth, 900)
  const point = getContainerPoint(event)
  const clickedNote = findNoteByPoint(point.x, point.y, width)
  if (!clickedNote) return

  isDragging.value = true
  dragNoteId.value = clickedNote.id
  emit('note-drag-start', clickedNote.id)
}

const handleMouseUp = (event: MouseEvent) => {
  if (!isDragging.value || !dragNoteId.value || !container.value) return

  const point = getContainerPoint(event)
  const width = Math.max(container.value.clientWidth, 900)
  const snapped = snapPointToGrid(point, width)
  const pitch = getPitchFromSnappedPoint(snapped)
  emit('note-drag-end', dragNoteId.value, {
    x: snapped.x,
    y: snapped.y,
    pitch,
    clef: snapped.clef,
    measureIndex: snapped.measureIndex,
    beatInMeasure: snapped.beat
  })

  isDragging.value = false
  dragNoteId.value = null
}

const handleResize = () => {
  drawScore()
}

onMounted(() => {
  nextTick(() => {
    drawScore()
    requestAnimationFrame(() => {
      drawScore()
    })

    if (container.value) {
      container.value.addEventListener('click', handleCanvasClick)
      container.value.addEventListener('mousemove', handleMouseMove)
      container.value.addEventListener('mousedown', handleMouseDown)
      container.value.addEventListener('mouseup', handleMouseUp)

      if (typeof ResizeObserver !== 'undefined') {
        resizeObserver = new ResizeObserver(() => {
          drawScore()
        })
        resizeObserver.observe(container.value)
      }
    }

    window.addEventListener('resize', handleResize)
  })
})

onUnmounted(() => {
  if (container.value) {
    container.value.removeEventListener('click', handleCanvasClick)
    container.value.removeEventListener('mousemove', handleMouseMove)
    container.value.removeEventListener('mousedown', handleMouseDown)
    container.value.removeEventListener('mouseup', handleMouseUp)
  }
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }

  window.removeEventListener('resize', handleResize)
})

watch(() => props.notes, () => {
  drawScore()
}, { deep: true })

watch(() => props.selectedNoteId, () => {
  drawScore()
})

watch(() => props.zoomLevel, () => {
  drawScore()
})

watch(() => props.beatSnap, () => {
  drawScore()
})

watch(() => props.timeSignature, () => {
  drawScore()
}, { deep: true })

watch(() => props.currentTool, (newTool) => {
  if (newTool !== 'note' && newTool !== 'chord' && newTool !== 'rest') {
    previewNote.value = null
  }
})
</script>

<style scoped>
.score-canvas-container {
  @apply relative w-full h-full;
}

.vf-host {
  @apply w-full h-full bg-white;
  min-height: 420px;
}

.measure-highlight {
  @apply absolute z-10 pointer-events-none rounded-md border border-red-500;
  background: rgba(239, 68, 68, 0.14);
}
.note-preview {
  @apply absolute w-8 h-8 text-xl text-blue-500 opacity-50 pointer-events-none
    flex items-center justify-center transform -translate-x-1/2 -translate-y-1/2;
}

.debug-toggle {
  @apply absolute top-3 right-3 z-20 px-2 py-1 text-xs rounded border border-slate-300 bg-white/90 text-slate-700 hover:bg-white;
}

.debug-panel {
  @apply absolute top-12 right-3 z-20 p-2 rounded border border-slate-300 bg-white/95 text-[11px] text-slate-700 leading-5;
}

.debug-marker {
  @apply absolute z-20 w-3 h-3 rounded-full border-2 border-rose-500 bg-rose-200 pointer-events-none transform -translate-x-1/2 -translate-y-1/2;
}
</style>


