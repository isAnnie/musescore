export interface Note {
  id: string
  voice?: string
  type: 'whole' | 'half' | 'quarter' | 'eighth' | 'sixteenth'
  pitch: string // 例如: 'C4', 'D5'
  duration: number
  position: { x: number; y: number }
  clef?: 'treble' | 'bass'
  measureIndex?: number
  beatInMeasure?: number
  isRest?: boolean
  dots?: number
  tieToNext?: boolean
  tieFromPrev?: boolean
  pedalStart?: boolean
  pedalEnd?: boolean
  accidental?: 'sharp' | 'flat' | 'natural'
  articulation?: 'staccato' | 'tenuto' | 'accent' | 'tremolo'
  selected?: boolean
  velocity?: number // MIDI力度 0-127
  channel?: number // MIDI通道 0-15
}

export interface Measure {
  id: string
  index: number
  timeSignature: TimeSignature
  notes: Note[]
  startTime: number // 开始时间（拍）
  endTime: number // 结束时间（拍）
}

export interface TimeSignature {
  numerator: number
  denominator: number
}

export interface Score {
  id: string
  title: string
  composer: string
  tempo: number
  timeSignature: TimeSignature
  keySignature: string
  notes: Note[]
  measures: Measure[]
  createdAt: Date
  updatedAt: Date
  isDraft: boolean
  tags?: string[]
  description?: string
  visibility?: 'public' | 'private' | 'unlisted'
}

export interface User {
  id: string
  username: string
  email: string
  avatar?: string
  scores: Score[]
}

// 音频事件接口
export interface AudioEvent {
  time: number
  note: Note
  synth: any // Tone.js 合成器实例
}

// 播放状态
export interface PlaybackState {
  isPlaying: boolean
  currentTime: number
  duration: number
  tempo: number
  metronome: boolean
}

// 编辑器工具
export type EditorTool = 
  | 'select'
  | 'note'
  | 'rest'
  | 'chord'
  | 'eraser'
  | 'text'
  | 'measure'
  | 'clef'

// 快捷键映射
export interface ShortcutMap {
  [key: string]: {
    action: string
    description: string
    handler: () => void
  }
}


