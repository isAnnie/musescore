import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import JSZip from 'jszip'
import type { Score, Note } from '@/types/score'

export const useScoreStore = defineStore('score', () => {
  const STORAGE_KEY = 'musescore:score-store'

  const getDefaultScores = (): Score[] => ([
    {
      id: '1',
      title: '新春序曲',
      composer: '传统音乐',
      tempo: 120,
      timeSignature: { numerator: 4, denominator: 4 },
      keySignature: 'C',
      notes: [],
      measures: [],
      createdAt: new Date('2026-02-17'),
      updatedAt: new Date('2026-02-20'),
      isDraft: false
    }
  ])

  const deserializeScore = (raw: any): Score => ({
    ...raw,
    createdAt: new Date(raw.createdAt),
    updatedAt: new Date(raw.updatedAt)
  })

  const loadPersistedState = (): { scores: Score[]; currentScoreId: string | null } => {
    const fallback = { scores: getDefaultScores(), currentScoreId: null as string | null }

    if (typeof window === 'undefined') {
      return fallback
    }

    const saved = localStorage.getItem(STORAGE_KEY)
    if (!saved) {
      return fallback
    }

    try {
      const parsed = JSON.parse(saved)
      const parsedScores = Array.isArray(parsed.scores) ? parsed.scores.map(deserializeScore) : getDefaultScores()
      const currentScoreId = typeof parsed.currentScoreId === 'string' ? parsed.currentScoreId : null

      return { scores: parsedScores, currentScoreId }
    } catch (error) {
      console.error('读取乐谱持久化数据失败:', error)
      return fallback
    }
  }

  const persistState = () => {
    if (typeof window === 'undefined') return

    const payload = {
      scores: scores.value,
      currentScoreId: currentScore.value?.id ?? null
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(payload))
  }

  const persisted = loadPersistedState()
  const currentScore = ref<Score | null>(null)
  const scores = ref<Score[]>(persisted.scores)

  // 历史记录
  const history = ref<Score[]>([])
  const historyIndex = ref(-1)

  if (persisted.currentScoreId) {
    currentScore.value = scores.value.find((item) => item.id === persisted.currentScoreId) ?? null
  }

  const recentScores = computed(() => {
    return [...scores.value]
      .sort((a, b) => b.updatedAt.getTime() - a.updatedAt.getTime())
      .slice(0, 5)
  })

  const durationToType = (duration: number): Note['type'] => {
    if (duration >= 3.5) return 'whole'
    if (duration >= 1.75) return 'half'
    if (duration >= 0.75) return 'quarter'
    if (duration >= 0.375) return 'eighth'
    return 'sixteenth'
  }

  const pitchToStepFromMiddleC = (pitch: string): number => {
    const match = pitch.match(/^([A-Ga-g])([#b]?)(\d)$/)
    if (!match) return 0
    const [, stepRaw, , octaveRaw] = match
    const step = stepRaw.toUpperCase()
    const octave = Number(octaveRaw)
    const stepIndex: Record<string, number> = { C: 0, D: 1, E: 2, F: 3, G: 4, A: 5, B: 6 }
    return (octave - 4) * 7 + (stepIndex[step] ?? 0)
  }

  const getBeatsPerMeasure = (numerator: number, denominator: number) => {
    if (!denominator) return 4
    return (numerator * 4) / denominator
  }

  const toEditorPosition = (
    measureIndex: number,
    beatInMeasure: number,
    beatsPerMeasure: number,
    clef: 'treble' | 'bass'
  ) => {
    const measuresPerRow = 4
    const staffPaddingLeft = 40
    const staffTop = 40
    const rowHeight = 260
    const grandStaffGap = 88
    const pitchStepY = 5
    const staveWidth = 240
    const noteStartOffset = 20
    const noteAreaWidth = 200

    const rowIndex = Math.floor(measureIndex / measuresPerRow)
    const colIndex = measureIndex % measuresPerRow
    const baseX = staffPaddingLeft + colIndex * staveWidth
    const x = baseX + noteStartOffset + (beatInMeasure / Math.max(1, beatsPerMeasure)) * noteAreaWidth
    const yBase = staffTop + rowIndex * rowHeight + (clef === 'bass' ? grandStaffGap : 0)
    const middleCForClef = clef === 'bass' ? 78 : 50

    return { x, yBase, middleCForClef, pitchStepY }
  }

  const parseKeySignature = (fifths: number, mode: string | null) => {
    const majorMap: Record<number, string> = {
      '-7': 'Cb',
      '-6': 'Gb',
      '-5': 'Db',
      '-4': 'Ab',
      '-3': 'Eb',
      '-2': 'Bb',
      '-1': 'F',
      0: 'C',
      1: 'G',
      2: 'D',
      3: 'A',
      4: 'E',
      5: 'B',
      6: 'F#',
      7: 'C#'
    }
    const minorMap: Record<number, string> = {
      '-7': 'Abm',
      '-6': 'Ebm',
      '-5': 'Bbm',
      '-4': 'Fm',
      '-3': 'Cm',
      '-2': 'Gm',
      '-1': 'Dm',
      0: 'Am',
      1: 'Em',
      2: 'Bm',
      3: 'F#m',
      4: 'C#m',
      5: 'G#m',
      6: 'D#m',
      7: 'A#m'
    }
    if (mode?.toLowerCase() === 'minor') {
      return minorMap[fifths] ?? 'Am'
    }
    return majorMap[fifths] ?? 'C'
  }

  const createImportedScore = (base: {
    title: string
    tempo: number
    timeSignature: { numerator: number; denominator: number }
    keySignature: string
    notes: Note[]
    composer?: string
  }): Score => {
    const now = new Date()
    return {
      id: Date.now().toString(),
      title: base.title,
      composer: base.composer?.trim() || '未知',
      tempo: base.tempo,
      timeSignature: base.timeSignature,
      keySignature: base.keySignature,
      notes: base.notes,
      measures: [],
      createdAt: now,
      updatedAt: now,
      isDraft: true
    }
  }

  const parseMusicXMLContent = (xmlText: string, fallbackTitle: string): Score => {
    const parser = new DOMParser()
    const doc = parser.parseFromString(xmlText, 'application/xml')
    const parserError = doc.querySelector('parsererror')
    if (parserError) {
      throw new Error('MusicXML文件格式不正确')
    }

    const scoreRoot = doc.querySelector('score-partwise')
    if (!scoreRoot) {
      throw new Error('仅支持score-partwise格式的MusicXML')
    }

    const firstPart = scoreRoot.querySelector('part')
    if (!firstPart) {
      throw new Error('MusicXML中未找到part数据')
    }

    const title = doc.querySelector('work > work-title')?.textContent?.trim()
      || doc.querySelector('movement-title')?.textContent?.trim()
      || fallbackTitle
    const composer = doc.querySelector('identification > creator[type="composer"]')?.textContent?.trim() || '未知'

    const notes: Note[] = []
    type PedalMarker = {
      kind: 'start' | 'end'
      measureIndex: number
      beatInMeasure: number
      clef?: 'treble' | 'bass'
      order: number
    }
    const pedalMarkers: PedalMarker[] = []
    const measures = Array.from(firstPart.querySelectorAll(':scope > measure'))

    let divisions = 1
    let tempo = 120
    let tsNumerator = 4
    let tsDenominator = 4
    let keySignature = 'C'
    const measureCursors = new Map<string, number>()
    const lastStarts = new Map<string, number>()

    measures.forEach((measure, measureIndex) => {
      const attributes = measure.querySelector(':scope > attributes')
      if (attributes) {
        const divisionsText = attributes.querySelector('divisions')?.textContent
        const parsedDivisions = Number(divisionsText)
        if (!Number.isNaN(parsedDivisions) && parsedDivisions > 0) {
          divisions = parsedDivisions
        }

        const beatsText = attributes.querySelector('time > beats')?.textContent
        const beatTypeText = attributes.querySelector('time > beat-type')?.textContent
        const nextNumerator = Number(beatsText)
        const nextDenominator = Number(beatTypeText)
        if (!Number.isNaN(nextNumerator) && nextNumerator > 0) {
          tsNumerator = nextNumerator
        }
        if (!Number.isNaN(nextDenominator) && nextDenominator > 0) {
          tsDenominator = nextDenominator
        }

        const fifthsText = attributes.querySelector('key > fifths')?.textContent
        const fifths = Number(fifthsText)
        if (!Number.isNaN(fifths)) {
          keySignature = parseKeySignature(fifths, attributes.querySelector('key > mode')?.textContent ?? null)
        }
      }

      const beatsPerMeasure = getBeatsPerMeasure(tsNumerator, tsDenominator)
      measureCursors.clear()
      lastStarts.clear()
      let sequenceCursorDiv = 0

      Array.from(measure.children).forEach((child, childIndex) => {
        const tag = child.tagName
        if (tag === 'backup' || tag === 'forward') {
          const durationDiv = Number(child.querySelector('duration')?.textContent ?? '0')
          if (Number.isNaN(durationDiv) || durationDiv <= 0) return
          const delta = tag === 'backup' ? -durationDiv : durationDiv
          const updated = (measureCursors.get('global') ?? 0) + delta
          measureCursors.set('global', Math.max(0, updated))
          sequenceCursorDiv = Math.max(0, sequenceCursorDiv + delta)
          return
        }

        if (tag === 'direction') {
          const soundTempo = child.querySelector(':scope > sound[tempo]')?.getAttribute('tempo')
          const perMinute = child.querySelector(':scope > direction-type metronome per-minute')?.textContent
          const parsedSoundTempo = Number(soundTempo)
          const parsedPerMinute = Number(perMinute)
          if (!Number.isNaN(parsedSoundTempo) && parsedSoundTempo > 0) {
            tempo = parsedSoundTempo
          } else if (!Number.isNaN(parsedPerMinute) && parsedPerMinute > 0) {
            tempo = parsedPerMinute
          }

          const pedalElement = child.querySelector(':scope > direction-type > pedal')
          if (pedalElement) {
            const pedalType = pedalElement.getAttribute('type')?.toLowerCase() ?? 'start'
            const offsetDiv = Number(child.querySelector(':scope > offset')?.textContent ?? '0')
            const pedalDiv = Math.max(
              0,
              sequenceCursorDiv + (Number.isNaN(offsetDiv) ? 0 : offsetDiv)
            )
            const pedalBeat = Math.max(
              0,
              Math.min(
                pedalDiv / Math.max(1, divisions),
                Math.max(0, beatsPerMeasure - 0.25)
              )
            )
            const staff = child.querySelector(':scope > staff')?.textContent?.trim()
            const clef = staff === '2' ? 'bass' : staff === '1' ? 'treble' : undefined
            const order = measureIndex * 100000 + pedalDiv * 10 + childIndex

            if (pedalType === 'stop' || pedalType === 'up') {
              pedalMarkers.push({
                kind: 'end',
                measureIndex,
                beatInMeasure: pedalBeat,
                clef,
                order
              })
            } else if (pedalType === 'change') {
              pedalMarkers.push({
                kind: 'end',
                measureIndex,
                beatInMeasure: pedalBeat,
                clef,
                order
              })
              pedalMarkers.push({
                kind: 'start',
                measureIndex,
                beatInMeasure: pedalBeat,
                clef,
                order: order + 1
              })
            } else if (pedalType !== 'continue') {
              pedalMarkers.push({
                kind: 'start',
                measureIndex,
                beatInMeasure: pedalBeat,
                clef,
                order
              })
            }
          }
          return
        }

        if (tag !== 'note') return
        const isRest = !!child.querySelector('rest')
        const durationDiv = Number(child.querySelector('duration')?.textContent ?? '0')
        const voice = child.querySelector('voice')?.textContent?.trim() || '1'
        const staffText = child.querySelector('staff')?.textContent?.trim()
        const staffNum = staffText === '2' ? '2' : '1'
        const trackKey = `${staffNum}:${voice}`
        const chord = !!child.querySelector('chord')

        if (!measureCursors.has(trackKey)) {
          measureCursors.set(trackKey, measureCursors.get('global') ?? 0)
        }

        const cursorDiv = measureCursors.get(trackKey) ?? 0
        const startDiv = chord ? (lastStarts.get(trackKey) ?? cursorDiv) : cursorDiv
        const startBeat = startDiv / Math.max(1, divisions)
        const durationBeatsRaw = durationDiv > 0 ? durationDiv / Math.max(1, divisions) : 0
        const durationBeats = durationBeatsRaw > 0 ? durationBeatsRaw : 1

        if (!isRest) {
          const step = child.querySelector('pitch > step')?.textContent?.trim()?.toUpperCase()
          const octave = child.querySelector('pitch > octave')?.textContent?.trim()
          if (step && octave) {
            const alter = Number(child.querySelector('pitch > alter')?.textContent ?? '0')
            const accidentalFromAlter: Note['accidental'] =
              alter > 0 ? 'sharp' : alter < 0 ? 'flat' : undefined
            const accidentalTag = child.querySelector('accidental')?.textContent?.trim()
            const accidental: Note['accidental'] =
              accidentalTag === 'sharp' ? 'sharp'
                : accidentalTag === 'flat' ? 'flat'
                  : accidentalTag === 'natural' ? 'natural'
                    : accidentalFromAlter
            const pitch = `${step}${alter > 0 ? '#' : alter < 0 ? 'b' : ''}${octave}`
            const clef: 'treble' | 'bass' = staffNum === '2' ? 'bass' : 'treble'
            const clampedBeat = Math.max(0, Math.min(startBeat, Math.max(0, beatsPerMeasure - 0.25)))
            const pos = toEditorPosition(measureIndex, clampedBeat, beatsPerMeasure, clef)
            const stepFromMiddleC = pitchToStepFromMiddleC(pitch)

            notes.push({
              id: `${Date.now()}-${measureIndex}-${startDiv}-${voice}-${notes.length}`,
              type: durationToType(durationBeats),
              pitch,
              duration: durationBeats,
              position: {
                x: pos.x,
                y: pos.yBase + pos.middleCForClef - stepFromMiddleC * pos.pitchStepY
              },
              clef,
              measureIndex,
              beatInMeasure: clampedBeat,
              dots: child.querySelectorAll('dot').length || undefined,
              tieToNext: !!child.querySelector('tie[type="start"]'),
              tieFromPrev: !!child.querySelector('tie[type="stop"]'),
              accidental
            })
          }
        }

        lastStarts.set(trackKey, startDiv)
        if (!chord) {
          measureCursors.set(trackKey, startDiv + Math.max(0, durationDiv))
          const nextGlobal = Math.max(measureCursors.get('global') ?? 0, measureCursors.get(trackKey) ?? 0)
          measureCursors.set('global', nextGlobal)
          sequenceCursorDiv += Math.max(0, durationDiv)
        }
      })
    })

    if (!notes.length) {
      throw new Error('未在MusicXML中解析到可用音符')
    }

    const pitchOrder = (pitch: string) => {
      const match = pitch.match(/^([A-Ga-g])([#b]?)(\d)$/)
      if (!match) return 0
      const [, stepRaw, , octaveRaw] = match
      const step = stepRaw.toUpperCase()
      const octave = Number(octaveRaw)
      const stepIndex: Record<string, number> = { C: 0, D: 1, E: 2, F: 3, G: 4, A: 5, B: 6 }
      return octave * 7 + (stepIndex[step] ?? 0)
    }

    const attachPedalMarker = (marker: PedalMarker) => {
      const candidates = notes
        .filter((note) => !note.isRest)
        .filter((note) => note.measureIndex === marker.measureIndex)
        .filter((note) => (marker.clef ? (note.clef ?? 'treble') === marker.clef : true))
        .filter((note) => typeof note.beatInMeasure === 'number')

      const pool = candidates.length
        ? candidates
        : notes
          .filter((note) => !note.isRest)
          .filter((note) => typeof note.measureIndex === 'number' && typeof note.beatInMeasure === 'number')

      if (!pool.length) return

      const target = [...pool].sort((a, b) => {
        const aMeasure = a.measureIndex ?? 0
        const bMeasure = b.measureIndex ?? 0
        const aBeat = a.beatInMeasure ?? 0
        const bBeat = b.beatInMeasure ?? 0
        const aDeltaMeasure = aMeasure - marker.measureIndex
        const bDeltaMeasure = bMeasure - marker.measureIndex
        const aFutureScore = aDeltaMeasure >= 0 ? 0 : 1
        const bFutureScore = bDeltaMeasure >= 0 ? 0 : 1
        if (aFutureScore !== bFutureScore) return aFutureScore - bFutureScore
        const aDistance = Math.abs(aDeltaMeasure) * 1000 + Math.abs(aBeat - marker.beatInMeasure)
        const bDistance = Math.abs(bDeltaMeasure) * 1000 + Math.abs(bBeat - marker.beatInMeasure)
        if (Math.abs(aDistance - bDistance) > 0.0001) return aDistance - bDistance
        return pitchOrder(a.pitch) - pitchOrder(b.pitch)
      })[0]

      if (!target) return
      if (marker.kind === 'start') {
        target.pedalStart = true
      } else {
        target.pedalEnd = true
      }
    }

    pedalMarkers
      .sort((a, b) => a.order - b.order)
      .forEach(attachPedalMarker)

    return createImportedScore({
      title,
      composer,
      tempo: Math.round(tempo),
      timeSignature: { numerator: tsNumerator, denominator: tsDenominator },
      keySignature,
      notes
    })
  }

  // 添加音符
  const addNote = (note: Note) => {
    if (currentScore.value) {
      currentScore.value.notes.push(note)
      currentScore.value.updatedAt = new Date()
      saveToHistory()
    }
  }

  // 更新音符
  const updateNote = (noteId: string, updates: Partial<Note>) => {
    if (currentScore.value) {
      const noteIndex = currentScore.value.notes.findIndex(n => n.id === noteId)
      if (noteIndex !== -1) {
        currentScore.value.notes[noteIndex] = {
          ...currentScore.value.notes[noteIndex],
          ...updates
        }
        currentScore.value.updatedAt = new Date()
        saveToHistory()
      }
    }
  }

  // 删除音符
  const removeNote = (noteId: string) => {
    if (currentScore.value) {
      currentScore.value.notes = currentScore.value.notes.filter(
        note => note.id !== noteId
      )
      currentScore.value.updatedAt = new Date()
      saveToHistory()
    }
  }

  // 创建新乐谱
  const createScore = (score: Score) => {
    scores.value.push(score)
    currentScore.value = score
    saveToHistory()
    return score
  }

  // 保存到历史记录
  const saveToHistory = () => {
    if (currentScore.value) {
      // 移除当前索引之后的历史
      history.value = history.value.slice(0, historyIndex.value + 1)
      
      // 添加当前状态到历史
      history.value.push(JSON.parse(JSON.stringify(currentScore.value)))
      historyIndex.value = history.value.length - 1
      
      // 限制历史记录长度
      if (history.value.length > 50) {
        history.value.shift()
        historyIndex.value = history.value.length - 1
      }
    }
  }

  // 撤销
  const undo = () => {
    if (historyIndex.value > 0) {
      historyIndex.value--
      const restored = JSON.parse(JSON.stringify(history.value[historyIndex.value])) as Score
      restored.createdAt = new Date(restored.createdAt)
      restored.updatedAt = new Date(restored.updatedAt)

      currentScore.value = restored

      const scoreIndex = scores.value.findIndex((score) => score.id === restored.id)
      if (scoreIndex !== -1) {
        scores.value[scoreIndex] = restored
      }
    }
  }

  // 重做
  const redo = () => {
    if (historyIndex.value < history.value.length - 1) {
      historyIndex.value++
      const restored = JSON.parse(JSON.stringify(history.value[historyIndex.value])) as Score
      restored.createdAt = new Date(restored.createdAt)
      restored.updatedAt = new Date(restored.updatedAt)

      currentScore.value = restored

      const scoreIndex = scores.value.findIndex((score) => score.id === restored.id)
      if (scoreIndex !== -1) {
        scores.value[scoreIndex] = restored
      }
    }
  }

  // 导入MIDI
  const importMIDI = async (file: File): Promise<Score> => {
    return new Promise((resolve) => {
      // 这里可以集成MIDI.js或其他MIDI解析库
      console.log('导入MIDI文件:', file)
      
      const newScore = createImportedScore({
        title: file.name.replace('.mid', ''),
        tempo: 120,
        timeSignature: { numerator: 4, denominator: 4 },
        keySignature: 'C',
        notes: []
      })
      
      scores.value.push(newScore)
      currentScore.value = newScore
      saveToHistory()
      
      resolve(newScore)
    })
  }

  const importMusicXML = async (file: File): Promise<Score> => {
    const xmlText = await file.text()
    const score = parseMusicXMLContent(
      xmlText,
      file.name.replace(/\.(musicxml|xml)$/i, '')
    )

    scores.value.push(score)
    currentScore.value = score
    saveToHistory()
    return score
  }

  const importMXL = async (file: File): Promise<Score> => {
    const zip = await JSZip.loadAsync(await file.arrayBuffer())

    let rootPath: string | null = null
    const containerFile = zip.file('META-INF/container.xml')
    if (containerFile) {
      const containerText = await containerFile.async('string')
      const parser = new DOMParser()
      const containerDoc = parser.parseFromString(containerText, 'application/xml')
      rootPath = containerDoc.querySelector('rootfile')?.getAttribute('full-path') ?? null
    }

    let xmlFile = rootPath ? zip.file(rootPath) : null
    if (!xmlFile) {
      const candidate = Object.values(zip.files).find((entry) => {
        if (entry.dir) return false
        const name = entry.name.toLowerCase()
        return (name.endsWith('.musicxml') || name.endsWith('.xml')) && !name.startsWith('meta-inf/')
      })
      xmlFile = candidate ?? null
    }

    if (!xmlFile) {
      throw new Error('MXL中未找到可解析的MusicXML内容')
    }

    const xmlText = await xmlFile.async('string')
    const score = parseMusicXMLContent(
      xmlText,
      file.name.replace(/\.mxl$/i, '')
    )

    scores.value.push(score)
    currentScore.value = score
    saveToHistory()
    return score
  }

  const importScoreFile = async (file: File): Promise<Score> => {
    const lowerName = file.name.toLowerCase()
    if (lowerName.endsWith('.musicxml') || lowerName.endsWith('.xml')) {
      return importMusicXML(file)
    }
    if (lowerName.endsWith('.mxl')) {
      return importMXL(file)
    }
    if (lowerName.endsWith('.mid') || lowerName.endsWith('.midi')) {
      return importMIDI(file)
    }
    throw new Error('暂不支持该文件格式，请使用 MusicXML / MXL / MIDI')
  }

  // 导出为MIDI
  const exportToMIDI = (score: Score): Blob => {
    // 生成MIDI二进制数据
    console.log('导出为MIDI:', score)
    return new Blob(['MIDI数据占位'], { type: 'audio/midi' })
  }

  // 导出为MusicXML
  const exportToMusicXML = (score: Score): string => {
    // 生成MusicXML字符串
    const xml = `<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE score-partwise PUBLIC "-//Recordare//DTD MusicXML 3.0 Partwise//EN" "http://www.musicxml.org/dtds/partwise.dtd">
<score-partwise version="3.0">
  <part-list>
    <score-part id="P1">
      <part-name>乐谱</part-name>
    </score-part>
  </part-list>
  <part id="P1">
    <!-- 这里添加具体的音符数据 -->
  </part>
</score-partwise>`
    
    return xml
  }

  watch(
    scores,
    () => {
      persistState()
    },
    { deep: true }
  )

  watch(
    () => currentScore.value?.id,
    () => {
      persistState()
    }
  )

  return {
    currentScore,
    scores,
    recentScores,
    addNote,
    updateNote,
    removeNote,
    createScore,
    undo,
    redo,
    importScoreFile,
    importMIDI,
    importMusicXML,
    importMXL,
    exportToMIDI,
    exportToMusicXML
  }
})
