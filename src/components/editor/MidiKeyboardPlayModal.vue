<template>
  <div class="midi-overlay">
    <div class="midi-panel">
      <div class="midi-header">
        <div>
          <div class="midi-title">MIDI Keyboard Play Mode</div>
          <div class="midi-subtitle">Waterfall synced to current score</div>
        </div>
        <button class="close-btn" type="button" @click="$emit('close')">Close</button>
      </div>

      <div class="waterfall-area">
        <div class="waterfall-glow"></div>
        <div class="hit-line"></div>

        <div
          v-for="note in visibleWaterfallNotes"
          :key="note.id"
          class="waterfall-note"
          :style="note.style"
        ></div>

        <div
          v-for="particle in impactParticles"
          :key="particle.id"
          class="impact-particle"
          :style="particle.style"
        ></div>

        <div
          v-for="glow in contactGlows"
          :key="glow.id"
          class="contact-glow"
          :style="glow.style"
        ></div>
      </div>

      <div ref="keyboardAreaRef" class="keyboard-area">
        <div
          v-for="key in keyLayout"
          :key="key.midi"
          :class="[
            'piano-key',
            key.isBlack ? 'black-key' : 'white-key',
            activeMidiSet.has(key.midi) ? 'key-active' : ''
          ]"
          :style="{
            left: `${key.left}px`,
            width: `${key.width}px`
          }"
        >
          <span v-if="!key.isBlack && key.label" class="key-label">{{ key.label }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

type WaterfallEvent = {
  id: string
  pitch: string
  startSec: number
  durationSec: number
}

const props = defineProps<{
  events: WaterfallEvent[]
  currentTime: number
}>()

defineEmits(['close'])

const WATERFALL_HEIGHT = 600
const PANEL_WIDTH = 1440
const NOTE_FALL_SPEED = 200

const keyboardAreaRef = ref<HTMLElement | null>(null)
const panelWidth = ref(PANEL_WIDTH)

const SEMITONE_MAP: Record<string, number> = {
  C: 0,
  'C#': 1,
  Db: 1,
  D: 2,
  'D#': 3,
  Eb: 3,
  E: 4,
  F: 5,
  'F#': 6,
  Gb: 6,
  G: 7,
  'G#': 8,
  Ab: 8,
  A: 9,
  'A#': 10,
  Bb: 10,
  B: 11
}

const BLACK_SEMITONES = new Set([1, 3, 6, 8, 10])

const pitchToMidi = (pitch: string) => {
  const match = pitch.match(/^([A-Ga-g])([#b]?)(-?\d)$/)
  if (!match) return null
  const [, stepRaw, accidentalRaw, octaveRaw] = match
  const step = stepRaw.toUpperCase() + accidentalRaw
  const octave = Number(octaveRaw)
  const semitone = SEMITONE_MAP[step]
  if (semitone === undefined || Number.isNaN(octave)) return null
  return (octave + 1) * 12 + semitone
}

const keyRange = computed(() => ({ min: 21, max: 108 }))

const keyLayout = computed(() => {
  const keys: Array<{ midi: number; isBlack: boolean; left: number; width: number; label: string }> = []
  const whites: number[] = []
  for (let midi = keyRange.value.min; midi <= keyRange.value.max; midi += 1) {
    const semitone = midi % 12
    if (!BLACK_SEMITONES.has(semitone)) whites.push(midi)
  }

  const whiteWidth = panelWidth.value / Math.max(1, whites.length)
  const blackWidth = whiteWidth * 0.62

  let whiteIndex = 0
  for (let midi = keyRange.value.min; midi <= keyRange.value.max; midi += 1) {
    const semitone = midi % 12
    const isBlack = BLACK_SEMITONES.has(semitone)
    if (!isBlack) {
      keys.push({
        midi,
        isBlack: false,
        left: whiteIndex * whiteWidth,
        width: whiteWidth,
        label: semitone === 0 ? `C${Math.floor(midi / 12) - 1}` : ''
      })
      whiteIndex += 1
      continue
    }
    keys.push({
      midi,
      isBlack: true,
      left: Math.max(0, whiteIndex * whiteWidth - blackWidth / 2),
      width: blackWidth,
      label: ''
    })
  }
  return keys
})

const keyMap = computed(() => {
  const map = new Map<number, { left: number; width: number }>()
  keyLayout.value.forEach((key) => {
    map.set(key.midi, { left: key.left, width: key.width })
  })
  return map
})

const preparedEvents = computed(() => {
  return props.events
    .map((event) => {
      const midi = pitchToMidi(event.pitch)
      if (midi === null) return null
      const key = keyMap.value.get(midi)
      if (!key) return null
      const durationSec = Math.max(0.04, event.durationSec)
      const barHeight = Math.max(10, durationSec * NOTE_FALL_SPEED)
      return {
        id: event.id,
        midi,
        startSec: event.startSec,
        endSec: event.startSec + durationSec,
        left: key.left + 1,
        width: Math.max(2, key.width - 2),
        barHeight
      }
    })
    .filter(
      (
        item
      ): item is {
        id: string
        midi: number
        startSec: number
        endSec: number
        left: number
        width: number
        barHeight: number
      } => item !== null
    )
})

const hitLineY = WATERFALL_HEIGHT - 6

const currentSec = computed(() => (Number.isFinite(props.currentTime) ? props.currentTime : 0))

const hashString = (value: string) => {
  let hash = 0
  for (let i = 0; i < value.length; i += 1) {
    hash = (hash * 31 + value.charCodeAt(i)) | 0
  }
  return hash >>> 0
}

const random01FromSeed = (seed: number) => {
  let t = seed + 0x6d2b79f5
  t = Math.imul(t ^ (t >>> 15), t | 1)
  t ^= t + Math.imul(t ^ (t >>> 7), t | 61)
  return ((t ^ (t >>> 14)) >>> 0) / 4294967296
}

const visibleWaterfallNotes = computed(() => {
  const current = currentSec.value
  return preparedEvents.value
    .map((event) => {
      const yTop = hitLineY - (event.startSec - current) * NOTE_FALL_SPEED - event.barHeight
      if (yTop > WATERFALL_HEIGHT || yTop + event.barHeight < 0) return null
      const overflowBottom = Math.max(0, yTop + event.barHeight - hitLineY)
      const visibleHeight = Math.max(0, event.barHeight - overflowBottom)
      if (visibleHeight <= 0) return null

      return {
        id: event.id,
        style: {
          left: `${event.left}px`,
          width: `${event.width}px`,
          height: `${event.barHeight}px`,
          transform: `translate3d(0, ${yTop}px, 0)`,
          clipPath: `inset(0 0 ${event.barHeight - visibleHeight}px 0)`
        }
      }
    })
    .filter((item): item is { id: string; style: Record<string, string> } => item !== null)
})

const impactParticles = computed(() => {
  const current = currentSec.value
  const particles: Array<{ id: string; style: Record<string, string> }> = []

  preparedEvents.value.forEach((event) => {
    if (current < event.startSec || current > event.endSec) return
    const duration = Math.max(0.04, event.endSec - event.startSec)
    const localElapsed = current - event.startSec
    const enterFade = Math.min(1, localElapsed / 0.07)
    const exitFade = Math.min(1, (event.endSec - current) / 0.08)
    const fade = Math.max(0, Math.min(enterFade, exitFade))
    const centerX = event.left + event.width / 2
    const centerY = hitLineY
    const hue = 172 + (event.midi % 12) * 4
    const particleCount = 48
    const baseSeed = hashString(event.id)
    const phase = localElapsed / duration

    for (let i = 0; i < particleCount; i += 1) {
      const seed = baseSeed + i * 131
      const randA = random01FromSeed(seed)
      const randB = random01FromSeed(seed + 17)
      const randC = random01FromSeed(seed + 31)
      const randD = random01FromSeed(seed + 47)
      const randE = random01FromSeed(seed + 73)

      const t = current * (5.2 + randA * 2.8) + randB * Math.PI * 2 + phase * 2.3
      const driftX = Math.cos(t) * (4.5 + randC * 10.5) + (randD - 0.5) * 4.2
      const driftY = -Math.abs(Math.sin(t * 1.25)) * (4.2 + randE * 12.5) - (1.2 + randA * 2.4)
      const spin = (randC - 0.5) * 220
      const size = 2 + randD * 3.6
      const scale = 0.95 + fade * 0.5
      const light = 70 + randE * 24
      const alpha = (0.42 + randB * 0.52) * fade
      const br1 = 25 + Math.round(randA * 55)
      const br2 = 25 + Math.round(randB * 55)
      const br3 = 25 + Math.round(randC * 55)
      const br4 = 25 + Math.round(randD * 55)

      particles.push({
        id: `${event.id}-p-${i}`,
        style: {
          left: `${centerX - size / 2}px`,
          top: `${centerY - size / 2}px`,
          width: `${size}px`,
          height: `${size}px`,
          opacity: `${alpha}`,
          borderRadius: `${br1}% ${br2}% ${br3}% ${br4}%`,
          background: `radial-gradient(circle at 30% 30%, hsla(${hue + 20}, 100%, 90%, ${alpha}), hsla(${hue}, 100%, ${light}%, ${alpha}))`,
          boxShadow: `0 0 ${8 + randE * 14}px hsla(${hue + 8}, 100%, 72%, ${alpha})`,
          transform: `translate3d(${driftX}px, ${driftY}px, 0) rotate(${spin * phase}deg) scale(${scale})`
        }
      })
    }
  })

  return particles
})

const contactGlows = computed(() => {
  const current = currentSec.value
  return preparedEvents.value
    .map((event) => {
      if (current < event.startSec || current > event.endSec) return null
      const duration = Math.max(0.04, event.endSec - event.startSec)
      const localElapsed = current - event.startSec
      const enterFade = Math.min(1, localElapsed / 0.06)
      const exitFade = Math.min(1, (event.endSec - current) / 0.08)
      const fade = Math.max(0, Math.min(enterFade, exitFade))
      const hue = 172 + (event.midi % 12) * 4
      return {
        id: `${event.id}-contact`,
        style: {
          left: `${event.left}px`,
          top: `${hitLineY - 3}px`,
          width: `${Math.max(3, event.width)}px`,
          opacity: `${0.65 * fade}`,
          background: `linear-gradient(90deg, hsla(${hue + 16}, 100%, 82%, 0), hsla(${hue + 6}, 100%, 76%, ${1 * fade}), hsla(${hue + 16}, 100%, 82%, 0))`,
          boxShadow: `0 0 ${14 + (localElapsed / duration) * 12}px hsla(${hue + 8}, 100%, 70%, ${0.78 * fade})`
        }
      }
    })
    .filter((item): item is { id: string; style: Record<string, string> } => item !== null)
})

const activeMidiSet = computed(() => {
  const current = currentSec.value
  const set = new Set<number>()
  preparedEvents.value.forEach((event) => {
    if (current >= event.startSec && current <= event.endSec) {
      set.add(event.midi)
    }
  })
  return set
})

const syncPanelWidth = () => {
  const nextWidth = Math.max(320, Math.round(keyboardAreaRef.value?.clientWidth || PANEL_WIDTH))
  if (nextWidth !== panelWidth.value) {
    panelWidth.value = nextWidth
  }
}

onMounted(() => {
  syncPanelWidth()
  window.addEventListener('resize', syncPanelWidth)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', syncPanelWidth)
})
</script>

<style scoped>
.midi-overlay {
  @apply fixed inset-0 z-[70] flex items-center justify-center;
  background: radial-gradient(circle at 50% 20%, rgba(37, 99, 235, 0.2), rgba(2, 6, 23, 0.88) 55%);
}

.midi-panel {
  @apply rounded-2xl border border-slate-500/30 overflow-hidden;
  width: 1440px;
  max-width: 98vw;
  background: linear-gradient(180deg, #0f172a 0%, #111827 100%);
  box-shadow: 0 30px 80px rgba(2, 6, 23, 0.55);
}

.midi-header {
  @apply flex items-center justify-between px-5 py-3 border-b border-slate-400/20;
}

.midi-title {
  @apply text-slate-100 font-semibold;
}

.midi-subtitle {
  @apply text-xs text-slate-300/80;
}

.close-btn {
  @apply px-3 py-1 text-sm rounded-md border border-slate-300/40 text-slate-100 hover:bg-slate-700/60 transition;
}

.waterfall-area {
  @apply relative overflow-hidden;
  height: 600px;
  background:
    radial-gradient(circle at 20% 15%, rgba(16, 185, 129, 0.2), transparent 40%),
    radial-gradient(circle at 80% 5%, rgba(59, 130, 246, 0.24), transparent 42%),
    linear-gradient(180deg, rgba(15, 23, 42, 0.9), rgba(3, 7, 18, 0.95));
}

.waterfall-glow {
  @apply absolute inset-0 pointer-events-none;
  background: linear-gradient(180deg, rgba(34, 211, 238, 0.08), transparent 35%, rgba(56, 189, 248, 0.05));
}

.hit-line {
  @apply absolute left-0 right-0;
  bottom: 6px;
  height: 2px;
  background: linear-gradient(90deg, rgba(34, 211, 238, 0.2), rgba(56, 189, 248, 0.95), rgba(34, 211, 238, 0.2));
  box-shadow: 0 0 20px rgba(56, 189, 248, 0.8), 0 0 40px rgba(34, 211, 238, 0.5);
}

.waterfall-note {
  @apply absolute rounded-sm;
  background: linear-gradient(180deg, #7dd3fc, #38bdf8 35%, #22d3ee 70%, #34d399);
  box-shadow:
    0 0 14px rgba(34, 211, 238, 0.7),
    0 0 26px rgba(52, 211, 153, 0.4),
    inset 0 0 10px rgba(186, 230, 253, 0.4);
  animation: notePulse 1s ease-in-out infinite;
  will-change: transform;
}

.waterfall-note::after {
  content: '';
  @apply absolute inset-0 rounded-sm;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.45), transparent 55%);
  mix-blend-mode: screen;
}

.impact-particle {
  @apply absolute pointer-events-none;
  z-index: 6;
  mix-blend-mode: screen;
  will-change: transform, opacity;
}

.contact-glow {
  @apply absolute pointer-events-none;
  z-index: 5;
  height: 5px;
  border-radius: 999px;
  mix-blend-mode: screen;
}

.keyboard-area {
  @apply relative border-t border-slate-400/25;
  height: 140px;
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.75), rgba(2, 6, 23, 0.95));
}

.piano-key {
  @apply absolute bottom-0 border border-slate-700 select-none;
}

.white-key {
  height: 100%;
  background: linear-gradient(180deg, #f8fafc, #e2e8f0);
}

.black-key {
  @apply z-10;
  top: 0;
  bottom: auto;
  height: 64%;
  background: linear-gradient(180deg, #0f172a, #020617);
  border-color: #0b1220;
}

.key-active.white-key {
  background: linear-gradient(180deg, #ecfeff, #a5f3fc);
  box-shadow:
    inset 0 0 12px rgba(6, 182, 212, 0.8),
    0 0 16px rgba(34, 211, 238, 0.55),
    0 0 30px rgba(34, 211, 238, 0.4);
  animation: keyFlash 180ms ease-out;
}

.key-active.black-key {
  background: linear-gradient(180deg, #155e75, #0f172a);
  box-shadow:
    inset 0 0 10px rgba(34, 211, 238, 0.5),
    0 0 12px rgba(34, 211, 238, 0.3),
    0 0 24px rgba(34, 211, 238, 0.25);
  animation: keyFlash 180ms ease-out;
}

.key-label {
  @apply absolute bottom-1 left-1 text-[10px] text-slate-500;
}

@keyframes notePulse {
  0% {
    filter: saturate(1) brightness(1);
  }
  50% {
    filter: saturate(1.2) brightness(1.08);
  }
  100% {
    filter: saturate(1) brightness(1);
  }
}

@keyframes keyFlash {
  0% {
    transform: translateZ(0) scaleY(1);
  }
  50% {
    transform: translateZ(0) scaleY(0.98);
  }
  100% {
    transform: translateZ(0) scaleY(1);
  }
}
</style>
